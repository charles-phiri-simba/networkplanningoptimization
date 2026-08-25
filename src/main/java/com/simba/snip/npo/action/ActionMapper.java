package com.simba.snip.npo.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.api.ActionApprovalDto;
import com.simba.snip.npo.api.ActionAuditEventDto;
import com.simba.snip.npo.api.ActionDetailDto;
import com.simba.snip.npo.api.ActionPolicyDto;
import com.simba.snip.npo.api.ActionResultDto;
import com.simba.snip.npo.persist.ActionApprovalEntity;
import com.simba.snip.npo.persist.ActionAuditEventEntity;
import com.simba.snip.npo.persist.ActionResultEntity;
import com.simba.snip.npo.persist.PolicyDecisionEntity;
import com.simba.snip.npo.persist.ProposedActionEntity;

import java.util.List;
import java.util.Map;

public final class ActionMapper {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private ActionMapper() {
    }

    public static ActionDetailDto toDto(
            ProposedActionEntity action,
            PolicyDecisionEntity policy,
            ActionApprovalEntity approval,
            ActionResultEntity result,
            List<ActionAuditEventEntity> audit,
            ObjectMapper objectMapper
    ) {
        return new ActionDetailDto(
                action.getId(),
                action.getAssuranceCaseId(),
                action.getActionType(),
                action.getCapabilityId(),
                action.getTargetType(),
                action.getTargetId(),
                readMap(action.getParameters(), objectMapper),
                action.getRationale(),
                action.getRiskLevel(),
                action.getPolicyDecision(),
                action.getStatus(),
                action.getProposedAt(),
                action.getProposedBy(),
                action.getExecutedBy(),
                action.isSynthetic(),
                action.getAgentRunId(),
                action.getAgentId(),
                policy == null ? null : new ActionPolicyDto(
                        policy.getId(),
                        policy.getDecision(),
                        policy.getPolicyId(),
                        policy.getReason(),
                        policy.getEvaluatedAt()
                ),
                approval == null ? null : new ActionApprovalDto(
                        approval.getId(),
                        approval.getDecision(),
                        approval.getDecidedBy(),
                        approval.getDecidedAt(),
                        approval.getComment()
                ),
                result == null ? null : new ActionResultDto(
                        result.getId(),
                        result.getCapabilityId(),
                        result.getStatus(),
                        result.getStartedAt(),
                        result.getCompletedAt(),
                        result.getOutput(),
                        result.getError(),
                        result.isSynthetic()
                ),
                audit.stream().map(item -> new ActionAuditEventDto(
                        item.getId(),
                        item.getEventType(),
                        item.getActor(),
                        item.getOccurredAt(),
                        item.getDetails()
                )).toList()
        );
    }

    private static Map<String, Object> readMap(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, MAP);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }
}
