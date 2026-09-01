package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "synchronization_checkpoint")
public class SynchronizationCheckpointEntity {

    @Id
    private UUID id;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "connector_id", nullable = false, length = 128)
    private String connectorId;

    @Column(name = "synchronization_scope", nullable = false, length = 64)
    private String synchronizationScope;

    @Column(name = "checkpoint_type", nullable = false, length = 32)
    private String checkpointType;

    @Column(name = "checkpoint_value", nullable = false, length = 256)
    private String checkpointValue;

    @Column(name = "source_version", length = 64)
    private String sourceVersion;

    @Column(name = "last_successful_execution_id")
    private UUID lastSuccessfulExecutionId;

    @Column(name = "last_successful_snapshot_id", length = 128)
    private String lastSuccessfulSnapshotId;

    @Column(name = "last_successful_started_at")
    private Instant lastSuccessfulStartedAt;

    @Column(name = "last_successful_completed_at")
    private Instant lastSuccessfulCompletedAt;

    @Column(name = "last_observed_at")
    private Instant lastObservedAt;

    @Column(name = "synchronization_mode", length = 32)
    private String synchronizationMode;

    @Column(length = 16)
    private String completeness;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static SynchronizationCheckpointEntity create(
            UUID id,
            String sourceSystem,
            String connectorId,
            String synchronizationScope,
            String checkpointType,
            String checkpointValue,
            String sourceVersion,
            String synchronizationMode,
            String completeness,
            long fencingToken,
            String status,
            Instant now
    ) {
        SynchronizationCheckpointEntity entity = new SynchronizationCheckpointEntity();
        entity.id = id;
        entity.sourceSystem = sourceSystem;
        entity.connectorId = connectorId;
        entity.synchronizationScope = synchronizationScope;
        entity.checkpointType = checkpointType;
        entity.checkpointValue = checkpointValue;
        entity.sourceVersion = sourceVersion;
        entity.synchronizationMode = synchronizationMode;
        entity.completeness = completeness;
        entity.fencingToken = fencingToken;
        entity.status = status;
        entity.createdAt = now;
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

    public String getCheckpointType() {
        return checkpointType;
    }

    public String getCheckpointValue() {
        return checkpointValue;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public UUID getLastSuccessfulExecutionId() {
        return lastSuccessfulExecutionId;
    }

    public String getLastSuccessfulSnapshotId() {
        return lastSuccessfulSnapshotId;
    }

    public Instant getLastSuccessfulStartedAt() {
        return lastSuccessfulStartedAt;
    }

    public Instant getLastSuccessfulCompletedAt() {
        return lastSuccessfulCompletedAt;
    }

    public Instant getLastObservedAt() {
        return lastObservedAt;
    }

    public String getSynchronizationMode() {
        return synchronizationMode;
    }

    public String getCompleteness() {
        return completeness;
    }

    public long getFencingToken() {
        return fencingToken;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void advance(
            UUID executionId,
            String snapshotId,
            Instant startedAt,
            Instant completedAt,
            String checkpointValue,
            String sourceVersion,
            String synchronizationMode,
            String completeness,
            long fencingToken,
            String status,
            Instant observedAt,
            Instant now
    ) {
        this.lastSuccessfulExecutionId = executionId;
        this.lastSuccessfulSnapshotId = snapshotId;
        this.lastSuccessfulStartedAt = startedAt;
        this.lastSuccessfulCompletedAt = completedAt;
        this.checkpointValue = checkpointValue;
        this.sourceVersion = sourceVersion;
        this.synchronizationMode = synchronizationMode;
        this.completeness = completeness;
        this.fencingToken = fencingToken;
        this.status = status;
        this.lastObservedAt = observedAt;
        this.updatedAt = now;
    }

    public void markStatus(String status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }
}
