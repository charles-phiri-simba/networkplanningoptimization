package com.simba.snip.npo.productionwritegateway;

import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.service.ConsumeCommand;
import com.simba.snip.npo.productionwritegateway.service.ConsumeResult;
import com.simba.snip.npo.productionwritegateway.service.ProductionGrantConsumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ProductionChangeGrantConsumeIT extends AbstractGatewayPostgresIT {

    @Autowired
    private ProductionGrantConsumeService consumeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentConsume_oneWinner() throws Exception {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        ConsumeCommand command = commandFrom(seeded, GrantType.FORWARD);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ConsumeResult> first = pool.submit(() -> {
                start.await();
                return consumeService.consume(command);
            });
            Future<ConsumeResult> second = pool.submit(() -> {
                start.await();
                return consumeService.consume(command);
            });
            start.countDown();
            ConsumeResult a = first.get();
            ConsumeResult b = second.get();
            int successes = (a.succeeded() ? 1 : 0) + (b.succeeded() ? 1 : 0);
            assertEquals(1, successes);
            ConsumeResult loser = a.succeeded() ? b : a;
            assertFalse(loser.succeeded());
            assertEquals(0, loser.rowsUpdated());
            assertEquals(ProductionReasonCode.PRODUCTION_GRANT_ALREADY_CONSUMED, loser.denyReason());
            assertEquals("CONSUMED", grantStatus(seeded.grantId()));
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM production_execution_grant WHERE grant_id = ? AND status = 'CONSUMED'",
                    Integer.class,
                    seeded.grantId()
            ));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void consumeDeny_wrongTarget() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        ConsumeCommand command = new ConsumeCommand(
                seeded.grantId(),
                seeded.productionChangeId(),
                seeded.phase15ExecutionId(),
                "wrong-target",
                seeded.fingerprint(),
                seeded.authorizationGeneration(),
                seeded.fencingToken(),
                seeded.operationBindingHash(),
                GrantType.FORWARD
        );
        ConsumeResult result = consumeService.consume(command);
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_wrongFingerprint() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        ConsumeCommand command = new ConsumeCommand(
                seeded.grantId(),
                seeded.productionChangeId(),
                seeded.phase15ExecutionId(),
                seeded.targetId(),
                GatewayTestFixture.HASH_B,
                seeded.authorizationGeneration(),
                seeded.fencingToken(),
                seeded.operationBindingHash(),
                GrantType.FORWARD
        );
        ConsumeResult result = consumeService.consume(command);
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_wrongAuthGeneration() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        ConsumeCommand command = new ConsumeCommand(
                seeded.grantId(),
                seeded.productionChangeId(),
                seeded.phase15ExecutionId(),
                seeded.targetId(),
                seeded.fingerprint(),
                seeded.authorizationGeneration() + 9,
                seeded.fencingToken(),
                seeded.operationBindingHash(),
                GrantType.FORWARD
        );
        ConsumeResult result = consumeService.consume(command);
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_wrongFencingToken() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        ConsumeCommand command = new ConsumeCommand(
                seeded.grantId(),
                seeded.productionChangeId(),
                seeded.phase15ExecutionId(),
                seeded.targetId(),
                seeded.fingerprint(),
                seeded.authorizationGeneration(),
                seeded.fencingToken() + 99,
                seeded.operationBindingHash(),
                GrantType.FORWARD
        );
        ConsumeResult result = consumeService.consume(command);
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_FENCING_MISMATCH);
    }

    @Test
    void consumeDeny_wrongOperationBinding() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        ConsumeCommand command = new ConsumeCommand(
                seeded.grantId(),
                seeded.productionChangeId(),
                seeded.phase15ExecutionId(),
                seeded.targetId(),
                seeded.fingerprint(),
                seeded.authorizationGeneration(),
                seeded.fencingToken(),
                GatewayTestFixture.HASH_B,
                GrantType.FORWARD
        );
        ConsumeResult result = consumeService.consume(command);
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_wrongGrantType() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        ConsumeCommand command = commandFrom(seeded, GrantType.ROLLBACK);
        ConsumeResult result = consumeService.consume(command);
        assertDeniedIssued(seeded, result, ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH);
    }

    @Test
    void consumeDeny_expired() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedGrant(
                jdbcTemplate,
                "ISSUED",
                "FORWARD",
                Instant.now().minus(1, ChronoUnit.MINUTES),
                GatewayTestFixture.HASH_A,
                1,
                7L
        );
        ConsumeResult result = consumeService.consume(commandFrom(seeded, GrantType.FORWARD));
        assertFalse(result.succeeded());
        assertEquals(0, result.rowsUpdated());
        assertEquals(ProductionReasonCode.PRODUCTION_GRANT_EXPIRED, result.denyReason());
        assertEquals("ISSUED", grantStatus(seeded.grantId()));
    }

    @Test
    void consumeDeny_revoked() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedGrant(
                jdbcTemplate,
                "REVOKED",
                "FORWARD",
                Instant.now().plus(10, ChronoUnit.MINUTES),
                GatewayTestFixture.HASH_A,
                1,
                7L
        );
        ConsumeResult result = consumeService.consume(commandFrom(seeded, GrantType.FORWARD));
        assertFalse(result.succeeded());
        assertEquals(0, result.rowsUpdated());
        assertEquals(ProductionReasonCode.PRODUCTION_GRANT_REVOKED, result.denyReason());
        assertEquals("REVOKED", grantStatus(seeded.grantId()));
    }

    @Test
    void consumeDeny_alreadyConsumed() {
        GatewayTestFixture.SeededGrant seeded = GatewayTestFixture.seedIssuedForwardGrant(jdbcTemplate);
        ConsumeResult first = consumeService.consume(commandFrom(seeded, GrantType.FORWARD));
        assertTrue(first.succeeded());
        ConsumeResult second = consumeService.consume(commandFrom(seeded, GrantType.FORWARD));
        assertFalse(second.succeeded());
        assertEquals(0, second.rowsUpdated());
        assertEquals(ProductionReasonCode.PRODUCTION_GRANT_ALREADY_CONSUMED, second.denyReason());
        assertEquals("CONSUMED", grantStatus(seeded.grantId()));
        Integer reset = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE grant_id = ? AND status = 'ISSUED'",
                Integer.class,
                seeded.grantId()
        );
        assertEquals(0, reset);
    }

    private void assertDeniedIssued(
            GatewayTestFixture.SeededGrant seeded,
            ConsumeResult result,
            ProductionReasonCode expected
    ) {
        assertFalse(result.succeeded());
        assertEquals(0, result.rowsUpdated());
        assertEquals(expected, result.denyReason());
        assertEquals("ISSUED", grantStatus(seeded.grantId()));
    }

    private ConsumeCommand commandFrom(GatewayTestFixture.SeededGrant seeded, GrantType grantType) {
        return new ConsumeCommand(
                seeded.grantId(),
                seeded.productionChangeId(),
                seeded.phase15ExecutionId(),
                seeded.targetId(),
                seeded.fingerprint(),
                seeded.authorizationGeneration(),
                seeded.fencingToken(),
                seeded.operationBindingHash(),
                grantType
        );
    }

    private String grantStatus(UUID grantId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?",
                String.class,
                grantId
        );
    }
}
