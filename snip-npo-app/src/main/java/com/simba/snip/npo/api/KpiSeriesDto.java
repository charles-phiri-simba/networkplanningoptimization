package com.simba.snip.npo.api;

import java.util.List;

public record KpiSeriesDto(
        String metric,
        KpiObservationDto current,
        List<KpiObservationDto> history,
        String trend
) {
}
