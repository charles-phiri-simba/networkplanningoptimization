package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeSimulatorE2EIT extends ProductionChangeITSupport {

    @Test
    void happyPath() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        mutationCounter().set(0);
        ProductionChangeDto executed = executeExpectingOk(authorized.productionChangeId());
        assertEquals(ProductionChangeStatus.VERIFIED.name(), executed.status());
        assertEquals(1, mutationCount());
        assertEquals("CONSUMED", grantStatus(authorized.productionChangeId()));
        assertEquals("VERIFIED", latestAttemptStatus(authorized.productionChangeId()));
        Integer retries = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_gateway_attempt WHERE production_change_id = ?",
                Integer.class,
                authorized.productionChangeId());
        assertEquals(1, retries);
    }
}
