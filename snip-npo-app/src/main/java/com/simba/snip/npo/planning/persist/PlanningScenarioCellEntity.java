package com.simba.snip.npo.planning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "planning_scenario_cell")
public class PlanningScenarioCellEntity {

    @Id
    private UUID id;

    @Column(name = "scenario_id", nullable = false)
    private UUID scenarioId;

    @Column(name = "cell_id", nullable = false, length = 64)
    private String cellId;

    @Column(nullable = false)
    private int ordinal;

    public static PlanningScenarioCellEntity create(UUID id, UUID scenarioId, String cellId, int ordinal) {
        PlanningScenarioCellEntity entity = new PlanningScenarioCellEntity();
        entity.id = id;
        entity.scenarioId = scenarioId;
        entity.cellId = cellId;
        entity.ordinal = ordinal;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public String getCellId() {
        return cellId;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
