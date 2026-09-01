package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "simulation_scenario_change")
public class SimulationScenarioChangeEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private SimulationScenarioEntity scenario;

    @Column(name = "parameter_id", nullable = false, length = 64)
    private String parameterId;

    @Column(name = "current_value", nullable = false, length = 64)
    private String currentValue;

    @Column(name = "proposed_value", nullable = false, length = 64)
    private String proposedValue;

    @Column(nullable = false, length = 32)
    private String unit;

    public static SimulationScenarioChangeEntity create(
            UUID id,
            SimulationScenarioEntity scenario,
            String parameterId,
            String currentValue,
            String proposedValue,
            String unit
    ) {
        SimulationScenarioChangeEntity entity = new SimulationScenarioChangeEntity();
        entity.id = id;
        entity.scenario = scenario;
        entity.parameterId = parameterId;
        entity.currentValue = currentValue;
        entity.proposedValue = proposedValue;
        entity.unit = unit;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public SimulationScenarioEntity getScenario() {
        return scenario;
    }

    public String getParameterId() {
        return parameterId;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public String getProposedValue() {
        return proposedValue;
    }

    public String getUnit() {
        return unit;
    }
}
