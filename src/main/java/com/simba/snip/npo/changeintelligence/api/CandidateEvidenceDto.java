package com.simba.snip.npo.changeintelligence.api;

import java.math.BigDecimal;
import java.util.UUID;

public record CandidateEvidenceDto(
        String candidateValue,
        boolean baselineCandidate,
        String validationOutcome,
        String validationReason,
        UUID simulationRunId,
        String simulationConfidence,
        BigDecimal benefitScore,
        String riskLevel,
        BigDecimal proposalScore,
        Integer rankOrder
) {
}
