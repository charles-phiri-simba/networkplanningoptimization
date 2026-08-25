package com.simba.snip.npo.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SimulationComparisonDto(
        UUID leftSimulationId,
        UUID rightSimulationId,
        UUID twinId,
        int baselineTwinVersion,
        String leftConfidence,
        String rightConfidence,
        List<String> leftLimitations,
        List<String> rightLimitations,
        List<Map<String, Object>> metricTradeoffs,
        boolean automaticOptimumSelected
) {
}
