package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_integration_conflict")
public class NetworkIntegrationConflictEntity {

    @Id
    private UUID id;

    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType;

    @Column(name = "canonical_entity_id", nullable = false, length = 64)
    private String canonicalEntityId;

    @Column(name = "conflict_scope", nullable = false, length = 64)
    private String conflictScope;

    @Column(name = "current_value", nullable = false, length = 256)
    private String currentValue;

    @Column(name = "incoming_value", nullable = false, length = 256)
    private String incomingValue;

    @Column(name = "authoritative_source", nullable = false, length = 64)
    private String authoritativeSource;

    @Column(name = "incoming_source", nullable = false, length = 64)
    private String incomingSource;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    public static NetworkIntegrationConflictEntity create(
            UUID id,
            UUID importId,
            String entityType,
            String canonicalEntityId,
            String conflictScope,
            String currentValue,
            String incomingValue,
            String authoritativeSource,
            String incomingSource,
            String reasonCode,
            Instant detectedAt
    ) {
        NetworkIntegrationConflictEntity entity = new NetworkIntegrationConflictEntity();
        entity.id = id;
        entity.importId = importId;
        entity.entityType = entityType;
        entity.canonicalEntityId = canonicalEntityId;
        entity.conflictScope = conflictScope;
        entity.currentValue = currentValue;
        entity.incomingValue = incomingValue;
        entity.authoritativeSource = authoritativeSource;
        entity.incomingSource = incomingSource;
        entity.reasonCode = reasonCode;
        entity.status = "OPEN";
        entity.detectedAt = detectedAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getImportId() {
        return importId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getCanonicalEntityId() {
        return canonicalEntityId;
    }

    public String getConflictScope() {
        return conflictScope;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public String getIncomingValue() {
        return incomingValue;
    }

    public String getAuthoritativeSource() {
        return authoritativeSource;
    }

    public String getIncomingSource() {
        return incomingSource;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getStatus() {
        return status;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }
}
