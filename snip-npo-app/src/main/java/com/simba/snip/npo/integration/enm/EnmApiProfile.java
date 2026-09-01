package com.simba.snip.npo.integration.enm;

import java.util.Set;

public record EnmApiProfile(
        String profileId,
        Set<String> inventoryObjectCategories,
        String paginationMode,
        Set<String> capabilities,
        String sourceVersion,
        String mappingStrategy
) {
    public static EnmApiProfile simulatorV1() {
        return new EnmApiProfile(
                "ENM_SIMULATOR_V1",
                Set.of("ManagedElement", "RadioFunction", "Cell"),
                "CONTINUATION_TOKEN",
                Set.of("INVENTORY_READ", "CONFIGURATION_READ", "PAGINATION", "SOURCE_VERSION"),
                "sim-1",
                "MANAGED_ELEMENT_SITE_RADIO_GNB_CELL"
        );
    }
}
