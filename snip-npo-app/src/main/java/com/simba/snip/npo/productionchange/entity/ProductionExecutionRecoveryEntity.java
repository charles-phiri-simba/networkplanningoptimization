package com.simba.snip.npo.productionchange.entity;

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

    public static ProductionExecutionRecoveryEntity create(
            UUID recoveryId,
            UUID productionChangeId,
            String status,
            String reasonCodes,
            Instant signaledAt
    ) {
        ProductionExecutionRecoveryEntity entity = new ProductionExecutionRecoveryEntity();
        entity.recoveryId = recoveryId;
        entity.productionChangeId = productionChangeId;
        entity.status = status;
        entity.reasonCodes = reasonCodes;
        entity.signaledAt = signaledAt;
        return entity;
    }

    public UUID getRecoveryId() { return recoveryId; }
    public UUID getProductionChangeId() { return productionChangeId; }
    public String getStatus() { return status; }
    public String getReasonCodes() { return reasonCodes; }
    public Instant getSignaledAt() { return signaledAt; }
}
