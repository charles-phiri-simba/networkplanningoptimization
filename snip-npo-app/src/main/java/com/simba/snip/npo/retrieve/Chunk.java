package com.simba.snip.npo.retrieve;

public record Chunk(
        String id,
        String sourceId,
        String locator,
        String snippet,
        String text
) {
    public static Chunk of(String sourceId, String locator, String snippet, String text) {
        return new Chunk(sourceId + "::" + locator, sourceId, locator, snippet, text);
    }
}
