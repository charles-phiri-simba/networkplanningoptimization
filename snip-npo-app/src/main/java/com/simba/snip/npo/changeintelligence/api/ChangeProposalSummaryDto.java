package com.simba.snip.npo.changeintelligence.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChangeProposalSummaryDto(
        UUID id,
        String proposalType,
        String status,
        String targetEntityType,
        String targetEntityId,
        String parameterName,
        String currentValue,
        String proposedValue,
        String unit,
        String networkKnowledgeConfidence,
        String assuranceConfidence,
        String simulationConfidence,
        String riskLevel,
        String benefitSummary,
        BigDecimal proposalScore,
        String failureCode,
        String failureReason,
        Instant createdAt,
        Instant evaluatedAt,
        Instant expiresAt,
        String invalidationReason,
        long version
) {
}
