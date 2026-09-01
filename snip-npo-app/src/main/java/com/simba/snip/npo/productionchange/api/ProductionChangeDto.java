package com.simba.snip.npo.productionchange.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductionChangeDto(
        UUID productionChangeId,
        UUID phase15ExecutionId,
        String productionTargetId,
        String status,
        String reasonCode,
        String cellId,
        String parameter,
        BigDecimal expectedValue,
        BigDecimal desiredValue,
        String productionFingerprint,
        int authorizationGeneration,
        Instant createdAt,
        Instant updatedAt
) {
}
