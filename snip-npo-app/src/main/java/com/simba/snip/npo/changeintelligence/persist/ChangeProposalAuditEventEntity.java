package com.simba.snip.npo.changeintelligence.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "change_proposal_audit_event")
public class ChangeProposalAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String actor;

    @Column(length = 1024)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static ChangeProposalAuditEventEntity create(
            UUID id,
            UUID proposalId,
            String eventType,
            String actor,
            String details,
            Instant occurredAt
    ) {
        ChangeProposalAuditEventEntity entity = new ChangeProposalAuditEventEntity();
        entity.id = id;
        entity.proposalId = proposalId;
        entity.eventType = eventType;
        entity.actor = actor;
        entity.details = details;
        entity.occurredAt = occurredAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public String getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
