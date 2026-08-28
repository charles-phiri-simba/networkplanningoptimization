package com.simba.snip.npo.integration.sync;

public enum SynchronizationCheckpointStatus {
    VALID,
    UNVERIFIED,
    INVALID,
    EXPIRED,
    RECOVERY_REQUIRED,
    CHECKPOINT_UNCERTAIN
}
