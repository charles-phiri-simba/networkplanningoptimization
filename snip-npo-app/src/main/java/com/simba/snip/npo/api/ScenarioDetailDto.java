package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record ScenarioDetailDto(
        UUID id,
        UUID twinId,
        int baselineTwinVersion,
        String name,
        String description,
        String status,
        Instant createdAt,
        String createdBy,
        boolean synthetic,
        String parameterId,
        String currentValue,
        String proposedValue,
        String unit
) {
}
