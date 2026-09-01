package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.sync.SynchronizationMode;
import com.simba.snip.npo.integration.sync.VendorIncrementalBatch;

public final class SynchronizationExecutionContext {

    private final ImportExecutionContext importContext;
    private final SynchronizationMode mode;
    private final String startingCheckpoint;
    private final boolean recoveryRequested;

    public SynchronizationExecutionContext(
            ImportExecutionContext importContext,
            SynchronizationMode mode,
            String startingCheckpoint,
            boolean recoveryRequested
    ) {
        this.importContext = importContext;
        this.mode = mode;
        this.startingCheckpoint = startingCheckpoint == null ? "" : startingCheckpoint;
        this.recoveryRequested = recoveryRequested;
    }

    public ImportExecutionContext importContext() {
        return importContext;
    }

    public SynchronizationMode mode() {
        return mode;
    }

    public String startingCheckpoint() {
        return startingCheckpoint;
    }

    public boolean recoveryRequested() {
        return recoveryRequested;
    }

    public void assertContinuing() {
        importContext.assertContinuing();
    }
}
