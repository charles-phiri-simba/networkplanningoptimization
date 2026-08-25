package com.simba.snip.npo.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class AgentMetrics {

    private static final Logger log = LoggerFactory.getLogger(AgentMetrics.class);

    private final AtomicLong runsStarted = new AtomicLong();
    private final AtomicLong runsCompleted = new AtomicLong();
    private final AtomicLong runsFailed = new AtomicLong();
    private final AtomicLong stepsStarted = new AtomicLong();
    private final AtomicLong stepsCompleted = new AtomicLong();
    private final AtomicLong stepsFailed = new AtomicLong();
    private final AtomicLong modelCalls = new AtomicLong();
    private final AtomicLong retries = new AtomicLong();
    private final AtomicLong actionsProposed = new AtomicLong();
    private final AtomicLong limitReached = new AtomicLong();

    public void incrementRunsStarted() {
        runsStarted.incrementAndGet();
    }

    public void incrementRunsCompleted() {
        runsCompleted.incrementAndGet();
    }

    public void incrementRunsFailed() {
        runsFailed.incrementAndGet();
    }

    public void incrementStepsStarted() {
        stepsStarted.incrementAndGet();
    }

    public void incrementStepsCompleted() {
        stepsCompleted.incrementAndGet();
    }

    public void incrementStepsFailed() {
        stepsFailed.incrementAndGet();
    }

    public void incrementModelCalls() {
        modelCalls.incrementAndGet();
    }

    public void incrementRetries() {
        retries.incrementAndGet();
    }

    public void incrementActionsProposed() {
        actionsProposed.incrementAndGet();
    }

    public void incrementLimitReached() {
        limitReached.incrementAndGet();
    }

    public void recordRunLatencyMs(long latencyMs) {
        log.info("agentRunLatencyMs={}", latencyMs);
    }

    public long runsStarted() {
        return runsStarted.get();
    }

    public long runsCompleted() {
        return runsCompleted.get();
    }

    public long runsFailed() {
        return runsFailed.get();
    }

    public long stepsStarted() {
        return stepsStarted.get();
    }

    public long stepsCompleted() {
        return stepsCompleted.get();
    }

    public long stepsFailed() {
        return stepsFailed.get();
    }

    public long modelCalls() {
        return modelCalls.get();
    }

    public long retries() {
        return retries.get();
    }

    public long actionsProposed() {
        return actionsProposed.get();
    }

    public long limitReached() {
        return limitReached.get();
    }
}
