package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class NetworkKnowledgeConfidenceEvaluator {

    public Evaluation evaluate(
            SynchronizationFreshness freshness,
            SynchronizationSourceHealth sourceHealth,
            boolean recoveryRequired,
            Optional<SynchronizationCheckpointEntity> checkpoint
    ) {
        List<KnowledgeConfidenceReason> reasons = new ArrayList<>();
        if (checkpoint.isEmpty() || checkpoint.get().getLastSuccessfulCompletedAt() == null) {
            reasons.add(KnowledgeConfidenceReason.NO_TRUSTED_BASELINE);
            return new Evaluation(NetworkKnowledgeConfidence.UNKNOWN, reasons);
        }
        if (recoveryRequired) {
            reasons.add(KnowledgeConfidenceReason.RECOVERY_REQUIRED);
            return new Evaluation(NetworkKnowledgeConfidence.LOW, reasons);
        }
        if (freshness == SynchronizationFreshness.STALE) {
            reasons.add(KnowledgeConfidenceReason.STALE_TRUSTED_STATE);
            return new Evaluation(NetworkKnowledgeConfidence.LOW, reasons);
        }
        if (freshness == SynchronizationFreshness.DEGRADED
                || sourceHealth == SynchronizationSourceHealth.DEGRADED
                || sourceHealth == SynchronizationSourceHealth.UNREACHABLE) {
            reasons.add(KnowledgeConfidenceReason.SOURCE_DEGRADED);
            return new Evaluation(NetworkKnowledgeConfidence.MEDIUM, reasons);
        }
        if (freshness == SynchronizationFreshness.AGING) {
            reasons.add(KnowledgeConfidenceReason.AGING_TRUSTED_STATE);
            return new Evaluation(NetworkKnowledgeConfidence.MEDIUM, reasons);
        }
        reasons.add(KnowledgeConfidenceReason.TRUSTED_FRESH_COMPLETE);
        return new Evaluation(NetworkKnowledgeConfidence.HIGH, reasons);
    }

    public record Evaluation(NetworkKnowledgeConfidence confidence, List<KnowledgeConfidenceReason> reasons) {
        public String reasonCodes() {
            return reasons.stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse("");
        }
    }
}
