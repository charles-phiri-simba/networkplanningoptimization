package com.simba.snip.npo.ingest;

import com.simba.snip.npo.retrieve.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentChunkerTest {

    @Test
    void mapsSourceIdAndLocatorOntoChunks() {
        String content = """
                # Title
                
                Source-id: sample-bler-midband
                Locator: section-1
                
                When BLER is high, confirm the measurement window.
                
                Then inspect neighbouring mid-band cells.
                """;

        List<Chunk> chunks = new DocumentChunker().chunk("bler-midband-checks.md", content);

        assertFalse(chunks.isEmpty());
        assertEquals("sample-bler-midband", chunks.get(0).sourceId());
        assertEquals("section-1#0", chunks.get(0).locator());
        assertFalse(chunks.get(0).snippet().isBlank());
        assertFalse(chunks.get(0).snippet().toLowerCase().contains("source-id:"));
        assertFalse(chunks.get(0).text().toLowerCase().contains("source-id:"));
        assertTrue(chunks.get(0).id().startsWith("sample-bler-midband::"));
    }
}
