package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_audit_event")
public class ActionAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "action_id", nullable = false)
    private UUID actionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    public static ActionAuditEventEntity create(
            UUID id, UUID actionId, String eventType, String actor, Instant occurredAt, String details
    ) {
        ActionAuditEventEntity entity = new ActionAuditEventEntity();
        entity.id = id;
        entity.actionId = actionId;
        entity.eventType = eventType;
        entity.actor = actor;
        entity.occurredAt = occurredAt;
        entity.details = details;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActionId() {
        return actionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getDetails() {
        return details;
    }
}
