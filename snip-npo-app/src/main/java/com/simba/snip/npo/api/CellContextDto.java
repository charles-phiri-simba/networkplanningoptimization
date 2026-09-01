package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.List;

public record CellContextDto(
        CellDto cell,
        GnbDto gnb,
        SiteDto site,
        List<RadioParameterDto> radioConfiguration,
        List<KpiObservationDto> kpis,
        List<NeighbourDto> neighbours,
        List<KpiSeriesDto> telemetry,
        ContextProvenanceDto provenance
) {
    public record RadioParameterDto(
            String parameterName,
            String parameterValue,
            String unit,
            Instant effectiveFrom
    ) {
    }

    public record ContextProvenanceDto(
            String source,
            boolean synthetic
    ) {
    }
}
