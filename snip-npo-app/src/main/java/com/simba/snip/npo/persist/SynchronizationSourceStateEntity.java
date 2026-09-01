package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "synchronization_source_state")
public class SynchronizationSourceStateEntity {

    @Id
    private UUID id;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "connector_id", nullable = false, length = 128)
    private String connectorId;

    @Column(name = "synchronization_scope", nullable = false, length = 64)
    private String synchronizationScope;

    @Column(nullable = false, length = 16)
    private String freshness;

    @Column(name = "source_health", nullable = false, length = 32)
    private String sourceHealth;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "last_started_at")
    private Instant lastStartedAt;

    @Column(name = "last_completed_at")
    private Instant lastCompletedAt;

    @Column(name = "latest_completed_execution_id")
    private UUID latestCompletedExecutionId;

    @Column(name = "latest_fencing_token", nullable = false)
    private long latestFencingToken;

    @Column(name = "recovery_required", nullable = false)
    private boolean recoveryRequired;

    @Column(name = "overlap_skipped_count", nullable = false)
    private long overlapSkippedCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static SynchronizationSourceStateEntity initial(
            UUID id,
            String sourceSystem,
            String connectorId,
            String synchronizationScope,
            Instant now
    ) {
        SynchronizationSourceStateEntity entity = new SynchronizationSourceStateEntity();
        entity.id = id;
        entity.sourceSystem = sourceSystem;
        entity.connectorId = connectorId;
        entity.synchronizationScope = synchronizationScope;
        entity.freshness = "UNKNOWN";
        entity.sourceHealth = "UNKNOWN";
        entity.consecutiveFailures = 0;
        entity.latestFencingToken = 0L;
        entity.recoveryRequired = false;
        entity.overlapSkippedCount = 0L;
        entity.updatedAt = now;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getSynchronizationScope() {
        return synchronizationScope;
    }

    public String getFreshness() {
        return freshness;
    }

    public String getSourceHealth() {
        return sourceHealth;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    public Instant getLastStartedAt() {
        return lastStartedAt;
    }

    public Instant getLastCompletedAt() {
        return lastCompletedAt;
    }

    public UUID getLatestCompletedExecutionId() {
        return latestCompletedExecutionId;
    }

    public long getLatestFencingToken() {
        return latestFencingToken;
    }

    public boolean isRecoveryRequired() {
        return recoveryRequired;
    }

    public long getOverlapSkippedCount() {
        return overlapSkippedCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void recordStarted(Instant startedAt, Instant now) {
        this.lastStartedAt = startedAt;
        this.sourceHealth = "SYNCHRONIZING";
        this.updatedAt = now;
    }

    public void recordOverlapSkip(Instant now) {
        this.overlapSkippedCount++;
        this.updatedAt = now;
    }

    public boolean applyIfAuthoritative(long fencingToken, Instant now) {
        if (fencingToken < this.latestFencingToken) {
            return false;
        }
        this.latestFencingToken = fencingToken;
        this.updatedAt = now;
        return true;
    }

    public void recordSuccess(
            UUID executionId,
            long fencingToken,
            Instant completedAt,
            String freshness,
            String sourceHealth,
            Instant now
    ) {
        if (!applyIfAuthoritative(fencingToken, now)) {
            return;
        }
        this.latestCompletedExecutionId = executionId;
        this.lastCompletedAt = completedAt;
        this.lastSuccessAt = completedAt;
        this.consecutiveFailures = 0;
        this.freshness = freshness;
        this.sourceHealth = sourceHealth;
        this.recoveryRequired = false;
    }

    public void recordFailure(
            long fencingToken,
            Instant failedAt,
            String freshness,
            String sourceHealth,
            int maxConsecutiveFailures,
            boolean recoveryRequired,
            Instant now
    ) {
        if (!applyIfAuthoritative(fencingToken, now)) {
            return;
        }
        this.lastFailureAt = failedAt;
        this.lastCompletedAt = failedAt;
        this.consecutiveFailures++;
        this.freshness = freshness;
        this.sourceHealth = sourceHealth;
        this.recoveryRequired = recoveryRequired || this.consecutiveFailures >= maxConsecutiveFailures;
    }

    public void markDisabled(Instant now) {
        this.sourceHealth = "DISABLED";
        this.freshness = "UNKNOWN";
        this.updatedAt = now;
    }

    public void markRecoveryRequired(Instant now) {
        this.recoveryRequired = true;
        this.sourceHealth = "RECOVERING";
        this.updatedAt = now;
    }
}
