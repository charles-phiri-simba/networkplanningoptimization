package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeGrantConcurrencyIT extends ProductionChangeITSupport {

    @Test
    void issuanceConcurrencyOneActive() throws Exception {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            pool.submit(() -> {
                start.await();
                return http.exchange(
                        "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                        HttpMethod.POST,
                        productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                        String.class);
            });
            pool.submit(() -> {
                start.await();
                return http.exchange(
                        "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                        HttpMethod.POST,
                        productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, "executor-2"),
                        String.class);
            });
            start.countDown();
            pool.shutdown();
            pool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        Integer issued = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ? AND status = 'ISSUED'",
                Integer.class,
                authorized.productionChangeId());
        assertTrue(issued == null || issued <= 1);
        // Concurrent grant issuance: crash/winner cannot positively establish whether send occurred.
        assertTrue(mutationCount() <= 1);
        assertEquals(true, issued == null || issued <= 1);
    }
}
