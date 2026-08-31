package com.simba.snip.npo.changeexecution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_execution_audit_event")
public class NetworkChangeExecutionAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String actor;

    @Column(length = 1024)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static NetworkChangeExecutionAuditEventEntity create(
            UUID id,
            UUID executionId,
            String eventType,
            String actor,
            String details,
            Instant occurredAt
    ) {
        NetworkChangeExecutionAuditEventEntity entity = new NetworkChangeExecutionAuditEventEntity();
        entity.id = id;
        entity.executionId = executionId;
        entity.eventType = eventType;
        entity.actor = actor;
        entity.details = details;
        entity.occurredAt = occurredAt;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getExecutionId() { return executionId; }
    public String getEventType() { return eventType; }
    public String getActor() { return actor; }
    public String getDetails() { return details; }
    public Instant getOccurredAt() { return occurredAt; }
}
