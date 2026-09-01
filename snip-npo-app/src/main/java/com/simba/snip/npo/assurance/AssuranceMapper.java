package com.simba.snip.npo.assurance;

import com.simba.snip.npo.api.AssuranceCaseDto;
import com.simba.snip.npo.api.AssuranceEvidenceDto;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.AssuranceEvidenceEntity;

import java.util.List;

public final class AssuranceMapper {

    private AssuranceMapper() {
    }

    public static AssuranceCaseDto toDto(AssuranceCaseEntity entity) {
        return new AssuranceCaseDto(
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
                toEvidence(entity)
        );
    }

    public static List<AssuranceEvidenceDto> toEvidence(AssuranceCaseEntity entity) {
        return entity.getEvidence().stream().map(AssuranceMapper::toEvidence).toList();
    }

    public static AssuranceEvidenceDto toEvidence(AssuranceEvidenceEntity entity) {
        return new AssuranceEvidenceDto(
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
