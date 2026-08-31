package com.simba.snip.npo.changeplanning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_plan")
public class NetworkChangePlanEntity {

    @Id
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "resolved_candidate_id")
    private UUID resolvedCandidateId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "plan_version", nullable = false)
    private int planVersion;

    @Column(name = "target_entity_type", nullable = false, length = 32)
    private String targetEntityType;

    @Column(name = "target_entity_id", nullable = false, length = 128)
    private String targetEntityId;

    @Column(name = "parameter_name", nullable = false, length = 64)
    private String parameterName;

    @Column(name = "expected_current_value", nullable = false, length = 32)
    private String expectedCurrentValue;

    @Column(name = "desired_value", nullable = false, length = 32)
    private String desiredValue;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "authorized_fingerprint", length = 64)
    private String authorizedFingerprint;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "source_snapshot_id", length = 128)
    private String sourceSnapshotId;

    @Column(name = "source_synchronization_execution_id")
    private UUID sourceSynchronizationExecutionId;

    @Column(name = "knowledge_confidence_at_creation", nullable = false, length = 16)
    private String knowledgeConfidenceAtCreation;

    @Column(name = "knowledge_reason_codes", nullable = false, length = 512)
    private String knowledgeReasonCodes;

    @Column(name = "impact_level", nullable = false, length = 16)
    private String impactLevel;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @Column(name = "risk_reason_codes", length = 512)
    private String riskReasonCodes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 128)
    private String reviewedBy;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "authorized_by", length = 128)
    private String authorizedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 128)
    private String cancelledBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "invalidation_reason", length = 64)
    private String invalidationReason;

    @Column(name = "predecessor_id")
    private UUID predecessorId;

    @Column(name = "superseded_by")
    private UUID supersededBy;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    public static NetworkChangePlanEntity createDraft(
            UUID id,
            UUID proposalId,
            UUID resolvedCandidateId,
            String targetEntityType,
            String targetEntityId,
            String parameterName,
            String expectedCurrentValue,
            String desiredValue,
            String fingerprint,
            String sourceSystem,
            String sourceSnapshotId,
            UUID sourceSynchronizationExecutionId,
            String knowledgeConfidenceAtCreation,
            String knowledgeReasonCodes,
            String impactLevel,
            String riskLevel,
            String riskReasonCodes,
            String createdBy,
            Instant createdAt,
            Instant expiresAt
    ) {
        NetworkChangePlanEntity entity = new NetworkChangePlanEntity();
        entity.id = id;
        entity.proposalId = proposalId;
        entity.resolvedCandidateId = resolvedCandidateId;
        entity.status = "DRAFT";
        entity.planVersion = 1;
        entity.targetEntityType = targetEntityType;
        entity.targetEntityId = targetEntityId;
        entity.parameterName = parameterName;
        entity.expectedCurrentValue = expectedCurrentValue;
        entity.desiredValue = desiredValue;
        entity.fingerprint = fingerprint;
        entity.sourceSystem = sourceSystem;
        entity.sourceSnapshotId = sourceSnapshotId;
        entity.sourceSynchronizationExecutionId = sourceSynchronizationExecutionId;
        entity.knowledgeConfidenceAtCreation = knowledgeConfidenceAtCreation;
        entity.knowledgeReasonCodes = knowledgeReasonCodes;
        entity.impactLevel = impactLevel;
        entity.riskLevel = riskLevel;
        entity.riskReasonCodes = riskReasonCodes;
        entity.createdBy = createdBy;
        entity.createdAt = createdAt;
        entity.expiresAt = expiresAt;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public UUID getResolvedCandidateId() { return resolvedCandidateId; }
    public String getStatus() { return status; }
    public int getPlanVersion() { return planVersion; }
    public String getTargetEntityType() { return targetEntityType; }
    public String getTargetEntityId() { return targetEntityId; }
    public String getParameterName() { return parameterName; }
    public String getExpectedCurrentValue() { return expectedCurrentValue; }
    public String getDesiredValue() { return desiredValue; }
    public String getFingerprint() { return fingerprint; }
    public String getAuthorizedFingerprint() { return authorizedFingerprint; }
    public String getSourceSystem() { return sourceSystem; }
    public String getSourceSnapshotId() { return sourceSnapshotId; }
    public UUID getSourceSynchronizationExecutionId() { return sourceSynchronizationExecutionId; }
    public String getKnowledgeConfidenceAtCreation() { return knowledgeConfidenceAtCreation; }
    public String getKnowledgeReasonCodes() { return knowledgeReasonCodes; }
    public String getImpactLevel() { return impactLevel; }
    public String getRiskLevel() { return riskLevel; }
    public String getRiskReasonCodes() { return riskReasonCodes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public String getAuthorizedBy() { return authorizedBy; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getInvalidatedAt() { return invalidatedAt; }
    public String getInvalidationReason() { return invalidationReason; }
    public UUID getPredecessorId() { return predecessorId; }
    public UUID getSupersededBy() { return supersededBy; }
    public long getVersion() { return version; }
    public String getCreatedBy() { return createdBy; }

    public void setStatus(String status) { this.status = status; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public void markReviewed(String reviewer, Instant now) {
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
    }

    public void markAuthorized(String authorizer, String authorizedFingerprint, Instant now) {
        this.authorizedBy = authorizer;
        this.authorizedAt = now;
        this.authorizedFingerprint = authorizedFingerprint;
        this.status = "AUTHORIZED";
    }

    public void markReadyForExecution(Instant now) {
        this.status = "READY_FOR_EXECUTION";
    }

    public void revertToAuthorized() {
        this.status = "AUTHORIZED";
    }

    public void markCancelled(String actor, Instant now) {
        this.status = "CANCELLED";
        this.cancelledBy = actor;
        this.cancelledAt = now;
    }

    public void markInvalidated(String reason, Instant now) {
        this.status = "INVALIDATED";
        this.invalidationReason = reason;
        this.invalidatedAt = now;
    }

    public void markExpired(Instant now) {
        this.status = "EXPIRED";
        this.invalidationReason = "PLAN_EXPIRED";
        this.invalidatedAt = now;
    }

    public void markBlocked() {
        this.status = "BLOCKED";
    }

    public void markInvalid() {
        this.status = "INVALID";
    }
}
