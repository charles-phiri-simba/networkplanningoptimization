package com.simba.snip.npo.changeexecution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_execution")
public class NetworkChangeExecutionEntity {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "plan_version", nullable = false)
    private int planVersion;

    @Column(name = "plan_fingerprint", nullable = false, length = 64)
    private String planFingerprint;

    @Column(name = "execution_target_id", nullable = false, length = 128)
    private String executionTargetId;

    @Column(name = "execution_target_type", nullable = false, length = 32)
    private String executionTargetType;

    @Column(name = "execution_target_environment", nullable = false, length = 32)
    private String executionTargetEnvironment;

    @Column(name = "adapter_profile_id", nullable = false, length = 64)
    private String adapterProfileId;

    @Column(name = "capability_profile_version", nullable = false, length = 32)
    private String capabilityProfileVersion;

    @Column(name = "cell_id", nullable = false, length = 128)
    private String cellId;

    @Column(name = "parameter_name", nullable = false, length = 64)
    private String parameterName;

    @Column(name = "execution_fingerprint", nullable = false, length = 64)
    private String executionFingerprint;

    @Column(name = "authorized_execution_fingerprint", length = 64)
    private String authorizedExecutionFingerprint;

    @Column(nullable = false, length = 48)
    private String status;

    @Column(name = "requested_by", nullable = false, length = 128)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_by", length = 128)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "authorized_by", length = 128)
    private String authorizedBy;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "admitted_at")
    private Instant admittedAt;

    @Column(name = "execution_window_opens_at")
    private Instant executionWindowOpensAt;

    @Column(name = "execution_window_closes_at")
    private Instant executionWindowClosesAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "lease_key", length = 256)
    private String leaseKey;

    @Column(name = "fencing_token")
    private Long fencingToken;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_detail_safe", length = 1024)
    private String failureDetailSafe;

    @Column(name = "verification_status", length = 32)
    private String verificationStatus;

    @Column(name = "verification_completed_at")
    private Instant verificationCompletedAt;

    @Column(name = "recovery_status", length = 32)
    private String recoveryStatus;

    @Column(name = "rollback_status", length = 32)
    private String rollbackStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static NetworkChangeExecutionEntity createRequested(
            UUID id,
            UUID planId,
            int planVersion,
            String planFingerprint,
            String executionTargetId,
            String executionTargetType,
            String executionTargetEnvironment,
            String adapterProfileId,
            String capabilityProfileVersion,
            String cellId,
            String parameterName,
            String executionFingerprint,
            String requestedBy,
            Instant now
    ) {
        NetworkChangeExecutionEntity entity = new NetworkChangeExecutionEntity();
        entity.id = id;
        entity.planId = planId;
        entity.planVersion = planVersion;
        entity.planFingerprint = planFingerprint;
        entity.executionTargetId = executionTargetId;
        entity.executionTargetType = executionTargetType;
        entity.executionTargetEnvironment = executionTargetEnvironment;
        entity.adapterProfileId = adapterProfileId;
        entity.capabilityProfileVersion = capabilityProfileVersion;
        entity.cellId = cellId;
        entity.parameterName = parameterName;
        entity.executionFingerprint = executionFingerprint;
        entity.status = "REQUESTED";
        entity.requestedBy = requestedBy;
        entity.requestedAt = now;
        entity.recoveryStatus = "NOT_REQUIRED";
        entity.rollbackStatus = "NOT_REQUESTED";
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public int getPlanVersion() { return planVersion; }
    public String getPlanFingerprint() { return planFingerprint; }
    public String getExecutionTargetId() { return executionTargetId; }
    public String getExecutionTargetType() { return executionTargetType; }
    public String getExecutionTargetEnvironment() { return executionTargetEnvironment; }
    public String getAdapterProfileId() { return adapterProfileId; }
    public String getCapabilityProfileVersion() { return capabilityProfileVersion; }
    public String getCellId() { return cellId; }
    public String getParameterName() { return parameterName; }
    public String getExecutionFingerprint() { return executionFingerprint; }
    public String getAuthorizedExecutionFingerprint() { return authorizedExecutionFingerprint; }
    public String getStatus() { return status; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getAuthorizedBy() { return authorizedBy; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public Instant getAdmittedAt() { return admittedAt; }
    public Instant getExecutionWindowOpensAt() { return executionWindowOpensAt; }
    public Instant getExecutionWindowClosesAt() { return executionWindowClosesAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getLeaseKey() { return leaseKey; }
    public Long getFencingToken() { return fencingToken; }
    public String getFailureCode() { return failureCode; }
    public String getFailureDetailSafe() { return failureDetailSafe; }
    public String getVerificationStatus() { return verificationStatus; }
    public Instant getVerificationCompletedAt() { return verificationCompletedAt; }
    public String getRecoveryStatus() { return recoveryStatus; }
    public String getRollbackStatus() { return rollbackStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public void setStatus(String status) { this.status = status; }
    public void setExecutionFingerprint(String executionFingerprint) { this.executionFingerprint = executionFingerprint; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public void markAdmitted(Instant now) {
        this.status = "READY_FOR_REVIEW";
        this.admittedAt = now;
        this.updatedAt = now;
    }

    public void markAdmissionRejected(String failureCode, String detail, Instant now) {
        this.status = "PRELIMINARY_ADMISSION_REJECTED";
        this.failureCode = failureCode;
        this.failureDetailSafe = detail;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markReviewed(String reviewer, Instant now) {
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
        this.status = "READY_FOR_EXECUTION_AUTHORIZATION";
        this.updatedAt = now;
    }

    public void markAuthorized(String authorizer, String authorizedFingerprint, Instant windowOpens, Instant windowCloses, Instant now) {
        this.authorizedBy = authorizer;
        this.authorizedAt = now;
        this.authorizedExecutionFingerprint = authorizedFingerprint;
        this.executionWindowOpensAt = windowOpens;
        this.executionWindowClosesAt = windowCloses;
        this.status = "AUTHORIZED";
        this.updatedAt = now;
    }

    public void markLeaseAcquired(String leaseKey, long fencingToken, Instant now) {
        this.leaseKey = leaseKey;
        this.fencingToken = fencingToken;
        this.updatedAt = now;
    }

    public void markExecuting(Instant now) {
        this.status = "EXECUTING";
        this.startedAt = now;
        this.updatedAt = now;
    }

    public void markApplied(Instant now) {
        this.status = "APPLIED";
        this.updatedAt = now;
    }

    public void markOutcomeUnknown(String failureCode, String detail, Instant now) {
        this.status = "EXECUTION_OUTCOME_UNKNOWN";
        this.failureCode = failureCode;
        this.failureDetailSafe = detail;
        this.updatedAt = now;
    }

    public void markVerifying(Instant now) {
        this.status = "VERIFYING";
        this.updatedAt = now;
    }

    public void markVerified(String verificationStatus, Instant now) {
        this.status = "VERIFIED";
        this.verificationStatus = verificationStatus;
        this.verificationCompletedAt = now;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markVerificationFailed(String failureCode, String detail, Instant now) {
        this.status = "VERIFICATION_FAILED";
        this.failureCode = failureCode;
        this.failureDetailSafe = detail;
        this.recoveryStatus = "REQUIRED";
        this.updatedAt = now;
    }

    public void markRecoveryRequired(Instant now) {
        this.status = "RECOVERY_REQUIRED";
        this.recoveryStatus = "REQUIRED";
        this.updatedAt = now;
    }

    public void markExecutionFailed(String failureCode, String detail, Instant now) {
        this.status = "EXECUTION_FAILED";
        this.failureCode = failureCode;
        this.failureDetailSafe = detail;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markCancelled(String detail, Instant now) {
        this.status = "CANCELLED_BEFORE_MUTATION";
        this.failureDetailSafe = detail;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markManualIntervention(String detail, Instant now) {
        this.status = "MANUAL_INTERVENTION_REQUIRED";
        this.failureCode = "MANUAL_INTERVENTION_REQUIRED";
        this.failureDetailSafe = detail;
        this.recoveryStatus = "MANUAL_INTERVENTION";
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markRolledBack(Instant now) {
        this.status = "ROLLED_BACK";
        this.rollbackStatus = "VERIFIED";
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public void markRollbackStatus(String rollbackStatus) {
        this.rollbackStatus = rollbackStatus;
    }

    public void clearLease() {
        this.leaseKey = null;
        this.fencingToken = null;
    }
}
