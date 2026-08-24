package com.simba.snip.npo.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KpiObservationFormatTest {

    @Test
    void ratioZeroPointTwelveIsTwelvePercent() {
        assertEquals("BLER_DL: 0.12 ratio (12%)", KpiObservationFormat.format("BLER_DL", 0.12, "ratio"));
        assertEquals("12%", KpiObservationFormat.percentFromRatio(0.12));
    }

    @Test
    void ratioZeroPointZeroThreeIsThreePercent() {
        assertEquals("BLER_UL: 0.03 ratio (3%)", KpiObservationFormat.format("BLER_UL", 0.03, "ratio"));
        assertEquals("3%", KpiObservationFormat.percentFromRatio(0.03));
    }

    @Test
    void nonRatioUnitsAreNotConverted() {
        assertEquals("THROUGHPUT_DL: 42.0 Mbps", KpiObservationFormat.format("THROUGHPUT_DL", 42.0, "Mbps"));
        assertEquals("LATENCY: 28.0 ms", KpiObservationFormat.format("LATENCY", 28.0, "ms"));
    }

    @Test
    void formattedContextKeepsRawRatioAndAddsPercent() {
        CellContext.KpiObservationView view = new CellContext.KpiObservationView(
                "BLER_DL", 0.12, "ratio", null, null, "seed-demo", "DEMO_SEED", true);
        assertEquals(0.12, view.value());
        assertEquals("ratio", view.unit());
        assertEquals("BLER_DL: 0.12 ratio (12%)", view.formatted());
        assertFalse(view.formatted().contains("0.12%"));
    }
}
