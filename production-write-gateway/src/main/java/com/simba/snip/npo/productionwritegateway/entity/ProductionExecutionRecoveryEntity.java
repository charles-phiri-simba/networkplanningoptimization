package com.simba.snip.npo.productionwritegateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_execution_recovery")
public class ProductionExecutionRecoveryEntity {

    @Id
    @Column(name = "recovery_id")
    private UUID recoveryId;

    @Column(name = "production_change_id", nullable = false)
    private UUID productionChangeId;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "reason_codes", length = 1024)
    private String reasonCodes;

    @Column(name = "signaled_at", nullable = false)
    private Instant signaledAt;

    public UUID getRecoveryId() {
        return recoveryId;
    }

    public void setRecoveryId(UUID recoveryId) {
        this.recoveryId = recoveryId;
    }

    public UUID getProductionChangeId() {
        return productionChangeId;
    }

    public void setProductionChangeId(UUID productionChangeId) {
        this.productionChangeId = productionChangeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReasonCodes() {
        return reasonCodes;
    }

    public void setReasonCodes(String reasonCodes) {
        this.reasonCodes = reasonCodes;
    }

    public Instant getSignaledAt() {
        return signaledAt;
    }

    public void setSignaledAt(Instant signaledAt) {
        this.signaledAt = signaledAt;
    }
}
