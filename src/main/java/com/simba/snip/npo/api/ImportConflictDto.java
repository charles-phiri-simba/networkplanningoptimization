package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record ImportConflictDto(
        UUID conflictId,
        UUID importId,
        String entityType,
        String canonicalEntityId,
        String conflictScope,
        String currentValue,
        String incomingValue,
        String authoritativeSource,
        String incomingSource,
        String reasonCode,
        String status,
        Instant detectedAt
) {
}
