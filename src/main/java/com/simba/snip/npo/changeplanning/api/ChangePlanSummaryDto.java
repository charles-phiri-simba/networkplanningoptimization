package com.simba.snip.npo.changeplanning.api;

import java.time.Instant;
import java.util.UUID;

public record ChangePlanSummaryDto(
        UUID id,
        UUID proposalId,
        String status,
        String targetEntityType,
        String targetEntityId,
        String parameterName,
        String expectedCurrentValue,
        String desiredValue,
        String impactLevel,
        Instant createdAt,
        Instant expiresAt
) {
}
