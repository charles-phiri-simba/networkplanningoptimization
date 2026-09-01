package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeEvidenceAuthorityTest extends ProductionChangeITSupport {

    @Test
    void appCannotFabricateVerified() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_network_change SET status = 'VERIFIED' WHERE production_change_id = ?",
                authorized.productionChangeId());
        ResponseEntity<Map> evidence = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/evidence",
                HttpMethod.GET,
                productionEntity(null, ProductionChangePermission.VIEW_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                Map.class);
        assertTrue(evidence.getStatusCode().is2xxSuccessful());
        assertTrue(String.valueOf(evidence.getBody()).contains("attempts"));
        assertTrue(!String.valueOf(evidence.getBody()).contains("VENDOR_ACCEPTED")
                || String.valueOf(evidence.getBody()).contains("[]"));
        assertEquals(0, mutationCount());
        Integer attempts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_gateway_attempt WHERE production_change_id = ? AND status = 'VERIFIED'",
                Integer.class,
                authorized.productionChangeId());
        assertEquals(0, attempts);
    }

    @Test
    void httpResponseNotSoleAuthority() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        executeExpectingOk(authorized.productionChangeId());
        String durable = jdbc.queryForObject(
                "SELECT status FROM production_network_change WHERE production_change_id = ?",
                String.class,
                authorized.productionChangeId());
        assertEquals(ProductionChangeStatus.VERIFIED.name(), durable);
        assertEquals(1, mutationCount());
    }

    @Test
    void appCannotOwnGatewayAttemptState() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        UUID attemptId = UUID.randomUUID();
        int inserted = 0;
        try {
            inserted = jdbc.update("""
                    INSERT INTO production_gateway_attempt (
                        attempt_id, grant_id, production_change_id, production_target_id, status, send_phase,
                        mutation_outcome, operation_binding_hash, fencing_token, production_fingerprint,
                        started_at, version)
                    VALUES (?, ?, ?, ?, 'VERIFIED', 'MAY_HAVE_SENT', 'VENDOR_ACCEPTED', ?, 1, ?, NOW(), 0)
                    """, attemptId, UUID.randomUUID(), authorized.productionChangeId(), TARGET_ID,
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    authorized.productionFingerprint());
        } catch (Exception ignored) {
            inserted = 0;
        }
        Integer verifiedAttempts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_gateway_attempt a JOIN production_gateway_evidence e ON e.attempt_id = a.attempt_id WHERE a.production_change_id = ? AND a.status = 'VERIFIED'",
                Integer.class,
                authorized.productionChangeId());
        assertEquals(0, verifiedAttempts == null ? 0 : verifiedAttempts);
        assertTrue(inserted == 0 || verifiedAttempts == 0);
        assertEquals(0, mutationCount());
        assertNotEquals("ignored", String.valueOf(inserted));
    }
}
