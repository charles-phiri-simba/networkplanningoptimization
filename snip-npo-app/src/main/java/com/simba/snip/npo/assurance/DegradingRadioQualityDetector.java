package com.simba.snip.npo.assurance;

import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.telemetry.Trend;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic DEGRADING_RADIO_QUALITY detector. Not LLM logic.
 */
public final class DegradingRadioQualityDetector {

    public record Detection(
            Severity severity,
            Confidence confidence,
            Instant observedAt,
            boolean synthetic,
            String source,
            List<EvidenceFact> evidence
    ) {
    }

    public record EvidenceFact(
            String evidenceType,
            String metric,
            Double value,
            String unit,
            Trend trend,
            Instant observedAt,
            String source,
            boolean synthetic,
            String description
    ) {
    }

    public record Thresholds(double warning, double major, double critical) {
    }

    private DegradingRadioQualityDetector() {
    }

    public static Optional<Detection> evaluate(CellContext context, Thresholds thresholds) {
        Optional<CellContext.KpiSeriesView> bler = series(context, AssuranceRules.METRIC_BLER_DL);
        if (bler.isEmpty()) {
            return Optional.empty();
        }
        CellContext.KpiSeriesView blerSeries = bler.get();
        if (blerSeries.trend() != Trend.INCREASING) {
            return Optional.empty();
        }
        Double value = blerSeries.current().value();
        if (value == null || value < thresholds.warning()) {
            return Optional.empty();
        }

        Optional<CellContext.KpiSeriesView> prb = series(context, AssuranceRules.METRIC_PRB_DL);
        boolean prbIncreasing = prb.isPresent() && prb.get().trend() == Trend.INCREASING;

        Severity severity = severity(value, prbIncreasing, thresholds);
        Confidence confidence = confidence(prb, prbIncreasing);
        Instant observedAt = blerSeries.current().observedAt();
        boolean synthetic = context.provenance().synthetic() || blerSeries.current().synthetic();
        String source = blerSeries.current().source();

        List<EvidenceFact> facts = new ArrayList<>();
        facts.add(new EvidenceFact(
                AssuranceRules.EVIDENCE_THRESHOLD,
                AssuranceRules.METRIC_BLER_DL,
                value,
                blerSeries.current().unit(),
                blerSeries.trend(),
                observedAt,
                source,
                blerSeries.current().synthetic(),
                "BLER_DL " + blerSeries.current().formatted()
                        + " crossed warning threshold " + thresholds.warning()
                        + " with trend INCREASING"
        ));
        facts.add(new EvidenceFact(
                AssuranceRules.EVIDENCE_TREND,
                AssuranceRules.METRIC_BLER_DL,
                value,
                blerSeries.current().unit(),
                blerSeries.trend(),
                observedAt,
                source,
                blerSeries.current().synthetic(),
                "BLER_DL trend is INCREASING (precomputed; not recalculated)"
        ));
        if (prb.isPresent()) {
            CellContext.KpiSeriesView prbSeries = prb.get();
            facts.add(new EvidenceFact(
                    AssuranceRules.EVIDENCE_CORRELATED_KPI,
                    AssuranceRules.METRIC_PRB_DL,
                    prbSeries.current().value(),
                    prbSeries.current().unit(),
                    prbSeries.trend(),
                    prbSeries.current().observedAt(),
                    prbSeries.current().source(),
                    prbSeries.current().synthetic(),
                    "PRB_UTILIZATION_DL " + prbSeries.current().formatted()
                            + " trend=" + prbSeries.trend()
                            + " (co-occurrence, not causal proof)"
            ));
        }
        return Optional.of(new Detection(severity, confidence, observedAt, synthetic, source, List.copyOf(facts)));
    }

    static Severity severity(double bler, boolean prbIncreasing, Thresholds thresholds) {
        if (bler >= thresholds.critical() && prbIncreasing) {
            return Severity.CRITICAL;
        }
        if (bler >= thresholds.major()) {
            return Severity.MAJOR;
        }
        return Severity.WARNING;
    }

    static Confidence confidence(Optional<CellContext.KpiSeriesView> prb, boolean prbIncreasing) {
        if (prbIncreasing) {
            return Confidence.HIGH;
        }
        if (prb.isPresent()) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }

    private static Optional<CellContext.KpiSeriesView> series(CellContext context, String metric) {
        return context.telemetry().stream().filter(s -> metric.equals(s.metric())).findFirst();
    }
}
