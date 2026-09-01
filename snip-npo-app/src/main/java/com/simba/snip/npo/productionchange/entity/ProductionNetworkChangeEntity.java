package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_network_change")
public class ProductionNetworkChangeEntity {

    @Id
    @Column(name = "production_change_id")
    private UUID productionChangeId;

    @Column(name = "phase15_execution_id", nullable = false)
    private UUID phase15ExecutionId;

    @Column(name = "production_target_id", nullable = false, length = 128)
    private String productionTargetId;

    @Column(name = "change_control_reference", nullable = false, length = 256)
    private String changeControlReference;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "production_fingerprint", length = 64)
    private String productionFingerprint;

    @Column(name = "authorization_generation", nullable = false)
    private int authorizationGeneration;

    @Column(name = "phase14_plan_id")
    private UUID phase14PlanId;

    @Column(name = "phase14_plan_fingerprint", length = 64)
    private String phase14PlanFingerprint;

    @Column(name = "phase15_execution_fingerprint", length = 64)
    private String phase15ExecutionFingerprint;

    @Column(name = "cell_id", nullable = false, length = 128)
    private String cellId;

    @Column(nullable = false, length = 64)
    private String parameter;

    @Column(name = "expected_value", nullable = false, columnDefinition = "numeric")
    private BigDecimal expectedValue;

    @Column(name = "desired_value", nullable = false, columnDefinition = "numeric")
    private BigDecimal desiredValue;

    @Column(name = "rollback_expected_value", columnDefinition = "numeric")
    private BigDecimal rollbackExpectedValue;

    @Column(name = "rollback_desired_value", columnDefinition = "numeric")
    private BigDecimal rollbackDesiredValue;

    @Column(name = "requester_principal_id", nullable = false, length = 128)
    private String requesterPrincipalId;

    @Column(name = "reviewer_principal_id", length = 128)
    private String reviewerPrincipalId;

    @Column(name = "authorizer_principal_id", length = 128)
    private String authorizerPrincipalId;

    @Column(name = "executor_principal_id", length = 128)
    private String executorPrincipalId;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "audit_chain_integrity", nullable = false, length = 16)
    private String auditChainIntegrity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static ProductionNetworkChangeEntity createRequested(
            UUID productionChangeId,
            UUID phase15ExecutionId,
            String productionTargetId,
            String changeControlReference,
            UUID phase14PlanId,
            String phase14PlanFingerprint,
            String phase15ExecutionFingerprint,
            String cellId,
            String parameter,
            BigDecimal expectedValue,
            BigDecimal desiredValue,
            BigDecimal rollbackExpectedValue,
            BigDecimal rollbackDesiredValue,
            String requesterPrincipalId,
            String productionFingerprint,
            Instant now
    ) {
        ProductionNetworkChangeEntity entity = new ProductionNetworkChangeEntity();
        entity.productionChangeId = productionChangeId;
        entity.phase15ExecutionId = phase15ExecutionId;
        entity.productionTargetId = productionTargetId;
        entity.changeControlReference = changeControlReference;
        entity.status = "REQUESTED";
        entity.productionFingerprint = productionFingerprint;
        entity.authorizationGeneration = 0;
        entity.phase14PlanId = phase14PlanId;
        entity.phase14PlanFingerprint = phase14PlanFingerprint;
        entity.phase15ExecutionFingerprint = phase15ExecutionFingerprint;
        entity.cellId = cellId;
        entity.parameter = parameter;
        entity.expectedValue = expectedValue;
        entity.desiredValue = desiredValue;
        entity.rollbackExpectedValue = rollbackExpectedValue;
        entity.rollbackDesiredValue = rollbackDesiredValue;
        entity.requesterPrincipalId = requesterPrincipalId;
        entity.auditChainIntegrity = "VALID";
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public UUID getProductionChangeId() { return productionChangeId; }
    public UUID getPhase15ExecutionId() { return phase15ExecutionId; }
    public String getProductionTargetId() { return productionTargetId; }
    public String getChangeControlReference() { return changeControlReference; }
    public String getStatus() { return status; }
    public String getProductionFingerprint() { return productionFingerprint; }
    public int getAuthorizationGeneration() { return authorizationGeneration; }
    public UUID getPhase14PlanId() { return phase14PlanId; }
    public String getPhase14PlanFingerprint() { return phase14PlanFingerprint; }
    public String getPhase15ExecutionFingerprint() { return phase15ExecutionFingerprint; }
    public String getCellId() { return cellId; }
    public String getParameter() { return parameter; }
    public BigDecimal getExpectedValue() { return expectedValue; }
    public BigDecimal getDesiredValue() { return desiredValue; }
    public BigDecimal getRollbackExpectedValue() { return rollbackExpectedValue; }
    public BigDecimal getRollbackDesiredValue() { return rollbackDesiredValue; }
    public String getRequesterPrincipalId() { return requesterPrincipalId; }
    public String getReviewerPrincipalId() { return reviewerPrincipalId; }
    public String getAuthorizerPrincipalId() { return authorizerPrincipalId; }
    public String getExecutorPrincipalId() { return executorPrincipalId; }
    public String getReasonCode() { return reasonCode; }
    public String getAuditChainIntegrity() { return auditChainIntegrity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public void setStatus(String status) { this.status = status; }
    public void setProductionFingerprint(String productionFingerprint) { this.productionFingerprint = productionFingerprint; }
    public void setAuthorizationGeneration(int authorizationGeneration) { this.authorizationGeneration = authorizationGeneration; }
    public void setReviewerPrincipalId(String reviewerPrincipalId) { this.reviewerPrincipalId = reviewerPrincipalId; }
    public void setAuthorizerPrincipalId(String authorizerPrincipalId) { this.authorizerPrincipalId = authorizerPrincipalId; }
    public void setExecutorPrincipalId(String executorPrincipalId) { this.executorPrincipalId = executorPrincipalId; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public void setAuditChainIntegrity(String auditChainIntegrity) { this.auditChainIntegrity = auditChainIntegrity; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setChangeControlReference(String changeControlReference) { this.changeControlReference = changeControlReference; }
}
