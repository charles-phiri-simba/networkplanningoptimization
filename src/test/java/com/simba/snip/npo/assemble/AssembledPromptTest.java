package com.simba.snip.npo.assemble;

import com.simba.snip.npo.assurance.AssuranceCaseView;
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

    @Test
    void renderIncludesAssuranceCaseAndOperationalEvidence() {
        Instant t = Instant.parse("2026-08-24T10:15:00Z");
        AssuranceCaseView.EvidenceView evidence = new AssuranceCaseView.EvidenceView(
                java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "THRESHOLD", "BLER_DL", 0.12, "ratio", "INCREASING", t, "SNIP_SIMULATOR", true,
                "BLER_DL crossed warning threshold");
        AssuranceCaseView assurance = new AssuranceCaseView(
                java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "DEGRADING_RADIO_QUALITY", "CELL", "CELL-001", "CRITICAL", "HIGH", "OPEN",
                t, t, t, "RULE_DEGRADING_RADIO_QUALITY_BLER_DL_V1", true, List.of(evidence));
        String prompt = new AssembledPrompt(
                "Why has SNIP raised a DEGRADING_RADIO_QUALITY assurance case for CELL-001, and what should I investigate first?",
                Optional.empty(),
                Optional.of(sampleContext()),
                List.of(new Chunk("sample-bler-midband::section-1#0", "sample-bler-midband", "section-1#0", "check BLER", "check BLER")),
                Optional.of(assurance)
        ).render();
        assertTrue(prompt.contains("ASSURANCE CASE"));
        assertTrue(prompt.contains("OPERATIONAL EVIDENCE"));
        assertTrue(prompt.contains("DEGRADING_RADIO_QUALITY"));
        assertTrue(prompt.contains("severity=CRITICAL"));
        assertTrue(prompt.contains("do not override them"));
        assertTrue(prompt.contains("Human review is required"));
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
