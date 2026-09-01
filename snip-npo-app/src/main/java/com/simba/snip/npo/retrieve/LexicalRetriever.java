package com.simba.snip.npo.retrieve;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic lexical retrieval. Kept as CI/fallback; not the Phase 1A.1 semantic path.
 */
@Component
@ConditionalOnProperty(name = "snip.retrieval-mode", havingValue = "lexical", matchIfMissing = true)
public class LexicalRetriever implements ChunkRetriever {

    private static final Set<String> STOP = Set.of(
            "a", "an", "the", "and", "or", "if", "on", "of", "to", "for", "in", "is", "it",
            "what", "should", "i", "check", "at", "how", "do", "does", "from", "that", "this",
            "with", "as", "be", "by", "not", "only", "use", "when", "then"
    );

    private final ChunkStore store;

    public LexicalRetriever(ChunkStore store) {
        this.store = store;
    }

    @Override
    public String mode() {
        return "lexical";
    }

    @Override
    public List<RetrievedChunk> retrieve(String question, int topK) {
        Set<String> queryTerms = terms(question);
        if (queryTerms.isEmpty()) {
            return List.of();
        }
        return store.all().stream()
                .map(chunk -> new RetrievedChunk(chunk, score(queryTerms, chunk)))
                .filter(scored -> scored.score() >= 2)
                .sorted(Comparator.comparingDouble((RetrievedChunk s) -> s.score()).reversed())
                .limit(topK)
                .toList();
    }

    static Set<String> terms(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() > 1 && !STOP.contains(token))
                .collect(Collectors.toSet());
    }

    private static double score(Set<String> queryTerms, Chunk chunk) {
        Set<String> chunkTerms = terms(chunk.text() + " " + chunk.sourceId());
        return queryTerms.stream().filter(chunkTerms::contains).count();
    }
}
