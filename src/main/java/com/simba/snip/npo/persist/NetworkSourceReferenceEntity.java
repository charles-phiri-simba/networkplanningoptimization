package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_source_reference")
public class NetworkSourceReferenceEntity {

    @Id
    private UUID id;

    @Column(name = "canonical_entity_type", nullable = false, length = 32)
    private String canonicalEntityType;

    @Column(name = "canonical_entity_id", nullable = false, length = 64)
    private String canonicalEntityId;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(nullable = false, length = 32)
    private String vendor;

    @Column(name = "source_entity_type", nullable = false, length = 32)
    private String sourceEntityType;

    @Column(name = "source_entity_id", nullable = false, length = 128)
    private String sourceEntityId;

    @Column(name = "source_dn", length = 256)
    private String sourceDn;

    @Column(nullable = false)
    private boolean authoritative;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "last_source_snapshot_id", nullable = false, length = 128)
    private String lastSourceSnapshotId;

    @Column(name = "vendor_schema_version", nullable = false, length = 64)
    private String vendorSchemaVersion;

    @Column(name = "source_observed_at", nullable = false)
    private Instant sourceObservedAt;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "import_batch_id", nullable = false)
    private UUID importBatchId;

    @Column(name = "source_status", nullable = false, length = 32)
    private String sourceStatus;

    public static NetworkSourceReferenceEntity create(
            UUID id,
            String canonicalEntityType,
            String canonicalEntityId,
            String sourceSystem,
            String vendor,
            String sourceEntityType,
            String sourceEntityId,
            String sourceDn,
            boolean authoritative,
            Instant seenAt,
            String lastSourceSnapshotId,
            String vendorSchemaVersion,
            Instant sourceObservedAt,
            Instant importedAt,
            UUID importBatchId,
            String sourceStatus
    ) {
        NetworkSourceReferenceEntity entity = new NetworkSourceReferenceEntity();
        entity.id = id;
        entity.canonicalEntityType = canonicalEntityType;
        entity.canonicalEntityId = canonicalEntityId;
        entity.sourceSystem = sourceSystem;
        entity.vendor = vendor;
        entity.sourceEntityType = sourceEntityType;
        entity.sourceEntityId = sourceEntityId;
        entity.sourceDn = sourceDn;
        entity.authoritative = authoritative;
        entity.firstSeenAt = seenAt;
        entity.lastSeenAt = seenAt;
        entity.lastSourceSnapshotId = lastSourceSnapshotId;
        entity.vendorSchemaVersion = vendorSchemaVersion;
        entity.sourceObservedAt = sourceObservedAt;
        entity.importedAt = importedAt;
        entity.importBatchId = importBatchId;
        entity.sourceStatus = sourceStatus;
        return entity;
    }

    public void markSeen(
            Instant seenAt,
            String lastSourceSnapshotId,
            String vendorSchemaVersion,
            Instant sourceObservedAt,
            Instant importedAt,
            UUID importBatchId,
            String sourceStatus
    ) {
        this.lastSeenAt = seenAt;
        this.lastSourceSnapshotId = lastSourceSnapshotId;
        this.vendorSchemaVersion = vendorSchemaVersion;
        this.sourceObservedAt = sourceObservedAt;
        this.importedAt = importedAt;
        this.importBatchId = importBatchId;
        this.sourceStatus = sourceStatus;
    }

    public void markMissing(Instant importedAt, UUID importBatchId) {
        this.sourceStatus = "MISSING";
        this.importedAt = importedAt;
        this.importBatchId = importBatchId;
    }

    public UUID getId() {
        return id;
    }

    public String getCanonicalEntityType() {
        return canonicalEntityType;
    }

    public String getCanonicalEntityId() {
        return canonicalEntityId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getVendor() {
        return vendor;
    }

    public String getSourceEntityType() {
        return sourceEntityType;
    }

    public String getSourceEntityId() {
        return sourceEntityId;
    }

    public String getSourceDn() {
        return sourceDn;
    }

    public boolean isAuthoritative() {
        return authoritative;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public String getLastSourceSnapshotId() {
        return lastSourceSnapshotId;
    }

    public String getVendorSchemaVersion() {
        return vendorSchemaVersion;
    }

    public Instant getSourceObservedAt() {
        return sourceObservedAt;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public String getSourceStatus() {
        return sourceStatus;
    }
}
