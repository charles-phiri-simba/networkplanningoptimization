package com.simba.snip.npo.ingest;

import com.simba.snip.npo.retrieve.Chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Splits a markdown sample note into retrievable chunks and maps citation metadata.
 */
public class DocumentChunker {

    private static final Pattern SOURCE_ID = Pattern.compile("(?im)^Source-id:\\s*(.+)$");
    private static final Pattern LOCATOR = Pattern.compile("(?im)^Locator:\\s*(.+)$");
    private static final int MAX_CHARS = 700;

    public List<Chunk> chunk(String filename, String content) {
        String sourceId = firstGroup(SOURCE_ID, content, filename.replaceAll("\\.md$", ""));
        String locator = firstGroup(LOCATOR, content, "body");
        List<String> parts = splitParagraphs(content);
        List<Chunk> chunks = new ArrayList<>();
        int index = 0;
        for (String part : parts) {
            String text = stripMetadataLines(part.trim());
            if (text.isEmpty()) {
                continue;
            }
            String snippet = text.length() > 180 ? text.substring(0, 177) + "..." : text;
            String chunkLocator = locator + "#" + index;
            chunks.add(Chunk.of(sourceId, chunkLocator, snippet, text));
            index++;
        }
        if (chunks.isEmpty() && content != null && !content.isBlank()) {
            String cleaned = stripMetadataLines(content.trim());
            chunks.add(Chunk.of(sourceId, locator, clip(cleaned, 180), cleaned));
        }
        return chunks;
    }

    static String stripMetadataLines(String text) {
        return Arrays.stream(text.replace("\r\n", "\n").split("\n"))
                .filter(line -> {
                    String trimmed = line.trim().toLowerCase(Locale.ROOT);
                    return !trimmed.startsWith("source-id:") && !trimmed.startsWith("locator:");
                })
                .collect(Collectors.joining("\n"))
                .trim();
    }

    static List<String> splitParagraphs(String content) {
        String[] raw = content.replace("\r\n", "\n").split("\n\\s*\n");
        List<String> parts = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String block : raw) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (buf.length() + trimmed.length() > MAX_CHARS && buf.length() > 0) {
                parts.add(buf.toString());
                buf.setLength(0);
            }
            if (buf.length() > 0) {
                buf.append("\n\n");
            }
            buf.append(trimmed);
        }
        if (buf.length() > 0) {
            parts.add(buf.toString());
        }
        return parts;
    }

    private static String firstGroup(Pattern pattern, String content, String fallback) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return fallback.toLowerCase(Locale.ROOT);
    }

    private static String clip(String text, int max) {
        String flat = text.replaceAll("\\s+", " ").trim();
        if (flat.isEmpty()) {
            return "";
        }
        return flat.length() > max ? flat.substring(0, max - 3) + "..." : flat;
    }
}
