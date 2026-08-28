package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_knowledge_status")
public class NetworkKnowledgeStatusEntity {

    @Id
    private UUID id;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "connector_id", nullable = false, length = 128)
    private String connectorId;

    @Column(name = "synchronization_scope", nullable = false, length = 64)
    private String synchronizationScope;

    @Column(nullable = false, length = 16)
    private String confidence;

    @Column(name = "reason_codes", nullable = false, length = 512)
    private String reasonCodes;

    @Column(nullable = false, length = 16)
    private String freshness;

    @Column(name = "source_health", nullable = false, length = 32)
    private String sourceHealth;

    @Column(name = "last_trusted_snapshot_id", length = 128)
    private String lastTrustedSnapshotId;

    @Column(name = "last_trusted_synchronization_at")
    private Instant lastTrustedSynchronizationAt;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static NetworkKnowledgeStatusEntity initial(
            UUID id,
            String sourceSystem,
            String connectorId,
            String synchronizationScope,
            Instant now
    ) {
        NetworkKnowledgeStatusEntity entity = new NetworkKnowledgeStatusEntity();
        entity.id = id;
        entity.sourceSystem = sourceSystem;
        entity.connectorId = connectorId;
        entity.synchronizationScope = synchronizationScope;
        entity.confidence = "UNKNOWN";
        entity.reasonCodes = "NO_TRUSTED_BASELINE";
        entity.freshness = "UNKNOWN";
        entity.sourceHealth = "UNKNOWN";
        entity.fencingToken = 0L;
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

    public String getConfidence() {
        return confidence;
    }

    public String getReasonCodes() {
        return reasonCodes;
    }

    public String getFreshness() {
        return freshness;
    }

    public String getSourceHealth() {
        return sourceHealth;
    }

    public String getLastTrustedSnapshotId() {
        return lastTrustedSnapshotId;
    }

    public Instant getLastTrustedSynchronizationAt() {
        return lastTrustedSynchronizationAt;
    }

    public long getFencingToken() {
        return fencingToken;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean applyIfAuthoritative(long fencingToken, Instant now) {
        if (fencingToken < this.fencingToken) {
            return false;
        }
        this.fencingToken = fencingToken;
        this.updatedAt = now;
        return true;
    }

    public void update(
            long fencingToken,
            String confidence,
            String reasonCodes,
            String freshness,
            String sourceHealth,
            String lastTrustedSnapshotId,
            Instant lastTrustedSynchronizationAt,
            Instant now
    ) {
        if (!applyIfAuthoritative(fencingToken, now)) {
            return;
        }
        this.confidence = confidence;
        this.reasonCodes = reasonCodes;
        this.freshness = freshness;
        this.sourceHealth = sourceHealth;
        this.lastTrustedSnapshotId = lastTrustedSnapshotId;
        this.lastTrustedSynchronizationAt = lastTrustedSynchronizationAt;
    }
}
