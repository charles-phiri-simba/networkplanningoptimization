package com.simba.snip.npo.productionwritegateway.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ProductionGatewayMetrics {

    private final AtomicLong grantConsumeConflicts = new AtomicLong();
    private final AtomicLong killSwitchDenials = new AtomicLong();
    private final AtomicLong rateLimitDenials = new AtomicLong();
    private final AtomicLong outcomeUnknown = new AtomicLong();
    private final AtomicLong verificationFailures = new AtomicLong();
    private final AtomicLong executionAttempts = new AtomicLong();
    private final AtomicLong verified = new AtomicLong();
    private final AtomicLong auditChainInvalid = new AtomicLong();
    private final AtomicLong targetSuspensions = new AtomicLong();

    public void incrementGrantConsumeConflicts() {
        grantConsumeConflicts.incrementAndGet();
    }

    public void incrementKillSwitchDenials() {
        killSwitchDenials.incrementAndGet();
    }

    public void incrementRateLimitDenials() {
        rateLimitDenials.incrementAndGet();
    }

    public void incrementOutcomeUnknown() {
        outcomeUnknown.incrementAndGet();
    }

    public void incrementVerificationFailures() {
        verificationFailures.incrementAndGet();
    }

    public void incrementExecutionAttempts() {
        executionAttempts.incrementAndGet();
    }

    public void incrementVerified() {
        verified.incrementAndGet();
    }

    public void incrementAuditChainInvalid() {
        auditChainInvalid.incrementAndGet();
    }

    public void incrementTargetSuspensions() {
        targetSuspensions.incrementAndGet();
    }

    public long grantConsumeConflicts() {
        return grantConsumeConflicts.get();
    }

    public long killSwitchDenials() {
        return killSwitchDenials.get();
    }

    public long rateLimitDenials() {
        return rateLimitDenials.get();
    }
}
