package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
import com.simba.snip.npo.changeintelligence.service.ChangeProposalValidityService;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.integration.sync.NetworkDriftService;
import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import com.simba.snip.npo.integration.sync.SynchronizationPolicy;
import com.simba.snip.npo.integration.sync.SynchronizationPolicyRegistry;
import com.simba.snip.npo.integration.sync.SynchronizationSourceStateService;
import com.simba.snip.npo.persist.CellEntity;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.NetworkDriftObservationEntity;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusEntity;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChangePlanValidityService {

    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangeProposalRepository proposalRepository;
    private final CellRepository cellRepository;
    private final RadioConfigurationRepository radioConfigurationRepository;
    private final SynchronizationPolicyRegistry policyRegistry;
    private final SynchronizationSourceStateService sourceStateService;
    private final NetworkDriftService driftService;
    private final KnowledgeGate knowledgeGate;
    private final ChangeProposalValidityService proposalValidityService;
    private final ChangePlanInvalidationPersistenceService invalidationPersistenceService;
    private final ChangePlanAuditService auditService;
    private final Clock clock;

    public ChangePlanValidityService(
            NetworkChangePlanRepository planRepository,
            NetworkChangeProposalRepository proposalRepository,
            CellRepository cellRepository,
            RadioConfigurationRepository radioConfigurationRepository,
            SynchronizationPolicyRegistry policyRegistry,
            SynchronizationSourceStateService sourceStateService,
            NetworkDriftService driftService,
            KnowledgeGate knowledgeGate,
            ChangeProposalValidityService proposalValidityService,
            ChangePlanInvalidationPersistenceService invalidationPersistenceService,
            ChangePlanAuditService auditService,
            Clock clock
    ) {
        this.planRepository = planRepository;
        this.proposalRepository = proposalRepository;
        this.cellRepository = cellRepository;
        this.radioConfigurationRepository = radioConfigurationRepository;
        this.policyRegistry = policyRegistry;
        this.sourceStateService = sourceStateService;
        this.driftService = driftService;
        this.knowledgeGate = knowledgeGate;
        this.proposalValidityService = proposalValidityService;
        this.invalidationPersistenceService = invalidationPersistenceService;
        this.auditService = auditService;
        this.clock = clock;
    }

    public record ValidityResult(boolean valid, ChangePlanFailureCode failureCode, String reason) {
        public static ValidityResult ok() {
            return new ValidityResult(true, null, null);
        }

        public static ValidityResult invalid(ChangePlanFailureCode code, String reason) {
            return new ValidityResult(false, code, reason);
        }
    }

    @Transactional
    public ValidityResult revalidate(NetworkChangePlanEntity plan) {
        Instant now = clock.instant();
        if (PlanStatus.EXPIRED.name().equals(plan.getStatus())
                || PlanStatus.INVALIDATED.name().equals(plan.getStatus())
                || PlanStatus.SUPERSEDED.name().equals(plan.getStatus())
                || PlanStatus.CANCELLED.name().equals(plan.getStatus())) {
            return ValidityResult.invalid(
                    ChangePlanFailureCode.INVALID_PLAN_STATE,
                    plan.getStatus()
            );
        }
        if (plan.getExpiresAt() != null && now.isAfter(plan.getExpiresAt())) {
            plan.markExpired(now);
            planRepository.save(plan);
            auditService.append(plan.getId(), "PLAN_EXPIRED", "system", "expired");
            return ValidityResult.invalid(ChangePlanFailureCode.PLAN_EXPIRED, "expired");
        }
        NetworkChangeProposalEntity proposal = proposalRepository.findById(plan.getProposalId()).orElse(null);
        if (proposal == null || !ProposalStatus.APPROVED.name().equals(proposal.getStatus())) {
            invalidationPersistenceService.persistInvalidation(
                    plan.getId(), ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name(), now, "proposal invalid");
            return ValidityResult.invalid(ChangePlanFailureCode.PLAN_PROPOSAL_INVALID, "proposal invalid");
        }
        CellEntity cell = cellRepository.findByCellId(plan.getTargetEntityId()).orElse(null);
        if (cell == null) {
            invalidationPersistenceService.persistInvalidation(
                    plan.getId(), ChangePlanFailureCode.PLAN_TARGET_NOT_FOUND.name(), now, "target missing");
            return ValidityResult.invalid(ChangePlanFailureCode.PLAN_TARGET_NOT_FOUND, "target missing");
        }
        String canonical = radioConfigurationRepository
                .findByCell_IdAndParameterName(cell.getId(), SimulatableParameterRegistry.TX_POWER)
                .map(r -> r.getParameterValue())
                .orElse(null);
        if (canonical == null
                || new BigDecimal(canonical).compareTo(new BigDecimal(plan.getExpectedCurrentValue())) != 0) {
            invalidationPersistenceService.persistInvalidation(
                    plan.getId(), ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH.name(), now, "current mismatch");
            return ValidityResult.invalid(ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH, canonical);
        }
        SynchronizationPolicy policy = resolvePolicy(plan);
        NetworkKnowledgeStatusEntity knowledge = sourceStateService.requireKnowledge(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        NetworkKnowledgeConfidence confidence = NetworkKnowledgeConfidence.valueOf(knowledge.getConfidence());
        if (knowledgeGate.blocksApproval(confidence)) {
            ChangePlanFailureCode code = confidence == NetworkKnowledgeConfidence.UNKNOWN
                    ? ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_UNKNOWN
                    : ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_LOW;
            invalidationPersistenceService.persistInvalidation(plan.getId(), code.name(), now, confidence.name());
            return ValidityResult.invalid(code, confidence.name());
        }
        if (hasRelevantDrift(plan, policy)) {
            invalidationPersistenceService.persistInvalidation(
                    plan.getId(), ChangePlanFailureCode.PLAN_RELEVANT_DRIFT_PRESENT.name(), now, "drift");
            return ValidityResult.invalid(ChangePlanFailureCode.PLAN_RELEVANT_DRIFT_PRESENT, "drift");
        }
        ChangeProposalValidityService.ValidityResult proposalValidity = proposalValidityService.revalidate(proposal);
        if (!proposalValidity.valid()) {
            invalidationPersistenceService.persistInvalidation(
                    plan.getId(), ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name(), now, proposalValidity.reason());
            return ValidityResult.invalid(ChangePlanFailureCode.PLAN_PROPOSAL_INVALID, proposalValidity.reason());
        }
        return ValidityResult.ok();
    }

    private boolean hasRelevantDrift(NetworkChangePlanEntity plan, SynchronizationPolicy policy) {
        List<NetworkDriftObservationEntity> drifts = driftService.list(plan.getSourceSystem(), policy.sourceScope());
        return drifts.stream()
                .anyMatch(d -> "OPEN".equals(d.getDriftStatus())
                        && plan.getTargetEntityId().equals(d.getEntityId()));
    }

    private SynchronizationPolicy resolvePolicy(NetworkChangePlanEntity plan) {
        return policyRegistry.policies().stream()
                .filter(p -> p.sourceSystem().equals(plan.getSourceSystem()))
                .findFirst()
                .orElse(policyRegistry.policies().get(0));
    }
}
