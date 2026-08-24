package com.simba.snip.npo.assemble;

import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.retrieve.Chunk;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssembledPromptTest {

    @Test
    void renderDistinguishesStructuredContextFromRetrievedKnowledge() {
        CellContext ctx = new CellContext(
                new CellContext.CellView("CELL-001", "demo", "NR", "n78", 1, 12, 40, "TDD", "ACTIVE"),
                new CellContext.GnbView("GNB-001", "g", "v", "m", "ACTIVE"),
                new CellContext.SiteView("SITE-001", "s", 0.0, 0.0, "ACTIVE"),
                List.of(new CellContext.RadioParameterView("txPower", "46", "dBm", Instant.parse("2026-08-01T00:00:00Z"))),
                List.of(new CellContext.KpiObservationView("BLER_DL", 0.12, "ratio", Instant.parse("2026-08-24T00:00:00Z"), "DEMO_SEED", true)),
                List.of(new CellContext.NeighbourView("CELL-002", "INTRA_FREQUENCY", "ACTIVE")),
                new CellContext.ContextProvenance("DEMO_SEED", true)
        );
        Chunk chunk = new Chunk("sample-bler-midband::section-1#0", "sample-bler-midband", "section-1#0", "check BLER", "check BLER");
        String prompt = new AssembledPrompt("Why might BLER be high on CELL-001?", Optional.empty(), Optional.of(ctx), List.of(chunk)).render();
        assertTrue(prompt.contains("STRUCTURED NETWORK CONTEXT"));
        assertTrue(prompt.contains("RETRIEVED ENGINEERING KNOWLEDGE"));
        assertTrue(prompt.contains("USER QUESTION"));
        assertTrue(prompt.contains("SAFETY / BEHAVIOURAL INSTRUCTIONS"));
        assertTrue(prompt.contains("CELL-001"));
        assertTrue(prompt.contains("BLER_DL: 0.12 ratio (12%)"));
        assertTrue(prompt.contains("sample-bler-midband"));
        assertTrue(prompt.contains("SYNTHETIC"));
    }
}
