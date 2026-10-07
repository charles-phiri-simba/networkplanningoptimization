package com.simba.snip.npo.planning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "planning_alternative")
public class PlanningAlternativeEntity {

    @Id
    private UUID id;

    @Column(name = "scenario_id", nullable = false)
    private UUID scenarioId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false)
    private int ordinal;

    public static PlanningAlternativeEntity create(UUID id, UUID scenarioId, String name, int ordinal) {
        PlanningAlternativeEntity entity = new PlanningAlternativeEntity();
        entity.id = id;
        entity.scenarioId = scenarioId;
        entity.name = name;
        entity.ordinal = ordinal;
        return entity;
    }

    public void rename(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public String getName() {
        return name;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
