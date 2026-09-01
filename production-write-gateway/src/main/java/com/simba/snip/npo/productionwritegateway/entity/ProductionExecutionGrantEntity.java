package com.simba.snip.npo.productionwritegateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_execution_grant")
public class ProductionExecutionGrantEntity {

    @Id
    @Column(name = "grant_id")
    private UUID grantId;

    @Column(name = "production_change_id", nullable = false)
    private UUID productionChangeId;

    @Column(name = "phase15_execution_id", nullable = false)
    private UUID phase15ExecutionId;

    @Column(name = "target_id", nullable = false, length = 128)
    private String targetId;

    @Column(name = "grant_type", nullable = false, length = 16)
    private String grantType;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "production_fingerprint", nullable = false, length = 64)
    private String productionFingerprint;

    @Column(name = "authorization_generation", nullable = false)
    private int authorizationGeneration;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;

    @Column(name = "operation_binding_hash", nullable = false, length = 64)
    private String operationBindingHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(nullable = false)
    private long version;

    public UUID getGrantId() {
        return grantId;
    }

    public void setGrantId(UUID grantId) {
        this.grantId = grantId;
    }

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

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
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

    public long getFencingToken() {
        return fencingToken;
    }

    public void setFencingToken(long fencingToken) {
        this.fencingToken = fencingToken;
    }

    public String getOperationBindingHash() {
        return operationBindingHash;
    }

    public void setOperationBindingHash(String operationBindingHash) {
        this.operationBindingHash = operationBindingHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
