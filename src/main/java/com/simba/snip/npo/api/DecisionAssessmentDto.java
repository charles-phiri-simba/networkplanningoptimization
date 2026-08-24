package com.simba.snip.npo.api;

import java.util.List;
import java.util.UUID;

public record DecisionAssessmentDto(
        UUID assuranceCaseId,
        String summary,
        List<String> likelyContributors,
        List<String> recommendedChecks,
        List<String> missingEvidence,
        String urgency,
        boolean humanReviewRequired,
        String severity,
        String confidence,
        String caseType,
        String status,
        List<AssuranceEvidenceDto> operationalEvidence,
        List<CitationDto> citations,
        boolean retrievalEmpty,
        String retrievalMode,
        long retrievalLatencyMs,
        long generationLatencyMs,
        long totalLatencyMs
) {
}
