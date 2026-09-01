package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.metrics.ExecutionMetrics;
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
public class ExecutionLeaseService {

    public record ExecutionLease(
            String leaseKey,
            String targetId,
            String cellId,
            String parameterName,
            UUID ownerExecutionId,
            String ownerInstanceId,
            long fencingToken,
            Instant acquiredAt,
            Instant heartbeatAt,
            Instant expiresAt
    ) {
        public static String key(String targetId, String cellId, String parameterName) {
            return "change-execution:" + targetId + ":" + cellId + ":" + parameterName;
        }
    }

    private static final RowMapper<ExecutionLease> LEASE_MAPPER = (rs, rowNum) -> new ExecutionLease(
            rs.getString("lease_key"),
            rs.getString("target_id"),
            rs.getString("cell_id"),
            rs.getString("parameter_name"),
            rs.getObject("owner_execution_id", UUID.class),
            rs.getString("owner_instance_id"),
            rs.getLong("fencing_token"),
            rs.getTimestamp("acquired_at").toInstant(),
            rs.getTimestamp("heartbeat_at").toInstant(),
            rs.getTimestamp("expires_at").toInstant()
    );

    private final JdbcTemplate jdbc;
    private final ChangeExecutionProperties properties;
    private final ExecutionMetrics metrics;

    public ExecutionLeaseService(
            JdbcTemplate jdbc,
            ChangeExecutionProperties properties,
            ExecutionMetrics metrics
    ) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ExecutionLease> acquire(String targetId, String cellId, String parameterName, UUID executionId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getLeaseDuration());
        String leaseKey = ExecutionLease.key(targetId, cellId, parameterName);
        List<ExecutionLease> acquired = jdbc.query(
                """
                INSERT INTO network_change_execution_lease (
                    lease_key, target_id, cell_id, parameter_name, owner_execution_id, owner_instance_id,
                    fencing_token, acquired_at, heartbeat_at, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                ON CONFLICT (lease_key) DO UPDATE SET
                    owner_execution_id = EXCLUDED.owner_execution_id,
                    owner_instance_id = EXCLUDED.owner_instance_id,
                    fencing_token = network_change_execution_lease.fencing_token + 1,
                    acquired_at = EXCLUDED.acquired_at,
                    heartbeat_at = EXCLUDED.heartbeat_at,
                    expires_at = EXCLUDED.expires_at
                WHERE network_change_execution_lease.expires_at < EXCLUDED.acquired_at
                RETURNING lease_key, target_id, cell_id, parameter_name, owner_execution_id, owner_instance_id,
                    fencing_token, acquired_at, heartbeat_at, expires_at
                """,
                LEASE_MAPPER,
                leaseKey,
                targetId,
                cellId,
                parameterName,
                executionId,
                properties.getInstanceId(),
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
    public void heartbeat(ExecutionLease lease) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getLeaseDuration());
        int updated = jdbc.update(
                """
                UPDATE network_change_execution_lease
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
            throw new ChangeExecutionException(
                    ExecutionFailureCode.EXECUTION_FENCING_TOKEN_STALE,
                    "execution lease heartbeat failed"
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assertOwnership(ExecutionLease lease) {
        Instant now = Instant.now();
        List<ExecutionLease> current = jdbc.query(
                """
                SELECT lease_key, target_id, cell_id, parameter_name, owner_execution_id, owner_instance_id,
                    fencing_token, acquired_at, heartbeat_at, expires_at
                FROM network_change_execution_lease
                WHERE lease_key = ? AND owner_execution_id = ? AND fencing_token = ? AND expires_at >= ?
                """,
                LEASE_MAPPER,
                lease.leaseKey(),
                lease.ownerExecutionId(),
                lease.fencingToken(),
                Timestamp.from(now)
        );
        if (current.isEmpty()) {
            throw new ChangeExecutionException(
                    ExecutionFailureCode.EXECUTION_FENCING_TOKEN_STALE,
                    "execution lease ownership is no longer valid"
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(ExecutionLease lease) {
        jdbc.update(
                """
                DELETE FROM network_change_execution_lease
                WHERE lease_key = ? AND owner_execution_id = ? AND fencing_token = ?
                """,
                lease.leaseKey(),
                lease.ownerExecutionId(),
                lease.fencingToken()
        );
    }
}
