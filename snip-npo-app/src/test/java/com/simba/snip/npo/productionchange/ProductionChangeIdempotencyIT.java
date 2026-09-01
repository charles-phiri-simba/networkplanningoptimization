package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionChangeIdempotencyIT extends ProductionChangeITSupport {

    @Test
    void duplicateExecuteAfterConsume() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        executeExpectingOk(authorized.productionChangeId());
        int mutations = mutationCount();
        ProductionChangeDto second = executeExpectingOk(authorized.productionChangeId());
        assertEquals("VERIFIED", second.status());
        assertEquals(mutations, mutationCount());
    }
}
