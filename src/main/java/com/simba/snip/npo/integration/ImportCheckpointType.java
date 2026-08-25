package com.simba.snip.npo.integration;

public enum ImportCheckpointType {
    SNAPSHOT_READ,
    NORMALIZATION_COMPLETED,
    VALIDATION_COMPLETED,
    RECONCILIATION_COMPLETED,
    CANONICAL_COMMIT_COMPLETED
}
