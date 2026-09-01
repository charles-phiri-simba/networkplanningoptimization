package com.simba.snip.npo.integration.sync;

import java.time.Duration;

public record SynchronizationPolicy(
        String sourceSystem,
        String connectorId,
        String sourceScope,
        boolean enabled,
        SynchronizationMode preferredMode,
        Duration cadence,
        Duration requestTimeout,
        Duration maxExecutionDuration,
        int maxConsecutiveFailures,
        Duration agingAfter,
        Duration staleAfter,
        SynchronizationOverlapPolicy overlapPolicy,
        int maxRetryAttempts,
        Duration retryBackoff,
        boolean allowRecoveryFullOnScheduled
) {
    public void validate() {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalArgumentException("sourceSystem is required");
        }
        if (connectorId == null || connectorId.isBlank()) {
            throw new IllegalArgumentException("connectorId is required");
        }
        if (sourceScope == null || sourceScope.isBlank()) {
            throw new IllegalArgumentException("sourceScope is required");
        }
        if (cadence == null || cadence.isZero() || cadence.isNegative()) {
            throw new IllegalArgumentException("cadence must be positive");
        }
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        if (maxExecutionDuration == null || maxExecutionDuration.isZero() || maxExecutionDuration.isNegative()) {
            throw new IllegalArgumentException("maxExecutionDuration must be positive");
        }
        if (maxConsecutiveFailures < 1) {
            throw new IllegalArgumentException("maxConsecutiveFailures must be >= 1");
        }
        if (agingAfter == null || staleAfter == null
                || agingAfter.isZero() || agingAfter.isNegative()
                || staleAfter.isZero() || staleAfter.isNegative()) {
            throw new IllegalArgumentException("agingAfter and staleAfter must be positive");
        }
        if (agingAfter.compareTo(staleAfter) >= 0) {
            throw new IllegalArgumentException("agingAfter must be less than staleAfter");
        }
        if (overlapPolicy != SynchronizationOverlapPolicy.SKIP) {
            throw new IllegalArgumentException("only SKIP overlap policy is supported");
        }
        if (maxRetryAttempts < 0) {
            throw new IllegalArgumentException("maxRetryAttempts must be >= 0");
        }
    }
}
