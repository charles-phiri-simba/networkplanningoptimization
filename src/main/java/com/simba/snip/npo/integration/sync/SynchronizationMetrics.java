package com.simba.snip.npo.integration.sync;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class SynchronizationMetrics {

    private final AtomicLong runs = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong overlapSkips = new AtomicLong();
    private final AtomicLong recoveryRequired = new AtomicLong();
    private final AtomicLong driftDetected = new AtomicLong();

    public void incrementRuns() {
        runs.incrementAndGet();
    }

    public void incrementSuccesses() {
        successes.incrementAndGet();
    }

    public void incrementFailures() {
        failures.incrementAndGet();
    }

    public void incrementOverlapSkips() {
        overlapSkips.incrementAndGet();
    }

    public void incrementRecoveryRequired() {
        recoveryRequired.incrementAndGet();
    }

    public void incrementDriftDetected() {
        driftDetected.incrementAndGet();
    }

    public long runs() {
        return runs.get();
    }

    public long successes() {
        return successes.get();
    }

    public long failures() {
        return failures.get();
    }

    public long overlapSkips() {
        return overlapSkips.get();
    }
}
