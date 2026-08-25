package com.simba.snip.npo.api;

import com.simba.snip.npo.agent.AgentOutputs;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentRunDetailDto(
        UUID id,
        String objective,
        String status,
        UUID assuranceCaseId,
        String initiatedBy,
        Instant startedAt,
        Instant completedAt,
        int maxSteps,
        int currentStep,
        int maxAgentCalls,
        int maxRetries,
        long timeoutMs,
        AgentPlanDto plan,
        AgentCaseMemoryDto caseMemory,
        List<AgentRunAuditEventDto> auditEvents,
        AgentOutputs.ContextResult context,
        AgentOutputs.AssuranceResult assurance,
        AgentOutputs.KnowledgeResult knowledge,
        AgentOutputs.DecisionResult decision,
        List<UUID> proposedActionIds
) {
}
