package com.simba.snip.npo.changeplanning.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChangePlanDetailDto(
        ChangePlanSummaryDto plan,
        String fingerprint,
        String authorizedFingerprint,
        String knowledgeConfidenceAtCreation,
        String riskLevel,
        String reviewedBy,
        Instant reviewedAt,
        String authorizedBy,
        Instant authorizedAt,
        String cancelledBy,
        Instant cancelledAt,
        String invalidationReason,
        Instant invalidatedAt,
        List<ChangePlanOperationDto> operations,
        List<ChangePlanRollbackDto> rollbackOperations,
        List<ChangePlanPreconditionDto> preconditions,
        List<ChangePlanReadinessDto> readinessAssessments
) {
}
