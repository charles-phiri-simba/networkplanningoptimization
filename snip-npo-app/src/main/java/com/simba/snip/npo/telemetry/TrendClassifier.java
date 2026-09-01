package com.simba.snip.npo.telemetry;

import java.util.List;

/**
 * Deterministic trend classification over a chronological series. Not LLM logic.
 */
public final class TrendClassifier {

    private static final double STABLE_EPSILON = 1e-12;

    private TrendClassifier() {
    }

    public static Trend classify(List<Double> chronologicalValues) {
        if (chronologicalValues == null || chronologicalValues.size() < 2) {
            return Trend.INSUFFICIENT_DATA;
        }
        Double first = chronologicalValues.get(0);
        Double last = chronologicalValues.get(chronologicalValues.size() - 1);
        if (first == null || last == null) {
            return Trend.INSUFFICIENT_DATA;
        }
        double delta = last - first;
        if (Math.abs(delta) < STABLE_EPSILON) {
            return Trend.STABLE;
        }
        return delta > 0 ? Trend.INCREASING : Trend.DECREASING;
    }
}
