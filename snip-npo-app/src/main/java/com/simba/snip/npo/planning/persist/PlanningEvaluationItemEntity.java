package com.simba.snip.npo.planning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "planning_evaluation_item")
public class PlanningEvaluationItemEntity {

    @Id
    private UUID id;

    @Column(name = "evaluation_id", nullable = false)
    private UUID evaluationId;

    @Column(name = "alternative_id", nullable = false)
    private UUID alternativeId;

    @Column(name = "cell_id", nullable = false, length = 64)
    private String cellId;

    @Column(name = "parameter_id", nullable = false, length = 64)
    private String parameterId;

    @Column(name = "intended_value", nullable = false, precision = 8, scale = 3)
    private BigDecimal intendedValue;

    @Column(nullable = false, length = 16)
    private String outcome;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", length = 512)
    private String failureMessage;

    @Column(name = "twin_id")
    private UUID twinId;

    @Column(name = "twin_version")
    private Integer twinVersion;

    @Column(name = "pinned_baseline_tx_power", precision = 8, scale = 3)
    private BigDecimal pinnedBaselineTxPower;

    @Column(name = "configuration_fingerprint", length = 128)
    private String configurationFingerprint;

    @Column(name = "simulation_scenario_id")
    private UUID simulationScenarioId;

    @Column(name = "simulation_run_id")
    private UUID simulationRunId;

    @Column(name = "model_id", length = 128)
    private String modelId;

    @Column(name = "model_version", length = 32)
    private String modelVersion;

    private Boolean synthetic;

    @Column(length = 16)
    private String confidence;

    @Column(name = "reused_existing_run", nullable = false)
    private boolean reusedExistingRun;

    public static PlanningEvaluationItemEntity failed(
            UUID id,
            UUID evaluationId,
            UUID alternativeId,
            String cellId,
            String parameterId,
            BigDecimal intendedValue,
            String failureCode,
            String failureMessage
    ) {
        PlanningEvaluationItemEntity entity = new PlanningEvaluationItemEntity();
        entity.id = id;
        entity.evaluationId = evaluationId;
        entity.alternativeId = alternativeId;
        entity.cellId = cellId;
        entity.parameterId = parameterId;
        entity.intendedValue = intendedValue;
        entity.outcome = "FAILED";
        entity.failureCode = failureCode;
        entity.failureMessage = trim(failureMessage);
        return entity;
    }

    public static PlanningEvaluationItemEntity succeeded(
            UUID id,
            UUID evaluationId,
            UUID alternativeId,
            String cellId,
            String parameterId,
            BigDecimal intendedValue,
            UUID twinId,
            int twinVersion,
            BigDecimal pinnedBaselineTxPower,
            String configurationFingerprint,
            UUID simulationScenarioId,
            UUID simulationRunId,
            String modelId,
            String modelVersion,
            boolean reusedExistingRun,
            String confidence
    ) {
        PlanningEvaluationItemEntity entity = new PlanningEvaluationItemEntity();
        entity.id = id;
        entity.evaluationId = evaluationId;
        entity.alternativeId = alternativeId;
        entity.cellId = cellId;
        entity.parameterId = parameterId;
        entity.intendedValue = intendedValue;
        entity.outcome = "SUCCEEDED";
        entity.twinId = twinId;
        entity.twinVersion = twinVersion;
        entity.pinnedBaselineTxPower = pinnedBaselineTxPower;
        entity.configurationFingerprint = configurationFingerprint;
        entity.simulationScenarioId = simulationScenarioId;
        entity.simulationRunId = simulationRunId;
        entity.modelId = modelId;
        entity.modelVersion = modelVersion;
        entity.synthetic = Boolean.TRUE;
        entity.confidence = confidence;
        entity.reusedExistingRun = reusedExistingRun;
        return entity;
    }

    private static String trim(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEvaluationId() {
        return evaluationId;
    }

    public UUID getAlternativeId() {
        return alternativeId;
    }

    public String getCellId() {
        return cellId;
    }

    public String getParameterId() {
        return parameterId;
    }

    public BigDecimal getIntendedValue() {
        return intendedValue;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public UUID getTwinId() {
        return twinId;
    }

    public Integer getTwinVersion() {
        return twinVersion;
    }

    public BigDecimal getPinnedBaselineTxPower() {
        return pinnedBaselineTxPower;
    }

    public String getConfigurationFingerprint() {
        return configurationFingerprint;
    }

    public UUID getSimulationScenarioId() {
        return simulationScenarioId;
    }

    public UUID getSimulationRunId() {
        return simulationRunId;
    }

    public String getModelId() {
        return modelId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public Boolean getSynthetic() {
        return synthetic;
    }

    public String getConfidence() {
        return confidence;
    }

    public boolean isReusedExistingRun() {
        return reusedExistingRun;
    }
}
