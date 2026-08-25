package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "simulation_result_metric")
public class SimulationResultMetricEntity {

    @Id
    private UUID id;

    @Column(name = "simulation_id", nullable = false)
    private UUID simulationId;

    @Column(nullable = false, length = 64)
    private String metric;

    @Column(name = "baseline_value", nullable = false)
    private double baselineValue;

    @Column(name = "candidate_value", nullable = false)
    private double candidateValue;

    @Column(nullable = false)
    private double delta;

    @Column(nullable = false, length = 32)
    private String unit;

    public static SimulationResultMetricEntity create(
            UUID id,
            UUID simulationId,
            String metric,
            double baselineValue,
            double candidateValue,
            double delta,
            String unit
    ) {
        SimulationResultMetricEntity entity = new SimulationResultMetricEntity();
        entity.id = id;
        entity.simulationId = simulationId;
        entity.metric = metric;
        entity.baselineValue = baselineValue;
        entity.candidateValue = candidateValue;
        entity.delta = delta;
        entity.unit = unit;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSimulationId() {
        return simulationId;
    }

    public String getMetric() {
        return metric;
    }

    public double getBaselineValue() {
        return baselineValue;
    }

    public double getCandidateValue() {
        return candidateValue;
    }

    public double getDelta() {
        return delta;
    }

    public String getUnit() {
        return unit;
    }
}
