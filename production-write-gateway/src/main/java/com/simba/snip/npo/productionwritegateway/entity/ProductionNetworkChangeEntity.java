package com.simba.snip.npo.productionwritegateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    @Column(name = "expected_value", nullable = false)
    private BigDecimal expectedValue;

    @Column(name = "desired_value", nullable = false)
    private BigDecimal desiredValue;

    @Column(name = "rollback_expected_value")
    private BigDecimal rollbackExpectedValue;

    @Column(name = "rollback_desired_value")
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

    @Column(nullable = false)
    private long version;

    public UUID getProductionChangeId() {
        return productionChangeId;
    }

    public void setProductionChangeId(UUID productionChangeId) {
        this.productionChangeId = productionChangeId;
    }

    public UUID getPhase15ExecutionId() {
        return phase15ExecutionId;
    }

    public void setPhase15ExecutionId(UUID phase15ExecutionId) {
        this.phase15ExecutionId = phase15ExecutionId;
    }

    public String getProductionTargetId() {
        return productionTargetId;
    }

    public void setProductionTargetId(String productionTargetId) {
        this.productionTargetId = productionTargetId;
    }

    public String getChangeControlReference() {
        return changeControlReference;
    }

    public void setChangeControlReference(String changeControlReference) {
        this.changeControlReference = changeControlReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProductionFingerprint() {
        return productionFingerprint;
    }

    public void setProductionFingerprint(String productionFingerprint) {
        this.productionFingerprint = productionFingerprint;
    }

    public int getAuthorizationGeneration() {
        return authorizationGeneration;
    }

    public void setAuthorizationGeneration(int authorizationGeneration) {
        this.authorizationGeneration = authorizationGeneration;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public BigDecimal getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(BigDecimal expectedValue) {
        this.expectedValue = expectedValue;
    }

    public BigDecimal getDesiredValue() {
        return desiredValue;
    }

    public void setDesiredValue(BigDecimal desiredValue) {
        this.desiredValue = desiredValue;
    }

    public BigDecimal getRollbackExpectedValue() {
        return rollbackExpectedValue;
    }

    public void setRollbackExpectedValue(BigDecimal rollbackExpectedValue) {
        this.rollbackExpectedValue = rollbackExpectedValue;
    }

    public BigDecimal getRollbackDesiredValue() {
        return rollbackDesiredValue;
    }

    public void setRollbackDesiredValue(BigDecimal rollbackDesiredValue) {
        this.rollbackDesiredValue = rollbackDesiredValue;
    }

    public String getAuditChainIntegrity() {
        return auditChainIntegrity;
    }

    public void setAuditChainIntegrity(String auditChainIntegrity) {
        this.auditChainIntegrity = auditChainIntegrity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getRequesterPrincipalId() {
        return requesterPrincipalId;
    }

    public void setRequesterPrincipalId(String requesterPrincipalId) {
        this.requesterPrincipalId = requesterPrincipalId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }
}
