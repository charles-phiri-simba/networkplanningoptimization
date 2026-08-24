package com.simba.snip.npo.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.domain.DomainConflictException;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.mcp.McpCapabilityGateway;
import com.simba.snip.npo.persist.ActionApprovalEntity;
import com.simba.snip.npo.persist.ActionApprovalRepository;
import com.simba.snip.npo.persist.ActionResultEntity;
import com.simba.snip.npo.persist.ActionResultRepository;
import com.simba.snip.npo.persist.ProposedActionEntity;
import com.simba.snip.npo.persist.ProposedActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ActionExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutionService.class);
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final ProposedActionRepository actionRepository;
    private final ActionApprovalRepository approvalRepository;
    private final ActionResultRepository resultRepository;
    private final ActionAuditService auditService;
    private final McpCapabilityGateway gateway;
    private final ActionMetrics metrics;
    private final ObjectMapper objectMapper;

    public ActionExecutionService(
            ProposedActionRepository actionRepository,
            ActionApprovalRepository approvalRepository,
            ActionResultRepository resultRepository,
            ActionAuditService auditService,
            McpCapabilityGateway gateway,
            ActionMetrics metrics,
            ObjectMapper objectMapper
    ) {
        this.actionRepository = actionRepository;
        this.approvalRepository = approvalRepository;
        this.resultRepository = resultRepository;
        this.auditService = auditService;
        this.gateway = gateway;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ProposedActionEntity execute(UUID actionId) {
        ProposedActionEntity action = actionRepository.findById(actionId)
                .orElseThrow(() -> new DomainNotFoundException("action", actionId.toString()));
        ActionApprovalEntity approval = approvalRepository.findByActionId(actionId).orElse(null);
        boolean approved = approval != null && ApprovalDecision.APPROVED.name().equals(approval.getDecision());
        ActionLifecycle.requireExecutable(
                ActionStatus.valueOf(action.getStatus()),
                PolicyOutcome.valueOf(action.getPolicyDecision()),
                approved
        );
        if (ActionStatus.SUCCEEDED.name().equals(action.getStatus())) {
            metrics.incrementIdempotentHits();
            log.info("idempotentExecutionHits=1 actionId={} mcpInvocationsSkipped=true", actionId);
            return action;
        }
        resultRepository.findByActionId(actionId).ifPresent(resultRepository::delete);
        resultRepository.flush();
        Map<String, Object> arguments = arguments(action);
        Instant started = Instant.now();
        action.setStatus(ActionStatus.EXECUTING.name());
        action.setExecutedBy(ActionRules.EXECUTOR);
        actionRepository.save(action);
        auditService.append(actionId, AuditEventType.MCP_INVOCATION_STARTED, ActionRules.EXECUTOR,
                "capabilityId=" + action.getCapabilityId());
        try {
            Map<String, Object> output = gateway.invoke(action, approval, arguments);
            Instant completed = Instant.now();
            resultRepository.save(ActionResultEntity.create(
                    UUID.randomUUID(),
                    actionId,
                    action.getCapabilityId(),
                    ResultStatus.SUCCEEDED.name(),
                    started,
                    completed,
                    writeJson(output),
                    null,
                    true
            ));
            action.setStatus(ActionStatus.SUCCEEDED.name());
            actionRepository.save(action);
            auditService.append(actionId, AuditEventType.MCP_INVOCATION_SUCCEEDED, ActionRules.EXECUTOR,
                    "capabilityId=" + action.getCapabilityId());
            log.info(
                    "mcpInvocations=1 actionId={} assuranceCaseId={} capabilityId={} status=SUCCEEDED executedBy={}",
                    actionId, action.getAssuranceCaseId(), action.getCapabilityId(), ActionRules.EXECUTOR
            );
            return action;
        } catch (RuntimeException ex) {
            metrics.incrementMcpFailures();
            Instant completed = Instant.now();
            resultRepository.save(ActionResultEntity.create(
                    UUID.randomUUID(),
                    actionId,
                    action.getCapabilityId() == null ? "none" : action.getCapabilityId(),
                    ResultStatus.FAILED.name(),
                    started,
                    completed,
                    null,
                    ex.getMessage(),
                    true
            ));
            action.setStatus(ActionStatus.FAILED.name());
            actionRepository.save(action);
            auditService.append(actionId, AuditEventType.MCP_INVOCATION_FAILED, ActionRules.EXECUTOR,
                    ex.getMessage() == null ? "failed" : ex.getMessage());
            log.info("mcpInvocationFailures=1 actionId={} error={}", actionId, ex.getMessage());
            return action;
        }
    }

    private Map<String, Object> arguments(ProposedActionEntity action) {
        Map<String, Object> params;
        try {
            params = objectMapper.readValue(action.getParameters(), MAP);
        } catch (JsonProcessingException ex) {
            params = new LinkedHashMap<>();
        }
        params.put("actionId", action.getId().toString());
        params.put("assuranceCaseId", action.getAssuranceCaseId().toString());
        params.putIfAbsent("cellId", action.getTargetId());
        return params;
    }

    private String writeJson(Map<String, Object> output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
