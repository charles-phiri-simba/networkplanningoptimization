package com.simba.snip.npo.changeexecution.adapter.spi;

import java.time.Instant;

public record ExecutionContext(
        String executionId,
        String targetId,
        String cellId,
        String parameterName,
        long fencingToken,
        Instant requestedAt
) {
}
