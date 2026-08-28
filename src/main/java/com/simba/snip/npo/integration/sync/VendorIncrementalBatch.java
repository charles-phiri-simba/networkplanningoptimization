package com.simba.snip.npo.integration.sync;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VendorIncrementalBatch(
        String sourceSystem,
        String connectorId,
        UUID executionId,
        String startingCheckpoint,
        String resultingCheckpoint,
        String sourceVersion,
        Instant observedAt,
        List<VendorIncrementalChange> changes,
        boolean complete,
        boolean continuityValid
) {
}
