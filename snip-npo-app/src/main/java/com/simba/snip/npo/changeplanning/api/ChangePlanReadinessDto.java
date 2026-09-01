package com.simba.snip.npo.changeplanning.api;

import java.time.Instant;

public record ChangePlanReadinessDto(
        Instant assessedAt,
        String result,
        String assessedFingerprint,
        String reasonCodes
) {
}
