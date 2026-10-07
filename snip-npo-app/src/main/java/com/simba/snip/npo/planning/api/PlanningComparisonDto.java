package com.simba.snip.npo.planning.api;

import java.util.List;
import java.util.UUID;

public record PlanningComparisonDto(
        UUID scenarioId,
        UUID evaluationId,
        String evaluationStatus,
        String intentFingerprint,
        boolean historical,
        List<PlanningComparisonRowDto> rows,
        PlanningTruthDto truth
) {
}
