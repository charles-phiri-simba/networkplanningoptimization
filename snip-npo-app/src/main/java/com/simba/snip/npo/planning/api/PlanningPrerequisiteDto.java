package com.simba.snip.npo.planning.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanningPrerequisiteDto(
        String cellId,
        boolean twinPresent,
        UUID twinId,
        Integer latestVersion,
        String freshness,
        BigDecimal observedTxPower,
        boolean canEvaluate
) {
}
