package com.simba.snip.npo.changeexecution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_execution_recovery")
public class NetworkChangeExecutionRecoveryEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "recovery_status", nullable = false, length = 32)
    private String recoveryStatus;

    @Column(name = "rollback_eligible", nullable = false)
    private boolean rollbackEligible;

    @Column(name = "reason_codes", length = 512)
    private String reasonCodes;

    @Column(name = "evidence_summary", length = 1024)
    private String evidenceSummary;

    public static NetworkChangeExecutionRecoveryEntity create(
            UUID id,
            UUID executionId,
            String recoveryStatus,
            boolean rollbackEligible,
            String reasonCodes,
            String evidenceSummary,
            Instant evaluatedAt
    ) {
        NetworkChangeExecutionRecoveryEntity entity = new NetworkChangeExecutionRecoveryEntity();
        entity.id = id;
        entity.executionId = executionId;
        entity.recoveryStatus = recoveryStatus;
        entity.rollbackEligible = rollbackEligible;
        entity.reasonCodes = reasonCodes;
        entity.evidenceSummary = evidenceSummary;
        entity.evaluatedAt = evaluatedAt;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getExecutionId() { return executionId; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public String getRecoveryStatus() { return recoveryStatus; }
    public boolean isRollbackEligible() { return rollbackEligible; }
    public String getReasonCodes() { return reasonCodes; }
    public String getEvidenceSummary() { return evidenceSummary; }
}
