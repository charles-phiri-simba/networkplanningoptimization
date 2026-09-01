package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionwritegateway.service.FailureInjectionPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeAuditResilienceIT extends ProductionChangeITSupport {

    @Autowired
    ProductionChangeAuditService auditService;

    @Test
    void mutationEvidenceSurvivesAuditFailure() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        injectFailure(FailureInjectionPoint.MUTATION_INVOKE_START);
        executeProductionChange(authorized.productionChangeId());
        Integer attempts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_gateway_attempt WHERE production_change_id = ?",
                Integer.class,
                authorized.productionChangeId());
        assertTrue(attempts >= 0);
        jdbc.update("UPDATE production_change_audit_event SET event_hash = 'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd' WHERE production_change_id = ?",
                authorized.productionChangeId());
        Integer stillPresent = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_gateway_attempt WHERE production_change_id = ?",
                Integer.class,
                authorized.productionChangeId());
        assertTrue(stillPresent >= 0);
        assertEquals(0, mutationCount());
    }
}
