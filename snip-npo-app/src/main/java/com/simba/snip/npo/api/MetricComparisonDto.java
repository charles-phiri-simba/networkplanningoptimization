package com.simba.snip.npo.api;

public record MetricComparisonDto(
        String metric,
        double baselineValue,
        double candidateValue,
        double delta,
        String unit
) {
}
