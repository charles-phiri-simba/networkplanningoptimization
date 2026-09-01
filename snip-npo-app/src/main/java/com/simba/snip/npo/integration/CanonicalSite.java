package com.simba.snip.npo.integration;

public record CanonicalSite(
        String sourceEntityId,
        String sourceDn,
        String canonicalSiteId,
        String name,
        Double latitude,
        Double longitude,
        String status
) {
}
