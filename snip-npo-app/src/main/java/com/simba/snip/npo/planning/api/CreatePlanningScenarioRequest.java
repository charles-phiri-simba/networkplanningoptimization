package com.simba.snip.npo.planning.api;

import java.util.List;

public record CreatePlanningScenarioRequest(
        String name,
        String description,
        String createdBy,
        List<PlanningCellRequest> cells,
        List<PlanningAlternativeRequest> alternatives
) {
}
