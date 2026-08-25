package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_run_audit_event")
public class AgentRunAuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "agent_id", length = 64)
    private String agentId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 512)
    private String summary;

    public static AgentRunAuditEventEntity create(
            UUID id,
            UUID runId,
            String eventType,
            String agentId,
            Instant occurredAt,
            String summary
    ) {
        AgentRunAuditEventEntity entity = new AgentRunAuditEventEntity();
        entity.id = id;
        entity.runId = runId;
        entity.eventType = eventType;
        entity.agentId = agentId;
        entity.occurredAt = occurredAt;
        entity.summary = summary;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRunId() {
        return runId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAgentId() {
        return agentId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getSummary() {
        return summary;
    }
}
