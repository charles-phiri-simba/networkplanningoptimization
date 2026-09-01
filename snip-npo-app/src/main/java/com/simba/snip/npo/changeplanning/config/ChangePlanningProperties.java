package com.simba.snip.npo.changeplanning.config;

import com.simba.snip.npo.domain.DomainValidationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "snip.change-planning")
public class ChangePlanningProperties {

    private boolean enabled = true;
    private Duration validityDuration = Duration.ofHours(24);
    private int maximumOperationCount = 1;
    private boolean requireRollback = true;
    private boolean requireCurrentValueMatch = true;
    private boolean requireHighOrMediumKnowledge = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getValidityDuration() {
        return validityDuration;
    }

    public void setValidityDuration(Duration validityDuration) {
        this.validityDuration = validityDuration;
    }

    public int getMaximumOperationCount() {
        return maximumOperationCount;
    }

    public void setMaximumOperationCount(int maximumOperationCount) {
        this.maximumOperationCount = maximumOperationCount;
    }

    public boolean isRequireRollback() {
        return requireRollback;
    }

    public void setRequireRollback(boolean requireRollback) {
        this.requireRollback = requireRollback;
    }

    public boolean isRequireCurrentValueMatch() {
        return requireCurrentValueMatch;
    }

    public void setRequireCurrentValueMatch(boolean requireCurrentValueMatch) {
        this.requireCurrentValueMatch = requireCurrentValueMatch;
    }

    public boolean isRequireHighOrMediumKnowledge() {
        return requireHighOrMediumKnowledge;
    }

    public void setRequireHighOrMediumKnowledge(boolean requireHighOrMediumKnowledge) {
        this.requireHighOrMediumKnowledge = requireHighOrMediumKnowledge;
    }

    public void validate() {
        if (validityDuration == null || validityDuration.isZero() || validityDuration.isNegative()) {
            throw new DomainValidationException("snip.change-planning.validity-duration must be positive");
        }
        if (maximumOperationCount <= 0) {
            throw new DomainValidationException("snip.change-planning.maximum-operation-count must be positive");
        }
    }
}
