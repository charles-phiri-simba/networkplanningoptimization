package com.simba.snip.npo.changeintelligence.service;

import com.simba.snip.npo.changeintelligence.ChangeProposalException;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
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

@Service
public class ChangeProposalValidityService {

    private final NetworkChangeProposalRepository proposalRepository;
    private final CellRepository cellRepository;
    private final RadioConfigurationRepository radioConfigurationRepository;
    private final SynchronizationPolicyRegistry policyRegistry;
    private final SynchronizationSourceStateService sourceStateService;
    private final NetworkDriftService driftService;
    private final KnowledgeGate knowledgeGate;
    private final ChangeProposalInvalidationPersistenceService invalidationPersistenceService;
    private final ChangeProposalAuditService auditService;
    private final ChangeProposalMetrics metrics;
    private final Clock clock;

    public ChangeProposalValidityService(
            NetworkChangeProposalRepository proposalRepository,
            CellRepository cellRepository,
            RadioConfigurationRepository radioConfigurationRepository,
            SynchronizationPolicyRegistry policyRegistry,
            SynchronizationSourceStateService sourceStateService,
            NetworkDriftService driftService,
            KnowledgeGate knowledgeGate,
            ChangeProposalInvalidationPersistenceService invalidationPersistenceService,
            ChangeProposalAuditService auditService,
            ChangeProposalMetrics metrics,
            Clock clock
    ) {
        this.proposalRepository = proposalRepository;
        this.cellRepository = cellRepository;
        this.radioConfigurationRepository = radioConfigurationRepository;
        this.policyRegistry = policyRegistry;
        this.sourceStateService = sourceStateService;
        this.driftService = driftService;
        this.knowledgeGate = knowledgeGate;
        this.invalidationPersistenceService = invalidationPersistenceService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    public record ValidityResult(boolean valid, ChangeProposalFailureCode failureCode, String reason) {
        public static ValidityResult ok() {
            return new ValidityResult(true, null, null);
        }

        public static ValidityResult invalid(ChangeProposalFailureCode code, String reason) {
            return new ValidityResult(false, code, reason);
        }
    }

    @Transactional
    public ValidityResult revalidate(NetworkChangeProposalEntity proposal) {
        Instant now = clock.instant();
        if (ProposalStatus.EXPIRED.name().equals(proposal.getStatus())
                || ProposalStatus.INVALIDATED.name().equals(proposal.getStatus())
                || ProposalStatus.SUPERSEDED.name().equals(proposal.getStatus())) {
            return ValidityResult.invalid(
                    ChangeProposalFailureCode.valueOf(proposal.getInvalidationReason() != null
                            ? proposal.getInvalidationReason()
                            : ChangeProposalFailureCode.INVALID_PROPOSAL_STATE.name()),
                    proposal.getStatus()
            );
        }
        if (proposal.getExpiresAt() != null && now.isAfter(proposal.getExpiresAt())) {
            proposal.markExpired(now);
            proposalRepository.save(proposal);
            auditService.append(proposal.getId(), "PROPOSAL_EXPIRED", "system", "expired");
            metrics.incrementExpirations();
            return ValidityResult.invalid(ChangeProposalFailureCode.PROPOSAL_EXPIRED, "expired");
        }

        CellEntity cell = cellRepository.findByCellId(proposal.getTargetEntityId())
                .orElseThrow(() -> new ChangeProposalException(
                        ChangeProposalFailureCode.CURRENT_STATE_UNAVAILABLE, "cell missing"));
        BigDecimal current = radioConfigurationRepository
                .findByCell_IdAndParameterName(cell.getId(), SimulatableParameterRegistry.TX_POWER)
                .map(r -> new BigDecimal(r.getParameterValue()))
                .orElse(null);
        if (current == null) {
            return ValidityResult.invalid(ChangeProposalFailureCode.CURRENT_STATE_UNAVAILABLE, "txPower missing");
        }
        if (current.compareTo(new BigDecimal(proposal.getCurrentValue())) != 0) {
            invalidationPersistenceService.persistInvalidation(
                    proposal.getId(),
                    ChangeProposalFailureCode.CURRENT_VALUE_CHANGED.name(),
                    now,
                    "current value changed",
                    null,
                    null
            );
            return ValidityResult.invalid(ChangeProposalFailureCode.CURRENT_VALUE_CHANGED, "canonical changed");
        }

        SynchronizationPolicy policy = policyRegistry.policies().stream()
                .filter(p -> p.sourceSystem().equals(proposal.getSourceSystem()))
                .findFirst()
                .orElse(policyRegistry.policies().get(0));
        NetworkKnowledgeStatusEntity knowledge = sourceStateService.requireKnowledge(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        NetworkKnowledgeConfidence confidence = NetworkKnowledgeConfidence.valueOf(knowledge.getConfidence());
        if (knowledgeGate.blocksApproval(confidence)) {
            invalidationPersistenceService.persistInvalidation(
                    proposal.getId(),
                    ChangeProposalFailureCode.KNOWLEDGE_CONFIDENCE_DEGRADED.name(),
                    now,
                    confidence.name(),
                    knowledge.getConfidence(),
                    knowledge.getReasonCodes()
            );
            return ValidityResult.invalid(ChangeProposalFailureCode.KNOWLEDGE_CONFIDENCE_DEGRADED, confidence.name());
        }

        List<NetworkDriftObservationEntity> drifts = driftService.list(
                proposal.getSourceSystem(), policy.sourceScope());
        boolean relevantDrift = drifts.stream()
                .anyMatch(d -> "OPEN".equals(d.getDriftStatus())
                        && proposal.getTargetEntityId().equals(d.getEntityId()));
        if (relevantDrift) {
            invalidationPersistenceService.persistInvalidation(
                    proposal.getId(),
                    ChangeProposalFailureCode.PROPOSAL_INVALIDATED.name(),
                    now,
                    "drift detected",
                    null,
                    null
            );
            return ValidityResult.invalid(ChangeProposalFailureCode.PROPOSAL_INVALIDATED, "drift");
        }

        return ValidityResult.ok();
    }
}
