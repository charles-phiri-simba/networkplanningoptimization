package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.policy.ProductionBlastRadiusPolicy;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionChangeRateLimitTest {

    @Test
    void blastRadiusOneOneOne() {
        ProductionChangeProperties properties = new ProductionChangeProperties();
        ProductionBlastRadiusPolicy policy = new ProductionBlastRadiusPolicy(properties);
        var ex = assertThrows(com.simba.snip.npo.productionchange.exception.ProductionChangeException.class,
                () -> policy.requireSingleCellParameterOperation(2, 1, 1));
        assertEquals(ProductionReasonCode.PRODUCTION_SCOPE_DENIED, ex.reasonCode());
        properties.setMaximumCellsPerExecution(2);
        assertThrows(DomainValidationException.class, properties::validate);
        properties = new ProductionChangeProperties();
        properties.setMaximumChangesPerTargetPerHour(-1);
        assertThrows(DomainValidationException.class, properties::validate);
    }
}
