package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeWindowIT extends ProductionChangeITSupport {

    @Test
    void windowRecheckDeniesSend() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_network_target SET change_window_policy = 'CLOSED' WHERE target_id = ?",
                TARGET_ID);
        jdbc.update("UPDATE network_change_execution SET execution_window_closes_at = NOW() - INTERVAL '1 minute' WHERE id = ?",
                authorized.phase15ExecutionId());
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertEquals(0, mutationCount());
        assertTrue(denied.getBody().contains("WINDOW") || denied.getBody().contains("PREFLIGHT")
                || denied.getBody().contains("FINGERPRINT"));
    }
}
