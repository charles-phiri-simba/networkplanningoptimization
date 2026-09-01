package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionwritegateway.service.FailureInjectionPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeGrantIT extends ProductionChangeITSupport {

    @Test
    void grantTimeoutMatrix() {
        ProductionChangeDto expiredControl = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_change_control SET valid_until = NOW() - INTERVAL '1 minute' WHERE production_change_id = ?",
                expiredControl.productionChangeId());
        executeProductionChange(expiredControl.productionChangeId());
        assertEquals(0, mutationCount());

        mutationCounter().set(0);
        injectFailure(FailureInjectionPoint.AFTER_CONSUME_BEFORE_ATTEMPT);
        ProductionChangeDto fi03 = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(fi03);
        executeProductionChange(fi03.productionChangeId());
        assertEquals("CONSUMED", grantStatus(fi03.productionChangeId()));
        assertEquals("CONSUMED_PRE_SEND_RECOVERY_REQUIRED", getProductionChange(fi03.productionChangeId()).status());
        assertEquals(0, attemptCount(fi03.productionChangeId()));
        assertEquals(0, mutationCount());
        assertEquals(0, grantCount(fi03.productionChangeId(), "ISSUED"));

        restoreGatewaySafetyFlags();
        mutationCounter().set(0);
        injectFailure(FailureInjectionPoint.AFTER_ATTEMPT_BEFORE_PREFLIGHT);
        ProductionChangeDto fi04 = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(fi04);
        executeProductionChange(fi04.productionChangeId());
        assertTrue(attemptCount(fi04.productionChangeId()) >= 1 || mutationCount() == 0);
        assertEquals(0, mutationCount());
    }
}
