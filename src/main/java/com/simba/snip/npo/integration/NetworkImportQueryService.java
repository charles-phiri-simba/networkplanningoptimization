package com.simba.snip.npo.integration;

import com.simba.snip.npo.api.ImportAuditEventDto;
import com.simba.snip.npo.api.ImportBatchDto;
import com.simba.snip.npo.api.ImportConflictDto;
import com.simba.snip.npo.api.ImportRejectionDto;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.persist.NetworkImportAuditEventRepository;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import com.simba.snip.npo.persist.NetworkImportRejectionRepository;
import com.simba.snip.npo.persist.NetworkIntegrationConflictEntity;
import com.simba.snip.npo.persist.NetworkIntegrationConflictRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NetworkImportQueryService {

    private final NetworkImportBatchRepository batchRepository;
    private final NetworkImportAuditEventRepository auditRepository;
    private final NetworkIntegrationConflictRepository conflictRepository;
    private final NetworkImportRejectionRepository rejectionRepository;

    public NetworkImportQueryService(
            NetworkImportBatchRepository batchRepository,
            NetworkImportAuditEventRepository auditRepository,
            NetworkIntegrationConflictRepository conflictRepository,
            NetworkImportRejectionRepository rejectionRepository
    ) {
        this.batchRepository = batchRepository;
        this.auditRepository = auditRepository;
        this.conflictRepository = conflictRepository;
        this.rejectionRepository = rejectionRepository;
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
                audit
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
