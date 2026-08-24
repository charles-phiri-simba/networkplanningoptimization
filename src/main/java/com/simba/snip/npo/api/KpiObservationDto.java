package com.simba.snip.npo.api;

import java.time.Instant;

public record KpiObservationDto(
        String metric,
        Double value,
        String unit,
        Instant observedAt,
        String source,
        boolean synthetic
) {
}
