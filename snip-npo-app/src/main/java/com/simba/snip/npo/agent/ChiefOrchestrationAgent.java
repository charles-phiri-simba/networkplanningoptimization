package com.simba.snip.npo.agent;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChiefOrchestrationAgent {

    private final AgentRegistry registry;
    private final AgentPermissionGuard permissions;
    private final AgentModelResolver modelResolver;
    private final AgentNarrator narrator;
    private final AgentMetrics metrics;

    public ChiefOrchestrationAgent(
            AgentRegistry registry,
            AgentPermissionGuard permissions,
            AgentModelResolver modelResolver,
            AgentNarrator narrator,
            AgentMetrics metrics
    ) {
        this.registry = registry;
        this.permissions = permissions;
        this.modelResolver = modelResolver;
        this.narrator = narrator;
        this.metrics = metrics;
    }

    public List<AgentOutputs.PlannedStep> createCanonicalPlan(String objective, String cellId) {
        permissions.assertAllowed(AgentRegistry.CHIEF, AgentServiceKind.RUN_CONTROL);
        AgentDefinition definition = registry.requireEnabled(AgentRegistry.CHIEF);
        AgentModelProfile profile = modelResolver.resolve(definition);
        narrator.narrate(definition, profile, "Create a structured four-step plan for objective=" + objective
                + " cell=" + cellId + ". Do not execute MCP.");
        metrics.incrementModelCalls();
        return List.of(
                new AgentOutputs.PlannedStep(
                        1,
                        AgentRole.CONTEXT,
                        "Retrieve " + cellId + " structured and temporal context.",
                        "assuranceCaseId,cellId",
                        "Structured cell/site/gNB/KPI/trend/provenance summary"
                ),
                new AgentOutputs.PlannedStep(
                        2,
                        AgentRole.ASSURANCE,
                        "Load the persisted Assurance Case and operational evidence.",
                        "assuranceCaseId",
                        "Case type, severity, confidence, evidence, missing evidence"
                ),
                new AgentOutputs.PlannedStep(
                        3,
                        AgentRole.KNOWLEDGE,
                        "Retrieve engineering guidance relevant to BLER and load.",
                        "cellId,objective",
                        "Grounded summary with citations or insufficientEvidence"
                ),
                new AgentOutputs.PlannedStep(
                        4,
                        AgentRole.DECISION,
                        "Synthesize findings and recommend the next safe Phase 4 action.",
                        "knowledge,context,assurance,objective",
                        "Decision summary and optional candidateAction"
                )
        );
    }
}
