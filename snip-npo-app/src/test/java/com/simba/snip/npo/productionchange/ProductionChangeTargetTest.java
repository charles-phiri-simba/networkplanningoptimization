package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.domain.ProductionTargetState;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.service.ProductionTargetAdministrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeTargetTest extends ProductionChangeITSupport {

    @Autowired
    ProductionTargetAdministrationService administrationService;

    @Test
    void targetChangeRevokesGrants() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("""
                INSERT INTO production_execution_grant (
                    grant_id, production_change_id, phase15_execution_id, target_id, grant_type, status,
                    production_fingerprint, authorization_generation, fencing_token, operation_binding_hash,
                    issued_at, expires_at, version)
                VALUES (?, ?, ?, ?, 'FORWARD', 'ISSUED', ?, ?, 1, ?, NOW(), NOW() + INTERVAL '5 minutes', 0)
                """, java.util.UUID.randomUUID(), authorized.productionChangeId(), authorized.phase15ExecutionId(),
                TARGET_ID, authorized.productionFingerprint(), authorized.authorizationGeneration(),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        administrationService.suspend(TARGET_ID, ActorPrincipal.of(PRINCIPAL_AUTHORIZER));
        assertEquals(0, grantCount(authorized.productionChangeId(), "ISSUED"));
        assertEquals("AUTHORIZATION_STALE", getProductionChange(authorized.productionChangeId()).status());
        assertEquals(0, mutationCount());
    }

    @Test
    void targetStatesEnforced() {
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-targets/" + TARGET_ID + "/suspend",
                HttpMethod.POST,
                productionEntity(null, ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_UNAUTHORIZED.name()));
        administrationService.suspend(TARGET_ID, ActorPrincipal.of("admin-1"));
        assertEquals(ProductionTargetState.SUSPENDED.name(),
                jdbc.queryForObject("SELECT target_state FROM production_network_target WHERE target_id = ?",
                        String.class, TARGET_ID));
        administrationService.resume(TARGET_ID, ActorPrincipal.of("admin-1"));
        jdbc.update("UPDATE production_network_target SET target_state = 'DISABLED' WHERE target_id = ?", TARGET_ID);
        try {
            administrationService.resume(TARGET_ID, ActorPrincipal.of("admin-1"));
        } catch (Exception ex) {
            assertTrue(ex.getMessage().toLowerCase().contains("disabled")
                    || ex.toString().contains("PRODUCTION_TARGET_DISABLED"));
        }
        assertEquals(0, mutationCount());
    }
}
