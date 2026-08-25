package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record ImportCheckpointDto(
        UUID checkpointId,
        UUID executionId,
        String checkpointType,
        Instant recordedAt,
        String details
) {
}
