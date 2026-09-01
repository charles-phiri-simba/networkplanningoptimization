package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record ImportRejectionDto(
        UUID rejectionId,
        UUID importId,
        String sourceEntityId,
        String entityType,
        String reasonCode,
        String details,
        Instant rejectedAt
) {
}
