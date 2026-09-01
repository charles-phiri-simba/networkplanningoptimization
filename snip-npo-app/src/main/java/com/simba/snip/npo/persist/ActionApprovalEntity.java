package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_approval")
public class ActionApprovalEntity {

    @Id
    private UUID id;

    @Column(name = "action_id", nullable = false)
    private UUID actionId;

    @Column(nullable = false, length = 16)
    private String decision;

    @Column(name = "decided_by", nullable = false, length = 64)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @Column(length = 512)
    private String comment;

    public static ActionApprovalEntity create(
            UUID id, UUID actionId, String decision, String decidedBy, Instant decidedAt, String comment
    ) {
        ActionApprovalEntity entity = new ActionApprovalEntity();
        entity.id = id;
        entity.actionId = actionId;
        entity.decision = decision;
        entity.decidedBy = decidedBy;
        entity.decidedAt = decidedAt;
        entity.comment = comment;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActionId() {
        return actionId;
    }

    public String getDecision() {
        return decision;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getComment() {
        return comment;
    }
}
