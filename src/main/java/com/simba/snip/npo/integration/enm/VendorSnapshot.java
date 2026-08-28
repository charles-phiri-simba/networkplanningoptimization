package com.simba.snip.npo.integration.enm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VendorSnapshot(
        String snapshotId,
        UUID executionId,
        String connectorId,
        String sourceVendor,
        String sourceSystem,
        Instant startedAt,
        Instant completedAt,
        SnapshotCompleteness completeness,
        Integer pagesExpected,
        int pagesReceived,
        int entitiesRead,
        List<String> warnings,
        String sourceVersion
) {
    public VendorSnapshot {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
