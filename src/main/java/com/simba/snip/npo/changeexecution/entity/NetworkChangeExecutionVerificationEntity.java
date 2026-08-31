package com.simba.snip.npo.changeexecution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_execution_verification")
public class NetworkChangeExecutionVerificationEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "attempt_id")
    private UUID attemptId;

    @Column(nullable = false, length = 16)
    private String direction;

    @Column(nullable = false, length = 32)
    private String outcome;

    @Column(name = "observed_value", length = 32)
    private String observedValue;

    @Column(name = "expected_value", length = 32)
    private String expectedValue;

    @Column(name = "target_revision")
    private Long targetRevision;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Column(name = "evidence_summary", length = 1024)
    private String evidenceSummary;

    public static NetworkChangeExecutionVerificationEntity create(
            UUID id,
            UUID executionId,
            UUID attemptId,
            String direction,
            String outcome,
            String observedValue,
            String expectedValue,
            Long targetRevision,
            String reasonCode,
            String evidenceSummary,
            Instant observedAt
    ) {
        NetworkChangeExecutionVerificationEntity entity = new NetworkChangeExecutionVerificationEntity();
        entity.id = id;
        entity.executionId = executionId;
        entity.attemptId = attemptId;
        entity.direction = direction;
        entity.outcome = outcome;
        entity.observedValue = observedValue;
        entity.expectedValue = expectedValue;
        entity.targetRevision = targetRevision;
        entity.reasonCode = reasonCode;
        entity.evidenceSummary = evidenceSummary;
        entity.observedAt = observedAt;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getExecutionId() { return executionId; }
    public UUID getAttemptId() { return attemptId; }
    public String getDirection() { return direction; }
    public String getOutcome() { return outcome; }
    public String getObservedValue() { return observedValue; }
    public String getExpectedValue() { return expectedValue; }
    public Long getTargetRevision() { return targetRevision; }
    public Instant getObservedAt() { return observedAt; }
    public String getReasonCode() { return reasonCode; }
    public String getEvidenceSummary() { return evidenceSummary; }
}
