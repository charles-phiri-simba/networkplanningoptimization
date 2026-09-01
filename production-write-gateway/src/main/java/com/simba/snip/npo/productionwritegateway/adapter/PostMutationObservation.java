package com.simba.snip.npo.productionwritegateway.adapter;

import java.math.BigDecimal;
import java.time.Instant;

public record PostMutationObservation(
        ObservationStatus status,
        BigDecimal observedValue,
        Instant observedAt
) {
}
