package com.simba.snip.npo.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final AtomicLong leaseAcquired = new AtomicLong();
    private final AtomicLong leaseRejected = new AtomicLong();
    private final AtomicLong leaseExpired = new AtomicLong();
    private final AtomicLong retries = new AtomicLong();
    private final AtomicLong replays = new AtomicLong();
    private final AtomicLong timeouts = new AtomicLong();
    private final AtomicLong concurrentRejected = new AtomicLong();
    private final Map<String, AtomicLong> failuresByCode = new ConcurrentHashMap<>();

    public void incrementStarted() {
        importsStarted.incrementAndGet();
    }

    public void incrementLeaseAcquired() {
        leaseAcquired.incrementAndGet();
    }

    public void incrementLeaseRejected() {
        leaseRejected.incrementAndGet();
    }

    public void incrementLeaseExpired() {
        leaseExpired.incrementAndGet();
    }

    public void incrementRetries() {
        retries.incrementAndGet();
    }

    public void incrementReplays() {
        replays.incrementAndGet();
    }

    public void incrementTimeouts() {
        timeouts.incrementAndGet();
    }

    public void incrementConcurrentRejected() {
        concurrentRejected.incrementAndGet();
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
                "importSucceeded executionId={} sourceSystem={} sourceScope={} snapshotId={} fencingToken={} created={} updated={} unchanged={} rejected={} conflicts={} missing={} latencyMs={}",
                ids.importId(),
                ids.sourceSystem(),
                ids.sourceScope(),
                ids.snapshotId(),
                ids.fencingToken(),
                result.created(),
                result.updated(),
                result.unchanged(),
                result.rejected(),
                result.conflicts(),
                result.missing(),
                latencyMs
        );
    }

    public void recordFailure(
            String importId,
            String sourceSystem,
            String sourceScope,
            String snapshotId,
            ImportFailureCode code,
            long latencyMs
    ) {
        importsFailed.incrementAndGet();
        failuresByCode.computeIfAbsent(code.name(), ignored -> new AtomicLong()).incrementAndGet();
        log.warn(
                "importFailed executionId={} sourceSystem={} sourceScope={} snapshotId={} failureCode={} latencyMs={}",
                importId, sourceSystem, sourceScope, snapshotId, code, latencyMs
        );
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

    public long leaseAcquired() {
        return leaseAcquired.get();
    }

    public long leaseRejected() {
        return leaseRejected.get();
    }

    public long leaseExpired() {
        return leaseExpired.get();
    }

    public long retries() {
        return retries.get();
    }

    public long replays() {
        return replays.get();
    }

    public long timeouts() {
        return timeouts.get();
    }

    public long concurrentRejected() {
        return concurrentRejected.get();
    }

    public record UUIDLike(
            String importId,
            String sourceSystem,
            String snapshotId,
            String sourceScope,
            Long fencingToken
    ) {
    }
}
