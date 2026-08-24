package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssuranceCaseDto(
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
        List<AssuranceEvidenceDto> evidence
) {
}
