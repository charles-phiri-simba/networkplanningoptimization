package com.simba.snip.npo.changeintelligence.policy;

import com.simba.snip.npo.action.RiskLevel;
import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import com.simba.snip.npo.twin.SimulationConfidence;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ChangeProposalScorer {

    public BigDecimal score(
            BigDecimal benefitScore,
            RiskLevel riskLevel,
            SimulationConfidence simulationConfidence,
            NetworkKnowledgeConfidence knowledgeConfidence
    ) {
        int riskPenalty = switch (riskLevel) {
            case LOW -> 1;
            case MEDIUM -> 3;
            case HIGH -> 6;
            case CRITICAL -> 10;
        };
        int simulationPenalty = simulationConfidence == SimulationConfidence.LOW ? 2 : 0;
        int knowledgePenalty = knowledgeConfidence == NetworkKnowledgeConfidence.MEDIUM ? 2 : 0;
        return benefitScore.multiply(BigDecimal.TEN)
                .subtract(BigDecimal.valueOf(riskPenalty + simulationPenalty + knowledgePenalty))
                .setScale(6, RoundingMode.HALF_UP);
    }
}
