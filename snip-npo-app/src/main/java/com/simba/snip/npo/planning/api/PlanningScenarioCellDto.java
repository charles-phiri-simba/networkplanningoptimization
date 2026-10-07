package com.simba.snip.npo.planning.api;

import java.util.UUID;

public record PlanningScenarioCellDto(
        UUID id,
        String cellId,
        int ordinal
) {
}
