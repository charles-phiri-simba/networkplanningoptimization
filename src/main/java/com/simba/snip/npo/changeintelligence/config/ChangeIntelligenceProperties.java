package com.simba.snip.npo.changeintelligence.config;

import com.simba.snip.npo.domain.DomainValidationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "snip.change-intelligence")
public class ChangeIntelligenceProperties {

    private int candidateStep = 1;
    private int maxDelta = 4;
    private int maxCandidates = 5;
    private int validityHours = 24;
    private BigDecimal minBenefitScore = new BigDecimal("0.01");

    public int getCandidateStep() {
        return candidateStep;
    }

    public void setCandidateStep(int candidateStep) {
        this.candidateStep = candidateStep;
    }

    public int getMaxDelta() {
        return maxDelta;
    }

    public void setMaxDelta(int maxDelta) {
        this.maxDelta = maxDelta;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public int getValidityHours() {
        return validityHours;
    }

    public void setValidityHours(int validityHours) {
        this.validityHours = validityHours;
    }

    public BigDecimal getMinBenefitScore() {
        return minBenefitScore;
    }

    public void setMinBenefitScore(BigDecimal minBenefitScore) {
        this.minBenefitScore = minBenefitScore;
    }

    public Duration validityDuration() {
        return Duration.ofHours(validityHours);
    }

    public void validate() {
        if (candidateStep <= 0) {
            throw new DomainValidationException("snip.change-intelligence.candidate-step must be positive");
        }
        if (maxDelta < 0) {
            throw new DomainValidationException("snip.change-intelligence.max-delta must be non-negative");
        }
        if (maxCandidates <= 0) {
            throw new DomainValidationException("snip.change-intelligence.max-candidates must be positive");
        }
        if (validityHours <= 0) {
            throw new DomainValidationException("snip.change-intelligence.validity-hours must be positive");
        }
    }
}
