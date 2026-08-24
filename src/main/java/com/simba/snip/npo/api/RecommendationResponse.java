package com.simba.snip.npo.api;

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
        Integer retrievalHitCount
) {
}
