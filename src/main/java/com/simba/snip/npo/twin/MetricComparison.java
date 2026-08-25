package com.simba.snip.npo.twin;

public record MetricComparison(
        String metric,
        double baselineValue,
        double candidateValue,
        double delta,
        String unit
) {
}
