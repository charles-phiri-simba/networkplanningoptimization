package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionGatewayFinalPreflightIT extends ProductionChangeITSupport {

    @Test
    void finalPreflightUnknownDeniesSend() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_network_target SET adapter_profile_id = '' WHERE target_id = ?", TARGET_ID);
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(0, mutationCount());
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertTrue(denied.getBody().contains("PREFLIGHT")
                || denied.getBody().contains("FINGERPRINT")
                || denied.getBody().contains("DENIED"));
    }
}
