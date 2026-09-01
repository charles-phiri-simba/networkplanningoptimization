package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

    @Version
    @Column(nullable = false)
    private long version;

    public static ProductionExecutionGrantEntity issue(
            UUID grantId,
            UUID productionChangeId,
            UUID phase15ExecutionId,
            String targetId,
            String grantType,
            String productionFingerprint,
            int authorizationGeneration,
            long fencingToken,
            String operationBindingHash,
            Instant issuedAt,
            Instant expiresAt
    ) {
        ProductionExecutionGrantEntity entity = new ProductionExecutionGrantEntity();
        entity.grantId = grantId;
        entity.productionChangeId = productionChangeId;
        entity.phase15ExecutionId = phase15ExecutionId;
        entity.targetId = targetId;
        entity.grantType = grantType;
        entity.status = "ISSUED";
        entity.productionFingerprint = productionFingerprint;
        entity.authorizationGeneration = authorizationGeneration;
        entity.fencingToken = fencingToken;
        entity.operationBindingHash = operationBindingHash;
        entity.issuedAt = issuedAt;
        entity.expiresAt = expiresAt;
        return entity;
    }

    public UUID getGrantId() { return grantId; }
    public UUID getProductionChangeId() { return productionChangeId; }
    public UUID getPhase15ExecutionId() { return phase15ExecutionId; }
    public String getTargetId() { return targetId; }
    public String getGrantType() { return grantType; }
    public String getStatus() { return status; }
    public String getProductionFingerprint() { return productionFingerprint; }
    public int getAuthorizationGeneration() { return authorizationGeneration; }
    public long getFencingToken() { return fencingToken; }
    public String getOperationBindingHash() { return operationBindingHash; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public long getVersion() { return version; }

    public void setStatus(String status) { this.status = status; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
}
