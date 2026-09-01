package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "proposed_action")
public class ProposedActionEntity {

    @Id
    private UUID id;

    @Column(name = "assurance_case_id", nullable = false)
    private UUID assuranceCaseId;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(name = "capability_id", length = 128)
    private String capabilityId;

    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String parameters;

    @Column(nullable = false, length = 1024)
    private String rationale;

    @Column(name = "risk_level", nullable = false, length = 16)
    private String riskLevel;

    @Column(name = "policy_decision", nullable = false, length = 32)
    private String policyDecision;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "proposed_at", nullable = false)
    private Instant proposedAt;

    @Column(name = "proposed_by", nullable = false, length = 64)
    private String proposedBy;

    @Column(name = "executed_by", length = 64)
    private String executedBy;

    @Column(nullable = false)
    private boolean synthetic;

    @Column(name = "agent_run_id")
    private UUID agentRunId;

    @Column(name = "agent_id", length = 64)
    private String agentId;

    public static ProposedActionEntity create(
            UUID id,
            UUID assuranceCaseId,
            String actionType,
            String capabilityId,
            String targetType,
            String targetId,
            String parameters,
            String rationale,
            String riskLevel,
            String policyDecision,
            String status,
            Instant proposedAt,
            String proposedBy,
            boolean synthetic
    ) {
        ProposedActionEntity entity = new ProposedActionEntity();
        entity.id = id;
        entity.assuranceCaseId = assuranceCaseId;
        entity.actionType = actionType;
        entity.capabilityId = capabilityId;
        entity.targetType = targetType;
        entity.targetId = targetId;
        entity.parameters = parameters;
        entity.rationale = rationale;
        entity.riskLevel = riskLevel;
        entity.policyDecision = policyDecision;
        entity.status = status;
        entity.proposedAt = proposedAt;
        entity.proposedBy = proposedBy;
        entity.synthetic = synthetic;
        return entity;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public void setAgentProvenance(UUID agentRunId, String agentId) {
        this.agentRunId = agentRunId;
        this.agentId = agentId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssuranceCaseId() {
        return assuranceCaseId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getParameters() {
        return parameters;
    }

    public String getRationale() {
        return rationale;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getPolicyDecision() {
        return policyDecision;
    }

    public String getStatus() {
        return status;
    }

    public Instant getProposedAt() {
        return proposedAt;
    }

    public String getProposedBy() {
        return proposedBy;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public UUID getAgentRunId() {
        return agentRunId;
    }

    public String getAgentId() {
        return agentId;
    }
}
