package com.simba.snip.npo.api;

import java.time.Instant;

public record KpiObservationDto(
        String metric,
        Double value,
        String unit,
        Instant observedAt,
        Instant eventTime,
        Instant ingestedAt,
        String eventId,
        String source,
        boolean synthetic
) {
}
