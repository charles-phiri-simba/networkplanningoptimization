package com.simba.snip.npo.integration;

import com.simba.snip.npo.persist.NetworkImportAuditEventEntity;
import com.simba.snip.npo.persist.NetworkImportAuditEventRepository;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import com.simba.snip.npo.persist.NetworkImportCheckpointEntity;
import com.simba.snip.npo.persist.NetworkImportCheckpointRepository;
import com.simba.snip.npo.persist.SourceProvenanceEntity;
import com.simba.snip.npo.persist.SourceProvenanceRepository;
import com.simba.snip.npo.persist.VendorSnapshotEntity;
import com.simba.snip.npo.persist.VendorSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NetworkImportBatchService {

    private final NetworkImportBatchRepository batchRepository;
    private final NetworkImportAuditEventRepository auditRepository;
    private final NetworkImportCheckpointRepository checkpointRepository;
    private final VendorSnapshotRepository vendorSnapshotRepository;
    private final SourceProvenanceRepository sourceProvenanceRepository;

    public NetworkImportBatchService(
            NetworkImportBatchRepository batchRepository,
            NetworkImportAuditEventRepository auditRepository,
            NetworkImportCheckpointRepository checkpointRepository,
            VendorSnapshotRepository vendorSnapshotRepository,
            SourceProvenanceRepository sourceProvenanceRepository
    ) {
        this.batchRepository = batchRepository;
        this.auditRepository = auditRepository;
        this.checkpointRepository = checkpointRepository;
        this.vendorSnapshotRepository = vendorSnapshotRepository;
        this.sourceProvenanceRepository = sourceProvenanceRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NetworkImportBatchEntity requested(
            UUID id,
            String sourceSystem,
            String vendor,
            String schemaVersion,
            FixtureKind kind,
            String sourceScope,
            ImportExecutionType executionType,
            int attemptNumber,
            UUID previousExecutionId,
            Instant requestedAt,
            String ownerInstanceId
    ) {
        NetworkImportBatchEntity batch = NetworkImportBatchEntity.requested(
                id,
                sourceSystem,
                vendor,
                schemaVersion,
                kind.name(),
                sourceScope,
                executionType.name(),
                attemptNumber,
                previousExecutionId,
                requestedAt,
                ownerInstanceId
        );
        return batchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSnapshot(UUID importId, String sourceSnapshotId, String schemaVersion, String canonicalSnapshotHash) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId).orElseThrow();
        batch.recordSnapshot(sourceSnapshotId, schemaVersion, canonicalSnapshotHash);
        batchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSynchronization(UUID importId, String mode, String initiator) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId).orElseThrow();
        batch.recordSynchronization(mode, initiator);
        batchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NetworkImportBatchEntity markRunning(UUID importId, Instant startedAt, long fencingToken) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId).orElseThrow();
        batch.markRunning(startedAt, fencingToken);
        return batchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(UUID importId, Instant completedAt, ReconciliationResult result) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId).orElseThrow();
        boolean updated = batch.complete(
                completedAt,
                result.entitiesRead(),
                result.created(),
                result.updated(),
                result.unchanged(),
                result.rejected(),
                result.conflicts(),
                result.missing()
        );
        if (updated) {
            batchRepository.saveAndFlush(batch);
        }
        return updated;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NetworkImportBatchEntity completeReplay(
            UUID importId,
            Instant completedAt,
            UUID originalSuccessfulExecutionId,
            String sourceSnapshotId,
            String schemaVersion,
            String canonicalSnapshotHash
    ) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId).orElseThrow();
        batch.completeReplay(completedAt, originalSuccessfulExecutionId, sourceSnapshotId, schemaVersion, canonicalSnapshotHash);
        return batchRepository.saveAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean terminalize(
            UUID importId,
            String status,
            Instant completedAt,
            ImportFailureCode failureCode,
            boolean retryable,
            String error
    ) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId).orElseThrow();
        boolean updated = batch.terminalize(status, completedAt, failureCode.name(), retryable, error);
        if (updated) {
            batchRepository.saveAndFlush(batch);
        }
        return updated;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendAudit(UUID importId, ImportAuditEventType type, Instant occurredAt, String details) {
        auditRepository.saveAndFlush(NetworkImportAuditEventEntity.create(
                UUID.randomUUID(), importId, type.name(), occurredAt, details));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void appendCheckpoint(UUID executionId, ImportCheckpointType type, Instant recordedAt, String details) {
        checkpointRepository.saveAndFlush(NetworkImportCheckpointEntity.create(
                UUID.randomUUID(), executionId, type.name(), recordedAt, details));
    }

    @Transactional(readOnly = true)
    public NetworkImportBatchEntity require(UUID importId) {
        return batchRepository.findById(importId).orElseThrow();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistVendorSnapshot(VendorSnapshotEntity entity) {
        vendorSnapshotRepository.saveAndFlush(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistProvenance(java.util.List<SourceProvenanceEntity> rows) {
        sourceProvenanceRepository.saveAll(rows);
        sourceProvenanceRepository.flush();
    }
}
