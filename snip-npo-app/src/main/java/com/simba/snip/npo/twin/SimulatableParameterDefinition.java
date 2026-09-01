package com.simba.snip.npo.twin;

import java.math.BigDecimal;

public record SimulatableParameterDefinition(
        String parameterId,
        String unit,
        BigDecimal minValue,
        BigDecimal maxValue,
        TwinScopeType scope,
        boolean enabled
) {
}
