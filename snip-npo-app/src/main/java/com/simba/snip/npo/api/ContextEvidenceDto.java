package com.simba.snip.npo.api;

public record ContextEvidenceDto(
        String cellId,
        String gnbId,
        String siteId,
        String source,
        boolean synthetic
) {
}
