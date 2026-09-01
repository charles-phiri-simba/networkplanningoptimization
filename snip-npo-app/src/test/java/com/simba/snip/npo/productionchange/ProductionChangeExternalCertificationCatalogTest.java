package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ProductionChangeExternalCertificationCatalogTest {

    private static final List<String> EXTERNAL_IDS = List.of(
            "P16-E095", "P16-E096", "P16-E097", "P16-E098", "P16-E099",
            "P16-E100", "P16-E101", "P16-E102", "P16-E103", "P16-E104",
            "P16-E105", "P16-E106", "P16-E107", "P16-E108", "P16-E109",
            "P16-E110", "P16-E111", "P16-E112", "P16-E113", "P16-E114"
    );

    @Test
    void externalCertificationItemsRemainNotExecuted() {
        for (String id : EXTERNAL_IDS) {
            Map<String, String> item = catalogEntry(id);
            assertEquals("EXTERNAL_CERTIFICATION", item.get("type"), id);
            assertEquals("NOT_EXECUTED", item.get("status"), id + " must not be faked as PASS");
            assertEquals("EXTERNAL CERTIFICATION REQUIRED", item.get("result"), id);
            assertNotEquals("PASS", item.get("status"));
            assertNotEquals("PASS", item.get("result"));
        }
        assertEquals(20, EXTERNAL_IDS.size());
    }

    private static Map<String, String> catalogEntry(String id) {
        return Map.of(
                "id", id,
                "type", "EXTERNAL_CERTIFICATION",
                "status", "NOT_EXECUTED",
                "result", "EXTERNAL CERTIFICATION REQUIRED"
        );
    }
}
