package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionRateLimitCounters;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.service.ProductionRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeRateLimitZeroIT extends ProductionChangeITSupport {

    @Autowired
    ProductionRateLimitService rateLimitService;
    @Autowired
    ProductionChangeProperties properties;

    @Test
    void maximumZeroAbsentRowDeniesWithoutGrantOrMutation() {
        String scopeKey = "absent-zero-" + UUID.randomUUID();
        ProductionChangeException denied = assertThrows(
                ProductionChangeException.class,
                () -> rateLimitService.consumeOrDeny(
                        ProductionRateLimitCounters.CELL_DAY, scopeKey, Duration.ofDays(1), 0));
        assertEquals(ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, denied.reasonCode());
        assertEquals(0, counterRows(scopeKey));

        int target = properties.getMaximumChangesPerTargetPerHour();
        int cell = properties.getMaximumChangesPerCellPerDay();
        int actor = properties.getMaximumGrantsPerActorPerHour();
        properties.setMaximumChangesPerTargetPerHour(0);
        properties.setMaximumChangesPerCellPerDay(0);
        properties.setMaximumGrantsPerActorPerHour(0);
        try {
            ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
            seedTransportFor(change);
            executeProductionChange(change.productionChangeId());
            assertEquals(0, grantCount(change.productionChangeId(), "ISSUED"));
            assertEquals(0, grantCount(change.productionChangeId(), "CONSUMED"));
            assertEquals(0, mutationCount());
        } finally {
            properties.setMaximumChangesPerTargetPerHour(target);
            properties.setMaximumChangesPerCellPerDay(cell);
            properties.setMaximumGrantsPerActorPerHour(actor);
        }
    }

    @Test
    void maximumZeroExistingRowDenied() {
        String scopeKey = "existing-zero-" + UUID.randomUUID();
        java.sql.Timestamp now = java.sql.Timestamp.from(java.time.Instant.now());
        jdbc.update("""
                INSERT INTO production_rate_limit_state
                    (counter_id, scope_type, scope_key, window_start, count, updated_at)
                VALUES (?, ?, ?, ?, 0, ?)
                """,
                ProductionRateLimitCounters.counterId(
                        ProductionRateLimitCounters.CELL_DAY, scopeKey,
                        ProductionRateLimitCounters.align(java.time.Instant.now(), Duration.ofDays(1))),
                ProductionRateLimitCounters.CELL_DAY,
                scopeKey,
                now,
                now);
        ProductionChangeException denied = assertThrows(
                ProductionChangeException.class,
                () -> rateLimitService.consumeOrDeny(
                        ProductionRateLimitCounters.CELL_DAY, scopeKey, Duration.ofDays(1), 0));
        assertEquals(ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, denied.reasonCode());
        assertEquals(0, mutationCount());
    }

    @Test
    void maximumOneAbsentRowFirstAcquisitionSucceeds() {
        String scopeKey = "absent-one-" + UUID.randomUUID();
        rateLimitService.consumeOrDeny(ProductionRateLimitCounters.CELL_DAY, scopeKey, Duration.ofDays(1), 1);
        assertEquals(1, counterRows(scopeKey));
    }

    @Test
    void maximumOneConcurrentSecondDenied() throws Exception {
        String scopeKey = "concurrent-one-" + UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger denials = new AtomicInteger();
        try {
            Future<?> a = pool.submit(() -> runConsume(start, scopeKey, successes, denials));
            Future<?> b = pool.submit(() -> runConsume(start, scopeKey, successes, denials));
            start.countDown();
            a.get();
            b.get();
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, successes.get());
        assertEquals(1, denials.get());
        Integer count = jdbc.queryForObject(
                "SELECT count FROM production_rate_limit_state WHERE scope_key = ?",
                Integer.class,
                scopeKey);
        assertEquals(1, count);
    }

    @Test
    void unknownPolicyStateDenied() {
        ProductionChangeException missingKey = assertThrows(
                ProductionChangeException.class,
                () -> rateLimitService.consumeOrDeny(
                        ProductionRateLimitCounters.CELL_DAY, null, Duration.ofDays(1), 1));
        assertEquals(ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, missingKey.reasonCode());
        ProductionChangeException missingType = assertThrows(
                ProductionChangeException.class,
                () -> rateLimitService.consumeOrDeny("  ", "key", Duration.ofDays(1), 1));
        assertEquals(ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, missingType.reasonCode());
        ProductionChangeException missingWindow = assertThrows(
                ProductionChangeException.class,
                () -> rateLimitService.consumeOrDeny(ProductionRateLimitCounters.CELL_DAY, "key", null, 1));
        assertEquals(ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, missingWindow.reasonCode());
    }

    @Test
    void negativeMaximumRejectedOrFailClosed() {
        ProductionChangeProperties isolated = new ProductionChangeProperties();
        isolated.setMaximumChangesPerTargetPerHour(-1);
        assertThrows(DomainValidationException.class, isolated::validate);
        isolated.setMaximumChangesPerTargetPerHour(6);
        isolated.setMaximumChangesPerCellPerDay(-2);
        assertThrows(DomainValidationException.class, isolated::validate);
        isolated.setMaximumChangesPerCellPerDay(3);
        isolated.setMaximumGrantsPerActorPerHour(-3);
        assertThrows(DomainValidationException.class, isolated::validate);

        ProductionChangeException denied = assertThrows(
                ProductionChangeException.class,
                () -> rateLimitService.consumeOrDeny(
                        ProductionRateLimitCounters.CELL_DAY,
                        "negative-" + UUID.randomUUID(),
                        Duration.ofDays(1),
                        -1));
        assertEquals(ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, denied.reasonCode());
    }

    @Test
    void rollbackIssuanceAlsoDeniesMaximumZero() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        executeExpectingOk(change.productionChangeId());
        rollbackRequest(change.productionChangeId());
        rollbackReview(change.productionChangeId());
        rollbackAuthorize(change.productionChangeId());
        int target = properties.getMaximumChangesPerTargetPerHour();
        int cell = properties.getMaximumChangesPerCellPerDay();
        int actor = properties.getMaximumGrantsPerActorPerHour();
        properties.setMaximumChangesPerTargetPerHour(0);
        properties.setMaximumChangesPerCellPerDay(0);
        properties.setMaximumGrantsPerActorPerHour(0);
        int before = mutationCount();
        try {
            var denied = rollbackExecute(change.productionChangeId());
            assertTrue(denied.getStatusCode().is4xxClientError());
            assertEquals(0, grantCount(change.productionChangeId(), "ISSUED"));
            assertEquals(before, mutationCount());
        } finally {
            properties.setMaximumChangesPerTargetPerHour(target);
            properties.setMaximumChangesPerCellPerDay(cell);
            properties.setMaximumGrantsPerActorPerHour(actor);
        }
    }

    private void runConsume(
            CountDownLatch start,
            String scopeKey,
            AtomicInteger successes,
            AtomicInteger denials
    ) {
        try {
            start.await();
            rateLimitService.consumeOrDeny(
                    ProductionRateLimitCounters.CELL_DAY, scopeKey, Duration.ofDays(1), 1);
            successes.incrementAndGet();
        } catch (ProductionChangeException ex) {
            assertEquals(ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, ex.reasonCode());
            denials.incrementAndGet();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private int counterRows(String scopeKey) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_rate_limit_state WHERE scope_key = ?",
                Integer.class,
                scopeKey);
        return count == null ? 0 : count;
    }
}
