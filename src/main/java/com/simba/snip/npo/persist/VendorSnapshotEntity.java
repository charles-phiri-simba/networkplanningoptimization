package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_snapshot")
public class VendorSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "snapshot_id", nullable = false, length = 128)
    private String snapshotId;

    @Column(name = "connector_id", nullable = false, length = 128)
    private String connectorId;

    @Column(name = "source_vendor", nullable = false, length = 32)
    private String sourceVendor;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false, length = 16)
    private String completeness;

    @Column(name = "pages_received", nullable = false)
    private int pagesReceived;

    @Column(name = "entities_read", nullable = false)
    private int entitiesRead;

    @Column(name = "source_version", length = 64)
    private String sourceVersion;

    @Column(length = 256)
    private String warnings;

    public static VendorSnapshotEntity create(
            UUID id,
            UUID executionId,
            String snapshotId,
            String connectorId,
            String sourceVendor,
            String sourceSystem,
            Instant startedAt,
            Instant completedAt,
            String completeness,
            int pagesReceived,
            int entitiesRead,
            String sourceVersion,
            String warnings
    ) {
        VendorSnapshotEntity entity = new VendorSnapshotEntity();
        entity.id = id;
        entity.executionId = executionId;
        entity.snapshotId = snapshotId;
        entity.connectorId = connectorId;
        entity.sourceVendor = sourceVendor;
        entity.sourceSystem = sourceSystem;
        entity.startedAt = startedAt;
        entity.completedAt = completedAt;
        entity.completeness = completeness;
        entity.pagesReceived = pagesReceived;
        entity.entitiesRead = entitiesRead;
        entity.sourceVersion = sourceVersion;
        entity.warnings = warnings;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public String getCompleteness() {
        return completeness;
    }

    public int getPagesReceived() {
        return pagesReceived;
    }

    public int getEntitiesRead() {
        return entitiesRead;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public String getWarnings() {
        return warnings;
    }
}
