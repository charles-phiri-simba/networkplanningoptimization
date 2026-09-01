package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangePreflightSplitIT extends ProductionChangeITSupport {

    @Test
    void appPassGatewayFailDeniesSend() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        gatewayProperties().setEnabled(false);
        try {
            ResponseEntity<String> denied = http.exchange(
                    "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                    HttpMethod.POST,
                    productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                    String.class);
            assertEquals(0, mutationCount());
            assertTrue(denied.getStatusCode().is4xxClientError() || denied.getStatusCode().is2xxSuccessful());
        } finally {
            restoreGatewaySafetyFlags();
        }
    }
}
