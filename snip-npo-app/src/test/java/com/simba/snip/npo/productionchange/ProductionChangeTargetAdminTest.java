package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeTargetAdminTest extends ProductionChangeITSupport {

    @Test
    void targetAdminRequiresPrivilege() {
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-targets/" + TARGET_ID + "/suspend",
                HttpMethod.POST,
                productionEntity(null, ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_UNAUTHORIZED.name()));
        ResponseEntity<String> allowed = http.exchange(
                "/api/v1/production-targets/" + TARGET_ID + "/suspend",
                HttpMethod.POST,
                productionEntity(null, ProductionChangePermission.ADMINISTER_PRODUCTION_TARGET, "admin-1"),
                String.class);
        assertTrue(allowed.getStatusCode().is2xxSuccessful());
        assertEquals("SUSPENDED", jdbc.queryForObject(
                "SELECT target_state FROM production_network_target WHERE target_id = ?",
                String.class, TARGET_ID));
        assertEquals(0, mutationCount());
    }
}
