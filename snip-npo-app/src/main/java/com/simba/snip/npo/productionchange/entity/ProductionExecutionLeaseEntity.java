package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_execution_lease")
public class ProductionExecutionLeaseEntity {

    @Id
    @Column(name = "lease_id")
    private UUID leaseId;

    @Column(name = "production_target_id", nullable = false, length = 128)
    private String productionTargetId;

    @Column(name = "cell_id", nullable = false, length = 128)
    private String cellId;

    @Column(nullable = false, length = 64)
    private String parameter;

    @Column(name = "holder_id", nullable = false, length = 128)
    private String holderId;

    @Column(name = "fencing_token", nullable = false)
    private long fencingToken;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "acquired_at", nullable = false)
    private Instant acquiredAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static ProductionExecutionLeaseEntity create(
            UUID leaseId,
            String productionTargetId,
            String cellId,
            String parameter,
            String holderId,
            long fencingToken,
            String status,
            Instant acquiredAt,
            Instant expiresAt
    ) {
        ProductionExecutionLeaseEntity entity = new ProductionExecutionLeaseEntity();
        entity.leaseId = leaseId;
        entity.productionTargetId = productionTargetId;
        entity.cellId = cellId;
        entity.parameter = parameter;
        entity.holderId = holderId;
        entity.fencingToken = fencingToken;
        entity.status = status;
        entity.acquiredAt = acquiredAt;
        entity.expiresAt = expiresAt;
        return entity;
    }

    public UUID getLeaseId() { return leaseId; }
    public String getProductionTargetId() { return productionTargetId; }
    public String getCellId() { return cellId; }
    public String getParameter() { return parameter; }
    public String getHolderId() { return holderId; }
    public long getFencingToken() { return fencingToken; }
    public String getStatus() { return status; }
    public Instant getAcquiredAt() { return acquiredAt; }
    public Instant getExpiresAt() { return expiresAt; }

    public void setStatus(String status) { this.status = status; }
    public void setHolderId(String holderId) { this.holderId = holderId; }
    public void setFencingToken(long fencingToken) { this.fencingToken = fencingToken; }
    public void setAcquiredAt(Instant acquiredAt) { this.acquiredAt = acquiredAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
