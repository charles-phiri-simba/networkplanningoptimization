package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "agent_plan")
public class AgentPlanEntity {

    @Id
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(nullable = false, length = 1024)
    private String objective;

    public static AgentPlanEntity create(UUID id, UUID runId, String objective) {
        AgentPlanEntity entity = new AgentPlanEntity();
        entity.id = id;
        entity.runId = runId;
        entity.objective = objective;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRunId() {
        return runId;
    }

    public String getObjective() {
        return objective;
    }
}
