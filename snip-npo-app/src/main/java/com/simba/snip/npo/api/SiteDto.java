package com.simba.snip.npo.api;

public record SiteDto(
        String siteId,
        String name,
        Double latitude,
        Double longitude,
        String status
) {
}
