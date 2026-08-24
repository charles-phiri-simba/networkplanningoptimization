package com.simba.snip.npo.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.domain.DomainRules;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.AssuranceCaseRepository;
import com.simba.snip.npo.persist.PolicyDecisionEntity;
import com.simba.snip.npo.persist.PolicyDecisionRepository;
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
public class ActionProposalService {

    private static final Logger log = LoggerFactory.getLogger(ActionProposalService.class);

    private final AssuranceCaseRepository assuranceCaseRepository;
    private final ProposedActionRepository actionRepository;
    private final PolicyDecisionRepository policyDecisionRepository;
    private final ActionPolicyEvaluator policyEvaluator;
    private final ActionAuditService auditService;
    private final ActionMetrics metrics;
    private final ObjectMapper objectMapper;

    public ActionProposalService(
            AssuranceCaseRepository assuranceCaseRepository,
            ProposedActionRepository actionRepository,
            PolicyDecisionRepository policyDecisionRepository,
            ActionPolicyEvaluator policyEvaluator,
            ActionAuditService auditService,
            ActionMetrics metrics,
            ObjectMapper objectMapper
    ) {
        this.assuranceCaseRepository = assuranceCaseRepository;
        this.actionRepository = actionRepository;
        this.policyDecisionRepository = policyDecisionRepository;
        this.policyEvaluator = policyEvaluator;
        this.auditService = auditService;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ProposedActionEntity propose(
            UUID assuranceCaseId,
            String rawType,
            String requestedCapabilityId,
            String targetType,
            String targetId,
            Map<String, Object> parameters,
            String rationale,
            String proposedBy
    ) {
        assuranceCaseRepository.loadById(assuranceCaseId)
                .orElseThrow(() -> new DomainNotFoundException("assurance case", assuranceCaseId.toString()));
        ActionType actionType = ActionSemantics.requireType(rawType);
        ActionSemantics semantics = ActionSemantics.of(actionType);
        if (requestedCapabilityId != null && !requestedCapabilityId.isBlank()
                && semantics.capabilityId() != null
                && !semantics.capabilityId().equals(requestedCapabilityId)) {
            throw new DomainValidationException("capabilityId is incompatible with actionType");
        }
        if (requestedCapabilityId != null && !requestedCapabilityId.isBlank()
                && semantics.capabilityId() == null) {
            throw new DomainValidationException("APPLY_CELL_PARAMETER_CHANGE has no registered capability");
        }
        String actor = DomainRules.requireDomainId(proposedBy == null || proposedBy.isBlank() ? "demo-user" : proposedBy, "proposedBy");
        String target = DomainRules.requireDomainId(targetId, "targetId");
        if (targetType == null || !ActionRules.ENTITY_CELL.equals(targetType)) {
            throw new DomainValidationException("targetType must be CELL");
        }
        Map<String, Object> params = parameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parameters);
        if (actionType == ActionType.SIMULATE_CELL_PARAMETER_CHANGE) {
            Object dryRun = params.get("dryRun");
            if (dryRun == null) {
                params.put("dryRun", true);
            } else if (!Boolean.TRUE.equals(dryRun) && !"true".equalsIgnoreCase(String.valueOf(dryRun))) {
                throw new DomainValidationException("simulation requires dryRun=true");
            }
            params.putIfAbsent("cellId", target);
        }
        UUID actionId = UUID.randomUUID();
        Instant now = Instant.now();
        ActionPolicyEvaluator.Evaluation evaluation = policyEvaluator.evaluate(actionType);
        ActionStatus status = switch (evaluation.decision()) {
            case ALLOW -> ActionStatus.POLICY_EVALUATED;
            case REQUIRE_APPROVAL -> ActionStatus.APPROVAL_REQUIRED;
            case DENY -> ActionStatus.DENIED;
        };
        ProposedActionEntity action = ProposedActionEntity.create(
                actionId,
                assuranceCaseId,
                actionType.name(),
                semantics.capabilityId(),
                ActionRules.ENTITY_CELL,
                target,
                writeJson(params),
                rationale == null || rationale.isBlank() ? "Proposed from Decision Intelligence." : rationale,
                semantics.riskLevel().name(),
                evaluation.decision().name(),
                status.name(),
                now,
                actor,
                true
        );
        actionRepository.save(action);
        policyDecisionRepository.save(PolicyDecisionEntity.create(
                UUID.randomUUID(),
                actionId,
                evaluation.decision().name(),
                evaluation.policyId(),
                evaluation.reason(),
                now
        ));
        metrics.incrementProposed();
        metrics.recordPolicy(evaluation.decision());
        auditService.append(actionId, AuditEventType.ACTION_PROPOSED, actor, "actionType=" + actionType);
        auditService.append(actionId, AuditEventType.POLICY_EVALUATED, actor,
                "decision=" + evaluation.decision() + " policyId=" + evaluation.policyId());
        if (evaluation.decision() == PolicyOutcome.REQUIRE_APPROVAL) {
            auditService.append(actionId, AuditEventType.APPROVAL_REQUESTED, actor, "human approval required");
        }
        if (evaluation.decision() == PolicyOutcome.DENY) {
            auditService.append(actionId, AuditEventType.ACTION_DENIED, actor, evaluation.reason());
        }
        log.info(
                "actionsProposed=1 actionId={} assuranceCaseId={} actionType={} riskLevel={} policyDecision={} status={}",
                actionId, assuranceCaseId, actionType, semantics.riskLevel(), evaluation.decision(), status
        );
        return action;
    }

    private String writeJson(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException ex) {
            throw new DomainValidationException("parameters must be JSON-serialisable");
        }
    }
}
