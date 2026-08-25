package com.simba.snip.npo.twin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class TwinMetrics {

    private static final Logger log = LoggerFactory.getLogger(TwinMetrics.class);

    private final AtomicLong twinSynchronizations = new AtomicLong();
    private final AtomicLong twinSynchronizationFailures = new AtomicLong();
    private final AtomicLong twinVersionsCreated = new AtomicLong();
    private final AtomicLong twinStaleDetections = new AtomicLong();
    private final AtomicLong simulationScenariosCreated = new AtomicLong();
    private final AtomicLong simulationRunsStarted = new AtomicLong();
    private final AtomicLong simulationRunsSucceeded = new AtomicLong();
    private final AtomicLong simulationRunsFailed = new AtomicLong();
    private final AtomicLong scenarioComparisons = new AtomicLong();

    public void incrementSynchronizations() {
        twinSynchronizations.incrementAndGet();
    }

    public void incrementSynchronizationFailures() {
        twinSynchronizationFailures.incrementAndGet();
    }

    public void incrementVersionsCreated() {
        twinVersionsCreated.incrementAndGet();
    }

    public void incrementStaleDetections() {
        twinStaleDetections.incrementAndGet();
    }

    public void incrementScenariosCreated() {
        simulationScenariosCreated.incrementAndGet();
    }

    public void incrementRunsStarted() {
        simulationRunsStarted.incrementAndGet();
    }

    public void incrementRunsSucceeded() {
        simulationRunsSucceeded.incrementAndGet();
    }

    public void incrementRunsFailed() {
        simulationRunsFailed.incrementAndGet();
    }

    public void incrementComparisons() {
        scenarioComparisons.incrementAndGet();
    }

    public void recordLatencyMs(long latencyMs) {
        log.debug("simulationLatencyMs={}", latencyMs);
    }

    public long twinSynchronizations() {
        return twinSynchronizations.get();
    }

    public long twinVersionsCreated() {
        return twinVersionsCreated.get();
    }

    public long twinStaleDetections() {
        return twinStaleDetections.get();
    }

    public long simulationRunsSucceeded() {
        return simulationRunsSucceeded.get();
    }

    public long simulationRunsFailed() {
        return simulationRunsFailed.get();
    }
}
