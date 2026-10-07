package com.simba.snip.npo.demo;

import java.util.List;

public final class DemoInventory {

    public static final List<String> SITE_IDS = List.of(
            "SITE-001", "SITE-002", "SITE-003", "SITE-004", "SITE-005", "SITE-006");
    public static final List<String> GNB_IDS = List.of(
            "GNB-001", "GNB-002", "GNB-003", "GNB-004", "GNB-005", "GNB-006");
    public static final List<String> CELL_IDS = List.of(
            "CELL-001", "CELL-002", "CELL-003", "CELL-004", "CELL-005", "CELL-006",
            "CELL-007", "CELL-008", "CELL-009", "CELL-010", "CELL-011", "CELL-012",
            "CELL-013", "CELL-014", "CELL-015", "CELL-016", "CELL-017", "CELL-018");
    public static final String FEATURED_CELL = "CELL-001";
    public static final String FEATURED_TX_POWER = "46";
    public static final String KNOWLEDGE_SOURCE = "ERICSSON_ENM_SIMULATOR";
    public static final String KNOWLEDGE_SCOPE = "DEFAULT";

    private DemoInventory() {
    }
}
