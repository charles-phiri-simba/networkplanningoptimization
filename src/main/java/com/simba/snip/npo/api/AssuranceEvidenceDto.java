package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record AssuranceEvidenceDto(
        UUID id,
        String evidenceType,
        String metric,
        Double value,
        String unit,
        String trend,
        Instant observedAt,
        String source,
        boolean synthetic,
        String description
) {
}
