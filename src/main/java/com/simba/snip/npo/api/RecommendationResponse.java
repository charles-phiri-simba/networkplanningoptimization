package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.List;

public record RecommendationResponse(
        String recommendation,
        List<CitationDto> citations,
        ContextUsedDto contextUsed,
        boolean retrievalEmpty,
        String retrievalMode,
        Long retrievalLatencyMs,
        Long generationLatencyMs,
        Long totalLatencyMs,
        Integer retrievalHitCount,
        ContextEvidenceDto contextEvidence,
        Long contextResolutionLatencyMs,
        String contextCellId,
        Boolean contextFound,
        Integer kpiObservationCount,
        Integer neighbourCount,
        Integer historyObservationCount,
        Instant lastEventTime
) {
}
