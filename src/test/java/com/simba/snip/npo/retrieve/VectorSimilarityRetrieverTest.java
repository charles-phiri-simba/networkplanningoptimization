package com.simba.snip.npo.retrieve;

import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.ingest.CorpusIngestor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorSimilarityRetrieverTest {

    @Test
    void semanticSearchReturnsBlerChunkWithProvenanceAndScore() {
        DeterministicEmbeddingModel embeddingModel = new DeterministicEmbeddingModel();
        VectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        Chunk bler = Chunk.of("sample-bler-midband", "section-1#0", "BLER checks",
                "When BLER is high on a mid-band cell check interference and neighbours");
        Chunk core = Chunk.of("sample-core-registration", "section-9#0", "AMF",
                "UE registration and authentication with AMF and AUSF");
        store.add(List.of(CorpusIngestor.toDocument(bler), CorpusIngestor.toDocument(core)));

        SnipProperties properties = new SnipProperties();
        properties.setRetrieveMinScore(0.05);
        VectorSimilarityRetriever retriever = new VectorSimilarityRetriever(store, properties);

        List<RetrievedChunk> hits = retriever.retrieve(
                "What should I check if BLER is high on a mid-band cell?", 3);

        assertEquals("vector", retriever.mode());
        assertFalse(hits.isEmpty());
        assertEquals("sample-bler-midband", hits.get(0).chunk().sourceId());
        assertEquals("section-1#0", hits.get(0).chunk().locator());
        assertTrue(hits.get(0).chunk().id().contains("sample-bler-midband"));
        assertTrue(hits.get(0).score() == null || hits.get(0).score() > 0);
    }

    @Test
    void unsupportedQuestionYieldsNoHitsAboveThreshold() {
        DeterministicEmbeddingModel embeddingModel = new DeterministicEmbeddingModel();
        VectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        Chunk bler = Chunk.of("sample-bler-midband", "section-1#0", "BLER checks",
                "When BLER is high on a mid-band cell check interference");
        store.add(List.of(CorpusIngestor.toDocument(bler)));

        SnipProperties properties = new SnipProperties();
        properties.setRetrieveMinScore(0.99);
        VectorSimilarityRetriever retriever = new VectorSimilarityRetriever(store, properties);

        List<RetrievedChunk> hits = retriever.retrieve("How do I bake sourdough bread at high altitude?", 3);

        assertTrue(hits.isEmpty());
    }

    @Test
    void documentMetadataRoundTrip() {
        Chunk chunk = Chunk.of("sample-interference", "section-3#1", "snippet text", "full text");
        Document document = CorpusIngestor.toDocument(chunk);
        assertEquals(chunk.id(), document.getId());
        assertEquals("sample-interference", document.getMetadata().get(VectorSimilarityRetriever.META_SOURCE));
        assertEquals("section-3#1", document.getMetadata().get(VectorSimilarityRetriever.META_LOCATOR));
        assertEquals("snippet text", document.getMetadata().get(VectorSimilarityRetriever.META_SNIPPET));
    }
}
