package com.simba.snip.npo.generate;

import com.simba.snip.npo.assemble.AssembledPrompt;
import com.simba.snip.npo.retrieve.Chunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Deterministic generator for CI and local default. Does not call an LLM.
 */
@Component
@ConditionalOnProperty(name = "snip.generator", havingValue = "stub", matchIfMissing = true)
public class StubRecommendationGenerator implements RecommendationGenerator {

    @Override
    public String generate(AssembledPrompt prompt, List<Chunk> chunks) {
        String joined = chunks.stream()
                .map(chunk -> chunk.text().replaceAll("\\s+", " ").trim())
                .collect(Collectors.joining(" "));
        String sources = chunks.stream()
                .map(Chunk::sourceId)
                .distinct()
                .collect(Collectors.joining(", "));
        StringBuilder sb = new StringBuilder();
        sb.append("Cited engineering recommendation (read-only). ");
        sb.append("Do not change the live network from this answer. ");
        prompt.kpi().ifPresent(kpi -> sb.append("Synthetic context ")
                .append(kpi.id())
                .append(" shows BLER ")
                .append(kpi.bler())
                .append(" on a ")
                .append(kpi.band())
                .append("-band cell. "));
        sb.append("From retrieved notes (").append(sources).append("): ").append(joined);
        return sb.toString();
    }
}
