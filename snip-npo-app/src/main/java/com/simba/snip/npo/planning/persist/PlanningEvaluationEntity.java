package com.simba.snip.npo.planning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "planning_evaluation")
public class PlanningEvaluationEntity {

    @Id
    private UUID id;

    @Column(name = "scenario_id", nullable = false)
    private UUID scenarioId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "intent_fingerprint", nullable = false, length = 64)
    private String intentFingerprint;

    @Column(name = "admission_fingerprint", length = 64)
    private String admissionFingerprint;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "succeeded_count", nullable = false)
    private int succeededCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    public static PlanningEvaluationEntity start(
            UUID id,
            UUID scenarioId,
            String intentFingerprint,
            String admissionFingerprint,
            String createdBy,
            Instant createdAt,
            int itemCount
    ) {
        PlanningEvaluationEntity entity = new PlanningEvaluationEntity();
        entity.id = id;
        entity.scenarioId = scenarioId;
        entity.status = "IN_PROGRESS";
        entity.intentFingerprint = intentFingerprint;
        entity.admissionFingerprint = admissionFingerprint;
        entity.createdBy = createdBy;
        entity.createdAt = createdAt;
        entity.itemCount = itemCount;
        entity.succeededCount = 0;
        entity.failedCount = 0;
        return entity;
    }

    public void finalizeStatus(String status, int succeededCount, int failedCount, Instant completedAt) {
        this.status = status;
        this.succeededCount = succeededCount;
        this.failedCount = failedCount;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public String getStatus() {
        return status;
    }

    public String getIntentFingerprint() {
        return intentFingerprint;
    }

    public String getAdmissionFingerprint() {
        return admissionFingerprint;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getSucceededCount() {
        return succeededCount;
    }

    public int getFailedCount() {
        return failedCount;
    }
}
