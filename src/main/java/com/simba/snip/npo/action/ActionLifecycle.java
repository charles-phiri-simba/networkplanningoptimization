package com.simba.snip.npo.action;

import com.simba.snip.npo.domain.DomainConflictException;

public final class ActionLifecycle {

    private ActionLifecycle() {
    }

    public static void requireExecutable(ActionStatus status, PolicyOutcome policy, boolean approved) {
        if (status == ActionStatus.SUCCEEDED) {
            return;
        }
        if (status == ActionStatus.DENIED || policy == PolicyOutcome.DENY) {
            throw new DomainConflictException("denied actions cannot be executed");
        }
        if (status == ActionStatus.REJECTED) {
            throw new DomainConflictException("rejected actions cannot be executed");
        }
        if (policy == PolicyOutcome.ALLOW && (status == ActionStatus.POLICY_EVALUATED || status == ActionStatus.FAILED)) {
            return;
        }
        if (policy == PolicyOutcome.REQUIRE_APPROVAL && approved
                && (status == ActionStatus.APPROVED || status == ActionStatus.FAILED)) {
            return;
        }
        if (policy == PolicyOutcome.REQUIRE_APPROVAL && !approved) {
            throw new DomainConflictException("approval is required before execution");
        }
        throw new DomainConflictException("action status " + status + " cannot be executed");
    }

    public static void requireApprovable(ActionStatus status, PolicyOutcome policy) {
        if (policy != PolicyOutcome.REQUIRE_APPROVAL || status != ActionStatus.APPROVAL_REQUIRED) {
            throw new DomainConflictException("action is not waiting for approval");
        }
    }
}
