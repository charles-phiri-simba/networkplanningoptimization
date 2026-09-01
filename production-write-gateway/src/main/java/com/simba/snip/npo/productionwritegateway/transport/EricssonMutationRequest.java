package com.simba.snip.npo.productionwritegateway.transport;

import java.math.BigDecimal;
import java.util.UUID;

public record EricssonMutationRequest(
        UUID productionChangeId,
        UUID attemptId,
        String cellId,
        String parameter,
        BigDecimal expectedValue,
        BigDecimal desiredValue,
        boolean requireAtomicCas
) {
}
