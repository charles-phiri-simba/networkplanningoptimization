package com.simba.snip.npo.agent;

import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.network.CellContext;
import com.simba.snip.npo.network.NetworkContextService;
import org.springframework.stereotype.Component;

@Component
public class ContextAgent {

    private final AgentRegistry registry;
    private final AgentPermissionGuard permissions;
    private final AgentModelResolver modelResolver;
    private final AgentNarrator narrator;
    private final AgentMetrics metrics;
    private final NetworkContextService networkContextService;
    private final SnipProperties properties;

    public ContextAgent(
            AgentRegistry registry,
            AgentPermissionGuard permissions,
            AgentModelResolver modelResolver,
            AgentNarrator narrator,
            AgentMetrics metrics,
            NetworkContextService networkContextService,
            SnipProperties properties
    ) {
        this.registry = registry;
        this.permissions = permissions;
        this.modelResolver = modelResolver;
        this.narrator = narrator;
        this.metrics = metrics;
        this.networkContextService = networkContextService;
        this.properties = properties;
    }

    public AgentOutputs.ContextResult invoke(String cellId) {
        permissions.assertAllowed(AgentRegistry.CONTEXT, AgentServiceKind.NETWORK_CONTEXT);
        if (AgentRegistry.CONTEXT.equals(properties.getAgentForceFailAgentId())) {
            throw new AgentStepException("forced specialist failure: context-agent");
        }
        CellContext context = networkContextService.resolve(cellId);
        AgentOutputs.ContextResult result = AgentOutputs.ContextResult.from(context);
        AgentDefinition definition = registry.requireEnabled(AgentRegistry.CONTEXT);
        AgentModelProfile profile = modelResolver.resolve(definition);
        narrator.narrate(definition, profile, "cell=" + result.cellId()
                + " kpis=" + result.currentKpis()
                + " trends=" + result.historyTrends()
                + " provenance=" + result.provenance());
        metrics.incrementModelCalls();
        return result;
    }
}
