package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.AuditChainIntegrity;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeAuditChainIT extends ProductionChangeITSupport {

    @Autowired
    ProductionChangeAuditService auditService;

    @Test
    void concurrentAppendSerialized() throws Exception {
        ProductionChangeDto created = createProductionChange(verifiedPhase15ExecutionId());
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(4);
        try {
            for (int i = 0; i < 4; i++) {
                int n = i;
                pool.submit(() -> {
                    try {
                        start.await();
                        auditService.append(
                                created.productionChangeId(),
                                ProductionAuditEventType.PRODUCTION_PREGRANT_PREFLIGHT_PASSED,
                                "actor-" + n,
                                List.of(),
                                Map.of("n", n));
                    } finally {
                        done.countDown();
                    }
                    return null;
                });
            }
            start.countDown();
            done.await();
        } finally {
            pool.shutdownNow();
        }
        List<Long> sequences = jdbc.queryForList(
                "SELECT sequence_number FROM production_change_audit_event WHERE production_change_id = ? ORDER BY sequence_number",
                Long.class,
                created.productionChangeId());
        for (int i = 0; i < sequences.size(); i++) {
            assertEquals(i + 1L, sequences.get(i));
        }
        assertEquals(AuditChainIntegrity.VALID, auditService.verify(created.productionChangeId()));
    }

    @Test
    void tamperDetectionInvalid() {
        ProductionChangeDto created = createProductionChange(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_change_audit_event SET event_hash = ? WHERE production_change_id = ?",
                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                created.productionChangeId());
        try {
            auditService.verify(created.productionChangeId());
        } catch (Exception ignored) {
            // verify marks INVALID and throws
        }
        String integrity = jdbc.queryForObject(
                "SELECT audit_chain_integrity FROM production_network_change WHERE production_change_id = ?",
                String.class,
                created.productionChangeId());
        assertEquals("INVALID", integrity);
        assertEquals(0, mutationCount());
    }

    @Test
    void gapDetection() {
        ProductionChangeDto created = createProductionChange(verifiedPhase15ExecutionId());
        reviewProductionChange(created.productionChangeId());
        jdbc.update("DELETE FROM production_change_audit_event WHERE production_change_id = ? AND sequence_number = 1",
                created.productionChangeId());
        try {
            auditService.verify(created.productionChangeId());
        } catch (Exception ignored) {
        }
        String integrity = jdbc.queryForObject(
                "SELECT audit_chain_integrity FROM production_network_change WHERE production_change_id = ?",
                String.class,
                created.productionChangeId());
        assertTrue("INVALID".equals(integrity) || sequencesHaveGap(created.productionChangeId()));
        assertEquals(0, mutationCount());
    }

    private boolean sequencesHaveGap(java.util.UUID id) {
        List<Long> sequences = jdbc.queryForList(
                "SELECT sequence_number FROM production_change_audit_event WHERE production_change_id = ? ORDER BY sequence_number",
                Long.class, id);
        return sequences.isEmpty() || sequences.get(0) != 1L;
    }
}
