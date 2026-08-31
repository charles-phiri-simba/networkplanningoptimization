package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeCandidateEntity;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.changeintelligence.policy.TwinCompatibilityChecker;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeCandidateRepository;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
import com.simba.snip.npo.changeintelligence.service.ChangeProposalValidityService;
import com.simba.snip.npo.changeplanning.ChangePlanException;
import com.simba.snip.npo.changeplanning.config.ChangePlanningProperties;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChangePlanEligibilityService {

    private static final List<String> ACTIVE_PLAN_STATUSES = List.of(
            PlanStatus.DRAFT.name(),
            PlanStatus.VALIDATING.name(),
            PlanStatus.PLANNED.name(),
            PlanStatus.SAFETY_EVALUATING.name(),
            PlanStatus.READY_FOR_REVIEW.name(),
            PlanStatus.AUTHORIZED.name(),
            PlanStatus.READY_FOR_EXECUTION.name()
    );

    private final ChangePlanningProperties properties;
    private final NetworkChangeProposalRepository proposalRepository;
    private final NetworkChangeCandidateRepository candidateRepository;
    private final NetworkChangePlanRepository planRepository;
    private final CellRepository cellRepository;
    private final RadioConfigurationRepository radioConfigurationRepository;
    private final SynchronizationPolicyRegistry policyRegistry;
    private final SynchronizationSourceStateService sourceStateService;
    private final NetworkDriftService driftService;
    private final KnowledgeGate knowledgeGate;
    private final TwinCompatibilityChecker twinCompatibilityChecker;
    private final ChangeProposalValidityService proposalValidityService;
    private final Clock clock;

    public ChangePlanEligibilityService(
            ChangePlanningProperties properties,
            NetworkChangeProposalRepository proposalRepository,
            NetworkChangeCandidateRepository candidateRepository,
            NetworkChangePlanRepository planRepository,
            CellRepository cellRepository,
            RadioConfigurationRepository radioConfigurationRepository,
            SynchronizationPolicyRegistry policyRegistry,
            SynchronizationSourceStateService sourceStateService,
            NetworkDriftService driftService,
            KnowledgeGate knowledgeGate,
            TwinCompatibilityChecker twinCompatibilityChecker,
            ChangeProposalValidityService proposalValidityService,
            Clock clock
    ) {
        this.properties = properties;
        this.proposalRepository = proposalRepository;
        this.candidateRepository = candidateRepository;
        this.planRepository = planRepository;
        this.cellRepository = cellRepository;
        this.radioConfigurationRepository = radioConfigurationRepository;
        this.policyRegistry = policyRegistry;
        this.sourceStateService = sourceStateService;
        this.driftService = driftService;
        this.knowledgeGate = knowledgeGate;
        this.twinCompatibilityChecker = twinCompatibilityChecker;
        this.proposalValidityService = proposalValidityService;
        this.clock = clock;
    }

    public record EligibilityResult(
            NetworkChangeProposalEntity proposal,
            NetworkChangeCandidateEntity rankOneCandidate,
            ParameterChangeIntent intent,
            String canonicalCurrentValue,
            NetworkKnowledgeStatusEntity knowledge
    ) {
    }

    public EligibilityResult evaluate(UUID proposalId) {
        if (!properties.isEnabled()) {
            throw new ChangePlanException(ChangePlanFailureCode.CHANGE_PLANNING_DISABLED, "disabled");
        }
        NetworkChangeProposalEntity proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new DomainNotFoundException("changeProposal", proposalId.toString()));
        if (!ProposalStatus.APPROVED.name().equals(proposal.getStatus())) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_PROPOSAL_NOT_APPROVED, proposal.getStatus());
        }
        ChangeProposalValidityService.ValidityResult validity = proposalValidityService.revalidate(proposal);
        if (!validity.valid()) {
            throw new ChangePlanException(
                    ChangePlanFailureCode.PLAN_PROPOSAL_INVALID,
                    validity.reason() == null ? "invalid proposal" : validity.reason()
            );
        }
        if (!SimulatableParameterRegistry.TX_POWER.equals(proposal.getParameterName())) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_PARAMETER_UNSUPPORTED, proposal.getParameterName());
        }
        CellEntity cell = cellRepository.findByCellId(proposal.getTargetEntityId())
                .orElseThrow(() -> new ChangePlanException(ChangePlanFailureCode.PLAN_TARGET_NOT_FOUND, proposal.getTargetEntityId()));
        String canonicalCurrent = radioConfigurationRepository
                .findByCell_IdAndParameterName(cell.getId(), SimulatableParameterRegistry.TX_POWER)
                .map(r -> r.getParameterValue())
                .orElse(null);
        if (canonicalCurrent == null) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH, "missing canonical");
        }
        if (properties.isRequireCurrentValueMatch()
                && new BigDecimal(canonicalCurrent).compareTo(new BigDecimal(proposal.getCurrentValue())) != 0) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH, canonicalCurrent);
        }
        Instant now = clock.instant();
        SynchronizationPolicy policy = resolvePolicy(proposal);
        NetworkKnowledgeStatusEntity knowledge = sourceStateService.requireKnowledge(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        NetworkKnowledgeConfidence confidence = NetworkKnowledgeConfidence.valueOf(knowledge.getConfidence());
        if (properties.isRequireHighOrMediumKnowledge() && knowledgeGate.blocksApproval(confidence)) {
            ChangePlanFailureCode code = confidence == NetworkKnowledgeConfidence.UNKNOWN
                    ? ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_UNKNOWN
                    : ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_LOW;
            throw new ChangePlanException(code, confidence.name());
        }
        if (hasRelevantDrift(proposal, policy)) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_RELEVANT_DRIFT_PRESENT, proposal.getTargetEntityId());
        }
        NetworkChangeCandidateEntity rankOne = resolveRankOneCandidate(proposal);
        if (proposal.getProposedValue() == null
                || !proposal.getProposedValue().equals(rankOne.getCandidateValue())) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_CANDIDATE_VALUE_MISMATCH, rankOne.getCandidateValue());
        }
        if (rankOne.getSimulationRunId() == null) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_TWIN_STALE, "simulation evidence missing");
        }
        TwinCompatibilityChecker.CompatibilityResult twin = twinCompatibilityChecker.check(
                proposal.getTargetEntityId(),
                new BigDecimal(proposal.getCurrentValue())
        );
        if (!twin.compatible()) {
            throw new ChangePlanException(
                    ChangePlanFailureCode.PLAN_TWIN_STALE,
                    twin.reason() == null ? "twin incompatible" : twin.reason()
            );
        }
        planRepository.findFirstByProposalIdAndStatusInOrderByCreatedAtDesc(proposalId, ACTIVE_PLAN_STATUSES)
                .ifPresent(existing -> {
                    throw new ChangePlanException(ChangePlanFailureCode.ACTIVE_PLAN_EXISTS, existing.getId().toString());
                });
        ParameterChangeIntent intent = new ParameterChangeIntent(
                proposal.getTargetEntityType(),
                proposal.getTargetEntityId(),
                proposal.getParameterName(),
                proposal.getCurrentValue(),
                proposal.getProposedValue()
        );
        return new EligibilityResult(proposal, rankOne, intent, canonicalCurrent, knowledge);
    }

    private NetworkChangeCandidateEntity resolveRankOneCandidate(NetworkChangeProposalEntity proposal) {
        List<NetworkChangeCandidateEntity> rankOnes = candidateRepository
                .findByProposal_IdOrderByRankOrderAscCandidateValueAsc(proposal.getId())
                .stream()
                .filter(c -> c.getRankOrder() != null && c.getRankOrder() == 1)
                .toList();
        if (rankOnes.isEmpty()) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_CANDIDATE_NOT_FOUND, proposal.getId().toString());
        }
        if (rankOnes.size() > 1) {
            throw new ChangePlanException(ChangePlanFailureCode.PLAN_CANDIDATE_AMBIGUOUS, proposal.getId().toString());
        }
        return rankOnes.get(0);
    }

    private boolean hasRelevantDrift(NetworkChangeProposalEntity proposal, SynchronizationPolicy policy) {
        List<NetworkDriftObservationEntity> drifts = driftService.list(proposal.getSourceSystem(), policy.sourceScope());
        return drifts.stream()
                .anyMatch(d -> "OPEN".equals(d.getDriftStatus())
                        && proposal.getTargetEntityId().equals(d.getEntityId()));
    }

    private SynchronizationPolicy resolvePolicy(NetworkChangeProposalEntity proposal) {
        return policyRegistry.policies().stream()
                .filter(p -> p.sourceSystem().equals(proposal.getSourceSystem()))
                .findFirst()
                .orElse(policyRegistry.policies().get(0));
    }
}
