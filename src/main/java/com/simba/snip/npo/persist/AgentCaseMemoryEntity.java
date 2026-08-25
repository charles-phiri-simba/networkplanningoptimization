package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_case_memory")
public class AgentCaseMemoryEntity {

    @Id
    private UUID id;

    @Column(name = "assurance_case_id", nullable = false)
    private UUID assuranceCaseId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(nullable = false, length = 1024)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String findings;

    @Column(name = "proposed_action_ids", nullable = false, columnDefinition = "TEXT")
    private String proposedActionIds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static AgentCaseMemoryEntity create(
            UUID id,
            UUID assuranceCaseId,
            UUID runId,
            String summary,
            String findings,
            String proposedActionIds,
            Instant createdAt
    ) {
        AgentCaseMemoryEntity entity = new AgentCaseMemoryEntity();
        entity.id = id;
        entity.assuranceCaseId = assuranceCaseId;
        entity.runId = runId;
        entity.summary = summary;
        entity.findings = findings;
        entity.proposedActionIds = proposedActionIds;
        entity.createdAt = createdAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssuranceCaseId() {
        return assuranceCaseId;
    }

    public UUID getRunId() {
        return runId;
    }

    public String getSummary() {
        return summary;
    }

    public String getFindings() {
        return findings;
    }

    public String getProposedActionIds() {
        return proposedActionIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
