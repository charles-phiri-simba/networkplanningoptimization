package com.simba.snip.npo.integration;

import java.util.Locale;
import java.util.Optional;

public final class CanonicalEnumerationNormalizer {

    private CanonicalEnumerationNormalizer() {
    }

    public static Optional<String> technology(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace('_', '-');
        return switch (key) {
            case "NR", "5G-NR", "5GNR", "NR-5G" -> Optional.of("NR");
            case "LTE", "4G-LTE", "4GLTE", "EUTRAN", "E-UTRAN" -> Optional.of("LTE");
            default -> Optional.empty();
        };
    }

    public static Optional<String> duplex(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "TDD" -> Optional.of("TDD");
            case "FDD" -> Optional.of("FDD");
            default -> Optional.empty();
        };
    }

    public static Optional<String> operationalStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of("ACTIVE");
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE", "UNLOCKED", "ENABLED", "ON", "WORKING" -> Optional.of("ACTIVE");
            case "INACTIVE", "LOCKED", "DISABLED", "OFF" -> Optional.of("INACTIVE");
            default -> Optional.empty();
        };
    }
}
