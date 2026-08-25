package com.simba.snip.npo.integration;

import com.simba.snip.npo.api.ImportAuditEventDto;
import com.simba.snip.npo.api.ImportBatchDto;
import com.simba.snip.npo.api.ImportCheckpointDto;
import com.simba.snip.npo.api.ImportConflictDto;
import com.simba.snip.npo.api.ImportRejectionDto;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.persist.NetworkImportAuditEventRepository;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import com.simba.snip.npo.persist.NetworkImportCheckpointRepository;
import com.simba.snip.npo.persist.NetworkImportRejectionRepository;
import com.simba.snip.npo.persist.NetworkIntegrationConflictEntity;
import com.simba.snip.npo.persist.NetworkIntegrationConflictRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NetworkImportQueryService {

    private final NetworkImportBatchRepository batchRepository;
    private final NetworkImportAuditEventRepository auditRepository;
    private final NetworkImportCheckpointRepository checkpointRepository;
    private final NetworkIntegrationConflictRepository conflictRepository;
    private final NetworkImportRejectionRepository rejectionRepository;
    private final ImportLeaseService leaseService;

    public NetworkImportQueryService(
            NetworkImportBatchRepository batchRepository,
            NetworkImportAuditEventRepository auditRepository,
            NetworkImportCheckpointRepository checkpointRepository,
            NetworkIntegrationConflictRepository conflictRepository,
            NetworkImportRejectionRepository rejectionRepository,
            ImportLeaseService leaseService
    ) {
        this.batchRepository = batchRepository;
        this.auditRepository = auditRepository;
        this.checkpointRepository = checkpointRepository;
        this.conflictRepository = conflictRepository;
        this.rejectionRepository = rejectionRepository;
        this.leaseService = leaseService;
    }

    public List<ImportBatchDto> listImports() {
        return batchRepository.findAllByOrderByStartedAtDesc().stream().map(this::toBatch).toList();
    }

    public ImportBatchDto importDetail(UUID importId) {
        NetworkImportBatchEntity batch = batchRepository.findById(importId)
                .orElseThrow(() -> new DomainNotFoundException("import", importId.toString()));
        List<ImportAuditEventDto> audit = auditRepository.findByImportIdOrderByOccurredAtAsc(importId).stream()
                .map(event -> new ImportAuditEventDto(
                        event.getId(), event.getEventType(), event.getOccurredAt(), event.getDetails()))
                .toList();
        return toBatch(batch, audit);
    }

    public List<ImportCheckpointDto> checkpoints(UUID executionId) {
        if (batchRepository.findById(executionId).isEmpty()) {
            throw new DomainNotFoundException("import", executionId.toString());
        }
        return checkpointRepository.findByExecutionIdOrderByRecordedAtAsc(executionId).stream()
                .map(checkpoint -> new ImportCheckpointDto(
                        checkpoint.getCheckpointId(),
                        checkpoint.getExecutionId(),
                        checkpoint.getCheckpointType(),
                        checkpoint.getRecordedAt(),
                        checkpoint.getDetails()
                ))
                .toList();
    }

    public Map<String, Object> runtimeHealth() {
        List<NetworkImportBatchEntity> running = batchRepository.findByStatus("RUNNING");
        Map<String, Object> lastSuccessful = new LinkedHashMap<>();
        batchRepository.findFirstBySourceSystemAndStatusOrderByCompletedAtDesc("ERICSSON_FIXTURE", "COMPLETED")
                .ifPresent(batch -> lastSuccessful.put("ERICSSON_FIXTURE", batch.getId().toString()));
        batchRepository.findFirstBySourceSystemAndStatusOrderByCompletedAtDesc("NOKIA_FIXTURE", "COMPLETED")
                .ifPresent(batch -> lastSuccessful.put("NOKIA_FIXTURE", batch.getId().toString()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activeImports", running.size());
        body.put("expiredLeases", leaseService.expiredLeaseCount());
        body.put("stuckExecutions", running.stream()
                .filter(execution -> execution.getLeaseFencingToken() == null
                        || leaseService.find(execution.getSourceSystem(), execution.getSourceScope())
                        .filter(lease -> lease.ownerExecutionId().equals(execution.getId()))
                        .isEmpty())
                .count());
        body.put("lastSuccessfulImportBySource", lastSuccessful);
        return body;
    }

    public List<ImportConflictDto> listConflicts() {
        return conflictRepository.findAllByOrderByDetectedAtDesc().stream().map(this::toConflict).toList();
    }

    public ImportConflictDto conflict(UUID conflictId) {
        NetworkIntegrationConflictEntity conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new DomainNotFoundException("conflict", conflictId.toString()));
        return toConflict(conflict);
    }

    public List<ImportRejectionDto> listRejections() {
        return rejectionRepository.findAllByOrderByRejectedAtDesc().stream()
                .map(rejection -> new ImportRejectionDto(
                        rejection.getId(),
                        rejection.getImportId(),
                        rejection.getSourceEntityId(),
                        rejection.getEntityType(),
                        rejection.getReasonCode(),
                        rejection.getDetails(),
                        rejection.getRejectedAt()
                ))
                .toList();
    }

    private ImportBatchDto toBatch(NetworkImportBatchEntity batch) {
        return toBatch(batch, List.of());
    }

    private ImportBatchDto toBatch(NetworkImportBatchEntity batch, List<ImportAuditEventDto> audit) {
        return new ImportBatchDto(
                batch.getId(),
                batch.getSourceSystem(),
                batch.getVendor(),
                batch.getSourceSnapshotId(),
                batch.getVendorSchemaVersion(),
                batch.getFixtureKind(),
                batch.getStartedAt(),
                batch.getCompletedAt(),
                batch.getStatus(),
                batch.getEntitiesRead(),
                batch.getEntitiesCreated(),
                batch.getEntitiesUpdated(),
                batch.getEntitiesUnchanged(),
                batch.getEntitiesRejected(),
                batch.getConflictsDetected(),
                batch.getMissingEntitiesDetected(),
                batch.getError(),
                audit,
                batch.getExecutionType(),
                batch.getAttemptNumber(),
                batch.getPreviousExecutionId(),
                batch.getOriginalSuccessfulExecutionId(),
                batch.getSourceScope(),
                batch.getCanonicalSnapshotHash(),
                batch.getFailureCode(),
                batch.getRetryable(),
                batch.getLeaseFencingToken(),
                batch.getRequestedAt()
        );
    }

    private ImportConflictDto toConflict(NetworkIntegrationConflictEntity conflict) {
        return new ImportConflictDto(
                conflict.getId(),
                conflict.getImportId(),
                conflict.getEntityType(),
                conflict.getCanonicalEntityId(),
                conflict.getConflictScope(),
                conflict.getCurrentValue(),
                conflict.getIncomingValue(),
                conflict.getAuthoritativeSource(),
                conflict.getIncomingSource(),
                conflict.getReasonCode(),
                conflict.getStatus(),
                conflict.getDetectedAt()
        );
    }
}
