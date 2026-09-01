package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeExecuteTest extends ProductionChangeITSupport {

    @Test
    void cancelBeforeMutationOnly() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        ResponseEntity<String> cancel = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/cancel",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(cancel.getStatusCode().is4xxClientError());
        seedTransportFor(authorized);
        ProductionChangeDto verified = executeExpectingOk(authorized.productionChangeId());
        assertEquals(ProductionChangeStatus.VERIFIED.name(), verified.status());
        ResponseEntity<String> after = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/cancel",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertTrue(after.getStatusCode().is4xxClientError());
        assertNotEquals(ProductionChangeStatus.CANCELLED_BEFORE_MUTATION.name(),
                getProductionChange(authorized.productionChangeId()).status());
        assertEquals(1, mutationCount());
    }
}
