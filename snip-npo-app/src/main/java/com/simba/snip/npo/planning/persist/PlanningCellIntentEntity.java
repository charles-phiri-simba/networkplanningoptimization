package com.simba.snip.npo.planning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "planning_cell_intent")
public class PlanningCellIntentEntity {

    @Id
    private UUID id;

    @Column(name = "alternative_id", nullable = false)
    private UUID alternativeId;

    @Column(name = "cell_id", nullable = false, length = 64)
    private String cellId;

    @Column(name = "parameter_id", nullable = false, length = 64)
    private String parameterId;

    @Column(name = "intended_value", nullable = false, precision = 8, scale = 3)
    private BigDecimal intendedValue;

    public static PlanningCellIntentEntity create(
            UUID id,
            UUID alternativeId,
            String cellId,
            String parameterId,
            BigDecimal intendedValue
    ) {
        PlanningCellIntentEntity entity = new PlanningCellIntentEntity();
        entity.id = id;
        entity.alternativeId = alternativeId;
        entity.cellId = cellId;
        entity.parameterId = parameterId;
        entity.intendedValue = intendedValue;
        return entity;
    }

    public UUID getId() {
        return id;
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
}
