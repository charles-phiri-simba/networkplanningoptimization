package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionPreGrantPreflightIT extends ProductionChangeITSupport {

    @Test
    void unknownInputDeniesGrant() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE network_knowledge_status SET confidence = 'UNKNOWN', freshness = 'UNKNOWN'");
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertEquals(0, grantCount(authorized.productionChangeId(), "ISSUED"));
        assertEquals(0, mutationCount());
        assertTrue(denied.getBody().contains("PREFLIGHT") || denied.getBody().contains("DENIED"));
    }
}
