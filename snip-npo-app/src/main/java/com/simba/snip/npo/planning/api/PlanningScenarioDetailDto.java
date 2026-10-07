package com.simba.snip.npo.planning.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanningScenarioDetailDto(
        UUID id,
        String name,
        String description,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        int rowVersion,
        List<PlanningScenarioCellDto> cells,
        List<PlanningAlternativeDto> alternatives,
        String evaluationView,
        UUID currentEvaluationId,
        PlanningTruthDto truth
) {
}
