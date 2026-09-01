package com.simba.snip.npo.changeintelligence.service;

import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ChangeProposalMetrics {

    private static final Logger log = LoggerFactory.getLogger(ChangeProposalMetrics.class);

    private final AtomicLong generationAttempts = new AtomicLong();
    private final AtomicLong generationBlocked = new AtomicLong();
    private final AtomicLong evaluated = new AtomicLong();
    private final AtomicLong recommended = new AtomicLong();
    private final AtomicLong approvals = new AtomicLong();
    private final AtomicLong rejections = new AtomicLong();
    private final AtomicLong invalidations = new AtomicLong();
    private final AtomicLong expirations = new AtomicLong();
    private final AtomicLong superseded = new AtomicLong();
    private final AtomicLong simulationFailures = new AtomicLong();

    public void incrementGenerationAttempts() {
        generationAttempts.incrementAndGet();
        log.debug("changeProposalGenerationAttempts=1");
    }

    public void incrementGenerationBlocked() {
        generationBlocked.incrementAndGet();
        log.debug("changeProposalGenerationBlocked=1");
    }

    public void incrementEvaluated() {
        evaluated.incrementAndGet();
        log.debug("changeProposalEvaluated=1");
    }

    public void incrementRecommended() {
        recommended.incrementAndGet();
        log.debug("changeProposalRecommended=1");
    }

    public void incrementApprovals() {
        approvals.incrementAndGet();
        log.info("changeProposalApprovals=1");
    }

    public void incrementRejections() {
        rejections.incrementAndGet();
        log.info("changeProposalRejections=1");
    }

    public void incrementInvalidations() {
        invalidations.incrementAndGet();
        log.debug("changeProposalInvalidations=1");
    }

    public void incrementExpirations() {
        expirations.incrementAndGet();
        log.debug("changeProposalExpirations=1");
    }

    public void incrementSuperseded() {
        superseded.incrementAndGet();
        log.debug("changeProposalSuperseded=1");
    }

    public void incrementSimulationFailures() {
        simulationFailures.incrementAndGet();
        log.debug("changeProposalSimulationFailures=1");
    }

    public void recordEvaluationDurationMs(long durationMs) {
        log.debug("changeProposalEvaluationDurationMs={}", durationMs);
    }

    public void recordKnowledgeCategory(NetworkKnowledgeConfidence confidence) {
        log.debug("changeProposalKnowledgeCategory={}", confidence.name());
    }

    public void recordRiskCategory(String riskLevel) {
        log.debug("changeProposalRiskCategory={}", riskLevel);
    }

    public long generationAttempts() {
        return generationAttempts.get();
    }

    public long recommendedCount() {
        return recommended.get();
    }

    public long approvals() {
        return approvals.get();
    }
}
