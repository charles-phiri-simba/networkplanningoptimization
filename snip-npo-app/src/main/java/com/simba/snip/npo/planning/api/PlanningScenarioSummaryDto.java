package com.simba.snip.npo.planning.api;

import java.time.Instant;
import java.util.UUID;

public record PlanningScenarioSummaryDto(
        UUID id,
        String name,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        int rowVersion,
        int cellCount,
        int alternativeCount,
        String evaluationView,
        PlanningTruthDto truth
) {
}
