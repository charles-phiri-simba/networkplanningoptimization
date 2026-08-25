package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_import_audit_event")
public class NetworkImportAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    public static NetworkImportAuditEventEntity create(
            UUID id, UUID importId, String eventType, Instant occurredAt, String details
    ) {
        NetworkImportAuditEventEntity entity = new NetworkImportAuditEventEntity();
        entity.id = id;
        entity.importId = importId;
        entity.eventType = eventType;
        entity.occurredAt = occurredAt;
        entity.details = details;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getImportId() {
        return importId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getDetails() {
        return details;
    }
}
