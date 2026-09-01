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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeApiTest extends ProductionChangeITSupport {

    @Test
    void createRejectsCallerMutationFields() {
        UUID phase15 = verifiedPhase15ExecutionId();
        Map<String, Object> body = createRequestBody(phase15, TARGET_ID, PRINCIPAL_CC_VALIDATOR,
                Instant.now().plus(2, ChronoUnit.HOURS));
        body.put("cellId", CELL);
        body.put("parameter", "txPower");
        body.put("desiredValue", 1);
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes",
                HttpMethod.POST,
                productionEntity(body, ProductionChangePermission.REQUEST_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_INVALID_REQUEST.name()));
        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM production_network_change", Integer.class);
        assertEquals(0, rows);
        assertEquals(0, mutationCount());
    }

    @Test
    void executeRejectsMutationOverride() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        Map<String, Object> override = new LinkedHashMap<>();
        override.put("desiredValue", 99);
        override.put("cellId", CELL);
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(override, ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_INVALID_REQUEST.name()));
        assertEquals(0, mutationCount());
        assertEquals(ProductionChangeStatus.AUTHORIZED.name(),
                getProductionChange(authorized.productionChangeId()).status());
    }

    @Test
    void createOnlyThreeFields() {
        UUID phase15 = verifiedPhase15ExecutionId();
        Map<String, Object> body = createRequestBody(phase15, TARGET_ID, PRINCIPAL_CC_VALIDATOR,
                Instant.now().plus(2, ChronoUnit.HOURS));
        body.put("executorPrincipalId", PRINCIPAL_EXECUTOR);
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes",
                HttpMethod.POST,
                productionEntity(body, ProductionChangePermission.REQUEST_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_INVALID_REQUEST.name()));
        assertEquals(0, mutationCount());
    }

    @Test
    void authorizeDoesNotExecute() {
        UUID phase15 = verifiedPhase15ExecutionId();
        ProductionChangeDto created = createProductionChange(phase15);
        reviewProductionChange(created.productionChangeId());
        ProductionChangeDto authorized = authorizeProductionChange(created.productionChangeId());
        assertEquals(ProductionChangeStatus.AUTHORIZED.name(), authorized.status());
        assertEquals(0, mutationCount());
        assertEquals(0, attemptCount(authorized.productionChangeId()));
        assertEquals(0, grantCount(authorized.productionChangeId(), "ISSUED"));
    }
}
