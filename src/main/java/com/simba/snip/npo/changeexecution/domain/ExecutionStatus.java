package com.simba.snip.npo.changeexecution.domain;

import java.util.List;

public enum ExecutionStatus {
    REQUESTED,
    PRELIMINARY_ADMISSION_CHECKING,
    PRELIMINARY_ADMISSION_REJECTED,
    READY_FOR_REVIEW,
    REVIEWED,
    READY_FOR_EXECUTION_AUTHORIZATION,
    AUTHORIZED,
    FINAL_PREFLIGHT_CHECKING,
    EXECUTING,
    APPLIED,
    EXECUTION_OUTCOME_UNKNOWN,
    VERIFYING,
    VERIFIED,
    EXECUTION_FAILED,
    VERIFICATION_FAILED,
    RECOVERY_REQUIRED,
    ROLLBACK_REQUESTED,
    ROLLBACK_REVIEWED,
    ROLLBACK_AUTHORIZED,
    ROLLING_BACK,
    ROLLBACK_APPLIED,
    ROLLBACK_OUTCOME_UNKNOWN,
    ROLLED_BACK,
    ROLLBACK_FAILED,
    MANUAL_INTERVENTION_REQUIRED,
    CANCELLED_BEFORE_MUTATION;

    public static final List<String> ACTIVE_NAMES = List.of(
            REQUESTED.name(),
            PRELIMINARY_ADMISSION_CHECKING.name(),
            READY_FOR_REVIEW.name(),
            REVIEWED.name(),
            READY_FOR_EXECUTION_AUTHORIZATION.name(),
            AUTHORIZED.name(),
            FINAL_PREFLIGHT_CHECKING.name(),
            EXECUTING.name(),
            APPLIED.name(),
            EXECUTION_OUTCOME_UNKNOWN.name(),
            VERIFYING.name(),
            RECOVERY_REQUIRED.name(),
            ROLLBACK_REQUESTED.name(),
            ROLLBACK_REVIEWED.name(),
            ROLLBACK_AUTHORIZED.name(),
            ROLLING_BACK.name(),
            ROLLBACK_APPLIED.name(),
            ROLLBACK_OUTCOME_UNKNOWN.name()
    );

    public boolean isActive() {
        return ACTIVE_NAMES.contains(name());
    }

    public boolean isTerminal() {
        return !isActive();
    }

    public boolean allowsCancelBeforeMutation() {
        return this == REQUESTED
                || this == PRELIMINARY_ADMISSION_CHECKING
                || this == READY_FOR_REVIEW
                || this == REVIEWED
                || this == READY_FOR_EXECUTION_AUTHORIZATION
                || this == AUTHORIZED
                || this == FINAL_PREFLIGHT_CHECKING;
    }

    public boolean allowsExecute() {
        return this == AUTHORIZED;
    }

    public boolean allowsVerify() {
        return this == APPLIED
                || this == EXECUTION_OUTCOME_UNKNOWN
                || this == VERIFYING
                || this == ROLLBACK_APPLIED
                || this == ROLLBACK_OUTCOME_UNKNOWN;
    }
}
