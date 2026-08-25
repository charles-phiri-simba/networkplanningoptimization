package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "agent_plan_step")
public class AgentPlanStepEntity {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(name = "agent_role", nullable = false, length = 32)
    private String agentRole;

    @Column(nullable = false, length = 512)
    private String task;

    @Column(name = "required_inputs", nullable = false, columnDefinition = "TEXT")
    private String requiredInputs;

    @Column(name = "expected_output", nullable = false, length = 512)
    private String expectedOutput;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    public static AgentPlanStepEntity create(
            UUID id,
            UUID planId,
            int stepNumber,
            String agentRole,
            String task,
            String requiredInputs,
            String expectedOutput,
            String status
    ) {
        AgentPlanStepEntity entity = new AgentPlanStepEntity();
        entity.id = id;
        entity.planId = planId;
        entity.stepNumber = stepNumber;
        entity.agentRole = agentRole;
        entity.task = task;
        entity.requiredInputs = requiredInputs;
        entity.expectedOutput = expectedOutput;
        entity.status = status;
        return entity;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlanId() {
        return planId;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getAgentRole() {
        return agentRole;
    }

    public String getTask() {
        return task;
    }

    public String getRequiredInputs() {
        return requiredInputs;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public String getStatus() {
        return status;
    }

    public String getOutputSummary() {
        return outputSummary;
    }
}
