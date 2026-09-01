package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeExpectedStateTest extends ProductionChangeITSupport {

    @Test
    void mismatchZeroMutation() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_MISMATCH);
        ResponseEntity<String> denied = executeAsString(authorized);
        assertTrue(denied.getStatusCode().is4xxClientError() || denied.getStatusCode().is2xxSuccessful());
        assertEquals(0, mutationCount());
        assertTrue(bodyOrReason(authorized, denied).contains(ProductionReasonCode.PRODUCTION_VENDOR_STATE_MISMATCH.name())
                || bodyOrReason(authorized, denied).contains("MISMATCH")
                || bodyOrReason(authorized, denied).contains("PREFLIGHT"));
    }

    @Test
    void unknownZeroMutation() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_UNAVAILABLE);
        ResponseEntity<String> denied = executeAsString(authorized);
        assertEquals(0, mutationCount());
        assertTrue(bodyOrReason(authorized, denied).contains(ProductionReasonCode.PRODUCTION_VERIFICATION_UNAVAILABLE.name())
                || bodyOrReason(authorized, denied).contains("UNAVAILABLE")
                || bodyOrReason(authorized, denied).contains("UNKNOWN")
                || bodyOrReason(authorized, denied).contains("PREFLIGHT")
                || bodyOrReason(authorized, denied).contains("DENIED"));
    }

    @Test
    void atomicUnsupportedDenies() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_network_target SET expected_state_guard_strength = 'ATOMIC' WHERE target_id = ?",
                TARGET_ID);
        testTransport().setAtomicSupported(false);
        ResponseEntity<String> denied = executeAsString(authorized);
        assertEquals(0, mutationCount());
        assertTrue(bodyOrReason(authorized, denied).contains(ProductionReasonCode.PRODUCTION_ATOMIC_UNSUPPORTED.name())
                || bodyOrReason(authorized, denied).contains("ATOMIC")
                || bodyOrReason(authorized, denied).contains("FINGERPRINT")
                || bodyOrReason(authorized, denied).contains("PREFLIGHT"));
    }

    @Test
    void readThenWriteDisallowed() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_network_target SET verification_policy = 'FORBID_READ_THEN_WRITE' WHERE target_id = ?",
                TARGET_ID);
        ResponseEntity<String> denied = executeAsString(authorized);
        assertEquals(0, mutationCount());
        assertTrue(bodyOrReason(authorized, denied).contains(ProductionReasonCode.PRODUCTION_POLICY_DENY.name())
                || bodyOrReason(authorized, denied).contains("POLICY")
                || bodyOrReason(authorized, denied).contains("FINGERPRINT")
                || bodyOrReason(authorized, denied).contains("PREFLIGHT"));
    }

    private ResponseEntity<String> executeAsString(ProductionChangeDto authorized) {
        return http.exchange(
                "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
    }

    private String bodyOrReason(ProductionChangeDto authorized, ResponseEntity<String> response) {
        String reason = jdbc.queryForObject(
                "SELECT COALESCE(reason_code,'') FROM production_network_change WHERE production_change_id = ?",
                String.class,
                authorized.productionChangeId());
        return String.valueOf(response.getBody()) + reason;
    }
}
