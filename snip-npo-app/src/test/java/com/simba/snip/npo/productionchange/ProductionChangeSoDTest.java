package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeSoDTest extends ProductionChangeITSupport {

    @Test
    void requesterCannotAuthorize() {
        UUID phase15 = verifiedPhase15ExecutionId();
        ProductionChangeDto created = createProductionChange(phase15);
        reviewProductionChange(created.productionChangeId());
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + created.productionChangeId() + "/authorize",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.AUTHORIZE_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_SOD_VIOLATION.name()));
        assertEquals(ProductionChangeStatus.REVIEWED.name(), getProductionChange(created.productionChangeId()).status());
        assertEquals(0, mutationCount());
    }

    @Test
    void authorizerCannotExecute() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_AUTHORIZER),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_SOD_VIOLATION.name()));
        assertEquals(0, mutationCount());
        assertEquals(0, grantCount(authorized.productionChangeId(), "ISSUED"));
    }

    @Test
    void displayNameCannotSatisfySoD() {
        UUID phase15 = verifiedPhase15ExecutionId();
        ProductionChangeDto created = createProductionChange(phase15);
        reviewProductionChange(created.productionChangeId());
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes/" + created.productionChangeId() + "/authorize",
                HttpMethod.POST,
                productionEntity(Map.of("displayName", "Someone Else"),
                        ProductionChangePermission.AUTHORIZE_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_SOD_VIOLATION.name())
                || denied.getBody().contains(ProductionReasonCode.PRODUCTION_INVALID_REQUEST.name()));
        assertNotEquals(ProductionChangeStatus.AUTHORIZED.name(),
                getProductionChange(created.productionChangeId()).status());
        assertEquals(0, mutationCount());
    }

    @Test
    void reviewerNotAuthorizerPermission() {
        UUID phase15 = verifiedPhase15ExecutionId();
        ProductionChangeDto created = createProductionChange(phase15);
        reviewProductionChange(created.productionChangeId());
        ResponseEntity<String> permissionDenied = http.exchange(
                "/api/v1/production-changes/" + created.productionChangeId() + "/authorize",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.REVIEW_PRODUCTION_CHANGE, PRINCIPAL_REVIEWER),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, permissionDenied.getStatusCode());
        assertTrue(permissionDenied.getBody().contains(ProductionReasonCode.PRODUCTION_UNAUTHORIZED.name()));
        ResponseEntity<String> sodDenied = http.exchange(
                "/api/v1/production-changes/" + created.productionChangeId() + "/authorize",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.AUTHORIZE_PRODUCTION_CHANGE, PRINCIPAL_REVIEWER),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, sodDenied.getStatusCode());
        assertTrue(sodDenied.getBody().contains(ProductionReasonCode.PRODUCTION_SOD_VIOLATION.name()));
        assertEquals(0, mutationCount());
    }
}
