package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.service.ProductionTargetHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeTargetHealthTest extends ProductionChangeITSupport {

    @Autowired
    ProductionTargetHealthService healthService;

    @Test
    void suspendedAfterGrantDeniesSend() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_network_target SET target_state = 'SUSPENDED' WHERE target_id = ?", TARGET_ID);
        ResponseEntity<ProductionChangeDto> denied = executeProductionChange(authorized.productionChangeId());
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertEquals(0, mutationCount());
        String reason = jdbc.queryForObject(
                "SELECT reason_code FROM production_network_change WHERE production_change_id = ?",
                String.class,
                authorized.productionChangeId());
        assertTrue(reason.contains(ProductionReasonCode.PRODUCTION_TARGET_SUSPENDED.name())
                || reason.contains("PREFLIGHT")
                || reason.contains("TARGET"));
    }

    @Test
    void autoSuspendOnFailures() {
        ActorPrincipal actor = ActorPrincipal.of("admin-1");
        healthService.recordVerificationFailure(TARGET_ID, actor);
        healthService.recordVerificationFailure(TARGET_ID, actor);
        healthService.recordVerificationFailure(TARGET_ID, actor);
        String state = jdbc.queryForObject(
                "SELECT target_state FROM production_network_target WHERE target_id = ?",
                String.class,
                TARGET_ID);
        assertEquals("SUSPENDED", state);
        assertEquals(0, mutationCount());
    }

    @Test
    void noAutomaticResume() {
        healthService.recordVerificationFailure(TARGET_ID, ActorPrincipal.of("admin-1"));
        healthService.recordVerificationFailure(TARGET_ID, ActorPrincipal.of("admin-1"));
        healthService.recordVerificationFailure(TARGET_ID, ActorPrincipal.of("admin-1"));
        jdbc.update("UPDATE production_target_health SET last_checked_at = NOW() - INTERVAL '1 day' WHERE production_target_id = ?",
                TARGET_ID);
        healthService.require(TARGET_ID);
        assertEquals("SUSPENDED", jdbc.queryForObject(
                "SELECT target_state FROM production_network_target WHERE target_id = ?",
                String.class, TARGET_ID));
        assertNotEquals("ACTIVE", jdbc.queryForObject(
                "SELECT target_state FROM production_network_target WHERE target_id = ?",
                String.class, TARGET_ID));
        assertEquals(0, mutationCount());
    }
}
