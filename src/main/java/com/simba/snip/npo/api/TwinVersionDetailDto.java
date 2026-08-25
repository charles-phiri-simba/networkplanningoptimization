package com.simba.snip.npo.api;

import com.simba.snip.npo.twin.TwinProvenance;
import com.simba.snip.npo.twin.TwinSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TwinVersionDetailDto(
        UUID id,
        UUID twinId,
        int version,
        Instant capturedAt,
        Instant synchronizedAt,
        Instant sourceEventTime,
        String sourceContextVersion,
        String freshness,
        TwinProvenance provenance,
        TwinSnapshot.CellIdentity cell,
        TwinSnapshot.ServingIdentity serving,
        List<TwinSnapshot.RadioParameter> configuration,
        List<TwinSnapshot.MetricValue> currentMetrics,
        List<TwinSnapshot.TemporalSummary> temporalSummary,
        List<TwinSnapshot.NeighbourSummary> neighbourSummary
) {
}
