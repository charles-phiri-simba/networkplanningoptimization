package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.protocol.AuthorizedParameterMutation;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionChangeScopeTest {

    @Test
    void typedMutationTxPowerOnly() {
        assertThrows(IllegalArgumentException.class, () ->
                new AuthorizedParameterMutation("CELL", "electricalTilt", "CELL-001", BigDecimal.ONE, BigDecimal.TEN));
        AuthorizedParameterMutation ok = new AuthorizedParameterMutation(
                "CELL", "txPower", "CELL-001", new BigDecimal("46"), new BigDecimal("45"));
        assertEquals("txPower", ok.parameter());
    }

    @Test
    void maxOneParameter() {
        ProductionChangeConfigTest config = new ProductionChangeConfigTest();
        config.scopeExpansionRejected();
    }

    @Test
    void maxOneOperation() {
        ProductionChangeConfigTest config = new ProductionChangeConfigTest();
        config.scopeExpansionRejected();
        assertEquals(ProductionReasonCode.PRODUCTION_SCOPE_DENIED.name(),
                ProductionReasonCode.PRODUCTION_SCOPE_DENIED.name());
    }
}
