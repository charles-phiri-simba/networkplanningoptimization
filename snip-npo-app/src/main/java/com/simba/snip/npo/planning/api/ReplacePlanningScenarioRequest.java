package com.simba.snip.npo.planning.api;

import java.util.List;

public record ReplacePlanningScenarioRequest(
        String name,
        String description,
        Integer rowVersion,
        List<PlanningCellRequest> cells,
        List<PlanningAlternativeRequest> alternatives
) {
}
