package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentCaseMemoryDto(
        UUID id,
        UUID assuranceCaseId,
        UUID runId,
        String summary,
        String findings,
        List<UUID> proposedActionIds,
        Instant createdAt
) {
}
