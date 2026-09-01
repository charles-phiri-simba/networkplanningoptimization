package com.simba.snip.npo.agent;

import java.util.UUID;

public record AgentRunCommand(
        String objective,
        UUID assuranceCaseId,
        String initiatedBy,
        Integer maxSteps,
        Integer maxAgentCalls,
        Integer maxRetries,
        Long timeoutMs
) {
}
