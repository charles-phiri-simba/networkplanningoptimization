package com.simba.snip.npo.changeexecution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_execution_attempt")
public class NetworkChangeExecutionAttemptEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(nullable = false, length = 16)
    private String direction;

    @Column(nullable = false, length = 32)
    private String outcome;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_detail_safe", length = 1024)
    private String failureDetailSafe;

    @Column(name = "target_revision_before")
    private Long targetRevisionBefore;

    @Column(name = "target_revision_after")
    private Long targetRevisionAfter;

    public static NetworkChangeExecutionAttemptEntity start(
            UUID id,
            UUID executionId,
            int attemptNumber,
            String direction,
            Long targetRevisionBefore,
            Instant startedAt
    ) {
        NetworkChangeExecutionAttemptEntity entity = new NetworkChangeExecutionAttemptEntity();
        entity.id = id;
        entity.executionId = executionId;
        entity.attemptNumber = attemptNumber;
        entity.direction = direction;
        entity.outcome = "SKIPPED";
        entity.startedAt = startedAt;
        entity.targetRevisionBefore = targetRevisionBefore;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getExecutionId() { return executionId; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getDirection() { return direction; }
    public String getOutcome() { return outcome; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getFailureCode() { return failureCode; }
    public String getFailureDetailSafe() { return failureDetailSafe; }
    public Long getTargetRevisionBefore() { return targetRevisionBefore; }
    public Long getTargetRevisionAfter() { return targetRevisionAfter; }

    public void complete(String outcome, String failureCode, String detail, Long revisionAfter, Instant completedAt) {
        this.outcome = outcome;
        this.failureCode = failureCode;
        this.failureDetailSafe = detail;
        this.targetRevisionAfter = revisionAfter;
        this.completedAt = completedAt;
    }
}
