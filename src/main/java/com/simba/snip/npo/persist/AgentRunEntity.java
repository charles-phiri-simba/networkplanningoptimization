package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_run")
public class AgentRunEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 1024)
    private String objective;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "assurance_case_id", nullable = false)
    private UUID assuranceCaseId;

    @Column(name = "initiated_by", nullable = false, length = 64)
    private String initiatedBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "max_steps", nullable = false)
    private int maxSteps;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Column(name = "max_agent_calls", nullable = false)
    private int maxAgentCalls;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Column(name = "timeout_ms", nullable = false)
    private long timeoutMs;

    public static AgentRunEntity create(
            UUID id,
            String objective,
            String status,
            UUID assuranceCaseId,
            String initiatedBy,
            Instant startedAt,
            int maxSteps,
            int maxAgentCalls,
            int maxRetries,
            long timeoutMs
    ) {
        AgentRunEntity entity = new AgentRunEntity();
        entity.id = id;
        entity.objective = objective;
        entity.status = status;
        entity.assuranceCaseId = assuranceCaseId;
        entity.initiatedBy = initiatedBy;
        entity.startedAt = startedAt;
        entity.maxSteps = maxSteps;
        entity.currentStep = 0;
        entity.maxAgentCalls = maxAgentCalls;
        entity.maxRetries = maxRetries;
        entity.timeoutMs = timeoutMs;
        return entity;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getObjective() {
        return objective;
    }

    public String getStatus() {
        return status;
    }

    public UUID getAssuranceCaseId() {
        return assuranceCaseId;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public int getMaxAgentCalls() {
        return maxAgentCalls;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}
