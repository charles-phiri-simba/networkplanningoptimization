package com.simba.snip.npo.assurance;

import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.AssuranceEvidenceEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic decision support lists. The LLM may explain them; it does not invent confirmed RCA.
 */
public final class DecisionSupportComposer {

    private DecisionSupportComposer() {
    }

    public static List<String> likelyContributors(AssuranceCaseEntity assuranceCase, CellContext context) {
        List<String> items = new ArrayList<>();
        boolean prbIncreasing = assuranceCase.getEvidence().stream()
                .anyMatch(e -> AssuranceRules.METRIC_PRB_DL.equals(e.getMetric())
                        && TrendName.INCREASING.equals(e.getTrend()));
        items.add("Interference or coverage degradation may be contributing (inference, not confirmed root cause).");
        if (prbIncreasing) {
            items.add("Congestion may be contributing because PRB_UTILIZATION_DL is INCREASING (inference, not confirmed root cause).");
        }
        if (context.neighbours().isEmpty()) {
            items.add("Neighbour context is incomplete, so a localised vs wider pattern cannot be confirmed.");
        }
        return List.copyOf(items);
    }

    public static List<String> recommendedChecks(AssuranceCaseEntity assuranceCase) {
        List<String> checks = new ArrayList<>();
        checks.add("Review downlink SINR distribution versus RSRP.");
        checks.add("Perform an external interference sweep on the affected band.");
        checks.add("Check neighbour relation completeness and forbidden lists.");
        boolean prb = assuranceCase.getEvidence().stream()
                .anyMatch(e -> AssuranceRules.METRIC_PRB_DL.equals(e.getMetric()));
        if (prb) {
            checks.add("Inspect scheduler occupancy and backhaul congestion indicators.");
        }
        checks.add("Confirm findings with a human reviewer before any network change.");
        return List.copyOf(checks);
    }

    public static List<String> missingEvidence(AssuranceCaseEntity assuranceCase, CellContext context) {
        List<String> missing = new ArrayList<>();
        if (assuranceCase.getEvidence().stream().noneMatch(e -> "SINR".equals(e.getMetric()))) {
            missing.add("SINR observations are not in the assurance evidence set.");
        }
        if (assuranceCase.getEvidence().stream().noneMatch(e -> "RSRP".equals(e.getMetric()))) {
            missing.add("RSRP observations are not in the assurance evidence set.");
        }
        boolean neighbourKpi = context.neighbours().stream().anyMatch(n -> n.targetCellId() != null)
                && assuranceCase.getEvidence().stream().noneMatch(e -> "NEIGHBOUR_BLER".equals(e.getEvidenceType()));
        if (neighbourKpi) {
            missing.add("Neighbour-cell KPI series are not attached as operational evidence.");
        }
        return List.copyOf(missing);
    }

    public static Urgency urgency(Severity severity) {
        return switch (severity) {
            case CRITICAL -> Urgency.IMMEDIATE;
            case MAJOR -> Urgency.HIGH;
            case WARNING -> Urgency.MEDIUM;
            case INFO -> Urgency.LOW;
        };
    }

    public static String evidenceSummary(AssuranceCaseEntity assuranceCase) {
        StringBuilder sb = new StringBuilder();
        for (AssuranceEvidenceEntity item : assuranceCase.getEvidence()) {
            sb.append("- ").append(item.getDescription()).append('\n');
        }
        return sb.toString();
    }

    private static final class TrendName {
        private static final String INCREASING = "INCREASING";

        private TrendName() {
        }
    }
}
