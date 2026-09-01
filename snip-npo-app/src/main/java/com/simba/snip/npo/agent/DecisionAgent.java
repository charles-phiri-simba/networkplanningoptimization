package com.simba.snip.npo.agent;

import com.simba.snip.npo.action.ActionSemantics;
import com.simba.snip.npo.action.ActionType;
import com.simba.snip.npo.config.SnipProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DecisionAgent {

    private final AgentRegistry registry;
    private final AgentPermissionGuard permissions;
    private final AgentModelResolver modelResolver;
    private final AgentNarrator narrator;
    private final AgentMetrics metrics;
    private final SnipProperties properties;

    public DecisionAgent(
            AgentRegistry registry,
            AgentPermissionGuard permissions,
            AgentModelResolver modelResolver,
            AgentNarrator narrator,
            AgentMetrics metrics,
            SnipProperties properties
    ) {
        this.registry = registry;
        this.permissions = permissions;
        this.modelResolver = modelResolver;
        this.narrator = narrator;
        this.metrics = metrics;
        this.properties = properties;
    }

    public AgentOutputs.DecisionResult invoke(
            String objective,
            String cellId,
            AgentOutputs.ContextResult context,
            AgentOutputs.AssuranceResult assurance,
            AgentOutputs.KnowledgeResult knowledge
    ) {
        permissions.assertAllowed(AgentRegistry.DECISION, AgentServiceKind.DECISION_SYNTHESIS);
        if (AgentRegistry.DECISION.equals(properties.getAgentForceFailAgentId())) {
            throw new AgentStepException("forced specialist failure: decision-agent");
        }
        List<String> missing = new ArrayList<>();
        if (context == null) {
            missing.add("Context Agent output is absent.");
        }
        if (assurance == null) {
            missing.add("Assurance Agent output is absent.");
        }
        if (knowledge == null || knowledge.insufficientEvidence()) {
            missing.add("Knowledge evidence is insufficient or absent.");
        }
        ActionType type = candidateType(objective);
        boolean humanReview = type != ActionType.GENERATE_REMEDIATION_PLAN;
        AgentOutputs.CandidateAction candidate = null;
        if (context != null && assurance != null && type != null) {
            ActionSemantics semantics = ActionSemantics.of(type);
            candidate = new AgentOutputs.CandidateAction(
                    type,
                    semantics.capabilityId(),
                    "CELL",
                    cellId,
                    "Agent-synthesized candidate from specialist evidence. Policy is not assigned by the Agent."
            );
        }
        List<String> contributors = new ArrayList<>();
        if (assurance != null) {
            contributors.add(assurance.caseType());
        }
        if (context != null) {
            contributors.addAll(context.historyTrends());
        }
        List<String> checks = List.of(
                "Review BLER_DL trend against the persisted Assurance Case.",
                "Do not apply live cell-parameter changes from this run."
        );
        AgentDefinition definition = registry.requireEnabled(AgentRegistry.DECISION);
        AgentModelProfile profile = modelResolver.resolve(definition);
        String summary = narrator.narrate(definition, profile, "objective=" + objective
                + " candidate=" + (candidate == null ? "none" : candidate.actionType())
                + " missing=" + missing
                + " Distinguish evidence from inference. Do not set risk or policy.");
        metrics.incrementModelCalls();
        return new AgentOutputs.DecisionResult(summary, contributors, checks, missing, candidate, humanReview);
    }

    static ActionType candidateType(String objective) {
        String text = objective == null ? "" : objective.toUpperCase();
        if (text.contains(ActionType.APPLY_CELL_PARAMETER_CHANGE.name())) {
            return ActionType.APPLY_CELL_PARAMETER_CHANGE;
        }
        if (text.contains(ActionType.SIMULATE_CELL_PARAMETER_CHANGE.name())) {
            return ActionType.SIMULATE_CELL_PARAMETER_CHANGE;
        }
        return ActionType.GENERATE_REMEDIATION_PLAN;
    }
}
