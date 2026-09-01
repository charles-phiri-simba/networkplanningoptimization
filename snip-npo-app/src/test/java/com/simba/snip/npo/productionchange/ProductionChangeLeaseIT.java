package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeLeaseIT extends ProductionChangeITSupport {

    @Test
    void fencingChangedAfterGrantDenies() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("""
                INSERT INTO production_execution_lease (
                    lease_id, production_target_id, cell_id, parameter, holder_id, fencing_token,
                    status, acquired_at, expires_at)
                VALUES (?, ?, ?, 'txPower', 'other-holder', 99, 'ACTIVE', NOW(), NOW() + INTERVAL '10 minutes')
                """, java.util.UUID.randomUUID(), TARGET_ID, CELL);
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertEquals(0, mutationCount());
        assertTrue(denied.getBody().contains("LEASE") || denied.getBody().contains("FENCING"));
    }

    @Test
    void leaseRequiredBeforeGrant() {
        String source = "";
        try {
            source = java.nio.file.Files.readString(ProductionChangeSourcePaths.appMainJava().resolve(
                    "com/simba/snip/npo/productionchange/service/ProductionExecutionOrchestrationService.java"));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        int leaseIdx = source.indexOf("leaseService.acquire");
        int grantIdx = source.indexOf("grantService.issue");
        assertTrue(leaseIdx >= 0 && grantIdx > leaseIdx);
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("DELETE FROM production_execution_lease");
        assertEquals(0, mutationCount());
        assertEquals(0, grantCount(authorized.productionChangeId(), "ISSUED"));
    }
}
