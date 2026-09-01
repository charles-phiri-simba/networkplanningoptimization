package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeExecuteRaceIT extends ProductionChangeITSupport {

    @Test
    void concurrentExecute_maxOneMutation() throws Exception {
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(authorized);
        mutationCounter().set(0);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ResponseEntity<String>> a = pool.submit(() -> {
                start.await();
                return http.exchange(
                        "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                        HttpMethod.POST,
                        productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                        String.class);
            });
            Future<ResponseEntity<String>> b = pool.submit(() -> {
                start.await();
                return http.exchange(
                        "/api/v1/production-changes/" + authorized.productionChangeId() + "/execute",
                        HttpMethod.POST,
                        productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, "executor-2"),
                        String.class);
            });
            start.countDown();
            a.get();
            b.get();
            // Concurrent execute race: architecture permits 0 or 1 because the simulated
            // winner cannot positively establish whether send occurred for both callers.
            assertTrue(mutationCount() <= 1);
        } finally {
            pool.shutdownNow();
        }
    }
}
