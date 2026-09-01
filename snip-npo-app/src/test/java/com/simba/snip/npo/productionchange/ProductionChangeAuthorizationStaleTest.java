package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeAuthorizationStaleTest extends ProductionChangeITSupport {

    @Test
    void staleAuthorizationBlocksGrant() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update(
                "UPDATE production_network_change SET production_fingerprint = ? WHERE production_change_id = ?",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                authorized.productionChangeId());
        ResponseEntity<ProductionChangeDto> denied = executeProductionChange(authorized.productionChangeId());
        assertTrue(denied.getStatusCode().is4xxClientError());
        ProductionChangeDto reloaded = getProductionChange(authorized.productionChangeId());
        assertNotEquals(ProductionChangeStatus.VERIFIED.name(), reloaded.status());
        assertTrue(reloaded.reasonCode() == null
                || reloaded.reasonCode().contains("FINGERPRINT")
                || reloaded.reasonCode().contains("STALE")
                || reloaded.reasonCode().contains("PREFLIGHT"));
        assertEquals(0, grantCount(authorized.productionChangeId(), "ISSUED"));
        assertEquals(0, mutationCount());
        assertTrue(denied.getBody() == null
                || String.valueOf(denied.getBody()).contains(ProductionReasonCode.PRODUCTION_FINGERPRINT_STALE.name())
                || jdbc.queryForObject(
                "SELECT reason_code FROM production_network_change WHERE production_change_id = ?",
                String.class,
                authorized.productionChangeId()).contains("FINGERPRINT")
                || jdbc.queryForObject(
                "SELECT reason_code FROM production_network_change WHERE production_change_id = ?",
                String.class,
                authorized.productionChangeId()).contains("STALE")
                || jdbc.queryForObject(
                "SELECT reason_code FROM production_network_change WHERE production_change_id = ?",
                String.class,
                authorized.productionChangeId()).contains("PREFLIGHT"));
    }
}
