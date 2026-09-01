package com.simba.snip.npo.assurance;

import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.telemetry.Trend;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DegradingRadioQualityDetectorTest {

    private static final DegradingRadioQualityDetector.Thresholds THRESHOLDS =
            new DegradingRadioQualityDetector.Thresholds(0.08, 0.10, 0.12);

    @Test
    void increasingBlerAtCriticalWithPrbIncreasingIsCriticalHigh() {
        Optional<DegradingRadioQualityDetector.Detection> detection =
                DegradingRadioQualityDetector.evaluate(context(0.12, Trend.INCREASING, 0.84, Trend.INCREASING), THRESHOLDS);
        assertTrue(detection.isPresent());
        assertEquals(Severity.CRITICAL, detection.get().severity());
        assertEquals(Confidence.HIGH, detection.get().confidence());
        assertEquals(3, detection.get().evidence().size());
    }

    @Test
    void increasingBlerAtMajorWithoutPrbIncreaseIsMajorMedium() {
        Optional<DegradingRadioQualityDetector.Detection> detection =
                DegradingRadioQualityDetector.evaluate(context(0.10, Trend.INCREASING, 0.41, Trend.STABLE), THRESHOLDS);
        assertTrue(detection.isPresent());
        assertEquals(Severity.MAJOR, detection.get().severity());
        assertEquals(Confidence.MEDIUM, detection.get().confidence());
    }

    @Test
    void increasingBlerAtWarningWithoutPrbIsWarningLow() {
        Optional<DegradingRadioQualityDetector.Detection> detection =
                DegradingRadioQualityDetector.evaluate(context(0.09, Trend.INCREASING, null, null), THRESHOLDS);
        assertTrue(detection.isPresent());
        assertEquals(Severity.WARNING, detection.get().severity());
        assertEquals(Confidence.LOW, detection.get().confidence());
    }

    @Test
    void thresholdNotCrossedProducesNoCase() {
        assertTrue(DegradingRadioQualityDetector.evaluate(
                context(0.04, Trend.INCREASING, 0.60, Trend.INCREASING), THRESHOLDS).isEmpty());
    }

    @Test
    void insufficientTrendProducesNoCase() {
        assertTrue(DegradingRadioQualityDetector.evaluate(
                context(0.12, Trend.INSUFFICIENT_DATA, 0.84, Trend.INSUFFICIENT_DATA), THRESHOLDS).isEmpty());
    }

    @Test
    void decreasingBlerProducesNoCase() {
        assertTrue(DegradingRadioQualityDetector.evaluate(
                context(0.12, Trend.DECREASING, 0.84, Trend.INCREASING), THRESHOLDS).isEmpty());
    }

    private static CellContext context(double bler, Trend blerTrend, Double prb, Trend prbTrend) {
        Instant t = Instant.parse("2026-08-24T10:15:00Z");
        CellContext.KpiObservationView blerObs = obs("BLER_DL", bler, t);
        List<CellContext.KpiSeriesView> series = new java.util.ArrayList<>();
        series.add(new CellContext.KpiSeriesView("BLER_DL", blerObs, List.of(blerObs), blerTrend));
        if (prb != null) {
            CellContext.KpiObservationView prbObs = obs("PRB_UTILIZATION_DL", prb, t);
            series.add(new CellContext.KpiSeriesView("PRB_UTILIZATION_DL", prbObs, List.of(prbObs), prbTrend));
        }
        return new CellContext(
                new CellContext.CellView("CELL-001", "demo", "NR", "n78", 1, 12, 40, "TDD", "ACTIVE"),
                new CellContext.GnbView("GNB-001", "g", "v", "m", "ACTIVE"),
                new CellContext.SiteView("SITE-001", "s", 0.0, 0.0, "ACTIVE"),
                List.of(),
                List.of(blerObs),
                List.of(new CellContext.NeighbourView("CELL-002", "INTRA_FREQUENCY", "ACTIVE")),
                series,
                new CellContext.ContextProvenance("SNIP_SIMULATOR", true)
        );
    }

    private static CellContext.KpiObservationView obs(String metric, double value, Instant t) {
        return new CellContext.KpiObservationView(
                metric, value, "ratio", t, t, "e-" + metric, "SNIP_SIMULATOR", true);
    }
}
