package com.simba.snip.npo.changeexecution.domain;

public enum SimulatorFailureMode {
    SUCCESS,
    REJECT_BEFORE_APPLY,
    TIMEOUT_BEFORE_APPLY,
    TIMEOUT_AFTER_APPLY,
    APPLY_WRONG_VALUE,
    READBACK_TIMEOUT,
    READBACK_STALE,
    ROLLBACK_FAILURE,
    ROLLBACK_TIMEOUT_AFTER_APPLY
}
