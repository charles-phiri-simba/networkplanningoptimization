package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeRollbackE2EIT extends ProductionChangeITSupport {

    @Test
    void rollbackHappyPath() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        ProductionChangeDto verified = executeExpectingOk(authorized.productionChangeId());
        assertEquals("VERIFIED", verified.status());
        int forwardMutations = mutationCount();
        rollbackRequest(verified.productionChangeId());
        rollbackReview(verified.productionChangeId());
        rollbackAuthorize(verified.productionChangeId());
        testTransport().seedCell(CELL, verified.desiredValue());
        testTransport().setFailureMode(com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport.FailureMode.NONE);
        var rolled = rollbackExecute(verified.productionChangeId());
        assertTrue(rolled.getStatusCode().is2xxSuccessful(), () -> String.valueOf(rolled.getBody()));
        assertEquals("ROLLED_BACK", getProductionChange(verified.productionChangeId()).status());
        assertEquals(forwardMutations + 1, mutationCount());
    }
}
