package com.simba.snip.npo.integration;

public record SourceConfiguration(
        String sourceEntityId,
        String sourceDn,
        String canonicalCellId,
        String parameterName,
        Double sourceValue,
        PowerUnit sourceUnit
) {
}
