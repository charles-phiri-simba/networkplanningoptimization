package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "simulation_limitation")
public class SimulationLimitationEntity {

    @Id
    private UUID id;

    @Column(name = "simulation_id", nullable = false)
    private UUID simulationId;

    @Column(nullable = false, length = 64)
    private String code;

    public static SimulationLimitationEntity create(UUID id, UUID simulationId, String code) {
        SimulationLimitationEntity entity = new SimulationLimitationEntity();
        entity.id = id;
        entity.simulationId = simulationId;
        entity.code = code;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSimulationId() {
        return simulationId;
    }

    public String getCode() {
        return code;
    }
}
