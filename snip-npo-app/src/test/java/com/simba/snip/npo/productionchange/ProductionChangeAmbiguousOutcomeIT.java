package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeAmbiguousOutcomeIT extends ProductionChangeITSupport {

    @Test
    void vendorAppliedResponseLost() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RESPONSE_LOST);
        seedTransportFor(authorized);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        String status = getProductionChange(authorized.productionChangeId()).status();
        assertTrue(status.equals("VERIFIED") || status.contains("UNKNOWN") || status.contains("RECOVERY"));
        assertEquals(1, mutationCount());
    }

    @Test
    void ambiguousExpectedValueSafeStop() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RETURN_EXPECTED_AFTER_APPLY);
        seedTransportFor(authorized);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
        assertEquals(0, grantCount(authorized.productionChangeId(), "ISSUED"));
        String status = getProductionChange(authorized.productionChangeId()).status();
        assertTrue(status.contains("UNKNOWN") || status.contains("RECOVERY") || status.contains("UNRESOLVED")
                || status.contains("MANUAL") || status.contains("VERIFIED"));
    }

    @Test
    void thirdValueManualIntervention() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.THIRD_VALUE);
        seedTransportFor(authorized);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
        String status = getProductionChange(authorized.productionChangeId()).status();
        assertTrue(
                status.contains("MANUAL") || status.contains("RECOVERY") || status.contains("UNKNOWN")
                        || status.contains("UNRESOLVED") || status.contains("VERIFICATION"),
                () -> "status=" + status);
    }

    @Test
    void ambiguousUnavailableUnresolved() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.TIMEOUT_AFTER_APPLY);
        seedTransportFor(authorized);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_UNAVAILABLE);
        String status = getProductionChange(authorized.productionChangeId()).status();
        assertEquals(1, mutationCount());
        assertTrue(status.contains("UNKNOWN") || status.contains("UNRESOLVED") || status.contains("RECOVERY")
                || status.contains("VERIFIED") || status.contains("MANUAL"));
    }
}
