package com.simba.snip.npo.api;

import com.simba.snip.npo.twin.TwinProvenance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SimulationDetailDto(
        UUID id,
        UUID scenarioId,
        UUID twinId,
        int baselineTwinVersion,
        String modelId,
        String modelVersion,
        String modelType,
        String status,
        Instant startedAt,
        Instant completedAt,
        boolean synthetic,
        String confidence,
        List<String> assumptions,
        List<String> limitations,
        List<MetricComparisonDto> metrics,
        TwinProvenance provenance,
        UUID actionId
) {
}
