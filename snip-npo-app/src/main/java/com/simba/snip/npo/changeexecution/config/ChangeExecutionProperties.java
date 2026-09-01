package com.simba.snip.npo.changeexecution.config;

import com.simba.snip.npo.changeexecution.domain.ExecutionTargetType;
import com.simba.snip.npo.domain.DomainValidationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "snip.change-execution")
public class ChangeExecutionProperties {

    private boolean enabled = false;
    private int maximumOperationCount = 1;
    private int maximumForwardAttempts = 1;
    private List<ExecutionTargetType> permittedTargetTypes = new ArrayList<>(List.of(ExecutionTargetType.SIMULATOR));
    private boolean requireExecutionReview = true;
    private boolean requireExecutionAuthorization = true;
    private boolean requireCurrentValueMatch = true;
    private boolean requireVerification = true;
    private boolean requireRollbackReview = true;
    private boolean requireRollbackAuthorization = true;
    private boolean automaticRollbackEnabled = false;
    private Duration leaseDuration = Duration.ofSeconds(30);
    private Duration executionWindowDuration = Duration.ofHours(1);
    private String instanceId = "local";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaximumOperationCount() {
        return maximumOperationCount;
    }

    public void setMaximumOperationCount(int maximumOperationCount) {
        this.maximumOperationCount = maximumOperationCount;
    }

    public int getMaximumForwardAttempts() {
        return maximumForwardAttempts;
    }

    public void setMaximumForwardAttempts(int maximumForwardAttempts) {
        this.maximumForwardAttempts = maximumForwardAttempts;
    }

    public List<ExecutionTargetType> getPermittedTargetTypes() {
        return permittedTargetTypes;
    }

    public void setPermittedTargetTypes(List<ExecutionTargetType> permittedTargetTypes) {
        this.permittedTargetTypes = permittedTargetTypes;
    }

    public boolean isRequireExecutionReview() {
        return requireExecutionReview;
    }

    public void setRequireExecutionReview(boolean requireExecutionReview) {
        this.requireExecutionReview = requireExecutionReview;
    }

    public boolean isRequireExecutionAuthorization() {
        return requireExecutionAuthorization;
    }

    public void setRequireExecutionAuthorization(boolean requireExecutionAuthorization) {
        this.requireExecutionAuthorization = requireExecutionAuthorization;
    }

    public boolean isRequireCurrentValueMatch() {
        return requireCurrentValueMatch;
    }

    public void setRequireCurrentValueMatch(boolean requireCurrentValueMatch) {
        this.requireCurrentValueMatch = requireCurrentValueMatch;
    }

    public boolean isRequireVerification() {
        return requireVerification;
    }

    public void setRequireVerification(boolean requireVerification) {
        this.requireVerification = requireVerification;
    }

    public boolean isRequireRollbackReview() {
        return requireRollbackReview;
    }

    public void setRequireRollbackReview(boolean requireRollbackReview) {
        this.requireRollbackReview = requireRollbackReview;
    }

    public boolean isRequireRollbackAuthorization() {
        return requireRollbackAuthorization;
    }

    public void setRequireRollbackAuthorization(boolean requireRollbackAuthorization) {
        this.requireRollbackAuthorization = requireRollbackAuthorization;
    }

    public boolean isAutomaticRollbackEnabled() {
        return automaticRollbackEnabled;
    }

    public void setAutomaticRollbackEnabled(boolean automaticRollbackEnabled) {
        this.automaticRollbackEnabled = automaticRollbackEnabled;
    }

    public Duration getLeaseDuration() {
        return leaseDuration;
    }

    public void setLeaseDuration(Duration leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

    public Duration getExecutionWindowDuration() {
        return executionWindowDuration;
    }

    public void setExecutionWindowDuration(Duration executionWindowDuration) {
        this.executionWindowDuration = executionWindowDuration;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void validate() {
        if (maximumOperationCount != 1) {
            throw new DomainValidationException("snip.change-execution.maximum-operation-count must equal 1");
        }
        if (maximumForwardAttempts != 1) {
            throw new DomainValidationException("snip.change-execution.maximum-forward-attempts must equal 1");
        }
        if (permittedTargetTypes == null || permittedTargetTypes.isEmpty()) {
            throw new DomainValidationException("snip.change-execution.permitted-target-types must not be empty");
        }
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new DomainValidationException("snip.change-execution.lease-duration must be positive");
        }
        if (executionWindowDuration == null || executionWindowDuration.isZero() || executionWindowDuration.isNegative()) {
            throw new DomainValidationException("snip.change-execution.execution-window-duration must be positive");
        }
        if (instanceId == null || instanceId.isBlank()) {
            throw new DomainValidationException("snip.change-execution.instance-id must not be blank");
        }
        if (!requireExecutionReview
                || !requireExecutionAuthorization
                || !requireCurrentValueMatch
                || !requireVerification
                || !requireRollbackReview
                || !requireRollbackAuthorization) {
            throw new DomainValidationException("mandatory change-execution safety gates cannot be disabled");
        }
        if (automaticRollbackEnabled) {
            throw new DomainValidationException("snip.change-execution.automatic-rollback-enabled must be false");
        }
    }
}
