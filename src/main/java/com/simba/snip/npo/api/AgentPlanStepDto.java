package com.simba.snip.npo.api;

import java.util.UUID;

public record AgentPlanStepDto(
        UUID id,
        int stepNumber,
        String agentRole,
        String task,
        String requiredInputs,
        String expectedOutput,
        String status,
        String outputSummary
) {
}
