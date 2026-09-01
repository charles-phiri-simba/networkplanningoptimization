package com.simba.snip.npo.productionchange.domain;

public enum ProductionRollbackStatus {
    REQUESTED,
    REVIEWED,
    REVIEW_REJECTED,
    AUTHORIZED,
    AUTHORIZATION_STALE,
    EXECUTE_DENIED,
    EXECUTING,
    ROLLED_BACK,
    OUTCOME_UNKNOWN,
    BLOCKED
}
