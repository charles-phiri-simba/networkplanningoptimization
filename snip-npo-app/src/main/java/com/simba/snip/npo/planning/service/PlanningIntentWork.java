package com.simba.snip.npo.planning.service;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanningIntentWork(
        UUID alternativeId,
        int alternativeOrdinal,
        String alternativeName,
        String cellId,
        String parameterId,
        BigDecimal intendedValue
) {
}
