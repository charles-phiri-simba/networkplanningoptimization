package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionChangeCanonicalIsolationTest extends ProductionChangeITSupport {

    @Test
    void noDirectCanonicalMutation() {
        String before = canonicalTxPower();
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        executeExpectingOk(authorized.productionChangeId());
        assertEquals("VERIFIED", getProductionChange(authorized.productionChangeId()).status());
        assertEquals(before, canonicalTxPower());
        assertEquals(1, mutationCount());
    }
}
