package com.simba.snip.npo.api;

import java.util.UUID;

public record CreateAgentRunRequest(
        String objective,
        UUID assuranceCaseId,
        String initiatedBy,
        Integer maxSteps,
        Integer maxAgentCalls,
        Integer maxRetries,
        Long timeoutMs
) {
}
