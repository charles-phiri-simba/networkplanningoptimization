package com.simba.snip.npo.changeplanning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_plan_audit_event")
public class NetworkChangePlanAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String actor;

    @Column(length = 1024)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static NetworkChangePlanAuditEventEntity create(
            UUID id,
            UUID planId,
            String eventType,
            String actor,
            String details,
            Instant occurredAt
    ) {
        NetworkChangePlanAuditEventEntity entity = new NetworkChangePlanAuditEventEntity();
        entity.id = id;
        entity.planId = planId;
        entity.eventType = eventType;
        entity.actor = actor;
        entity.details = details;
        entity.occurredAt = occurredAt;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public String getEventType() { return eventType; }
    public String getActor() { return actor; }
    public String getDetails() { return details; }
    public Instant getOccurredAt() { return occurredAt; }
}
