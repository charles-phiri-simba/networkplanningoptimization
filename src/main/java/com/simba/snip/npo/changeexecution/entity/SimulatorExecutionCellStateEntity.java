package com.simba.snip.npo.changeexecution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulator_execution_cell_state")
public class SimulatorExecutionCellStateEntity {

    @Id
    private UUID id;

    @Column(name = "target_id", nullable = false, length = 128)
    private String targetId;

    @Column(name = "cell_id", nullable = false, length = 128)
    private String cellId;

    @Column(name = "parameter_name", nullable = false, length = 64)
    private String parameterName;

    @Column(name = "parameter_value", nullable = false, length = 32)
    private String parameterValue;

    @Column(nullable = false)
    private long revision;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static SimulatorExecutionCellStateEntity create(
            UUID id,
            String targetId,
            String cellId,
            String parameterName,
            String parameterValue,
            Instant updatedAt
    ) {
        SimulatorExecutionCellStateEntity entity = new SimulatorExecutionCellStateEntity();
        entity.id = id;
        entity.targetId = targetId;
        entity.cellId = cellId;
        entity.parameterName = parameterName;
        entity.parameterValue = parameterValue;
        entity.revision = 0L;
        entity.updatedAt = updatedAt;
        return entity;
    }

    public UUID getId() { return id; }
    public String getTargetId() { return targetId; }
    public String getCellId() { return cellId; }
    public String getParameterName() { return parameterName; }
    public String getParameterValue() { return parameterValue; }
    public long getRevision() { return revision; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void applyValue(String value, Instant updatedAt) {
        this.parameterValue = value;
        this.revision++;
        this.updatedAt = updatedAt;
    }
}
