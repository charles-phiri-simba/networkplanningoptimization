package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_result")
public class ActionResultEntity {

    @Id
    private UUID id;

    @Column(name = "action_id", nullable = false)
    private UUID actionId;

    @Column(name = "capability_id", nullable = false, length = 128)
    private String capabilityId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(nullable = false)
    private boolean synthetic;

    public static ActionResultEntity create(
            UUID id,
            UUID actionId,
            String capabilityId,
            String status,
            Instant startedAt,
            Instant completedAt,
            String output,
            String error,
            boolean synthetic
    ) {
        ActionResultEntity entity = new ActionResultEntity();
        entity.id = id;
        entity.actionId = actionId;
        entity.capabilityId = capabilityId;
        entity.status = status;
        entity.startedAt = startedAt;
        entity.completedAt = completedAt;
        entity.output = output;
        entity.error = error;
        entity.synthetic = synthetic;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActionId() {
        return actionId;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
