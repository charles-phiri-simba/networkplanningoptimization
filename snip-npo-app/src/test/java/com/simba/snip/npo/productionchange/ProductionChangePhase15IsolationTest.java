package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.changeexecution.api.ExecutionDetailDto;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionChangePhase15IsolationTest extends ProductionChangeITSupport {

    @Test
    void phase15ExecutionUnchanged() {
        UUID planId = readyPlanId();
        ExecutionDetailDto created = createPhase15Execution(planId);
        reviewPhase15(created.executionId());
        authorizePhase15(created.executionId());
        ExecutionDetailDto executed = executePhase15(created.executionId());
        assertEquals("VERIFIED", executed.status());
        Integer productionRows = jdbc.queryForObject("SELECT COUNT(*) FROM production_network_change", Integer.class);
        assertEquals(0, productionRows);
        assertEquals(0, mutationCount());
    }
}
