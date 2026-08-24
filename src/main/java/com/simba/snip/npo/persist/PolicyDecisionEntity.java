package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policy_decision")
public class PolicyDecisionEntity {

    @Id
    private UUID id;

    @Column(name = "action_id", nullable = false)
    private UUID actionId;

    @Column(nullable = false, length = 32)
    private String decision;

    @Column(name = "policy_id", nullable = false, length = 64)
    private String policyId;

    @Column(nullable = false, length = 512)
    private String reason;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    public static PolicyDecisionEntity create(
            UUID id, UUID actionId, String decision, String policyId, String reason, Instant evaluatedAt
    ) {
        PolicyDecisionEntity entity = new PolicyDecisionEntity();
        entity.id = id;
        entity.actionId = actionId;
        entity.decision = decision;
        entity.policyId = policyId;
        entity.reason = reason;
        entity.evaluatedAt = evaluatedAt;
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

    public String getPolicyId() {
        return policyId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }
}
