package com.simba.snip.npo.domain;

public final class DomainRules {

    private DomainRules() {
    }

    public static String requireDomainId(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            throw new DomainValidationException(field + " exceeds 64 characters");
        }
        return trimmed;
    }

    public static void requireDistinctCells(String sourceCellId, String targetCellId) {
        if (sourceCellId.equals(targetCellId)) {
            throw new DomainValidationException("Neighbour source and target must differ");
        }
    }
}
