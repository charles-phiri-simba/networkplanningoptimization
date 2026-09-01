package com.simba.snip.npo.changeintelligence.service;

import com.simba.snip.npo.api.ScenarioChangeRequest;
import com.simba.snip.npo.changeintelligence.ChangeProposalException;
import com.simba.snip.npo.changeintelligence.config.ChangeIntelligenceProperties;
import com.simba.snip.npo.changeintelligence.model.CandidateValidationOutcome;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.changeintelligence.model.GenerationInitiator;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.model.ProposalType;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeCandidateEntity;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.policy.ChangeProposalBenefitAssessor;
import com.simba.snip.npo.changeintelligence.policy.ChangeProposalConstraintValidator;
import com.simba.snip.npo.changeintelligence.policy.ChangeProposalRanker;
import com.simba.snip.npo.changeintelligence.policy.ChangeProposalScorer;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.changeintelligence.policy.RiskAssessor;
import com.simba.snip.npo.changeintelligence.policy.TwinCompatibilityChecker;
import com.simba.snip.npo.changeintelligence.policy.TxPowerCandidateGenerator;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeCandidateRepository;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import com.simba.snip.npo.integration.sync.SynchronizationCheckpointService;
import com.simba.snip.npo.integration.sync.SynchronizationPolicy;
import com.simba.snip.npo.integration.sync.SynchronizationPolicyRegistry;
import com.simba.snip.npo.integration.sync.SynchronizationSourceStateService;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.AssuranceCaseRepository;
import com.simba.snip.npo.persist.CellEntity;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusEntity;
import com.simba.snip.npo.persist.NetworkTwinEntity;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.persist.SimulationScenarioEntity;
import com.simba.snip.npo.twin.DigitalTwinSimulationService;
import com.simba.snip.npo.twin.SimulatableParameterDefinition;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import com.simba.snip.npo.twin.SimulationConfidence;
import com.simba.snip.npo.twin.TwinScenarioService;
import com.simba.snip.npo.twin.TwinScopeType;
import com.simba.snip.npo.twin.TwinSynchronizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NetworkChangeProposalGenerationService {

    private final ChangeIntelligenceProperties properties;
    private final CellRepository cellRepository;
    private final RadioConfigurationRepository radioConfigurationRepository;
    private final AssuranceCaseRepository assuranceCaseRepository;
    private final SynchronizationPolicyRegistry policyRegistry;
    private final SynchronizationSourceStateService sourceStateService;
    private final SynchronizationCheckpointService checkpointService;
    private final TxPowerCandidateGenerator candidateGenerator;
    private final ChangeProposalConstraintValidator constraintValidator;
    private final KnowledgeGate knowledgeGate;
    private final TwinCompatibilityChecker twinCompatibilityChecker;
    private final TwinSynchronizationService twinSynchronizationService;
    private final TwinScenarioService twinScenarioService;
    private final DigitalTwinSimulationService simulationService;
    private final ChangeProposalBenefitAssessor benefitAssessor;
    private final RiskAssessor riskAssessor;
    private final ChangeProposalScorer scorer;
    private final ChangeProposalRanker ranker;
    private final NetworkChangeProposalRepository proposalRepository;
    private final NetworkChangeCandidateRepository candidateRepository;
    private final ChangeProposalAuditService auditService;
    private final ChangeProposalMetrics metrics;
    private final Clock clock;

    public NetworkChangeProposalGenerationService(
            ChangeIntelligenceProperties properties,
            CellRepository cellRepository,
            RadioConfigurationRepository radioConfigurationRepository,
            AssuranceCaseRepository assuranceCaseRepository,
            SynchronizationPolicyRegistry policyRegistry,
            SynchronizationSourceStateService sourceStateService,
            SynchronizationCheckpointService checkpointService,
            TxPowerCandidateGenerator candidateGenerator,
            ChangeProposalConstraintValidator constraintValidator,
            KnowledgeGate knowledgeGate,
            TwinCompatibilityChecker twinCompatibilityChecker,
            TwinSynchronizationService twinSynchronizationService,
            TwinScenarioService twinScenarioService,
            DigitalTwinSimulationService simulationService,
            ChangeProposalBenefitAssessor benefitAssessor,
            RiskAssessor riskAssessor,
            ChangeProposalScorer scorer,
            ChangeProposalRanker ranker,
            NetworkChangeProposalRepository proposalRepository,
            NetworkChangeCandidateRepository candidateRepository,
            ChangeProposalAuditService auditService,
            ChangeProposalMetrics metrics,
            Clock clock
    ) {
        this.properties = properties;
        this.cellRepository = cellRepository;
        this.radioConfigurationRepository = radioConfigurationRepository;
        this.assuranceCaseRepository = assuranceCaseRepository;
        this.policyRegistry = policyRegistry;
        this.sourceStateService = sourceStateService;
        this.checkpointService = checkpointService;
        this.candidateGenerator = candidateGenerator;
        this.constraintValidator = constraintValidator;
        this.knowledgeGate = knowledgeGate;
        this.twinCompatibilityChecker = twinCompatibilityChecker;
        this.twinSynchronizationService = twinSynchronizationService;
        this.twinScenarioService = twinScenarioService;
        this.simulationService = simulationService;
        this.benefitAssessor = benefitAssessor;
        this.riskAssessor = riskAssessor;
        this.scorer = scorer;
        this.ranker = ranker;
        this.proposalRepository = proposalRepository;
        this.candidateRepository = candidateRepository;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    public record GenerationRequest(
            String targetEntityType,
            String targetEntityId,
            String parameterName,
            UUID assuranceCaseId,
            String decisionReference,
            GenerationInitiator initiator,
            String requestedBy
    ) {
    }

    @Transactional
    public NetworkChangeProposalEntity generate(GenerationRequest request) {
        long started = System.nanoTime();
        metrics.incrementGenerationAttempts();
        properties.validate();
        Instant now = clock.instant();
        auditService.append(null, "PROPOSAL_GENERATION_REQUESTED", request.requestedBy(),
                request.targetEntityId() + "/" + request.parameterName());

        if (!TwinScopeType.CELL.name().equals(request.targetEntityType())) {
            throw new ChangeProposalException(ChangeProposalFailureCode.UNSUPPORTED_TARGET, "only CELL supported");
        }
        if (!SimulatableParameterRegistry.TX_POWER.equals(request.parameterName())) {
            throw new ChangeProposalException(ChangeProposalFailureCode.UNSUPPORTED_PARAMETER, request.parameterName());
        }
        constraintValidator.validateParameter(request.parameterName(), TwinScopeType.CELL);

        CellEntity cell = cellRepository.findByCellId(request.targetEntityId())
                .orElseThrow(() -> new DomainNotFoundException("cell", request.targetEntityId()));
        BigDecimal currentValue = radioConfigurationRepository
                .findByCell_IdAndParameterName(cell.getId(), SimulatableParameterRegistry.TX_POWER)
                .map(r -> new BigDecimal(r.getParameterValue()))
                .orElse(null);
        var currentCheck = constraintValidator.validateCurrentValuePresent(currentValue);
        if (!currentCheck.valid()) {
            metrics.incrementGenerationBlocked();
            throw new ChangeProposalException(currentCheck.failureCode(), currentCheck.reason());
        }

        SynchronizationPolicy policy = policyRegistry.policies().stream()
                .filter(SynchronizationPolicy::enabled)
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("no synchronization policy configured"));
        NetworkKnowledgeStatusEntity knowledge = sourceStateService.requireKnowledge(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        NetworkKnowledgeConfidence knowledgeConfidence = NetworkKnowledgeConfidence.valueOf(knowledge.getConfidence());
        KnowledgeGate.GateResult gate = knowledgeGate.evaluate(knowledgeConfidence);

        String assuranceConfidence = null;
        if (request.assuranceCaseId() != null) {
            AssuranceCaseEntity assuranceCase = assuranceCaseRepository.findById(request.assuranceCaseId())
                    .orElseThrow(() -> new DomainNotFoundException("assuranceCase", request.assuranceCaseId().toString()));
            assuranceConfidence = assuranceCase.getConfidence();
        }

        supersedeExisting(request, now);

        UUID proposalId = UUID.randomUUID();
        NetworkChangeProposalEntity proposal = NetworkChangeProposalEntity.createDraft(
                proposalId,
                ProposalType.RADIO_TX_POWER_OPTIMIZATION.name(),
                request.targetEntityType(),
                request.targetEntityId(),
                request.parameterName(),
                currentValue.stripTrailingZeros().toPlainString(),
                SimulatableParameterRegistry.find(request.parameterName()).map(SimulatableParameterDefinition::unit).orElse("dBm"),
                policy.sourceSystem(),
                knowledge.getLastTrustedSnapshotId(),
                checkpointService.find(policy.sourceSystem(), policy.sourceScope())
                        .map(cp -> cp.getLastSuccessfulExecutionId())
                        .orElse(null),
                knowledge.getConfidence(),
                knowledge.getReasonCodes(),
                assuranceConfidence,
                request.assuranceCaseId(),
                request.decisionReference(),
                request.initiator().name(),
                request.requestedBy(),
                now,
                now.plus(properties.validityDuration())
        );
        proposal.markEvaluating(ProposalStatus.VALIDATING.name());
        proposalRepository.save(proposal);

        TwinCompatibilityChecker.CompatibilityResult twinCheck =
                twinCompatibilityChecker.check(request.targetEntityId(), currentValue);
        if (!twinCheck.compatible()) {
            return finalizeFailure(proposalId, twinCheck.failureCode(), twinCheck.reason(), now);
        }

        NetworkTwinEntity twin = twinSynchronizationService.synchronizeCell(request.targetEntityId());
        SimulatableParameterDefinition definition = SimulatableParameterRegistry.requireEnabled(
                request.parameterName(), TwinScopeType.CELL);

        List<BigDecimal> candidates = candidateGenerator.generate(currentValue);
        if (candidates.isEmpty()) {
            return finalizeFailure(proposalId, ChangeProposalFailureCode.NO_VALID_CANDIDATES, "no candidates", now);
        }

        proposal.markEvaluating(ProposalStatus.SIMULATING.name());
        proposalRepository.save(proposal);

        List<ChangeProposalRanker.CandidateEvaluation> evaluations = new ArrayList<>();
        List<NetworkChangeCandidateEntity> candidateEntities = new ArrayList<>();
        boolean anySimulationSucceeded = false;

        for (BigDecimal candidateValue : candidates) {
            boolean baseline = candidateValue.compareTo(currentValue) == 0;
            var validation = constraintValidator.validateCandidate(definition, currentValue, candidateValue);
            if (!validation.valid()) {
                candidateEntities.add(persistCandidate(
                        proposal, candidateValue, baseline, CandidateValidationOutcome.INVALID,
                        validation.failureCode().name(), null, null, null, null, null, null, null, null, now));
                continue;
            }
            if (baseline) {
                candidateEntities.add(persistCandidate(
                        proposal, candidateValue, true, CandidateValidationOutcome.SKIPPED_BASELINE,
                        "BASELINE", null, null, null, null, null, null, null, null, now));
                continue;
            }
            try {
                SimulationScenarioEntity scenario = twinScenarioService.create(
                        twin.getId(),
                        "phase13-" + proposalId,
                        "Phase 13 candidate evaluation",
                        request.requestedBy(),
                        null,
                        new ScenarioChangeRequest(
                                request.parameterName(),
                                currentValue.doubleValue(),
                                candidateValue.doubleValue()
                        )
                );
                Map<String, Object> args = new LinkedHashMap<>();
                args.put("dryRun", true);
                args.put("scenarioId", scenario.getId().toString());
                args.put("cellId", request.targetEntityId());
                args.put("parameter", request.parameterName());
                args.put("currentValue", currentValue.doubleValue());
                Map<String, Object> simulationResult = simulationService.executeFromMcp(args);
                anySimulationSucceeded = true;
                UUID simulationId = UUID.fromString(String.valueOf(simulationResult.get("simulationId")));
                SimulationConfidence simulationConfidence = SimulationConfidence.valueOf(
                        String.valueOf(simulationResult.get("confidence")));
                ChangeProposalBenefitAssessor.BenefitResult benefit = benefitAssessor.assessFromSimulationMap(simulationResult);
                RiskAssessor.RiskResult risk = riskAssessor.assess(
                        currentValue, candidateValue, knowledgeConfidence, simulationConfidence);
                BigDecimal proposalScore = scorer.score(
                        benefit.score(), risk.level(), simulationConfidence, knowledgeConfidence);
                evaluations.add(new ChangeProposalRanker.CandidateEvaluation(
                        candidateValue, benefit.score(), risk.level(), proposalScore, false));
                candidateEntities.add(persistCandidate(
                        proposal,
                        candidateValue,
                        false,
                        CandidateValidationOutcome.VALID,
                        null,
                        simulationId,
                        simulationConfidence.name(),
                        benefit.score(),
                        String.join(",", benefit.reasonCodes()),
                        risk.level().name(),
                        String.join(",", risk.reasonCodes()),
                        proposalScore,
                        null,
                        now));
            } catch (RuntimeException ex) {
                candidateEntities.add(persistCandidate(
                        proposal, candidateValue, false, CandidateValidationOutcome.INVALID,
                        ChangeProposalFailureCode.SIMULATION_FAILED.name(), null, null, null, null, null, null, null, null, now));
            }
        }

        if (!anySimulationSucceeded) {
            candidateRepository.saveAll(candidateEntities);
            metrics.incrementSimulationFailures();
            return finalizeFailure(proposalId, ChangeProposalFailureCode.SIMULATION_FAILED, "all simulations failed", now);
        }

        List<ChangeProposalRanker.RankedCandidate> ranked = ranker.rank(evaluations, currentValue);
        java.util.Map<String, Integer> rankByValue = new java.util.HashMap<>();
        for (ChangeProposalRanker.RankedCandidate rankedCandidate : ranked) {
            rankByValue.put(rankedCandidate.candidateValue().stripTrailingZeros().toPlainString(), rankedCandidate.rank());
        }
        List<NetworkChangeCandidateEntity> rankedEntities = new ArrayList<>();
        for (NetworkChangeCandidateEntity candidate : candidateEntities) {
            Integer rank = rankByValue.get(candidate.getCandidateValue());
            rankedEntities.add(NetworkChangeCandidateEntity.create(
                    candidate.getId(),
                    candidate.getProposal(),
                    candidate.getCandidateValue(),
                    candidate.isBaselineCandidate(),
                    candidate.getValidationOutcome(),
                    candidate.getValidationReason(),
                    candidate.getSimulationRunId(),
                    candidate.getSimulationConfidence(),
                    candidate.getBenefitScore(),
                    candidate.getBenefitReasonCodes(),
                    candidate.getRiskLevel(),
                    candidate.getRiskReasonCodes(),
                    candidate.getProposalScore(),
                    rank,
                    candidate.getCreatedAt()
            ));
        }
        candidateRepository.saveAll(rankedEntities);

        ChangeProposalRanker.RankedCandidate best = ranked.isEmpty() ? null : ranked.get(0);
        BigDecimal minBenefit = properties.getMinBenefitScore();
        if (best == null || best.benefitScore().compareTo(minBenefit) <= 0) {
            return finalizeEvaluated(
                    proposalId,
                    ProposalStatus.EVALUATED.name(),
                    null,
                    null,
                    ChangeProposalFailureCode.NO_BENEFICIAL_CANDIDATE.name(),
                    "no candidate beats baseline by minBenefitScore",
                    now,
                    gate);
        }

        if (!gate.allowsRecommendation()) {
            return finalizeEvaluated(
                    proposalId,
                    ProposalStatus.EVALUATED.name(),
                    null,
                    null,
                    gate.blockCode().name(),
                    gate.reasonCode(),
                    now,
                    gate);
        }

        proposal = reload(proposalId);
        proposal.completeEvaluation(
                ProposalStatus.RECOMMENDED.name(),
                best.candidateValue().stripTrailingZeros().toPlainString(),
                SimulationConfidence.LOW.name(),
                "ranked candidate benefit=" + best.benefitScore(),
                best.benefitScore(),
                best.riskLevel().name(),
                "RANK=1",
                best.proposalScore(),
                null,
                gate.degraded() ? gate.reasonCode() : null,
                now
        );
        proposalRepository.save(proposal);
        metrics.incrementRecommended();
        metrics.recordEvaluationDurationMs((System.nanoTime() - started) / 1_000_000);
        auditService.append(proposalId, "RECOMMENDATION_PRODUCED", request.requestedBy(),
                "proposed=" + proposal.getProposedValue());
        return proposal;
    }

    private void supersedeExisting(GenerationRequest request, Instant now) {
        proposalRepository.findFirstByTargetEntityTypeAndTargetEntityIdAndParameterNameAndStatusOrderByCreatedAtDesc(
                        request.targetEntityType(), request.targetEntityId(), request.parameterName(),
                        ProposalStatus.RECOMMENDED.name())
                .ifPresent(existing -> {
                    existing.markSuperseded(UUID.randomUUID(), now);
                    proposalRepository.save(existing);
                    auditService.append(existing.getId(), "PROPOSAL_SUPERSEDED", request.requestedBy(), "superseded");
                    metrics.incrementSuperseded();
                });
    }

    private NetworkChangeProposalEntity reload(UUID proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new DomainNotFoundException("changeProposal", proposalId.toString()));
    }

    private NetworkChangeProposalEntity finalizeFailure(
            UUID proposalId,
            ChangeProposalFailureCode code,
            String reason,
            Instant now
    ) {
        metrics.incrementGenerationBlocked();
        NetworkChangeProposalEntity proposal = reload(proposalId);
        proposal.completeEvaluation(
                code == ChangeProposalFailureCode.SIMULATION_FAILED
                        ? ProposalStatus.SIMULATION_FAILED.name()
                        : ProposalStatus.INVALID.name(),
                null, null, null, null, null, null, null, code.name(), reason, now);
        proposalRepository.save(proposal);
        auditService.append(proposalId, "PROPOSAL_GENERATION_BLOCKED", proposal.getCreatedBy(), code.name());
        return proposal;
    }

    private NetworkChangeProposalEntity finalizeEvaluated(
            UUID proposalId,
            String status,
            String proposedValue,
            String simulationConfidence,
            String failureCode,
            String failureReason,
            Instant now,
            KnowledgeGate.GateResult gate
    ) {
        metrics.incrementEvaluated();
        NetworkChangeProposalEntity proposal = reload(proposalId);
        proposal.completeEvaluation(
                status,
                proposedValue,
                simulationConfidence,
                null,
                null,
                null,
                null,
                null,
                failureCode,
                failureReason,
                now
        );
        if (gate.degraded() && failureCode == null) {
            proposal.refreshKnowledgeSnapshot(proposal.getNetworkKnowledgeConfidence(), gate.reasonCode());
        }
        proposalRepository.save(proposal);
        auditService.append(proposalId, "PROPOSAL_EVALUATED", proposal.getCreatedBy(), status);
        return proposal;
    }

    private NetworkChangeCandidateEntity persistCandidate(
            NetworkChangeProposalEntity proposal,
            BigDecimal candidateValue,
            boolean baseline,
            CandidateValidationOutcome outcome,
            String validationReason,
            UUID simulationRunId,
            String simulationConfidence,
            BigDecimal benefitScore,
            String benefitReasons,
            String riskLevel,
            String riskReasons,
            BigDecimal proposalScore,
            Integer rankOrder,
            Instant now
    ) {
        return NetworkChangeCandidateEntity.create(
                UUID.randomUUID(),
                proposal,
                candidateValue.stripTrailingZeros().toPlainString(),
                baseline,
                outcome.name(),
                validationReason,
                simulationRunId,
                simulationConfidence,
                benefitScore,
                benefitReasons,
                riskLevel,
                riskReasons,
                proposalScore,
                rankOrder,
                now
        );
    }
}
