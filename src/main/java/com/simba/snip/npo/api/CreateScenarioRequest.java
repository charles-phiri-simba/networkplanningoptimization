package com.simba.snip.npo.api;

public record CreateScenarioRequest(
        String name,
        String description,
        String createdBy,
        Integer baselineTwinVersion,
        ScenarioChangeRequest change
) {
}
