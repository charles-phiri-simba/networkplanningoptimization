package com.simba.snip.npo.assurance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class AssuranceMetrics {

    private static final Logger log = LoggerFactory.getLogger(AssuranceMetrics.class);

    private final AtomicLong detected = new AtomicLong();
    private final AtomicLong created = new AtomicLong();
    private final AtomicLong updated = new AtomicLong();
    private final AtomicLong noMatch = new AtomicLong();

    public void incrementDetected() {
        detected.incrementAndGet();
    }

    public void incrementCreated() {
        created.incrementAndGet();
    }

    public void incrementUpdated() {
        updated.incrementAndGet();
    }

    public void incrementNoMatch() {
        noMatch.incrementAndGet();
    }

    public void recordDetectionLatencyMs(long latencyMs) {
        log.debug("assuranceDetectionLatencyMs={}", latencyMs);
    }

    public void recordAssessmentLatencyMs(long latencyMs) {
        log.debug("decisionAssessmentLatencyMs={}", latencyMs);
    }

    public long detected() {
        return detected.get();
    }

    public long created() {
        return created.get();
    }

    public long updated() {
        return updated.get();
    }

    public long noMatch() {
        return noMatch.get();
    }
}
