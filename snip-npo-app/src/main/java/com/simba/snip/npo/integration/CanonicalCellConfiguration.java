package com.simba.snip.npo.integration;

public record CanonicalCellConfiguration(
        String sourceEntityId,
        String sourceDn,
        String canonicalCellId,
        String parameterName,
        double txPowerDbm,
        String unit
) {
}
