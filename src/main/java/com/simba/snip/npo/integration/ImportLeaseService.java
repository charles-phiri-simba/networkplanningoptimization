package com.simba.snip.npo.integration;

import com.simba.snip.npo.config.IntegrationRuntimeProperties;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImportLeaseService {

    private static final RowMapper<ImportLease> LEASE_MAPPER = (rs, rowNum) -> new ImportLease(
            rs.getString("lease_key"),
            rs.getString("source_system"),
            rs.getString("source_scope"),
            rs.getObject("owner_execution_id", UUID.class),
            rs.getString("owner_instance_id"),
            rs.getLong("fencing_token"),
            rs.getTimestamp("acquired_at").toInstant(),
            rs.getTimestamp("heartbeat_at").toInstant(),
            rs.getTimestamp("expires_at").toInstant()
    );

    private final JdbcTemplate jdbc;
    private final IntegrationRuntimeProperties properties;
    private final NetworkImportBatchRepository batchRepository;
    private final NetworkImportBatchService batchService;
    private final IntegrationMetrics metrics;

    public ImportLeaseService(
            JdbcTemplate jdbc,
            IntegrationRuntimeProperties properties,
            NetworkImportBatchRepository batchRepository,
            NetworkImportBatchService batchService,
            IntegrationMetrics metrics
    ) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.batchRepository = batchRepository;
        this.batchService = batchService;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ImportLease> acquire(
            String sourceSystem, String sourceScope, UUID executionId, String ownerInstanceId
    ) {
        recoverExpired(sourceSystem, sourceScope);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getLeaseDuration());
        String leaseKey = ImportLease.key(sourceSystem, sourceScope);
        List<ImportLease> acquired = jdbc.query(
                """
                INSERT INTO network_import_lease (
                    lease_key, source_system, source_scope, owner_execution_id, owner_instance_id,
                    fencing_token, acquired_at, heartbeat_at, expires_at
                ) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?)
                ON CONFLICT (lease_key) DO UPDATE SET
                    owner_execution_id = EXCLUDED.owner_execution_id,
                    owner_instance_id = EXCLUDED.owner_instance_id,
                    fencing_token = network_import_lease.fencing_token + 1,
                    acquired_at = EXCLUDED.acquired_at,
                    heartbeat_at = EXCLUDED.heartbeat_at,
                    expires_at = EXCLUDED.expires_at
                WHERE network_import_lease.expires_at < EXCLUDED.acquired_at
                RETURNING lease_key, source_system, source_scope, owner_execution_id, owner_instance_id,
                    fencing_token, acquired_at, heartbeat_at, expires_at
                """,
                LEASE_MAPPER,
                leaseKey,
                sourceSystem,
                sourceScope,
                executionId,
                ownerInstanceId,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(expiresAt)
        );
        if (acquired.isEmpty()) {
            metrics.incrementLeaseRejected();
            return Optional.empty();
        }
        metrics.incrementLeaseAcquired();
        return Optional.of(acquired.get(0));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void heartbeat(ImportLease lease) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getLeaseDuration());
        int updated = jdbc.update(
                """
                UPDATE network_import_lease
                SET heartbeat_at = ?, expires_at = ?
                WHERE lease_key = ? AND owner_execution_id = ? AND fencing_token = ? AND expires_at >= ?
                """,
                Timestamp.from(now),
                Timestamp.from(expiresAt),
                lease.leaseKey(),
                lease.ownerExecutionId(),
                lease.fencingToken(),
                Timestamp.from(now)
        );
        if (updated != 1) {
            throw new ImportRuntimeException(ImportFailureCode.LEASE_LOST, "lease heartbeat failed");
        }
    }

    @Transactional
    public void assertOwnership(ImportLease lease) {
        Instant now = Instant.now();
        List<ImportLease> current = jdbc.query(
                """
                SELECT lease_key, source_system, source_scope, owner_execution_id, owner_instance_id,
                    fencing_token, acquired_at, heartbeat_at, expires_at
                FROM network_import_lease
                WHERE lease_key = ? AND owner_execution_id = ? AND fencing_token = ? AND expires_at >= ?
                FOR UPDATE
                """,
                LEASE_MAPPER,
                lease.leaseKey(),
                lease.ownerExecutionId(),
                lease.fencingToken(),
                Timestamp.from(now)
        );
        if (current.isEmpty()) {
            throw new ImportRuntimeException(ImportFailureCode.LEASE_LOST, "lease ownership is no longer valid");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(ImportLease lease) {
        jdbc.update(
                """
                DELETE FROM network_import_lease
                WHERE lease_key = ? AND owner_execution_id = ? AND fencing_token = ?
                """,
                lease.leaseKey(),
                lease.ownerExecutionId(),
                lease.fencingToken()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recoverExpired() {
        return recoverExpired(null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recoverExpired(String sourceSystem, String sourceScope) {
        List<NetworkImportBatchEntity> running = sourceSystem == null
                ? batchRepository.findByStatus("RUNNING")
                : batchRepository.findBySourceSystemAndSourceScopeAndStatus(sourceSystem, sourceScope, "RUNNING");
        int recovered = 0;
        Instant now = Instant.now();
        for (NetworkImportBatchEntity execution : running) {
            if (ownsValidLease(execution, now)) {
                continue;
            }
            if (batchService.terminalize(
                    execution.getId(),
                    "FAILED",
                    now,
                    ImportFailureCode.LEASE_EXPIRED,
                    true,
                    "lease expired or missing for running execution"
            )) {
                metrics.incrementLeaseExpired();
                recovered++;
            }
        }
        return recovered;
    }

    public Optional<ImportLease> find(String sourceSystem, String sourceScope) {
        List<ImportLease> rows = jdbc.query(
                """
                SELECT lease_key, source_system, source_scope, owner_execution_id, owner_instance_id,
                    fencing_token, acquired_at, heartbeat_at, expires_at
                FROM network_import_lease
                WHERE source_system = ? AND source_scope = ?
                """,
                LEASE_MAPPER,
                sourceSystem,
                sourceScope
        );
        return rows.stream().findFirst();
    }

    public long expiredLeaseCount() {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_import_lease WHERE expires_at < ?",
                Long.class,
                Timestamp.from(Instant.now())
        );
        return count == null ? 0 : count;
    }

    private boolean ownsValidLease(NetworkImportBatchEntity execution, Instant now) {
        if (execution.getLeaseFencingToken() == null) {
            return false;
        }
        List<ImportLease> rows = jdbc.query(
                """
                SELECT lease_key, source_system, source_scope, owner_execution_id, owner_instance_id,
                    fencing_token, acquired_at, heartbeat_at, expires_at
                FROM network_import_lease
                WHERE owner_execution_id = ? AND fencing_token = ? AND expires_at >= ?
                """,
                LEASE_MAPPER,
                execution.getId(),
                execution.getLeaseFencingToken(),
                Timestamp.from(now)
        );
        return !rows.isEmpty();
    }
}
