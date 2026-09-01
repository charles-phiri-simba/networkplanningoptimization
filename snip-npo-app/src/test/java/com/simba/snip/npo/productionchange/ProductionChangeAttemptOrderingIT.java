package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionChangeAttemptOrderingIT extends ProductionChangeITSupport {

    @Test
    void noAttemptBeforeConsume() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        assertEquals(0, attemptCount(authorized.productionChangeId()));
        assertEquals(0, grantCount(authorized.productionChangeId(), "CONSUMED"));
        assertEquals(0, mutationCount());
    }
}
