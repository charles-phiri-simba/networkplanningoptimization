package com.simba.snip.npo.api;

import java.util.List;
import java.util.UUID;

public record AgentPlanDto(
        UUID id,
        UUID runId,
        String objective,
        List<AgentPlanStepDto> steps
) {
}
