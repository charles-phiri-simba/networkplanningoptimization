package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.persist.NetworkImportBatchEntity;

import java.util.UUID;

public record SynchronizationExecutionResult(
        NetworkImportBatchEntity batch,
        SynchronizationMode mode,
        boolean overlapSkipped,
        UUID executionId
) {
    public static SynchronizationExecutionResult skipped() {
        return new SynchronizationExecutionResult(null, null, true, null);
    }
}
