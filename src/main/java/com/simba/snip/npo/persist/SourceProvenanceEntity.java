package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_provenance")
public class SourceProvenanceEntity {

    @Id
    private UUID id;

    @Column(name = "canonical_entity_type", nullable = false, length = 32)
    private String canonicalEntityType;

    @Column(name = "canonical_entity_id", nullable = false, length = 64)
    private String canonicalEntityId;

    @Column(name = "source_vendor", nullable = false, length = 32)
    private String sourceVendor;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "source_object_type", nullable = false, length = 32)
    private String sourceObjectType;

    @Column(name = "source_object_id", nullable = false, length = 128)
    private String sourceObjectId;

    @Column(name = "source_snapshot_id", nullable = false, length = 128)
    private String sourceSnapshotId;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "import_execution_id", nullable = false)
    private UUID importExecutionId;

    public static SourceProvenanceEntity create(
            UUID id,
            String canonicalEntityType,
            String canonicalEntityId,
            String sourceVendor,
            String sourceSystem,
            String sourceObjectType,
            String sourceObjectId,
            String sourceSnapshotId,
            Instant observedAt,
            UUID importExecutionId
    ) {
        SourceProvenanceEntity entity = new SourceProvenanceEntity();
        entity.id = id;
        entity.canonicalEntityType = canonicalEntityType;
        entity.canonicalEntityId = canonicalEntityId;
        entity.sourceVendor = sourceVendor;
        entity.sourceSystem = sourceSystem;
        entity.sourceObjectType = sourceObjectType;
        entity.sourceObjectId = sourceObjectId;
        entity.sourceSnapshotId = sourceSnapshotId;
        entity.observedAt = observedAt;
        entity.importExecutionId = importExecutionId;
        return entity;
    }

    public String getCanonicalEntityId() {
        return canonicalEntityId;
    }

    public String getSourceObjectId() {
        return sourceObjectId;
    }

    public String getSourceSnapshotId() {
        return sourceSnapshotId;
    }

    public UUID getImportExecutionId() {
        return importExecutionId;
    }

    public String getSourceVendor() {
        return sourceVendor;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourceObjectType() {
        return sourceObjectType;
    }

    public String getCanonicalEntityType() {
        return canonicalEntityType;
    }

    public Instant getObservedAt() {
        return observedAt;
    }
}
