package com.simba.snip.npo.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ActionMetrics {

    private static final Logger log = LoggerFactory.getLogger(ActionMetrics.class);

    private final AtomicLong proposed = new AtomicLong();
    private final AtomicLong policyAllow = new AtomicLong();
    private final AtomicLong policyRequireApproval = new AtomicLong();
    private final AtomicLong policyDeny = new AtomicLong();
    private final AtomicLong approved = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong mcpInvocations = new AtomicLong();
    private final AtomicLong mcpFailures = new AtomicLong();
    private final AtomicLong idempotentHits = new AtomicLong();

    public void incrementProposed() {
        proposed.incrementAndGet();
    }

    public void recordPolicy(PolicyOutcome outcome) {
        switch (outcome) {
            case ALLOW -> policyAllow.incrementAndGet();
            case REQUIRE_APPROVAL -> policyRequireApproval.incrementAndGet();
            case DENY -> policyDeny.incrementAndGet();
        }
    }

    public void incrementApproved() {
        approved.incrementAndGet();
    }

    public void incrementRejected() {
        rejected.incrementAndGet();
    }

    public void incrementMcpInvocations() {
        mcpInvocations.incrementAndGet();
    }

    public void incrementMcpFailures() {
        mcpFailures.incrementAndGet();
    }

    public void incrementIdempotentHits() {
        idempotentHits.incrementAndGet();
    }

    public void recordMcpLatencyMs(long latencyMs) {
        log.debug("mcpInvocationLatencyMs={}", latencyMs);
    }

    public long mcpInvocations() {
        return mcpInvocations.get();
    }

    public long mcpFailures() {
        return mcpFailures.get();
    }

    public long idempotentHits() {
        return idempotentHits.get();
    }
}
