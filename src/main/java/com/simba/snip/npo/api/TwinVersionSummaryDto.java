package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record TwinVersionSummaryDto(
        UUID id,
        int version,
        Instant capturedAt,
        Instant synchronizedAt,
        Instant sourceEventTime,
        String sourceContextVersion
) {
}
