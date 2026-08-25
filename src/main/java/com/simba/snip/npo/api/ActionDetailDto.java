package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ActionDetailDto(
        UUID id,
        UUID assuranceCaseId,
        String actionType,
        String capabilityId,
        String targetType,
        String targetId,
        Map<String, Object> parameters,
        String rationale,
        String riskLevel,
        String policyDecision,
        String status,
        Instant proposedAt,
        String proposedBy,
        String executedBy,
        boolean synthetic,
        UUID agentRunId,
        String agentId,
        ActionPolicyDto policy,
        ActionApprovalDto approval,
        ActionResultDto result,
        List<ActionAuditEventDto> auditEvents
) {
}
