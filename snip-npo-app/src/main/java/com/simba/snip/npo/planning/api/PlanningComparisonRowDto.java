package com.simba.snip.npo.planning.api;

import com.simba.snip.npo.api.MetricComparisonDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlanningComparisonRowDto(
        UUID alternativeId,
        String alternativeName,
        int alternativeOrdinal,
        String cellId,
        String parameterId,
        BigDecimal baselineTxPower,
        BigDecimal intendedTxPower,
        List<MetricComparisonDto> metrics,
        String confidence,
        Boolean synthetic,
        String modelId,
        String modelVersion,
        UUID twinId,
        Integer twinVersion,
        UUID simulationRunId,
        String outcome
) {
}
