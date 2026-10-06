package com.simba.snip.npo.changeexecution.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeExecutionEvidenceNullSafetyTest {

    @Test
    void operationAuthorizationRecoveryAndAuditMapsRetainLegitimateNulls() {
        Map<String, Object> operations = ChangeExecutionQueryService.nullCapable(
                "sequenceNumber", 1,
                "operationType", "SET_PARAMETER",
                "expectedCurrentValue", null,
                "desiredValue", null
        );
        Map<String, Object> authorizations = ChangeExecutionQueryService.nullCapable(
                "authorizationType", "EXECUTE",
                "authorizer", null,
                "authorizedFingerprint", "fp",
                "authorizedAt", null
        );
        Map<String, Object> recoveries = ChangeExecutionQueryService.nullCapable(
                "recoveryStatus", "NOT_REQUIRED",
                "rollbackEligible", false,
                "reasonCodes", null,
                "evaluatedAt", null
        );
        Map<String, Object> audit = ChangeExecutionQueryService.nullCapable(
                "eventType", "REQUESTED",
                "actor", "system",
                "details", null,
                "occurredAt", null
        );

        assertTrue(operations.containsKey("expectedCurrentValue"));
        assertTrue(operations.containsKey("desiredValue"));
        assertNull(operations.get("expectedCurrentValue"));
        assertNull(operations.get("desiredValue"));
        assertTrue(authorizations.containsKey("authorizer"));
        assertNull(authorizations.get("authorizer"));
        assertTrue(recoveries.containsKey("reasonCodes"));
        assertNull(recoveries.get("reasonCodes"));
        assertTrue(audit.containsKey("details"));
        assertNull(audit.get("details"));
    }
}
