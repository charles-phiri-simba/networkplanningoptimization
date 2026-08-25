package com.simba.snip.npo.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentRunMemory {

    private AgentOutputs.ContextResult context;
    private AgentOutputs.AssuranceResult assurance;
    private AgentOutputs.KnowledgeResult knowledge;
    private AgentOutputs.DecisionResult decision;
    private final List<UUID> proposedActionIds = new ArrayList<>();

    public AgentOutputs.ContextResult context() {
        return context;
    }

    public void setContext(AgentOutputs.ContextResult context) {
        this.context = context;
    }

    public AgentOutputs.AssuranceResult assurance() {
        return assurance;
    }

    public void setAssurance(AgentOutputs.AssuranceResult assurance) {
        this.assurance = assurance;
    }

    public AgentOutputs.KnowledgeResult knowledge() {
        return knowledge;
    }

    public void setKnowledge(AgentOutputs.KnowledgeResult knowledge) {
        this.knowledge = knowledge;
    }

    public AgentOutputs.DecisionResult decision() {
        return decision;
    }

    public void setDecision(AgentOutputs.DecisionResult decision) {
        this.decision = decision;
    }

    public List<UUID> proposedActionIds() {
        return List.copyOf(proposedActionIds);
    }

    public void addProposedAction(UUID actionId) {
        proposedActionIds.add(actionId);
    }
}
