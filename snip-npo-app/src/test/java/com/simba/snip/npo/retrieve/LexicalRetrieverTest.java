package com.simba.snip.npo.retrieve;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LexicalRetrieverTest {

    @Test
    void prefersBlerChunkOverUnrelatedCoreNote() {
        InMemoryChunkStore store = new InMemoryChunkStore();
        store.replaceAll(List.of(
                Chunk.of("sample-bler-midband", "section-1#0", "BLER mid-band",
                        "When BLER is high on a mid-band cell check interference"),
                Chunk.of("sample-core-registration", "section-9#0", "AMF registration",
                        "UE registration and authentication with AMF")
        ));
        LexicalRetriever retriever = new LexicalRetriever(store);

        List<RetrievedChunk> hits = retriever.retrieve(
                "What should I check if BLER is high on a mid-band cell?", 3);

        assertEquals("lexical", retriever.mode());
        assertFalse(hits.isEmpty(), "BLER question should retrieve at least one chunk");
        assertEquals("sample-bler-midband", hits.get(0).chunk().sourceId());
        int coreRank = -1;
        for (int i = 0; i < hits.size(); i++) {
            if ("sample-core-registration".equals(hits.get(i).chunk().sourceId())) {
                coreRank = i;
            }
        }
        assertFalse(coreRank == 0, "unrelated core note must not rank first");
    }
}
