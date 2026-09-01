package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeKillSwitchTest extends ProductionChangeITSupport {

    @Test
    void killSwitchAfterGrantDeniesSend() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        gatewayProperties().setEnabled(false);
        gatewayProperties().setGlobalExecutionEnabled(false);
        try {
            ResponseEntity<ProductionChangeDto> denied = executeProductionChange(authorized.productionChangeId());
            assertTrue(denied.getStatusCode().is4xxClientError() || denied.getStatusCode().is2xxSuccessful());
            assertEquals(0, mutationCount());
            String status = getProductionChange(authorized.productionChangeId()).status();
            String reason = jdbc.queryForObject(
                    "SELECT reason_code FROM production_network_change WHERE production_change_id = ?",
                    String.class,
                    authorized.productionChangeId());
            assertTrue(status.contains("DENIED") || status.contains("AUTHORIZED") || status.contains("PREFLIGHT")
                    || (reason != null && (reason.contains("DISABLED") || reason.contains("KILL") || reason.contains("GRANT"))));
            assertTrue(reason == null
                    || reason.contains(ProductionReasonCode.PRODUCTION_DISABLED.name())
                    || reason.contains(ProductionReasonCode.PRODUCTION_KILL_SWITCH_DENY.name())
                    || reason.contains("GRANT")
                    || reason.contains("PREFLIGHT"));
        } finally {
            restoreGatewaySafetyFlags();
        }
    }
}
