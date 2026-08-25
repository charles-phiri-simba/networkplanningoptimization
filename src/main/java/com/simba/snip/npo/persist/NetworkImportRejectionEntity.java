package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_import_rejection")
public class NetworkImportRejectionEntity {

    @Id
    private UUID id;

    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(name = "source_entity_id", length = 128)
    private String sourceEntityId;

    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(nullable = false, length = 1024)
    private String details;

    @Column(name = "rejected_at", nullable = false)
    private Instant rejectedAt;

    public static NetworkImportRejectionEntity create(
            UUID id,
            UUID importId,
            String sourceEntityId,
            String entityType,
            String reasonCode,
            String details,
            Instant rejectedAt
    ) {
        NetworkImportRejectionEntity entity = new NetworkImportRejectionEntity();
        entity.id = id;
        entity.importId = importId;
        entity.sourceEntityId = sourceEntityId;
        entity.entityType = entityType;
        entity.reasonCode = reasonCode;
        entity.details = details;
        entity.rejectedAt = rejectedAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getImportId() {
        return importId;
    }

    public String getSourceEntityId() {
        return sourceEntityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getDetails() {
        return details;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }
}
