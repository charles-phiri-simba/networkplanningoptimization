package com.simba.snip.npo.changeexecution.api;

import java.time.Instant;
import java.util.UUID;

public record ExecutionSummaryDto(
        UUID executionId,
        UUID planId,
        String status,
        String executionTargetId,
        String cellId,
        String parameterName,
        String executionFingerprint,
        Instant requestedAt,
        Instant authorizedAt,
        Instant completedAt
) {
}
