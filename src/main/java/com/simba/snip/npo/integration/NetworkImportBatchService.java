package com.simba.snip.npo.integration;

import com.simba.snip.npo.persist.NetworkImportAuditEventEntity;
import com.simba.snip.npo.persist.NetworkImportAuditEventRepository;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NetworkImportBatchService {

    private final NetworkImportBatchRepository batchRepository;
    private final NetworkImportAuditEventRepository auditRepository;

    public NetworkImportBatchService(
            NetworkImportBatchRepository batchRepository,
            NetworkImportAuditEventRepository auditRepository
    ) {
        this.batchRepository = batchRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NetworkImportBatchEntity start(
            String sourceSystem, String vendor, String schemaVersion, FixtureKind kind, Instant startedAt
    ) {
        NetworkImportBatchEntity batch = NetworkImportBatchEntity.start(
                UUID.randomUUID(), sourceSystem, vendor, schemaVersion, kind.name(), startedAt);
        return batchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSnapshot(UUID importId, String sourceSnapshotId, String schemaVersion) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId).orElseThrow();
        batch.recordSnapshot(sourceSnapshotId, schemaVersion);
        batchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID importId, Instant completedAt, ReconciliationResult result) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId).orElseThrow();
        batch.complete(
                completedAt,
                result.entitiesRead(),
                result.created(),
                result.updated(),
                result.unchanged(),
                result.rejected(),
                result.conflicts(),
                result.missing()
        );
        batchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID importId, Instant completedAt, String error) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId).orElseThrow();
        batch.fail(completedAt, error);
        batchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendAudit(UUID importId, ImportAuditEventType type, Instant occurredAt, String details) {
        auditRepository.saveAndFlush(NetworkImportAuditEventEntity.create(
                UUID.randomUUID(), importId, type.name(), occurredAt, details));
    }

    @Transactional(readOnly = true)
    public NetworkImportBatchEntity require(UUID importId) {
        return batchRepository.findById(importId).orElseThrow();
    }
}
