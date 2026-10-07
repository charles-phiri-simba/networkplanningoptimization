package com.simba.snip.npo.planning.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanningEvaluationDto(
        UUID id,
        UUID scenarioId,
        String status,
        String intentFingerprint,
        String admissionFingerprint,
        String createdBy,
        Instant createdAt,
        Instant completedAt,
        int itemCount,
        int succeededCount,
        int failedCount,
        boolean historical,
        List<PlanningEvaluationItemDto> items,
        PlanningTruthDto truth
) {
}
