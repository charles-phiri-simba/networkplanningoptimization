package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeRollbackTest extends ProductionChangeITSupport {

    @Test
    void rollbackExpectedMismatch() {
        ProductionChangeDto verified = verifiedForward();
        rollbackRequest(verified.productionChangeId());
        rollbackReview(verified.productionChangeId());
        rollbackAuthorize(verified.productionChangeId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_MISMATCH);
        int before = mutationCount();
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + verified.productionChangeId() + "/rollback/execute",
                HttpMethod.POST,
                productionEntity(null, ProductionChangePermission.EXECUTE_PRODUCTION_ROLLBACK, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError() || denied.getStatusCode().is2xxSuccessful());
        assertEquals(before, mutationCount());
        String body = String.valueOf(denied.getBody());
        String stored = jdbc.queryForObject(
                "SELECT COALESCE(reason_code,'') FROM production_network_change WHERE production_change_id = ?",
                String.class, verified.productionChangeId());
        assertTrue(
                body.contains(ProductionReasonCode.PRODUCTION_EXPECTED_STATE_MISMATCH.name())
                        || body.contains(ProductionReasonCode.PRODUCTION_VENDOR_STATE_MISMATCH.name())
                        || stored.contains("MISMATCH")
                        || stored.contains("PREFLIGHT")
                        || stored.contains("EXPECTED_STATE"),
                () -> "body=" + body + " stored=" + stored);
    }

    @Test
    void noAutoRollbackOrEmergencyBypass() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.APPLY_WRONG_VALUE);
        seedTransportFor(authorized);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        Integer rollbackAttempts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ? AND grant_type = 'ROLLBACK'",
                Integer.class,
                authorized.productionChangeId());
        assertEquals(0, rollbackAttempts);
        assertTrue(new com.simba.snip.npo.productionchange.config.ProductionChangeProperties()
                .isAutomaticRollbackEnabled() == false);
    }

    private ProductionChangeDto verifiedForward() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        return executeExpectingOk(authorized.productionChangeId());
    }
}
