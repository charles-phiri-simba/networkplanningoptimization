package com.simba.snip.npo.planning.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanningEvaluationItemDto(
        UUID id,
        UUID evaluationId,
        UUID alternativeId,
        String alternativeName,
        int alternativeOrdinal,
        String cellId,
        String parameterId,
        BigDecimal intendedValue,
        String outcome,
        String failureCode,
        String failureMessage,
        UUID twinId,
        Integer twinVersion,
        BigDecimal pinnedBaselineTxPower,
        String configurationFingerprint,
        UUID simulationScenarioId,
        UUID simulationRunId,
        String modelId,
        String modelVersion,
        Boolean synthetic,
        String confidence,
        boolean reusedExistingRun
) {
}
