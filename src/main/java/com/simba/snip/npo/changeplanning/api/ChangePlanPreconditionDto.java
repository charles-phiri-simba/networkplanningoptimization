package com.simba.snip.npo.changeplanning.api;

import java.time.Instant;

public record ChangePlanPreconditionDto(
        String preconditionType,
        String expectedCondition,
        String observedValue,
        String result,
        String reasonCode,
        Instant checkedAt
) {
}
