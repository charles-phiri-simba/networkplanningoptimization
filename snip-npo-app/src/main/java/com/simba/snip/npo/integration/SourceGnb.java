package com.simba.snip.npo.integration;

public record SourceGnb(
        String sourceEntityId,
        String sourceDn,
        String canonicalGnbId,
        String canonicalSiteId,
        String name,
        String equipmentVendor,
        String model,
        String operationalStateRaw
) {
}
