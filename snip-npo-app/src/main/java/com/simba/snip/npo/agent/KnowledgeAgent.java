package com.simba.snip.npo.agent;

import com.simba.snip.npo.api.CitationDto;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.retrieve.ChunkRetriever;
import com.simba.snip.npo.retrieve.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class KnowledgeAgent {

    private final AgentRegistry registry;
    private final AgentPermissionGuard permissions;
    private final AgentModelResolver modelResolver;
    private final AgentNarrator narrator;
    private final AgentMetrics metrics;
    private final ChunkRetriever retriever;
    private final SnipProperties properties;

    public KnowledgeAgent(
            AgentRegistry registry,
            AgentPermissionGuard permissions,
            AgentModelResolver modelResolver,
            AgentNarrator narrator,
            AgentMetrics metrics,
            ChunkRetriever retriever,
            SnipProperties properties
    ) {
        this.registry = registry;
        this.permissions = permissions;
        this.modelResolver = modelResolver;
        this.narrator = narrator;
        this.metrics = metrics;
        this.retriever = retriever;
        this.properties = properties;
    }

    public AgentOutputs.KnowledgeResult invoke(UUID runId, String cellId, String objective) {
        permissions.assertAllowed(AgentRegistry.KNOWLEDGE, AgentServiceKind.KNOWLEDGE_RAG);
        failIfForced();
        AgentDefinition definition = registry.requireEnabled(AgentRegistry.KNOWLEDGE);
        String query = "BLER load radio quality " + (cellId == null ? "" : cellId) + " " + objective;
        List<RetrievedChunk> retrieved = retriever.retrieve(query, properties.getRetrieveTopK());
        boolean insufficient = retrieved.isEmpty();
        List<CitationDto> citations = retrieved.stream()
                .map(hit -> new CitationDto(
                        hit.chunk().sourceId(),
                        hit.chunk().locator(),
                        hit.chunk().snippet(),
                        hit.chunk().id(),
                        hit.score()
                ))
                .toList();
        List<String> sources = citations.stream().map(CitationDto::sourceId).distinct().toList();
        String payload = insufficient
                ? "No grounded source retrieved. Do not invent citations. runId=" + runId
                : "Sources=" + sources + " snippets=" + citations.stream().map(CitationDto::snippet).toList();
        AgentModelProfile profile = modelResolver.resolve(definition);
        String summary = narrator.narrate(definition, profile, payload);
        metrics.incrementModelCalls();
        if (insufficient) {
            summary = "Insufficient engineering evidence was retrieved. " + summary;
        }
        return new AgentOutputs.KnowledgeResult(summary, citations, sources, insufficient);
    }

    private void failIfForced() {
        if (AgentRegistry.KNOWLEDGE.equals(properties.getAgentForceFailAgentId())) {
            throw new AgentStepException("forced specialist failure: knowledge-agent");
        }
    }
}
