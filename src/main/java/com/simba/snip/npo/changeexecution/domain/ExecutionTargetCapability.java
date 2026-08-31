package com.simba.snip.npo.changeexecution.domain;

public enum ExecutionTargetCapability {
    PARAMETER_WRITE,
    PARAMETER_READBACK,
    ROLLBACK,
    EXPECTED_STATE_GUARD,
    IDEMPOTENT_OPERATION
}
