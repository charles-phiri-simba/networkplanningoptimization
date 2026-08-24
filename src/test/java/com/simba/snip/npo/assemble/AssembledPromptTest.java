package com.simba.snip.npo.assemble;

import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.retrieve.Chunk;
import com.simba.snip.npo.telemetry.Trend;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssembledPromptTest {

    @Test
    void renderDistinguishesStructuredContextFromRetrievedKnowledge() {
        String prompt = new AssembledPrompt(
                "Why might BLER be high on CELL-001?",
                Optional.empty(),
                Optional.of(sampleContext()),
                List.of(new Chunk("sample-bler-midband::section-1#0", "sample-bler-midband", "section-1#0", "check BLER", "check BLER"))
        ).render();
        assertTrue(prompt.contains("STRUCTURED NETWORK CONTEXT"));
        assertTrue(prompt.contains("RETRIEVED ENGINEERING KNOWLEDGE"));
        assertTrue(prompt.contains("USER QUESTION"));
        assertTrue(prompt.contains("SAFETY / BEHAVIOURAL INSTRUCTIONS"));
        assertTrue(prompt.contains("CELL-001"));
        assertTrue(prompt.contains("BLER_DL: 0.12 ratio (12%)"));
        assertTrue(prompt.contains("sample-bler-midband"));
        assertTrue(prompt.contains("SYNTHETIC"));
    }

    @Test
    void renderIncludesPrecomputedTemporalTrends() {
        String prompt = new AssembledPrompt(
                "What is happening on CELL-001, and what should I investigate?",
                Optional.empty(),
                Optional.of(sampleContext()),
                List.of(new Chunk("sample-bler-midband::section-1#0", "sample-bler-midband", "section-1#0", "check BLER", "check BLER"))
        ).render();
        assertTrue(prompt.contains("TEMPORAL KPI HISTORY / TRENDS"));
        assertTrue(prompt.contains("trend: INCREASING"));
        assertTrue(prompt.contains("TREND values are precomputed"));
        assertTrue(prompt.contains("eventId=high-bler-load-bler-dl-04"));
    }

    private static CellContext sampleContext() {
        Instant t1 = Instant.parse("2026-08-24T10:00:00Z");
        Instant t2 = Instant.parse("2026-08-24T10:15:00Z");
        CellContext.KpiObservationView previous = new CellContext.KpiObservationView(
                "BLER_DL", 0.09, "ratio", t1, t1, "high-bler-load-bler-dl-03", "SNIP_SIMULATOR", true);
        CellContext.KpiObservationView current = new CellContext.KpiObservationView(
                "BLER_DL", 0.12, "ratio", t2, t2, "high-bler-load-bler-dl-04", "SNIP_SIMULATOR", true);
        return new CellContext(
                new CellContext.CellView("CELL-001", "demo", "NR", "n78", 1, 12, 40, "TDD", "ACTIVE"),
                new CellContext.GnbView("GNB-001", "g", "v", "m", "ACTIVE"),
                new CellContext.SiteView("SITE-001", "s", 0.0, 0.0, "ACTIVE"),
                List.of(new CellContext.RadioParameterView("txPower", "46", "dBm", Instant.parse("2026-08-01T00:00:00Z"))),
                List.of(current),
                List.of(new CellContext.NeighbourView("CELL-002", "INTRA_FREQUENCY", "ACTIVE")),
                List.of(new CellContext.KpiSeriesView("BLER_DL", current, List.of(previous, current), Trend.INCREASING)),
                new CellContext.ContextProvenance("SNIP_SIMULATOR", true)
        );
    }
}
