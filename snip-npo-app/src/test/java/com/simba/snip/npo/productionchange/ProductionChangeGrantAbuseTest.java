package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeGrantAbuseTest extends ProductionChangeITSupport {

    @Autowired
    ProductionChangeProperties properties;

    @Test
    void issuanceRateLimitEnforced() {
        int original = properties.getMaximumGrantsPerActorPerHour();
        properties.setMaximumGrantsPerActorPerHour(1);
        try {
            ProductionChangeDto first = reviewedAndAuthorized(verifiedPhase15ExecutionId());
            seedTransportFor(first);
            executeProductionChange(first.productionChangeId());
            ProductionChangeDto second = reviewedAndAuthorized(verifiedPhase15ExecutionId());
            ResponseEntity<String> denied = http.exchange(
                    "/api/v1/production-changes/" + second.productionChangeId() + "/execute",
                    HttpMethod.POST,
                    productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                    String.class);
            assertTrue(denied.getStatusCode().is4xxClientError());
            assertTrue(denied.getBody().contains("GRANT") || denied.getBody().contains("RATE"));
        } finally {
            properties.setMaximumGrantsPerActorPerHour(original);
        }
    }

    @Test
    void singleActiveForwardGrant() {
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
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertEquals(1, grantCount(authorized.productionChangeId(), "ISSUED"));
        assertEquals(0, mutationCount());
    }
}
