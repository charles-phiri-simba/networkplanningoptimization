package com.simba.snip.npo.planning.api;

import java.util.List;
import java.util.UUID;

public record PlanningAlternativeDto(
        UUID id,
        String name,
        int ordinal,
        List<PlanningCellIntentDto> intents
) {
}
