package com.simba.snip.npo.twin;

import com.simba.snip.npo.domain.DomainConflictException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic synthetic engineering model — not vendor-calibrated RF physics.
 *
 * <p>Let {@code R = currentTxPower_dBm − proposedTxPower_dBm} (positive when lowering power).
 *
 * <pre>
 * BLER_DL'            = clamp(BLER_DL * (1 + 0.025 * R), 0, 1)
 * PRB_UTILIZATION_DL' = clamp(PRB_UTILIZATION_DL * (1 − 0.01 * R), 0, 1)
 * THROUGHPUT_DL'      = max(0, THROUGHPUT_DL * (1 − 0.02 * R))   if present
 * </pre>
 *
 * <p>Outputs are rounded to 6 decimal places, {@code RoundingMode.HALF_UP}. Identical input
 * always yields identical output. Confidence is {@code LOW}.
 */
@Component
public class CellParameterSimulationModel {

    public static final String MODEL_ID = "snip.synthetic.cell-parameter.v1";
    public static final String MODEL_VERSION = "1.0";
    public static final String MODEL_TYPE = "RULE_BASED";
    public static final String METRIC_BLER_DL = "BLER_DL";
    public static final String METRIC_PRB_DL = "PRB_UTILIZATION_DL";
    public static final String METRIC_THROUGHPUT_DL = "THROUGHPUT_DL";
    public static final String METRIC_TX_POWER = "txPower";
    public static final String UNIT_RATIO = "ratio";
    public static final String UNIT_MBPS = "Mbps";
    public static final String UNIT_DBM = "dBm";

    private static final int SCALE = 6;
    private static final BigDecimal BLER_SENSITIVITY = new BigDecimal("0.025");
    private static final BigDecimal PRB_SENSITIVITY = new BigDecimal("0.01");
    private static final BigDecimal THROUGHPUT_SENSITIVITY = new BigDecimal("0.02");

    public record SimulationInput(
            BigDecimal currentTxPower,
            BigDecimal proposedTxPower,
            Double blerDl,
            Double prbUtilizationDl,
            Double throughputDl,
            String blerTrend
    ) {
    }

    public record ModelOutput(
            List<MetricComparison> metrics,
            SimulationConfidence confidence,
            List<SimulationLimitation> limitations,
            List<String> assumptions,
            SimulationModelMetadata metadata
    ) {
    }

    public ModelOutput predict(SimulationInput input) {
        if (input == null || input.currentTxPower() == null || input.proposedTxPower() == null) {
            throw new DomainConflictException("simulation model failure: txPower inputs are required");
        }
        if (input.blerDl() == null || input.prbUtilizationDl() == null) {
            throw new DomainConflictException("simulation model failure: BLER_DL and PRB_UTILIZATION_DL are required");
        }
        BigDecimal reduction = input.currentTxPower().subtract(input.proposedTxPower());
        List<MetricComparison> metrics = new ArrayList<>();
        metrics.add(compare(
                METRIC_TX_POWER,
                input.currentTxPower().doubleValue(),
                input.proposedTxPower().doubleValue(),
                UNIT_DBM
        ));
        metrics.add(compare(
                METRIC_BLER_DL,
                input.blerDl(),
                scale(clamp01(multiply(input.blerDl(), factor(BLER_SENSITIVITY, reduction)))),
                UNIT_RATIO
        ));
        metrics.add(compare(
                METRIC_PRB_DL,
                input.prbUtilizationDl(),
                scale(clamp01(multiply(input.prbUtilizationDl(), factor(PRB_SENSITIVITY.negate(), reduction)))),
                UNIT_RATIO
        ));
        if (input.throughputDl() != null) {
            double predicted = Math.max(0.0, scale(multiply(input.throughputDl(), factor(THROUGHPUT_SENSITIVITY.negate(), reduction))));
            metrics.add(compare(METRIC_THROUGHPUT_DL, input.throughputDl(), predicted, UNIT_MBPS));
        }
        return new ModelOutput(
                List.copyOf(metrics),
                SimulationConfidence.LOW,
                List.of(SimulationLimitation.values()),
                assumptions(input, reduction),
                new SimulationModelMetadata(MODEL_ID, MODEL_VERSION, MODEL_TYPE, assumptions(input, reduction))
        );
    }

    private static List<String> assumptions(SimulationInput input, BigDecimal reduction) {
        List<String> assumptions = new ArrayList<>();
        assumptions.add("Synthetic engineering model — not vendor-calibrated RF physics.");
        assumptions.add("Isolated cell; neighbour coupling is not modelled.");
        assumptions.add("Linear first-order sensitivity to txPower delta R=" + reduction.toPlainString() + " dB.");
        assumptions.add("No mobility model and no traffic forecast are applied.");
        if (input.blerTrend() != null && !input.blerTrend().isBlank()) {
            assumptions.add("Observed BLER_DL trend=" + input.blerTrend() + " is recorded; it does not change the formula.");
        }
        return List.copyOf(assumptions);
    }

    private static MetricComparison compare(String metric, double baseline, double candidate, String unit) {
        return new MetricComparison(metric, scale(baseline), scale(candidate), scale(candidate - baseline), unit);
    }

    private static BigDecimal factor(BigDecimal sensitivity, BigDecimal reduction) {
        return BigDecimal.ONE.add(sensitivity.multiply(reduction));
    }

    private static double multiply(double value, BigDecimal factor) {
        return BigDecimal.valueOf(value).multiply(factor).doubleValue();
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }

    private static double scale(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
