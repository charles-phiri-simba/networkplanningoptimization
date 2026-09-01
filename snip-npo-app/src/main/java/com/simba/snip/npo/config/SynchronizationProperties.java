package com.simba.snip.npo.config;

import com.simba.snip.npo.integration.sync.SynchronizationMode;
import com.simba.snip.npo.integration.sync.SynchronizationOverlapPolicy;
import com.simba.snip.npo.integration.sync.SynchronizationPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "snip.integration.sync")
public class SynchronizationProperties {

    private boolean schedulerEnabled = true;
    private Duration schedulerTick = Duration.ofSeconds(5);
    private Duration schedulerJitter = Duration.ofMillis(500);
    private int maxDueSourcesPerTick = 4;
    private List<PolicyEntry> policies = new ArrayList<>();

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }

    public Duration getSchedulerTick() {
        return schedulerTick;
    }

    public void setSchedulerTick(Duration schedulerTick) {
        this.schedulerTick = schedulerTick;
    }

    public Duration getSchedulerJitter() {
        return schedulerJitter;
    }

    public void setSchedulerJitter(Duration schedulerJitter) {
        this.schedulerJitter = schedulerJitter;
    }

    public int getMaxDueSourcesPerTick() {
        return maxDueSourcesPerTick;
    }

    public void setMaxDueSourcesPerTick(int maxDueSourcesPerTick) {
        this.maxDueSourcesPerTick = maxDueSourcesPerTick;
    }

    public List<PolicyEntry> getPolicies() {
        return policies;
    }

    public void setPolicies(List<PolicyEntry> policies) {
        this.policies = policies == null ? new ArrayList<>() : policies;
    }

    public List<SynchronizationPolicy> validatedPolicies() {
        List<SynchronizationPolicy> resolved = new ArrayList<>();
        for (PolicyEntry entry : policies) {
            SynchronizationPolicy policy = entry.toPolicy();
            policy.validate();
            resolved.add(policy);
        }
        return List.copyOf(resolved);
    }

    public static class PolicyEntry {
        private String sourceSystem = "ERICSSON_ENM";
        private String connectorId = "ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER";
        private String sourceScope = "default";
        private boolean enabled = true;
        private String preferredMode = "INCREMENTAL";
        private Duration cadence = Duration.ofMinutes(15);
        private Duration requestTimeout = Duration.ofSeconds(2);
        private Duration maxExecutionDuration = Duration.ofSeconds(30);
        private int maxConsecutiveFailures = 3;
        private Duration agingAfter = Duration.ofMinutes(30);
        private Duration staleAfter = Duration.ofHours(2);
        private String overlapPolicy = "SKIP";
        private int maxRetryAttempts = 2;
        private Duration retryBackoff = Duration.ofSeconds(5);
        private boolean allowRecoveryFullOnScheduled = false;

        public SynchronizationPolicy toPolicy() {
            return new SynchronizationPolicy(
                    sourceSystem,
                    connectorId,
                    sourceScope,
                    enabled,
                    SynchronizationMode.valueOf(preferredMode.trim().toUpperCase()),
                    cadence,
                    requestTimeout,
                    maxExecutionDuration,
                    maxConsecutiveFailures,
                    agingAfter,
                    staleAfter,
                    SynchronizationOverlapPolicy.valueOf(overlapPolicy.trim().toUpperCase()),
                    maxRetryAttempts,
                    retryBackoff,
                    allowRecoveryFullOnScheduled
            );
        }

        public String getSourceSystem() {
            return sourceSystem;
        }

        public void setSourceSystem(String sourceSystem) {
            this.sourceSystem = sourceSystem;
        }

        public String getConnectorId() {
            return connectorId;
        }

        public void setConnectorId(String connectorId) {
            this.connectorId = connectorId;
        }

        public String getSourceScope() {
            return sourceScope;
        }

        public void setSourceScope(String sourceScope) {
            this.sourceScope = sourceScope;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPreferredMode() {
            return preferredMode;
        }

        public void setPreferredMode(String preferredMode) {
            this.preferredMode = preferredMode;
        }

        public Duration getCadence() {
            return cadence;
        }

        public void setCadence(Duration cadence) {
            this.cadence = cadence;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }

        public Duration getMaxExecutionDuration() {
            return maxExecutionDuration;
        }

        public void setMaxExecutionDuration(Duration maxExecutionDuration) {
            this.maxExecutionDuration = maxExecutionDuration;
        }

        public int getMaxConsecutiveFailures() {
            return maxConsecutiveFailures;
        }

        public void setMaxConsecutiveFailures(int maxConsecutiveFailures) {
            this.maxConsecutiveFailures = maxConsecutiveFailures;
        }

        public Duration getAgingAfter() {
            return agingAfter;
        }

        public void setAgingAfter(Duration agingAfter) {
            this.agingAfter = agingAfter;
        }

        public Duration getStaleAfter() {
            return staleAfter;
        }

        public void setStaleAfter(Duration staleAfter) {
            this.staleAfter = staleAfter;
        }

        public String getOverlapPolicy() {
            return overlapPolicy;
        }

        public void setOverlapPolicy(String overlapPolicy) {
            this.overlapPolicy = overlapPolicy;
        }

        public int getMaxRetryAttempts() {
            return maxRetryAttempts;
        }

        public void setMaxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
        }

        public Duration getRetryBackoff() {
            return retryBackoff;
        }

        public void setRetryBackoff(Duration retryBackoff) {
            this.retryBackoff = retryBackoff;
        }

        public boolean isAllowRecoveryFullOnScheduled() {
            return allowRecoveryFullOnScheduled;
        }

        public void setAllowRecoveryFullOnScheduled(boolean allowRecoveryFullOnScheduled) {
            this.allowRecoveryFullOnScheduled = allowRecoveryFullOnScheduled;
        }
    }
}
