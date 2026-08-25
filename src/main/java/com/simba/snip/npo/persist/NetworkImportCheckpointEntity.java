package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_import_checkpoint")
public class NetworkImportCheckpointEntity {

    @Id
    @Column(name = "checkpoint_id")
    private UUID checkpointId;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "checkpoint_type", nullable = false, length = 64)
    private String checkpointType;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    public static NetworkImportCheckpointEntity create(
            UUID checkpointId, UUID executionId, String checkpointType, Instant recordedAt, String details
    ) {
        NetworkImportCheckpointEntity entity = new NetworkImportCheckpointEntity();
        entity.checkpointId = checkpointId;
        entity.executionId = executionId;
        entity.checkpointType = checkpointType;
        entity.recordedAt = recordedAt;
        entity.details = details;
        return entity;
    }

    public UUID getCheckpointId() {
        return checkpointId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getCheckpointType() {
        return checkpointType;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getDetails() {
        return details;
    }
}
