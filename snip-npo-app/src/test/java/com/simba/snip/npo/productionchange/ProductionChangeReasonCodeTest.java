package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeReasonCodeTest extends ProductionChangeITSupport {

    @Test
    void reasonCodesSanitized() {
        Set<String> codes = Arrays.stream(ProductionReasonCode.values()).map(Enum::name).collect(Collectors.toSet());
        assertTrue(codes.contains(ProductionReasonCode.PRODUCTION_SOD_VIOLATION.name()));
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.REJECT);
        ResponseEntity<String> response = http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        String body = String.valueOf(response.getBody());
        assertFalse(body.toLowerCase().contains("password"));
        assertFalse(body.toLowerCase().contains("secret"));
        assertFalse(body.contains("BEGIN CERTIFICATE"));
        String reason = jdbc.queryForObject(
                "SELECT COALESCE(reason_code,'') FROM production_network_change WHERE production_change_id = ?",
                String.class,
                authorized.productionChangeId());
        assertTrue(reason.isBlank() || codes.contains(reason) || reason.startsWith("PRODUCTION_"));
    }
}
