package com.simba.snip.npo.agent;

import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.assurance.AssuranceCaseView;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AssuranceAgent {

    private final AgentRegistry registry;
    private final AgentPermissionGuard permissions;
    private final AgentModelResolver modelResolver;
    private final AgentNarrator narrator;
    private final AgentMetrics metrics;
    private final AssuranceCaseService assuranceCaseService;
    private final SnipProperties properties;

    public AssuranceAgent(
            AgentRegistry registry,
            AgentPermissionGuard permissions,
            AgentModelResolver modelResolver,
            AgentNarrator narrator,
            AgentMetrics metrics,
            AssuranceCaseService assuranceCaseService,
            SnipProperties properties
    ) {
        this.registry = registry;
        this.permissions = permissions;
        this.modelResolver = modelResolver;
        this.narrator = narrator;
        this.metrics = metrics;
        this.assuranceCaseService = assuranceCaseService;
        this.properties = properties;
    }

    public AgentOutputs.AssuranceResult invoke(UUID caseId) {
        permissions.assertAllowed(AgentRegistry.ASSURANCE, AgentServiceKind.ASSURANCE_READ);
        if (AgentRegistry.ASSURANCE.equals(properties.getAgentForceFailAgentId())) {
            throw new AgentStepException("forced specialist failure: assurance-agent");
        }
        AssuranceCaseEntity entity = assuranceCaseService.findById(caseId)
                .orElseThrow(() -> new DomainNotFoundException("assurance case", caseId.toString()));
        AssuranceCaseView view = AssuranceCaseView.from(entity);
        List<String> evidence = view.evidence().stream()
                .map(item -> item.metric() + "=" + item.value() + " trend=" + item.trend())
                .toList();
        List<String> missing = new ArrayList<>();
        if (evidence.isEmpty()) {
            missing.add("No operational evidence rows were persisted on the case.");
        }
        AgentDefinition definition = registry.requireEnabled(AgentRegistry.ASSURANCE);
        AgentModelProfile profile = modelResolver.resolve(definition);
        narrator.narrate(definition, profile, "caseType=" + view.caseType()
                + " severity=" + view.severity()
                + " confidence=" + view.confidence()
                + " status=" + view.status()
                + " evidence=" + evidence
                + ". Do not change severity or confidence.");
        metrics.incrementModelCalls();
        return new AgentOutputs.AssuranceResult(
                view.id(),
                view.caseType(),
                view.severity(),
                view.confidence(),
                view.status(),
                evidence,
                missing
        );
    }
}
