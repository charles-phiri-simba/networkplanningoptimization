package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionwritegateway.service.ConsumeCommand;
import com.simba.snip.npo.productionwritegateway.service.ConsumeResult;
import com.simba.snip.npo.productionwritegateway.service.ProductionGrantConsumeService;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeRollbackIT extends ProductionChangeITSupport {

    @Test
    void rollbackGrantConsumeRace() throws Exception {
        ProductionChangeDto verified = verifiedForward();
        rollbackRequest(verified.productionChangeId());
        rollbackReview(verified.productionChangeId());
        rollbackAuthorize(verified.productionChangeId());
        jdbc.update("""
                INSERT INTO production_execution_grant (
                    grant_id, production_change_id, phase15_execution_id, target_id, grant_type, status,
                    production_fingerprint, authorization_generation, fencing_token, operation_binding_hash,
                    issued_at, expires_at, version)
                VALUES (?, ?, ?, ?, 'ROLLBACK', 'ISSUED', ?, ?, 1, ?, NOW(), NOW() + INTERVAL '5 minutes', 0)
                """, UUID.randomUUID(), verified.productionChangeId(), verified.phase15ExecutionId(), TARGET_ID,
                verified.productionFingerprint(), verified.authorizationGeneration(),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT * FROM production_execution_grant WHERE production_change_id = ? AND grant_type = 'ROLLBACK' AND status = 'ISSUED'",
                verified.productionChangeId());
        ConsumeCommand command = new ConsumeCommand(
                (UUID) row.get("grant_id"),
                verified.productionChangeId(),
                verified.phase15ExecutionId(),
                TARGET_ID,
                (String) row.get("production_fingerprint"),
                ((Number) row.get("authorization_generation")).intValue(),
                ((Number) row.get("fencing_token")).longValue(),
                (String) row.get("operation_binding_hash"),
                GrantType.ROLLBACK);
        ProductionGrantConsumeService consume = GATEWAY_CTX.getBean(ProductionGrantConsumeService.class);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ConsumeResult> a = pool.submit(() -> { start.await(); return consume.consume(command); });
            Future<ConsumeResult> b = pool.submit(() -> { start.await(); return consume.consume(command); });
            start.countDown();
            int successes = (a.get().succeeded() ? 1 : 0) + (b.get().succeeded() ? 1 : 0);
            assertEquals(1, successes);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rollbackAmbiguousNoBlindRetry() {
        ProductionChangeDto verified = verifiedForward();
        rollbackRequest(verified.productionChangeId());
        rollbackReview(verified.productionChangeId());
        rollbackAuthorize(verified.productionChangeId());
        testTransport().seedCell(CELL, verified.desiredValue());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RESPONSE_LOST);
        int before = mutationCount();
        rollbackExecute(verified.productionChangeId());
        int afterFirst = mutationCount();
        rollbackExecute(verified.productionChangeId());
        assertTrue(afterFirst - before <= 1);
        assertEquals(afterFirst, mutationCount());
    }

    private ProductionChangeDto verifiedForward() {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        return executeExpectingOk(authorized.productionChangeId());
    }
}
