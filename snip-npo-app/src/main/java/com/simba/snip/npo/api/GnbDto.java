package com.simba.snip.npo.api;

public record GnbDto(
        String gnbId,
        String name,
        String siteId,
        String vendor,
        String model,
        String status
) {
}
