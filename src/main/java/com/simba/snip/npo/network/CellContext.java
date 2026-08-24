package com.simba.snip.npo.network;

import java.time.Instant;
import java.util.List;

public record CellContext(
        CellView cell,
        GnbView gnb,
        SiteView site,
        List<RadioParameterView> radioConfiguration,
        List<KpiObservationView> kpis,
        List<NeighbourView> neighbours,
        ContextProvenance provenance
) {
    public record SiteView(
            String siteId,
            String name,
            Double latitude,
            Double longitude,
            String status
    ) {
    }

    public record GnbView(
            String gnbId,
            String name,
            String vendor,
            String model,
            String status
    ) {
    }

    public record CellView(
            String cellId,
            String name,
            String technology,
            String band,
            Integer arfcn,
            Integer pci,
            Integer bandwidthMhz,
            String duplexMode,
            String status
    ) {
    }

    public record RadioParameterView(
            String parameterName,
            String parameterValue,
            String unit,
            Instant effectiveFrom
    ) {
    }

    public record KpiObservationView(
            String metric,
            Double value,
            String unit,
            Instant observedAt,
            String source,
            boolean synthetic
    ) {
        public String formatted() {
            return KpiObservationFormat.format(metric, value, unit);
        }
    }

    public record NeighbourView(
            String targetCellId,
            String relationType,
            String status
    ) {
    }

    public record ContextProvenance(
            String source,
            boolean synthetic
    ) {
    }
}
