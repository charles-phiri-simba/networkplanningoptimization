package com.simba.snip.npo.changeexecution.adapter.spi;

import com.simba.snip.npo.changeexecution.domain.AttemptOutcome;

public record MutationResult(
        AttemptOutcome outcome,
        String failureCode,
        String failureDetailSafe,
        Long targetRevisionBefore,
        Long targetRevisionAfter,
        String appliedValue
) {
    public static MutationResult applied(Long revisionBefore, Long revisionAfter, String appliedValue) {
        return new MutationResult(AttemptOutcome.APPLIED, null, null, revisionBefore, revisionAfter, appliedValue);
    }

    public static MutationResult rejected(String failureCode, String detail) {
        return new MutationResult(AttemptOutcome.REJECTED, failureCode, detail, null, null, null);
    }

    public static MutationResult timeout(String failureCode, String detail, Long revisionBefore, Long revisionAfter) {
        return new MutationResult(AttemptOutcome.TIMEOUT, failureCode, detail, revisionBefore, revisionAfter, null);
    }

    public static MutationResult outcomeUnknown(String failureCode, String detail, Long revisionBefore, Long revisionAfter) {
        return new MutationResult(AttemptOutcome.OUTCOME_UNKNOWN, failureCode, detail, revisionBefore, revisionAfter, null);
    }
}
