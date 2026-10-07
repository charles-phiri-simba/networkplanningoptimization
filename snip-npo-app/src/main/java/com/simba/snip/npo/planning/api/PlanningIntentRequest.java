package com.simba.snip.npo.planning.api;

import java.math.BigDecimal;

public record PlanningIntentRequest(
        String cellId,
        String parameterId,
        BigDecimal intendedValue
) {
}
