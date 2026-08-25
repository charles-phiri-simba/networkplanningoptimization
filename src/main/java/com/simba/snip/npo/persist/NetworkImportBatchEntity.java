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

    public static NetworkImportBatchEntity start(
            UUID id,
            String sourceSystem,
            String vendor,
            String vendorSchemaVersion,
            String fixtureKind,
            Instant startedAt
    ) {
        NetworkImportBatchEntity entity = new NetworkImportBatchEntity();
        entity.id = id;
        entity.sourceSystem = sourceSystem;
        entity.vendor = vendor;
        entity.sourceSnapshotId = "UNREAD";
        entity.vendorSchemaVersion = vendorSchemaVersion;
        entity.fixtureKind = fixtureKind;
        entity.startedAt = startedAt;
        entity.status = "STARTED";
        return entity;
    }

    public void recordSnapshot(String sourceSnapshotId, String vendorSchemaVersion) {
        this.sourceSnapshotId = sourceSnapshotId;
        this.vendorSchemaVersion = vendorSchemaVersion;
    }

    public void complete(
            Instant completedAt,
            int entitiesRead,
            int entitiesCreated,
            int entitiesUpdated,
            int entitiesUnchanged,
            int entitiesRejected,
            int conflictsDetected,
            int missingEntitiesDetected
    ) {
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
    }

    public void fail(Instant completedAt, String error) {
        this.completedAt = completedAt;
        this.status = "FAILED";
        this.error = error;
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
}
