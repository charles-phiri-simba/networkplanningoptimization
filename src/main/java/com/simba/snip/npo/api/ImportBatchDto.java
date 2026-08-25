package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportBatchDto(
        UUID importId,
        String sourceSystem,
        String vendor,
        String sourceSnapshotId,
        String vendorSchemaVersion,
        String fixtureKind,
        Instant startedAt,
        Instant completedAt,
        String status,
        int entitiesRead,
        int entitiesCreated,
        int entitiesUpdated,
        int entitiesUnchanged,
        int entitiesRejected,
        int conflictsDetected,
        int missingEntitiesDetected,
        String error,
        List<ImportAuditEventDto> audit
) {
}
