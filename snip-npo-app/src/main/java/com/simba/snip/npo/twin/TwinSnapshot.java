package com.simba.snip.npo.twin;

import java.time.Instant;
import java.util.List;

public record TwinSnapshot(
        CellIdentity cell,
        ServingIdentity serving,
        List<RadioParameter> configuration,
        List<MetricValue> currentMetrics,
        List<TemporalSummary> temporalSummary,
        List<NeighbourSummary> neighbourSummary
) {
    public record CellIdentity(
            String cellId,
            String name,
            String technology,
            String band,
            Integer pci,
            String status
    ) {
    }

    public record ServingIdentity(
            String gnbId,
            String gnbName,
            String siteId,
            String siteName
    ) {
    }

    public record RadioParameter(
            String parameterName,
            String parameterValue,
            String unit,
            Instant effectiveFrom
    ) {
    }

    public record MetricValue(
            String metric,
            Double value,
            String unit,
            Instant observedAt
    ) {
    }

    public record TemporalSummary(
            String metric,
            String trend,
            Double current,
            int historyCount
    ) {
    }

    public record NeighbourSummary(
            String targetCellId,
            String relationType,
            String status
    ) {
    }
}
