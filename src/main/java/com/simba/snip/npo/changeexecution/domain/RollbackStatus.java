package com.simba.snip.npo.changeexecution.domain;

public enum RollbackStatus {
    NOT_REQUESTED,
    REQUESTED,
    REVIEWED,
    AUTHORIZED,
    EXECUTING,
    APPLIED,
    OUTCOME_UNKNOWN,
    VERIFIED,
    FAILED,
    MANUAL_INTERVENTION
}
