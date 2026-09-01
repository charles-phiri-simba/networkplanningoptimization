package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeControlTest extends ProductionChangeITSupport {

    @Test
    void expiredTicketAfterGrantDenies() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_change_control SET valid_until = NOW() - INTERVAL '1 minute' WHERE production_change_id = ?",
                authorized.productionChangeId());
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertEquals(0, mutationCount());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_EXPIRED.name())
                || denied.getBody().contains(ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_INVALID.name())
                || denied.getBody().contains("PREFLIGHT"));
    }

    @Test
    void changeControlRequiredNotAuthorization() {
        UUID phase15 = verifiedPhase15ExecutionId();
        ProductionChangeDto created = createProductionChange(phase15);
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + created.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_UNAUTHORIZED.name())
                || denied.getBody().contains(ProductionReasonCode.PRODUCTION_AUTHORIZATION_MISSING.name()));
        assertEquals(0, mutationCount());
        assertEquals(ProductionChangeStatus.READY_FOR_REVIEW.name(), getProductionChange(created.productionChangeId()).status());
    }

    @Test
    void manualChangeControlOnly() {
        UUID phase15 = verifiedPhase15ExecutionId();
        Map<String, Object> body = createRequestBody(phase15, TARGET_ID, PRINCIPAL_CC_VALIDATOR,
                Instant.now().plus(2, ChronoUnit.HOURS));
        @SuppressWarnings("unchecked")
        Map<String, Object> cc = (Map<String, Object>) body.get("changeControlReference");
        cc.put("system", "SERVICENOW");
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes",
                HttpMethod.POST,
                productionEntity(body, ProductionChangePermission.REQUEST_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_INVALID.name()));
        assertEquals(0, mutationCount());
    }

    @Test
    void requesterCannotSelfValidateChangeControl() {
        UUID phase15 = verifiedPhase15ExecutionId();
        Map<String, Object> body = createRequestBody(phase15, TARGET_ID, PRINCIPAL_REQUESTER,
                Instant.now().plus(2, ChronoUnit.HOURS));
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes",
                HttpMethod.POST,
                productionEntity(body, ProductionChangePermission.REQUEST_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_SOD_VIOLATION.name()));
        assertEquals(0, mutationCount());
    }
}
