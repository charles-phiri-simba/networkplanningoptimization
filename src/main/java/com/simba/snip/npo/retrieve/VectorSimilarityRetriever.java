package com.simba.snip.npo.retrieve;

import com.simba.snip.npo.config.SnipProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnBean(VectorStore.class)
@ConditionalOnProperty(name = "snip.retrieval-mode", havingValue = "vector")
public class VectorSimilarityRetriever implements ChunkRetriever {

    public static final String META_ID = "chunkId";
    public static final String META_SOURCE = "sourceId";
    public static final String META_LOCATOR = "locator";
    public static final String META_SNIPPET = "snippet";

    private final VectorStore vectorStore;
    private final SnipProperties properties;

    public VectorSimilarityRetriever(VectorStore vectorStore, SnipProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    @Override
    public String mode() {
        return "vector";
    }

    @Override
    public List<RetrievedChunk> retrieve(String question, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(properties.getRetrieveMinScore())
                .build();
        List<Document> hits = vectorStore.similaritySearch(request);
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }
        List<RetrievedChunk> result = new ArrayList<>();
        for (Document document : hits) {
            Map<String, Object> meta = document.getMetadata();
            String sourceId = stringMeta(meta, META_SOURCE);
            String locator = stringMeta(meta, META_LOCATOR);
            String snippet = stringMeta(meta, META_SNIPPET);
            String id = stringMeta(meta, META_ID);
            String text = document.getText();
            if (snippet == null || snippet.isBlank()) {
                snippet = text != null && text.length() > 180 ? text.substring(0, 177) + "..." : text;
            }
            Chunk chunk = new Chunk(
                    id != null ? id : sourceId + "::" + locator,
                    sourceId,
                    locator,
                    snippet,
                    text
            );
            Double score = document.getScore();
            result.add(new RetrievedChunk(chunk, score));
        }
        return result;
    }

    private static String stringMeta(Map<String, Object> meta, String key) {
        Object value = meta.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
