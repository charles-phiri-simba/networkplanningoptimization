package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulation_run")
public class SimulationRunEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private SimulationScenarioEntity scenario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "twin_id", nullable = false)
    private NetworkTwinEntity twin;

    @Column(name = "baseline_twin_version", nullable = false)
    private int baselineTwinVersion;

    @Column(name = "model_id", nullable = false, length = 128)
    private String modelId;

    @Column(name = "model_version", nullable = false, length = 32)
    private String modelVersion;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false)
    private boolean synthetic;

    @Column(length = 16)
    private String confidence;

    @Column(columnDefinition = "TEXT")
    private String assumptions;

    @Column(columnDefinition = "TEXT")
    private String provenance;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "action_id")
    private UUID actionId;

    public static SimulationRunEntity createSucceeded(
            UUID id,
            SimulationScenarioEntity scenario,
            NetworkTwinEntity twin,
            int baselineTwinVersion,
            String modelId,
            String modelVersion,
            Instant startedAt,
            Instant completedAt,
            String confidence,
            String assumptions,
            String provenance,
            UUID actionId
    ) {
        SimulationRunEntity entity = new SimulationRunEntity();
        entity.id = id;
        entity.scenario = scenario;
        entity.twin = twin;
        entity.baselineTwinVersion = baselineTwinVersion;
        entity.modelId = modelId;
        entity.modelVersion = modelVersion;
        entity.status = "SUCCEEDED";
        entity.startedAt = startedAt;
        entity.completedAt = completedAt;
        entity.synthetic = true;
        entity.confidence = confidence;
        entity.assumptions = assumptions;
        entity.provenance = provenance;
        entity.actionId = actionId;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public SimulationScenarioEntity getScenario() {
        return scenario;
    }

    public NetworkTwinEntity getTwin() {
        return twin;
    }

    public int getBaselineTwinVersion() {
        return baselineTwinVersion;
    }

    public String getModelId() {
        return modelId;
    }

    public String getModelVersion() {
        return modelVersion;
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

    public boolean isSynthetic() {
        return synthetic;
    }

    public String getConfidence() {
        return confidence;
    }

    public String getAssumptions() {
        return assumptions;
    }

    public String getProvenance() {
        return provenance;
    }

    public String getError() {
        return error;
    }

    public UUID getActionId() {
        return actionId;
    }
}
