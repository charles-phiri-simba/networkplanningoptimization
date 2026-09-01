package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionwritegateway.service.FailureInjectionPoint;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeVerificationIT extends ProductionChangeITSupport {

    @Test
    void verificationUsesSeparateReadback() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        ProductionChangeDto executed = executeExpectingOk(authorized.productionChangeId());
        assertEquals("VERIFIED", executed.status());
        Integer evidence = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_gateway_evidence e JOIN production_gateway_attempt a ON a.attempt_id = e.attempt_id WHERE a.production_change_id = ?",
                Integer.class,
                authorized.productionChangeId());
        assertTrue(evidence >= 1);
        assertEquals(1, mutationCount());
    }

    @Test
    void verificationPersistFailureSurvives() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        injectFailure(FailureInjectionPoint.VERIFICATION_PERSIST_FAIL);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
        Integer attempts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_gateway_attempt WHERE production_change_id = ?",
                Integer.class,
                authorized.productionChangeId());
        assertTrue(attempts >= 1);
    }

    @Test
    void vendorAcceptedNotVerifiedUntilReadback() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        injectFailure(FailureInjectionPoint.BEFORE_VERIFICATION);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        String status = getProductionChange(authorized.productionChangeId()).status();
        assertNotEquals("VERIFIED", status);
        assertEquals(1, mutationCount());
    }

    @Test
    void verificationFailureRecoveryNotAutoRollback() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.APPLY_WRONG_VALUE);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
        Integer rollback = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ? AND grant_type = 'ROLLBACK'",
                Integer.class,
                authorized.productionChangeId());
        assertEquals(0, rollback);
        String status = getProductionChange(authorized.productionChangeId()).status();
        assertTrue(
                status.contains("RECOVERY") || status.contains("VERIFICATION") || status.contains("FAILED")
                        || status.contains("MANUAL") || status.contains("UNKNOWN"),
                () -> "status=" + status);
    }
}
