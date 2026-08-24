package com.simba.snip.npo.assurance;

import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.AssuranceEvidenceEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssuranceCaseView(
        UUID id,
        String caseType,
        String affectedEntityType,
        String affectedEntityId,
        String severity,
        String confidence,
        String status,
        Instant detectedAt,
        Instant firstObservedAt,
        Instant lastObservedAt,
        String ruleId,
        boolean synthetic,
        List<EvidenceView> evidence
) {
    public static AssuranceCaseView from(AssuranceCaseEntity entity) {
        return new AssuranceCaseView(
                entity.getId(),
                entity.getCaseType(),
                entity.getAffectedEntityType(),
                entity.getAffectedEntityId(),
                entity.getSeverity(),
                entity.getConfidence(),
                entity.getStatus(),
                entity.getDetectedAt(),
                entity.getFirstObservedAt(),
                entity.getLastObservedAt(),
                entity.getRuleId(),
                entity.isSynthetic(),
                entity.getEvidence().stream().map(EvidenceView::from).toList()
        );
    }

    public record EvidenceView(
            UUID id,
            String evidenceType,
            String metric,
            Double value,
            String unit,
            String trend,
            Instant observedAt,
            String source,
            boolean synthetic,
            String description
    ) {
        static EvidenceView from(AssuranceEvidenceEntity entity) {
            return new EvidenceView(
                    entity.getId(),
                    entity.getEvidenceType(),
                    entity.getMetric(),
                    entity.getValue(),
                    entity.getUnit(),
                    entity.getTrend(),
                    entity.getObservedAt(),
                    entity.getSource(),
                    entity.isSynthetic(),
                    entity.getDescription()
            );
        }
    }
}
