package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionwritegateway.service.FailureInjectionPoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeGatewayHandoffIT extends ProductionChangeITSupport {

    @Test
    void consumedPreAttemptCrashRecovery() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        injectFailure(FailureInjectionPoint.AFTER_CONSUME_BEFORE_ATTEMPT);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        String status = getProductionChange(authorized.productionChangeId()).status();
        assertEquals("CONSUMED_PRE_SEND_RECOVERY_REQUIRED", status);
        assertEquals("CONSUMED", grantStatus(authorized.productionChangeId()));
        assertEquals(0, grantCount(authorized.productionChangeId(), "ISSUED"));
        assertEquals(0, attemptCount(authorized.productionChangeId()));
        assertEquals(0, mutationCount());
        Integer grantTotal = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ?",
                Integer.class,
                authorized.productionChangeId());
        assertEquals(1, grantTotal);
    }

    @Test
    void attemptPersistedBeforeSendCrash() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        injectFailure(FailureInjectionPoint.AFTER_ATTEMPT_BEFORE_PREFLIGHT);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(attemptCount(authorized.productionChangeId()) >= 1);
        assertEquals("PRE_SEND", jdbc.queryForObject(
                "SELECT status FROM production_gateway_attempt WHERE production_change_id = ? ORDER BY started_at DESC LIMIT 1",
                String.class,
                authorized.productionChangeId()));
        assertEquals(0, mutationCount());
    }
}
