package com.simba.snip.npo.productionchange.protocol;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Canonical JSON for Phase 16 fingerprints and audit hashing:
 * UTF-8, lexicographically sorted keys, omit nulls, no insignificant whitespace,
 * timestamps as UTC millis ISO-8601, numbers as plain JSON numbers, enums as uppercase names.
 */
public final class CanonicalJson {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private CanonicalJson() {
    }

    public static String serialize(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                sorted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, entry.getKey());
                sb.append(':');
                write(sb, entry.getValue());
            }
            sb.append('}');
            return;
        }
        if (value instanceof Collection<?> collection) {
            sb.append('[');
            boolean first = true;
            for (Object item : collection) {
                if (item == null) {
                    continue;
                }
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, item);
            }
            sb.append(']');
            return;
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            List<Object> list = new ArrayList<>();
            for (Object item : array) {
                list.add(item);
            }
            write(sb, list);
            return;
        }
        if (value instanceof Instant instant) {
            writeString(sb, TIMESTAMP.format(instant.truncatedTo(ChronoUnit.MILLIS)));
            return;
        }
        if (value instanceof UUID uuid) {
            writeString(sb, uuid.toString());
            return;
        }
        if (value instanceof Enum<?> enumeration) {
            writeString(sb, enumeration.name());
            return;
        }
        if (value instanceof Boolean bool) {
            sb.append(bool ? "true" : "false");
            return;
        }
        if (value instanceof BigDecimal decimal) {
            sb.append(decimal.stripTrailingZeros().toPlainString());
            return;
        }
        if (value instanceof Number number) {
            if (number instanceof Double || number instanceof Float) {
                sb.append(BigDecimal.valueOf(number.doubleValue()).stripTrailingZeros().toPlainString());
            } else {
                sb.append(number.toString());
            }
            return;
        }
        writeString(sb, String.valueOf(value));
    }

    private static void writeString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
