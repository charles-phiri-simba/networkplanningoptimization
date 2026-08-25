package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record TwinDetailDto(
        UUID id,
        String name,
        String scopeType,
        String scopeId,
        String status,
        int latestVersion,
        Instant createdAt,
        Instant synchronizedAt,
        boolean synthetic,
        String freshness
) {
}
