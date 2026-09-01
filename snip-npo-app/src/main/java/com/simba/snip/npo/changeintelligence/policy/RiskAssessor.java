package com.simba.snip.npo.changeintelligence.policy;

import com.simba.snip.npo.action.RiskLevel;
import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import com.simba.snip.npo.twin.SimulationConfidence;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class RiskAssessor {

    public record RiskResult(RiskLevel level, List<String> reasonCodes) {
    }

    public RiskResult assess(
            BigDecimal currentValue,
            BigDecimal candidateValue,
            NetworkKnowledgeConfidence knowledgeConfidence,
            SimulationConfidence simulationConfidence
    ) {
        BigDecimal delta = candidateValue.subtract(currentValue).abs();
        RiskLevel base = delta.compareTo(BigDecimal.valueOf(4)) >= 0
                ? RiskLevel.HIGH
                : delta.compareTo(BigDecimal.valueOf(2)) >= 0
                ? RiskLevel.MEDIUM
                : RiskLevel.LOW;
        List<String> reasons = new ArrayList<>();
        reasons.add("PARAMETER_DELTA=" + delta.toPlainString());
        RiskLevel level = base;
        if (knowledgeConfidence == NetworkKnowledgeConfidence.MEDIUM && level.ordinal() < RiskLevel.MEDIUM.ordinal()) {
            level = RiskLevel.MEDIUM;
            reasons.add("KNOWLEDGE_MEDIUM_DEGRADED");
        }
        if (simulationConfidence == SimulationConfidence.LOW) {
            reasons.add("SIMULATION_LOW_CONFIDENCE");
            if (level.ordinal() < RiskLevel.MEDIUM.ordinal()) {
                level = RiskLevel.MEDIUM;
            }
        }
        return new RiskResult(level, reasons);
    }
}
