package com.simba.snip.npo.productionchange.protocol;

import java.math.BigDecimal;

public record AuthorizedParameterMutation(
        String objectType,
        String parameter,
        String cellId,
        BigDecimal expectedValue,
        BigDecimal desiredValue
) {
    public AuthorizedParameterMutation {
        if (!"CELL".equals(objectType) || !"txPower".equals(parameter)) {
            throw new IllegalArgumentException("typed CELL/txPower mutation only");
        }
    }
}
