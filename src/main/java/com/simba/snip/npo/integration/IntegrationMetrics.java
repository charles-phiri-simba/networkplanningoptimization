package com.simba.snip.npo.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class IntegrationMetrics {

    private static final Logger log = LoggerFactory.getLogger(IntegrationMetrics.class);

    private final AtomicLong importsStarted = new AtomicLong();
    private final AtomicLong importsSucceeded = new AtomicLong();
    private final AtomicLong importsFailed = new AtomicLong();
    private final AtomicLong recordsRead = new AtomicLong();
    private final AtomicLong recordsCreated = new AtomicLong();
    private final AtomicLong recordsUpdated = new AtomicLong();
    private final AtomicLong recordsUnchanged = new AtomicLong();
    private final AtomicLong recordsRejected = new AtomicLong();
    private final AtomicLong conflictsDetected = new AtomicLong();
    private final AtomicLong missingEntitiesDetected = new AtomicLong();

    public void incrementStarted() {
        importsStarted.incrementAndGet();
    }

    public void recordSuccess(ReconciliationResult result, long latencyMs, UUIDLike ids) {
        importsSucceeded.incrementAndGet();
        recordsRead.addAndGet(result.entitiesRead());
        recordsCreated.addAndGet(result.created());
        recordsUpdated.addAndGet(result.updated());
        recordsUnchanged.addAndGet(result.unchanged());
        recordsRejected.addAndGet(result.rejected());
        conflictsDetected.addAndGet(result.conflicts());
        missingEntitiesDetected.addAndGet(result.missing());
        log.info(
                "importSucceeded importId={} sourceSystem={} snapshotId={} created={} updated={} unchanged={} rejected={} conflicts={} missing={} latencyMs={}",
                ids.importId(),
                ids.sourceSystem(),
                ids.snapshotId(),
                result.created(),
                result.updated(),
                result.unchanged(),
                result.rejected(),
                result.conflicts(),
                result.missing(),
                latencyMs
        );
    }

    public void recordFailure(String importId, String sourceSystem, String error, long latencyMs) {
        importsFailed.incrementAndGet();
        log.warn("importFailed importId={} sourceSystem={} latencyMs={} error={}", importId, sourceSystem, latencyMs, error);
    }

    public long importsStarted() {
        return importsStarted.get();
    }

    public long importsSucceeded() {
        return importsSucceeded.get();
    }

    public long importsFailed() {
        return importsFailed.get();
    }

    public long conflictsDetected() {
        return conflictsDetected.get();
    }

    public record UUIDLike(String importId, String sourceSystem, String snapshotId) {
    }
}
