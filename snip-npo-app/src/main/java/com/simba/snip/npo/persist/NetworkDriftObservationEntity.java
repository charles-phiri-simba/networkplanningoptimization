package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_drift_observation")
public class NetworkDriftObservationEntity {

    @Id
    private UUID id;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "connector_id", nullable = false, length = 128)
    private String connectorId;

    @Column(name = "synchronization_scope", nullable = false, length = 64)
    private String synchronizationScope;

    @Column(name = "drift_type", nullable = false, length = 32)
    private String driftType;

    @Column(name = "drift_status", nullable = false, length = 16)
    private String driftStatus;

    @Column(name = "entity_type", length = 32)
    private String entityType;

    @Column(name = "entity_id", length = 128)
    private String entityId;

    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "fencing_token")
    private Long fencingToken;

    @Column(nullable = false, length = 512)
    private String summary;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_execution_id")
    private UUID resolutionExecutionId;

    public static NetworkDriftObservationEntity open(
            UUID id,
            String sourceSystem,
            String connectorId,
            String synchronizationScope,
            String driftType,
            String entityType,
            String entityId,
            UUID executionId,
            long fencingToken,
            String summary,
            Instant detectedAt
    ) {
        NetworkDriftObservationEntity entity = new NetworkDriftObservationEntity();
        entity.id = id;
        entity.sourceSystem = sourceSystem;
        entity.connectorId = connectorId;
        entity.synchronizationScope = synchronizationScope;
        entity.driftType = driftType;
        entity.driftStatus = "OPEN";
        entity.entityType = entityType;
        entity.entityId = entityId;
        entity.executionId = executionId;
        entity.fencingToken = fencingToken;
        entity.summary = summary;
        entity.detectedAt = detectedAt;
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

    public String getDriftType() {
        return driftType;
    }

    public String getDriftStatus() {
        return driftStatus;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public Long getFencingToken() {
        return fencingToken;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public UUID getResolutionExecutionId() {
        return resolutionExecutionId;
    }

    public void resolve(UUID resolutionExecutionId, long fencingToken, Instant resolvedAt) {
        if (this.fencingToken != null && fencingToken < this.fencingToken) {
            return;
        }
        this.driftStatus = "RESOLVED";
        this.resolutionExecutionId = resolutionExecutionId;
        this.fencingToken = fencingToken;
        this.resolvedAt = resolvedAt;
    }
}
