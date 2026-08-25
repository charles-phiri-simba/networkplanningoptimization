package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_import_batch")
public class NetworkImportBatchEntity {

    @Id
    private UUID id;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(nullable = false, length = 32)
    private String vendor;

    @Column(name = "source_snapshot_id", nullable = false, length = 128)
    private String sourceSnapshotId;

    @Column(name = "vendor_schema_version", nullable = false, length = 64)
    private String vendorSchemaVersion;

    @Column(name = "fixture_kind", nullable = false, length = 32)
    private String fixtureKind;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "entities_read", nullable = false)
    private int entitiesRead;

    @Column(name = "entities_created", nullable = false)
    private int entitiesCreated;

    @Column(name = "entities_updated", nullable = false)
    private int entitiesUpdated;

    @Column(name = "entities_unchanged", nullable = false)
    private int entitiesUnchanged;

    @Column(name = "entities_rejected", nullable = false)
    private int entitiesRejected;

    @Column(name = "conflicts_detected", nullable = false)
    private int conflictsDetected;

    @Column(name = "missing_entities_detected", nullable = false)
    private int missingEntitiesDetected;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "execution_type", nullable = false, length = 32)
    private String executionType;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "previous_execution_id")
    private UUID previousExecutionId;

    @Column(name = "original_successful_execution_id")
    private UUID originalSuccessfulExecutionId;

    @Column(name = "source_scope", nullable = false, length = 64)
    private String sourceScope;

    @Column(name = "canonical_snapshot_hash", length = 64)
    private String canonicalSnapshotHash;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "retryable")
    private Boolean retryable;

    @Column(name = "owner_instance_id", length = 64)
    private String ownerInstanceId;

    @Column(name = "lease_fencing_token")
    private Long leaseFencingToken;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    public static NetworkImportBatchEntity requested(
            UUID id,
            String sourceSystem,
            String vendor,
            String vendorSchemaVersion,
            String fixtureKind,
            String sourceScope,
            String executionType,
            int attemptNumber,
            UUID previousExecutionId,
            Instant requestedAt,
            String ownerInstanceId
    ) {
        NetworkImportBatchEntity entity = new NetworkImportBatchEntity();
        entity.id = id;
        entity.sourceSystem = sourceSystem;
        entity.vendor = vendor;
        entity.sourceSnapshotId = "UNREAD";
        entity.vendorSchemaVersion = vendorSchemaVersion;
        entity.fixtureKind = fixtureKind;
        entity.sourceScope = sourceScope;
        entity.executionType = executionType;
        entity.attemptNumber = attemptNumber;
        entity.previousExecutionId = previousExecutionId;
        entity.requestedAt = requestedAt;
        entity.startedAt = requestedAt;
        entity.status = "REQUESTED";
        entity.ownerInstanceId = ownerInstanceId;
        return entity;
    }

    public void recordSnapshot(String sourceSnapshotId, String vendorSchemaVersion, String canonicalSnapshotHash) {
        this.sourceSnapshotId = sourceSnapshotId;
        this.vendorSchemaVersion = vendorSchemaVersion;
        this.canonicalSnapshotHash = canonicalSnapshotHash;
    }

    public void markRunning(Instant startedAt, Long leaseFencingToken) {
        this.startedAt = startedAt;
        this.leaseFencingToken = leaseFencingToken;
        this.status = "RUNNING";
    }

    public boolean complete(
            Instant completedAt,
            int entitiesRead,
            int entitiesCreated,
            int entitiesUpdated,
            int entitiesUnchanged,
            int entitiesRejected,
            int conflictsDetected,
            int missingEntitiesDetected
    ) {
        if (!"RUNNING".equals(this.status) && !"REQUESTED".equals(this.status)) {
            return false;
        }
        this.completedAt = completedAt;
        this.status = "COMPLETED";
        this.entitiesRead = entitiesRead;
        this.entitiesCreated = entitiesCreated;
        this.entitiesUpdated = entitiesUpdated;
        this.entitiesUnchanged = entitiesUnchanged;
        this.entitiesRejected = entitiesRejected;
        this.conflictsDetected = conflictsDetected;
        this.missingEntitiesDetected = missingEntitiesDetected;
        this.error = null;
        this.failureCode = null;
        this.retryable = null;
        return true;
    }

    public void completeReplay(
            Instant completedAt,
            UUID originalSuccessfulExecutionId,
            String sourceSnapshotId,
            String vendorSchemaVersion,
            String canonicalSnapshotHash
    ) {
        this.executionType = "REPLAY";
        this.status = "COMPLETED";
        this.completedAt = completedAt;
        this.originalSuccessfulExecutionId = originalSuccessfulExecutionId;
        this.sourceSnapshotId = sourceSnapshotId;
        this.vendorSchemaVersion = vendorSchemaVersion;
        this.canonicalSnapshotHash = canonicalSnapshotHash;
        this.entitiesRead = 0;
        this.entitiesCreated = 0;
        this.entitiesUpdated = 0;
        this.entitiesUnchanged = 0;
        this.entitiesRejected = 0;
        this.conflictsDetected = 0;
        this.missingEntitiesDetected = 0;
        this.error = null;
        this.failureCode = null;
        this.retryable = null;
    }

    public boolean terminalize(String status, Instant completedAt, String failureCode, boolean retryable, String error) {
        if ("COMPLETED".equals(this.status)
                || "FAILED".equals(this.status)
                || "TIMED_OUT".equals(this.status)
                || "REJECTED".equals(this.status)) {
            return false;
        }
        this.completedAt = completedAt;
        this.status = status;
        this.failureCode = failureCode;
        this.retryable = retryable;
        this.error = error;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getVendor() {
        return vendor;
    }

    public String getSourceSnapshotId() {
        return sourceSnapshotId;
    }

    public String getVendorSchemaVersion() {
        return vendorSchemaVersion;
    }

    public String getFixtureKind() {
        return fixtureKind;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getStatus() {
        return status;
    }

    public int getEntitiesRead() {
        return entitiesRead;
    }

    public int getEntitiesCreated() {
        return entitiesCreated;
    }

    public int getEntitiesUpdated() {
        return entitiesUpdated;
    }

    public int getEntitiesUnchanged() {
        return entitiesUnchanged;
    }

    public int getEntitiesRejected() {
        return entitiesRejected;
    }

    public int getConflictsDetected() {
        return conflictsDetected;
    }

    public int getMissingEntitiesDetected() {
        return missingEntitiesDetected;
    }

    public String getError() {
        return error;
    }

    public String getExecutionType() {
        return executionType;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public UUID getPreviousExecutionId() {
        return previousExecutionId;
    }

    public UUID getOriginalSuccessfulExecutionId() {
        return originalSuccessfulExecutionId;
    }

    public String getSourceScope() {
        return sourceScope;
    }

    public String getCanonicalSnapshotHash() {
        return canonicalSnapshotHash;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public String getOwnerInstanceId() {
        return ownerInstanceId;
    }

    public Long getLeaseFencingToken() {
        return leaseFencingToken;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
