package com.simba.snip.npo.changeintelligence.policy;

import com.simba.snip.npo.action.RiskLevel;
import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import com.simba.snip.npo.twin.CellParameterSimulationModel;
import com.simba.snip.npo.twin.MetricComparison;
import com.simba.snip.npo.twin.SimulationConfidence;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ChangeProposalBenefitAssessor {

    private static final BigDecimal PRB_WEIGHT = new BigDecimal("100");
    private static final BigDecimal BLER_WEIGHT = new BigDecimal("50");
    private static final BigDecimal THROUGHPUT_WEIGHT = new BigDecimal("0.1");

    public record BenefitResult(BigDecimal score, String summary, List<String> reasonCodes) {
    }

    public BenefitResult assess(List<MetricComparison> metrics) {
        double baselinePrb = metric(metrics, CellParameterSimulationModel.METRIC_PRB_DL);
        double candidatePrb = candidate(metrics, CellParameterSimulationModel.METRIC_PRB_DL);
        double baselineBler = metric(metrics, CellParameterSimulationModel.METRIC_BLER_DL);
        double candidateBler = candidate(metrics, CellParameterSimulationModel.METRIC_BLER_DL);
        double baselineThroughput = metric(metrics, CellParameterSimulationModel.METRIC_THROUGHPUT_DL);
        double candidateThroughput = candidate(metrics, CellParameterSimulationModel.METRIC_THROUGHPUT_DL);

        BigDecimal prbImprovement = BigDecimal.valueOf(baselinePrb - candidatePrb);
        BigDecimal blerPenalty = BigDecimal.valueOf(candidateBler - baselineBler);
        BigDecimal throughputGain = BigDecimal.valueOf(candidateThroughput - baselineThroughput);

        BigDecimal score = prbImprovement.multiply(PRB_WEIGHT)
                .subtract(blerPenalty.multiply(BLER_WEIGHT))
                .add(throughputGain.multiply(THROUGHPUT_WEIGHT))
                .setScale(6, RoundingMode.HALF_UP);

        List<String> reasons = new ArrayList<>();
        reasons.add("PRB_IMPROVEMENT=" + prbImprovement.toPlainString());
        reasons.add("BLER_PENALTY=" + blerPenalty.toPlainString());
        if (baselineThroughput > 0 || candidateThroughput > 0) {
            reasons.add("THROUGHPUT_DELTA=" + throughputGain.toPlainString());
        }
        String summary = "deterministic benefit from PRB/BLER/throughput deltas";
        return new BenefitResult(score, summary, reasons);
    }

    public BenefitResult assessFromSimulationMap(Map<String, Object> simulationResult) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) simulationResult.get("metrics");
        if (metrics == null) {
            return new BenefitResult(BigDecimal.ZERO, "no metrics", List.of("NO_METRICS"));
        }
        List<MetricComparison> comparisons = metrics.stream()
                .map(m -> new MetricComparison(
                        String.valueOf(m.get("metric")),
                        toDouble(m.get("baselineValue")),
                        toDouble(m.get("candidateValue")),
                        toDouble(m.get("delta")),
                        String.valueOf(m.get("unit"))
                ))
                .toList();
        return assess(comparisons);
    }

    private static double metric(List<MetricComparison> metrics, String name) {
        return metrics.stream()
                .filter(m -> name.equals(m.metric()))
                .map(MetricComparison::baselineValue)
                .findFirst()
                .orElse(0.0);
    }

    private static double candidate(List<MetricComparison> metrics, String name) {
        return metrics.stream()
                .filter(m -> name.equals(m.metric()))
                .map(MetricComparison::candidateValue)
                .findFirst()
                .orElse(0.0);
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }
}
