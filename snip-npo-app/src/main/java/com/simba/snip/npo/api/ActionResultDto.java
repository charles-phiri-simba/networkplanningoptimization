package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record ActionResultDto(
        UUID id,
        String capabilityId,
        String status,
        Instant startedAt,
        Instant completedAt,
        String output,
        String error,
        boolean synthetic
) {
}
