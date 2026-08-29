package com.simba.snip.npo.changeintelligence.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_proposal")
public class NetworkChangeProposalEntity {

    @Id
    private UUID id;

    @Column(name = "proposal_type", nullable = false, length = 64)
    private String proposalType;

    @Column(name = "target_entity_type", nullable = false, length = 32)
    private String targetEntityType;

    @Column(name = "target_entity_id", nullable = false, length = 128)
    private String targetEntityId;

    @Column(name = "parameter_name", nullable = false, length = 64)
    private String parameterName;

    @Column(name = "current_value", nullable = false, length = 32)
    private String currentValue;

    @Column(name = "proposed_value", length = 32)
    private String proposedValue;

    @Column(nullable = false, length = 16)
    private String unit;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "source_snapshot_id", length = 128)
    private String sourceSnapshotId;

    @Column(name = "source_synchronization_execution_id")
    private UUID sourceSynchronizationExecutionId;

    @Column(name = "network_knowledge_confidence", nullable = false, length = 16)
    private String networkKnowledgeConfidence;

    @Column(name = "knowledge_reason_codes", nullable = false, length = 512)
    private String knowledgeReasonCodes;

    @Column(name = "assurance_confidence", length = 16)
    private String assuranceConfidence;

    @Column(name = "simulation_confidence", length = 16)
    private String simulationConfidence;

    @Column(name = "assurance_case_id")
    private UUID assuranceCaseId;

    @Column(name = "decision_reference", length = 256)
    private String decisionReference;

    @Column(name = "benefit_summary", length = 512)
    private String benefitSummary;

    @Column(name = "benefit_score", precision = 12, scale = 6)
    private BigDecimal benefitScore;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @Column(name = "risk_reason_codes", length = 512)
    private String riskReasonCodes;

    @Column(name = "proposal_score", precision = 12, scale = 6)
    private BigDecimal proposalScore;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "generation_initiator", nullable = false, length = 32)
    private String generationInitiator;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "invalidation_reason", length = 64)
    private String invalidationReason;

    @Column(name = "superseded_by")
    private UUID supersededBy;

    @Column(name = "predecessor_id")
    private UUID predecessorId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    public static NetworkChangeProposalEntity createDraft(
            UUID id,
            String proposalType,
            String targetEntityType,
            String targetEntityId,
            String parameterName,
            String currentValue,
            String unit,
            String sourceSystem,
            String sourceSnapshotId,
            UUID sourceSynchronizationExecutionId,
            String networkKnowledgeConfidence,
            String knowledgeReasonCodes,
            String assuranceConfidence,
            UUID assuranceCaseId,
            String decisionReference,
            String generationInitiator,
            String createdBy,
            Instant createdAt,
            Instant expiresAt
    ) {
        NetworkChangeProposalEntity entity = new NetworkChangeProposalEntity();
        entity.id = id;
        entity.proposalType = proposalType;
        entity.targetEntityType = targetEntityType;
        entity.targetEntityId = targetEntityId;
        entity.parameterName = parameterName;
        entity.currentValue = currentValue;
        entity.unit = unit;
        entity.sourceSystem = sourceSystem;
        entity.sourceSnapshotId = sourceSnapshotId;
        entity.sourceSynchronizationExecutionId = sourceSynchronizationExecutionId;
        entity.networkKnowledgeConfidence = networkKnowledgeConfidence;
        entity.knowledgeReasonCodes = knowledgeReasonCodes;
        entity.assuranceConfidence = assuranceConfidence;
        entity.assuranceCaseId = assuranceCaseId;
        entity.decisionReference = decisionReference;
        entity.status = "DRAFT";
        entity.generationInitiator = generationInitiator;
        entity.createdBy = createdBy;
        entity.createdAt = createdAt;
        entity.expiresAt = expiresAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public String getProposalType() {
        return proposalType;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public String getTargetEntityId() {
        return targetEntityId;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public String getProposedValue() {
        return proposedValue;
    }

    public String getUnit() {
        return unit;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourceSnapshotId() {
        return sourceSnapshotId;
    }

    public UUID getSourceSynchronizationExecutionId() {
        return sourceSynchronizationExecutionId;
    }

    public String getNetworkKnowledgeConfidence() {
        return networkKnowledgeConfidence;
    }

    public String getKnowledgeReasonCodes() {
        return knowledgeReasonCodes;
    }

    public String getAssuranceConfidence() {
        return assuranceConfidence;
    }

    public String getSimulationConfidence() {
        return simulationConfidence;
    }

    public UUID getAssuranceCaseId() {
        return assuranceCaseId;
    }

    public String getDecisionReference() {
        return decisionReference;
    }

    public String getBenefitSummary() {
        return benefitSummary;
    }

    public BigDecimal getBenefitScore() {
        return benefitScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getRiskReasonCodes() {
        return riskReasonCodes;
    }

    public BigDecimal getProposalScore() {
        return proposalScore;
    }

    public String getStatus() {
        return status;
    }

    public String getGenerationInitiator() {
        return generationInitiator;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public String getInvalidationReason() {
        return invalidationReason;
    }

    public UUID getSupersededBy() {
        return supersededBy;
    }

    public UUID getPredecessorId() {
        return predecessorId;
    }

    public long getVersion() {
        return version;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void markEvaluating(String status) {
        this.status = status;
    }

    public void completeEvaluation(
            String status,
            String proposedValue,
            String simulationConfidence,
            String benefitSummary,
            BigDecimal benefitScore,
            String riskLevel,
            String riskReasonCodes,
            BigDecimal proposalScore,
            String failureCode,
            String failureReason,
            Instant evaluatedAt
    ) {
        this.status = status;
        this.proposedValue = proposedValue;
        this.simulationConfidence = simulationConfidence;
        this.benefitSummary = benefitSummary;
        this.benefitScore = benefitScore;
        this.riskLevel = riskLevel;
        this.riskReasonCodes = riskReasonCodes;
        this.proposalScore = proposalScore;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.evaluatedAt = evaluatedAt;
    }

    public void markApproved(Instant now) {
        this.status = "APPROVED";
        this.evaluatedAt = this.evaluatedAt == null ? now : this.evaluatedAt;
    }

    public void markRejected(Instant now) {
        this.status = "REJECTED";
        this.evaluatedAt = this.evaluatedAt == null ? now : this.evaluatedAt;
    }

    public void markInvalidated(String reason, Instant now) {
        this.status = "INVALIDATED";
        this.invalidationReason = reason;
        this.invalidatedAt = now;
    }

    public void markExpired(Instant now) {
        this.status = "EXPIRED";
        this.invalidatedAt = now;
        this.invalidationReason = "PROPOSAL_EXPIRED";
    }

    public void markSuperseded(UUID successorId, Instant now) {
        this.status = "SUPERSEDED";
        this.supersededBy = successorId;
        this.invalidatedAt = now;
        this.invalidationReason = "PROPOSAL_SUPERSEDED";
    }

    public void refreshKnowledgeSnapshot(String confidence, String reasonCodes) {
        this.networkKnowledgeConfidence = confidence;
        this.knowledgeReasonCodes = reasonCodes;
    }
}
