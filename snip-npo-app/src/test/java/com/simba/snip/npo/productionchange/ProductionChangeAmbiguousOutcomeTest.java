package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeAmbiguousOutcomeTest extends ProductionChangeITSupport {

    @Test
    void noBlindRetryAfterMayHaveSent() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RESPONSE_LOST);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        int afterFirst = mutationCount();
        assertEquals(1, afterFirst);
        ResponseEntity<String> second = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(afterFirst, mutationCount());
        assertTrue(second.getStatusCode().is4xxClientError() || second.getStatusCode().is2xxSuccessful());
        Integer grants = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ? AND status = 'ISSUED'",
                Integer.class,
                authorized.productionChangeId());
        assertEquals(0, grants);
    }

    @Test
    void noAutoRegrantAfterAmbiguous() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.TIMEOUT_AFTER_APPLY);
        http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
        assertEquals(0, grantCount(authorized.productionChangeId(), "ISSUED"));
    }
}
