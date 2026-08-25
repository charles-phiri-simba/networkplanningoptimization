package com.simba.snip.npo.agent;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AgentRegistry {

    public static final String CHIEF = "chief-orchestrator";
    public static final String KNOWLEDGE = "knowledge-agent";
    public static final String CONTEXT = "context-agent";
    public static final String ASSURANCE = "assurance-agent";
    public static final String DECISION = "decision-agent";
    public static final String SHARED_PROFILE = "shared-llm";

    private final Map<String, AgentDefinition> agents;

    public AgentRegistry() {
        Map<String, AgentDefinition> registered = new LinkedHashMap<>();
        registered.put(CHIEF, new AgentDefinition(
                CHIEF,
                AgentRole.CHIEF_ORCHESTRATOR,
                "Plan, delegate, enforce limits, and stop the run.",
                true,
                Set.of(AgentServiceKind.RUN_CONTROL),
                SHARED_PROFILE,
                0.1,
                256,
                8000L,
                4
        ));
        registered.put(KNOWLEDGE, new AgentDefinition(
                KNOWLEDGE,
                AgentRole.KNOWLEDGE,
                "Retrieve and summarise authoritative engineering knowledge.",
                true,
                Set.of(AgentServiceKind.KNOWLEDGE_RAG),
                SHARED_PROFILE,
                0.2,
                512,
                8000L,
                2
        ));
        registered.put(CONTEXT, new AgentDefinition(
                CONTEXT,
                AgentRole.CONTEXT,
                "Read structured cell/site/gNB state, KPIs, trends, and provenance.",
                true,
                Set.of(AgentServiceKind.NETWORK_CONTEXT),
                SHARED_PROFILE,
                0.0,
                512,
                8000L,
                2
        ));
        registered.put(ASSURANCE, new AgentDefinition(
                ASSURANCE,
                AgentRole.ASSURANCE,
                "Explain persisted Assurance Case evidence without altering severity.",
                true,
                Set.of(AgentServiceKind.ASSURANCE_READ),
                SHARED_PROFILE,
                0.0,
                512,
                8000L,
                2
        ));
        registered.put(DECISION, new AgentDefinition(
                DECISION,
                AgentRole.DECISION,
                "Synthesize specialist evidence and recommend a bounded Phase 4 action type.",
                true,
                Set.of(AgentServiceKind.DECISION_SYNTHESIS),
                SHARED_PROFILE,
                0.4,
                512,
                8000L,
                2
        ));
        this.agents = Map.copyOf(registered);
    }

    public List<AgentDefinition> list() {
        return List.copyOf(agents.values());
    }

    public Optional<AgentDefinition> find(String agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }

    public AgentDefinition require(String agentId) {
        return find(agentId).orElseThrow(() -> new IllegalArgumentException("unknown agent: " + agentId));
    }

    public AgentDefinition requireEnabled(String agentId) {
        AgentDefinition definition = require(agentId);
        if (!definition.enabled()) {
            throw new IllegalStateException("agent disabled: " + agentId);
        }
        return definition;
    }

    public AgentDefinition requireRole(AgentRole role) {
        return list().stream()
                .filter(item -> item.role() == role)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no agent for role: " + role));
    }
}
