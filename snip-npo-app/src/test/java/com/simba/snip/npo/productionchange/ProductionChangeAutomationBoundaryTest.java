package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeAutomationBoundaryTest extends ProductionChangeITSupport {

    @Test
    void closedLoopExecuteDenied() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), "CLOSED_LOOP_OPTIMIZER", "optimizer-1"),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_UNAUTHORIZED.name()));
        assertEquals(0, mutationCount());
    }
}
