package com.simba.snip.npo.planning.api;

import java.util.List;

public record PlanningAlternativeRequest(
        String name,
        Integer ordinal,
        List<PlanningIntentRequest> intents
) {
}
