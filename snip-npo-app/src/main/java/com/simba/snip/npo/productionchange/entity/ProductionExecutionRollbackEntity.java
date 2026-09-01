package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_execution_rollback")
public class ProductionExecutionRollbackEntity {

    @Id
    @Column(name = "rollback_id")
    private UUID rollbackId;

    @Column(name = "production_change_id", nullable = false)
    private UUID productionChangeId;

    @Column(nullable = false, length = 48)
    private String status;

    @Column(name = "rollback_fingerprint", length = 64)
    private String rollbackFingerprint;

    @Column(name = "authorization_generation")
    private Integer authorizationGeneration;

    @Column(name = "requester_principal_id", length = 128)
    private String requesterPrincipalId;

    @Column(name = "reviewer_principal_id", length = 128)
    private String reviewerPrincipalId;

    @Column(name = "authorizer_principal_id", length = 128)
    private String authorizerPrincipalId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ProductionExecutionRollbackEntity createRequested(
            UUID rollbackId,
            UUID productionChangeId,
            String requesterPrincipalId,
            String rollbackFingerprint,
            Instant now
    ) {
        ProductionExecutionRollbackEntity entity = new ProductionExecutionRollbackEntity();
        entity.rollbackId = rollbackId;
        entity.productionChangeId = productionChangeId;
        entity.status = "REQUESTED";
        entity.rollbackFingerprint = rollbackFingerprint;
        entity.requesterPrincipalId = requesterPrincipalId;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public UUID getRollbackId() { return rollbackId; }
    public UUID getProductionChangeId() { return productionChangeId; }
    public String getStatus() { return status; }
    public String getRollbackFingerprint() { return rollbackFingerprint; }
    public Integer getAuthorizationGeneration() { return authorizationGeneration; }
    public String getRequesterPrincipalId() { return requesterPrincipalId; }
    public String getReviewerPrincipalId() { return reviewerPrincipalId; }
    public String getAuthorizerPrincipalId() { return authorizerPrincipalId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setRollbackFingerprint(String rollbackFingerprint) { this.rollbackFingerprint = rollbackFingerprint; }
    public void setAuthorizationGeneration(Integer authorizationGeneration) { this.authorizationGeneration = authorizationGeneration; }
    public void setReviewerPrincipalId(String reviewerPrincipalId) { this.reviewerPrincipalId = reviewerPrincipalId; }
    public void setAuthorizerPrincipalId(String authorizerPrincipalId) { this.authorizerPrincipalId = authorizerPrincipalId; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
