package com.simba.snip.npo.api;

public record CitationDto(
        String sourceId,
        String locator,
        String snippet,
        String chunkId,
        Double score
) {
}
