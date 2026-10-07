package com.simba.snip.npo.planning.api;

public record EvaluatePlanningScenarioRequest(
        String createdBy,
        Integer rowVersion
) {
}
