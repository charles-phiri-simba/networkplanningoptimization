package com.simba.snip.npo.integration;

public record SourceSite(
        String sourceEntityId,
        String sourceDn,
        String canonicalSiteId,
        String name,
        Double latitude,
        Double longitude,
        String operationalStateRaw
) {
}
