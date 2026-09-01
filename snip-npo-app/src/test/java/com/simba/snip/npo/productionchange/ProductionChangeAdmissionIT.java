package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeAdmissionIT extends ProductionChangeITSupport {

    @Test
    void mutationDetailsFromGovernedStateOnly() {
        ProductionChangeDto created = createProductionChange(verifiedPhase15ExecutionId());
        assertEquals(CELL, created.cellId());
        assertEquals("txPower", created.parameter());
        assertTrue(created.expectedValue() != null);
        assertTrue(created.desiredValue() != null);
        assertNotEquals(created.expectedValue(), created.desiredValue());
        assertEquals(0, mutationCount());
    }

    @Test
    void authorizationIndependence() {
        ProductionChangeDto created = createProductionChange(verifiedPhase15ExecutionId());
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + created.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_UNAUTHORIZED.name())
                || denied.getBody().contains(ProductionReasonCode.PRODUCTION_AUTHORIZATION_MISSING.name()));
        assertEquals(0, mutationCount());
        assertNotEquals("AUTHORIZED", created.status());
    }
}
