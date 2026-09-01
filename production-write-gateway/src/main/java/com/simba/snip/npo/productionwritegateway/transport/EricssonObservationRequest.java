package com.simba.snip.npo.productionwritegateway.transport;

import java.math.BigDecimal;

public record EricssonObservationRequest(
        String cellId,
        String parameter,
        BigDecimal compareValue
) {
}
