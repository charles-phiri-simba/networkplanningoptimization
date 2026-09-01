package com.simba.snip.npo.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.api.AgentCaseMemoryDto;
import com.simba.snip.npo.api.AgentPlanDto;
import com.simba.snip.npo.api.AgentPlanStepDto;
import com.simba.snip.npo.api.AgentRunAuditEventDto;
import com.simba.snip.npo.api.AgentRunDetailDto;
import com.simba.snip.npo.persist.AgentCaseMemoryEntity;
import com.simba.snip.npo.persist.AgentPlanEntity;
import com.simba.snip.npo.persist.AgentPlanStepEntity;
import com.simba.snip.npo.persist.AgentRunAuditEventEntity;
import com.simba.snip.npo.persist.AgentRunEntity;

import java.util.List;
import java.util.UUID;

public final class AgentMapper {

    private static final TypeReference<List<UUID>> UUIDS = new TypeReference<>() {
    };

    private AgentMapper() {
    }

    public static AgentRunDetailDto toDto(
            AgentRunEntity run,
            AgentPlanEntity plan,
            List<AgentPlanStepEntity> steps,
            AgentCaseMemoryEntity memory,
            List<AgentRunAuditEventEntity> audit,
            ObjectMapper objectMapper
    ) {
        AgentPlanDto planDto = plan == null ? null : new AgentPlanDto(
                plan.getId(),
                plan.getRunId(),
                plan.getObjective(),
                steps.stream().map(step -> new AgentPlanStepDto(
                        step.getId(),
                        step.getStepNumber(),
                        step.getAgentRole(),
                        step.getTask(),
                        step.getRequiredInputs(),
                        step.getExpectedOutput(),
                        step.getStatus(),
                        step.getOutputSummary()
                )).toList()
        );
        AgentCaseMemoryDto memoryDto = memory == null ? null : new AgentCaseMemoryDto(
                memory.getId(),
                memory.getAssuranceCaseId(),
                memory.getRunId(),
                memory.getSummary(),
                memory.getFindings(),
                readIds(memory.getProposedActionIds(), objectMapper),
                memory.getCreatedAt()
        );
        return new AgentRunDetailDto(
                run.getId(),
                run.getObjective(),
                run.getStatus(),
                run.getAssuranceCaseId(),
                run.getInitiatedBy(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getMaxSteps(),
                run.getCurrentStep(),
                run.getMaxAgentCalls(),
                run.getMaxRetries(),
                run.getTimeoutMs(),
                planDto,
                memoryDto,
                audit.stream().map(item -> new AgentRunAuditEventDto(
                        item.getId(),
                        item.getEventType(),
                        item.getAgentId(),
                        item.getOccurredAt(),
                        item.getSummary()
                )).toList(),
                null,
                null,
                null,
                null,
                memoryDto == null ? List.of() : memoryDto.proposedActionIds()
        );
    }

    private static List<UUID> readIds(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, UUIDS);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
