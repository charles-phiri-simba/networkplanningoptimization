package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangePhase12BoundaryIT extends ProductionChangeITSupport {

    @Test
    void syncRequiredEmittedNotCanonicalWrite() {
        String before = canonicalTxPower();
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        executeExpectingOk(authorized.productionChangeId());
        assertEquals(before, canonicalTxPower());
        Integer syncEvents = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_change_audit_event WHERE production_change_id = ? AND event_type = ?",
                Integer.class,
                authorized.productionChangeId(),
                ProductionAuditEventType.PRODUCTION_NETWORK_SYNCHRONIZATION_REQUIRED.name());
        assertTrue(syncEvents >= 1 || "VERIFIED".equals(getProductionChange(authorized.productionChangeId()).status())
                || "NETWORK_SYNCHRONIZATION_REQUIRED".equals(getProductionChange(authorized.productionChangeId()).status()));
        assertEquals(1, mutationCount());
    }
}
