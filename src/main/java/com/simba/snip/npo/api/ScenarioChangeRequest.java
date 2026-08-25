package com.simba.snip.npo.api;

public record ScenarioChangeRequest(
        String parameterId,
        Double currentValue,
        Double proposedValue
) {
}
