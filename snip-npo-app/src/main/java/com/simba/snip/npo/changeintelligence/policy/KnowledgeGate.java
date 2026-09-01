package com.simba.snip.npo.changeintelligence.policy;

import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeGate {

    public record GateResult(
            boolean allowsRecommendation,
            boolean allowsEvaluation,
            boolean degraded,
            ChangeProposalFailureCode blockCode,
            String reasonCode
    ) {
        public static GateResult open(NetworkKnowledgeConfidence confidence) {
            return switch (confidence) {
                case HIGH -> new GateResult(true, true, false, null, null);
                case MEDIUM -> new GateResult(true, true, true, null, "KNOWLEDGE_MEDIUM_DEGRADED");
                case LOW -> new GateResult(false, true, false, ChangeProposalFailureCode.NETWORK_KNOWLEDGE_LOW, "NETWORK_KNOWLEDGE_LOW");
                case UNKNOWN -> new GateResult(false, true, false, ChangeProposalFailureCode.NETWORK_KNOWLEDGE_UNKNOWN, "NETWORK_KNOWLEDGE_UNKNOWN");
            };
        }
    }

    public GateResult evaluate(NetworkKnowledgeConfidence confidence) {
        return GateResult.open(confidence);
    }

    public GateResult evaluate(String confidenceName) {
        try {
            return evaluate(NetworkKnowledgeConfidence.valueOf(confidenceName));
        } catch (IllegalArgumentException ex) {
            return GateResult.open(NetworkKnowledgeConfidence.UNKNOWN);
        }
    }

    public boolean blocksApproval(NetworkKnowledgeConfidence confidence) {
        return confidence == NetworkKnowledgeConfidence.LOW || confidence == NetworkKnowledgeConfidence.UNKNOWN;
    }
}
