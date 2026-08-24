package com.simba.snip.npo.network;

import com.simba.snip.npo.telemetry.Trend;

import java.time.Instant;
import java.util.List;

public record CellContext(
        CellView cell,
        GnbView gnb,
        SiteView site,
        List<RadioParameterView> radioConfiguration,
        List<KpiObservationView> kpis,
        List<NeighbourView> neighbours,
        List<KpiSeriesView> telemetry,
        ContextProvenance provenance
) {
    public Instant lastEventTime() {
        return telemetry.stream()
                .map(series -> series.current().observedAt())
                .max(Instant::compareTo)
                .orElse(null);
    }

    public int historyObservationCount() {
        return telemetry.stream().mapToInt(series -> series.history().size()).sum();
    }

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
            Instant ingestedAt,
            String eventId,
            String source,
            boolean synthetic
    ) {
        public String formatted() {
            return KpiObservationFormat.format(metric, value, unit);
        }
    }

    public record KpiSeriesView(
            String metric,
            KpiObservationView current,
            List<KpiObservationView> history,
            Trend trend
    ) {
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
