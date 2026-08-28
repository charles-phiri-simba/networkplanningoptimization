package com.simba.snip.npo.integration.sync;

public record SynchronizationExecutionRequest(
        SynchronizationPolicy policy,
        SynchronizationInitiator initiator,
        SynchronizationMode requestedMode,
        boolean recoveryRequested
) {
}
