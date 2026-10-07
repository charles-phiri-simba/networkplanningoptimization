package com.simba.snip.npo.planning.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanningCellIntentDto(
        UUID id,
        String cellId,
        String parameterId,
        BigDecimal intendedValue
) {
}
