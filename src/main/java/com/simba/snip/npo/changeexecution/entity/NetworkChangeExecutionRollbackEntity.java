package com.simba.snip.npo.changeexecution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_execution_rollback")
public class NetworkChangeExecutionRollbackEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "requested_by", length = 128)
    private String requestedBy;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "reviewed_by", length = 128)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "authorized_by", length = 128)
    private String authorizedBy;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "authorized_rollback_fingerprint", length = 64)
    private String authorizedRollbackFingerprint;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_detail_safe", length = 1024)
    private String failureDetailSafe;

    public static NetworkChangeExecutionRollbackEntity createPending(UUID id, UUID executionId) {
        NetworkChangeExecutionRollbackEntity entity = new NetworkChangeExecutionRollbackEntity();
        entity.id = id;
        entity.executionId = executionId;
        entity.status = "NOT_REQUESTED";
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getExecutionId() { return executionId; }
    public String getStatus() { return status; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getAuthorizedBy() { return authorizedBy; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public String getAuthorizedRollbackFingerprint() { return authorizedRollbackFingerprint; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getFailureCode() { return failureCode; }
    public String getFailureDetailSafe() { return failureDetailSafe; }

    public void markRequested(String requester, Instant now) {
        this.status = "REQUESTED";
        this.requestedBy = requester;
        this.requestedAt = now;
    }

    public void markReviewed(String reviewer, Instant now) {
        this.status = "REVIEWED";
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
    }

    public void markAuthorized(String authorizer, String fingerprint, Instant now) {
        this.status = "AUTHORIZED";
        this.authorizedBy = authorizer;
        this.authorizedAt = now;
        this.authorizedRollbackFingerprint = fingerprint;
    }

    public void markExecuting(Instant now) {
        this.status = "EXECUTING";
        this.startedAt = now;
    }

    public void markApplied(Instant now) {
        this.status = "APPLIED";
    }

    public void markOutcomeUnknown(String failureCode, String detail, Instant now) {
        this.status = "OUTCOME_UNKNOWN";
        this.failureCode = failureCode;
        this.failureDetailSafe = detail;
    }

    public void markVerified(Instant now) {
        this.status = "VERIFIED";
        this.completedAt = now;
    }

    public void markFailed(String failureCode, String detail, Instant now) {
        this.status = "FAILED";
        this.failureCode = failureCode;
        this.failureDetailSafe = detail;
        this.completedAt = now;
    }
}
