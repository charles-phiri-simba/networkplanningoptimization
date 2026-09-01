package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.changeintelligence.policy.TwinCompatibilityChecker;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
import com.simba.snip.npo.changeintelligence.service.ChangeProposalValidityService;
import com.simba.snip.npo.changeplanning.config.ChangePlanningProperties;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.changeplanning.model.PreconditionResult;
import com.simba.snip.npo.changeplanning.model.PreconditionType;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationDependencyEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanPreconditionEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChangePlanPreconditionService {

    private final ChangePlanningProperties properties;
    private final CellRepository cellRepository;
    private final RadioConfigurationRepository radioConfigurationRepository;
    private final SynchronizationPolicyRegistry policyRegistry;
    private final SynchronizationSourceStateService sourceStateService;
    private final NetworkDriftService driftService;
    private final KnowledgeGate knowledgeGate;
    private final TwinCompatibilityChecker twinCompatibilityChecker;
    private final NetworkChangeProposalRepository proposalRepository;
    private final ChangeProposalValidityService proposalValidityService;
    private final ChangePlanRollbackService rollbackService;
    private final ChangePlanDependencyService dependencyService;

    public ChangePlanPreconditionService(
            ChangePlanningProperties properties,
            CellRepository cellRepository,
            RadioConfigurationRepository radioConfigurationRepository,
            SynchronizationPolicyRegistry policyRegistry,
            SynchronizationSourceStateService sourceStateService,
            NetworkDriftService driftService,
            KnowledgeGate knowledgeGate,
            TwinCompatibilityChecker twinCompatibilityChecker,
            NetworkChangeProposalRepository proposalRepository,
            ChangeProposalValidityService proposalValidityService,
            ChangePlanRollbackService rollbackService,
            ChangePlanDependencyService dependencyService
    ) {
        this.properties = properties;
        this.cellRepository = cellRepository;
        this.radioConfigurationRepository = radioConfigurationRepository;
        this.policyRegistry = policyRegistry;
        this.sourceStateService = sourceStateService;
        this.driftService = driftService;
        this.knowledgeGate = knowledgeGate;
        this.twinCompatibilityChecker = twinCompatibilityChecker;
        this.proposalRepository = proposalRepository;
        this.proposalValidityService = proposalValidityService;
        this.rollbackService = rollbackService;
        this.dependencyService = dependencyService;
    }

    public record PreconditionEvaluation(
            PreconditionType type,
            String observedValue,
            PreconditionResult result,
            String reasonCode,
            String evidenceReference
    ) {
    }

    public record EvaluationContext(
            NetworkChangePlanEntity plan,
            ParameterChangeIntent intent,
            List<NetworkChangePlanOperationEntity> operations,
            List<NetworkChangePlanOperationDependencyEntity> dependencies,
            NetworkChangePlanRollbackOperationEntity rollback,
            boolean evaluateAuthorization
    ) {
    }

    public static List<ChangePlanFingerprintService.PreconditionDefinition> defaultDefinitions(ParameterChangeIntent intent) {
        List<ChangePlanFingerprintService.PreconditionDefinition> definitions = new ArrayList<>();
        for (PreconditionType type : PreconditionType.values()) {
            definitions.add(new ChangePlanFingerprintService.PreconditionDefinition(type, expectedFor(type, intent)));
        }
        definitions.sort(Comparator.comparing(d -> d.type().name()));
        return definitions;
    }

    public static List<NetworkChangePlanPreconditionEntity> createPersisted(
            UUID planId,
            List<ChangePlanFingerprintService.PreconditionDefinition> definitions,
            Instant createdAt
    ) {
        List<NetworkChangePlanPreconditionEntity> entities = new ArrayList<>();
        int sequence = 1;
        for (ChangePlanFingerprintService.PreconditionDefinition definition : definitions) {
            entities.add(NetworkChangePlanPreconditionEntity.create(
                    UUID.randomUUID(),
                    planId,
                    definition.type().name(),
                    definition.expectedCondition(),
                    null,
                    PreconditionResult.UNKNOWN.name(),
                    null,
                    null,
                    null,
                    sequence++
            ));
        }
        return entities;
    }

    public List<PreconditionEvaluation> evaluateAtCreation(EvaluationContext context, Instant now) {
        return evaluate(context, now, false);
    }

    public List<PreconditionEvaluation> evaluateAtReadiness(EvaluationContext context, Instant now) {
        return evaluate(context, now, true);
    }

    public void applyEvaluations(
            List<NetworkChangePlanPreconditionEntity> entities,
            List<PreconditionEvaluation> evaluations,
            Instant checkedAt
    ) {
        Map<PreconditionType, PreconditionEvaluation> byType = new EnumMap<>(PreconditionType.class);
        for (PreconditionEvaluation evaluation : evaluations) {
            byType.put(evaluation.type(), evaluation);
        }
        for (NetworkChangePlanPreconditionEntity entity : entities) {
            PreconditionType type = PreconditionType.valueOf(entity.getPreconditionType());
            PreconditionEvaluation evaluation = byType.get(type);
            if (evaluation == null) {
                continue;
            }
            entity.updateEvaluation(
                    evaluation.observedValue(),
                    evaluation.result().name(),
                    evaluation.reasonCode(),
                    checkedAt,
                    evaluation.evidenceReference()
            );
        }
    }

    public boolean allMandatoryPass(List<PreconditionEvaluation> evaluations) {
        return evaluations.stream().allMatch(e -> e.result().countsAsPass());
    }

    public ChangePlanFailureCode firstFailureCode(List<PreconditionEvaluation> evaluations) {
        for (PreconditionEvaluation evaluation : evaluations) {
            if (evaluation.result().countsAsPass()) {
                continue;
            }
            if (evaluation.reasonCode() != null) {
                try {
                    return ChangePlanFailureCode.valueOf(evaluation.reasonCode());
                } catch (IllegalArgumentException ignored) {
                    // fall through
                }
            }
        }
        return ChangePlanFailureCode.INVALID_PLAN_STATE;
    }

    private List<PreconditionEvaluation> evaluate(
            EvaluationContext context,
            Instant now,
            boolean readinessPhase
    ) {
        List<PreconditionEvaluation> evaluations = new ArrayList<>();
        for (PreconditionType type : PreconditionType.values()) {
            evaluations.add(evaluateType(type, context, now, readinessPhase));
        }
        evaluations.sort(Comparator.comparing(e -> e.type().name()));
        return evaluations;
    }

    private PreconditionEvaluation evaluateType(
            PreconditionType type,
            EvaluationContext context,
            Instant now,
            boolean readinessPhase
    ) {
        return switch (type) {
            case EXPECTED_PARAMETER_VALUE -> evaluateExpectedParameterValue(context);
            case NETWORK_KNOWLEDGE_CONFIDENCE -> evaluateKnowledge(context, now);
            case SOURCE_SYNCHRONIZATION_FRESHNESS -> evaluateFreshness(context, now);
            case NO_RELEVANT_DRIFT -> evaluateDrift(context, now);
            case TWIN_COMPATIBILITY -> evaluateTwin(context);
            case PROPOSAL_STILL_VALID -> evaluateProposal(context);
            case TARGET_EXISTS -> evaluateTargetExists(context);
            case ROLLBACK_AVAILABLE -> evaluateRollback(context);
            case DEPENDENCY_GRAPH_VALID -> evaluateDependencyGraph(context);
            case FINGERPRINT_CURRENT -> evaluateFingerprint(context);
            case AUTHORIZATION_CURRENT -> evaluateAuthorization(context, readinessPhase);
        };
    }

    private PreconditionEvaluation evaluateExpectedParameterValue(EvaluationContext context) {
        CellEntity cell = cellRepository.findByCellId(context.intent().targetId()).orElse(null);
        if (cell == null) {
            return new PreconditionEvaluation(
                    PreconditionType.EXPECTED_PARAMETER_VALUE,
                    null,
                    PreconditionResult.FAIL,
                    ChangePlanFailureCode.PLAN_TARGET_NOT_FOUND.name(),
                    "cell:" + context.intent().targetId()
            );
        }
        String canonical = radioConfigurationRepository
                .findByCell_IdAndParameterName(cell.getId(), SimulatableParameterRegistry.TX_POWER)
                .map(r -> r.getParameterValue())
                .orElse(null);
        if (canonical == null) {
            return new PreconditionEvaluation(
                    PreconditionType.EXPECTED_PARAMETER_VALUE,
                    null,
                    PreconditionResult.UNKNOWN,
                    ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH.name(),
                    "radio_configuration:" + context.intent().targetId()
            );
        }
        boolean matches = new BigDecimal(canonical).compareTo(new BigDecimal(context.intent().expectedCurrentValue())) == 0;
        return new PreconditionEvaluation(
                PreconditionType.EXPECTED_PARAMETER_VALUE,
                canonical,
                matches ? PreconditionResult.PASS : PreconditionResult.FAIL,
                matches ? null : ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH.name(),
                "radio_configuration:" + context.intent().targetId()
        );
    }

    private PreconditionEvaluation evaluateKnowledge(EvaluationContext context, Instant now) {
        SynchronizationPolicy policy = resolvePolicy(context.plan());
        NetworkKnowledgeStatusEntity knowledge = sourceStateService.requireKnowledge(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        NetworkKnowledgeConfidence confidence = NetworkKnowledgeConfidence.valueOf(knowledge.getConfidence());
        if (properties.isRequireHighOrMediumKnowledge() && knowledgeGate.blocksApproval(confidence)) {
            PreconditionResult result = confidence == NetworkKnowledgeConfidence.UNKNOWN
                    ? PreconditionResult.UNKNOWN
                    : PreconditionResult.FAIL;
            ChangePlanFailureCode code = confidence == NetworkKnowledgeConfidence.UNKNOWN
                    ? ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_UNKNOWN
                    : ChangePlanFailureCode.PLAN_NETWORK_KNOWLEDGE_LOW;
            return new PreconditionEvaluation(
                    PreconditionType.NETWORK_KNOWLEDGE_CONFIDENCE,
                    confidence.name(),
                    result,
                    code.name(),
                    "knowledge:" + policy.sourceSystem()
            );
        }
        return new PreconditionEvaluation(
                PreconditionType.NETWORK_KNOWLEDGE_CONFIDENCE,
                confidence.name(),
                PreconditionResult.PASS,
                null,
                "knowledge:" + policy.sourceSystem()
        );
    }

    private PreconditionEvaluation evaluateFreshness(EvaluationContext context, Instant now) {
        SynchronizationPolicy policy = resolvePolicy(context.plan());
        NetworkKnowledgeStatusEntity knowledge = sourceStateService.requireKnowledge(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        String freshness = knowledge.getFreshness();
        if ("STALE".equals(freshness)) {
            return new PreconditionEvaluation(
                    PreconditionType.SOURCE_SYNCHRONIZATION_FRESHNESS,
                    freshness,
                    PreconditionResult.STALE,
                    ChangePlanFailureCode.PLAN_SYNCHRONIZATION_STALE.name(),
                    "freshness:" + policy.sourceSystem()
            );
        }
        if ("UNKNOWN".equals(freshness)) {
            return new PreconditionEvaluation(
                    PreconditionType.SOURCE_SYNCHRONIZATION_FRESHNESS,
                    freshness,
                    PreconditionResult.UNKNOWN,
                    ChangePlanFailureCode.PLAN_SYNCHRONIZATION_STALE.name(),
                    "freshness:" + policy.sourceSystem()
            );
        }
        return new PreconditionEvaluation(
                PreconditionType.SOURCE_SYNCHRONIZATION_FRESHNESS,
                freshness,
                PreconditionResult.PASS,
                null,
                "freshness:" + policy.sourceSystem()
        );
    }

    private PreconditionEvaluation evaluateDrift(EvaluationContext context, Instant now) {
        SynchronizationPolicy policy = resolvePolicy(context.plan());
        List<NetworkDriftObservationEntity> drifts = driftService.list(context.plan().getSourceSystem(), policy.sourceScope());
        boolean relevant = drifts.stream()
                .anyMatch(d -> "OPEN".equals(d.getDriftStatus())
                        && context.plan().getTargetEntityId().equals(d.getEntityId()));
        return new PreconditionEvaluation(
                PreconditionType.NO_RELEVANT_DRIFT,
                relevant ? "OPEN" : "NONE",
                relevant ? PreconditionResult.FAIL : PreconditionResult.PASS,
                relevant ? ChangePlanFailureCode.PLAN_RELEVANT_DRIFT_PRESENT.name() : null,
                "drift:" + context.plan().getTargetEntityId()
        );
    }

    private PreconditionEvaluation evaluateTwin(EvaluationContext context) {
        TwinCompatibilityChecker.CompatibilityResult twin = twinCompatibilityChecker.check(
                context.intent().targetId(),
                new BigDecimal(context.intent().expectedCurrentValue())
        );
        return new PreconditionEvaluation(
                PreconditionType.TWIN_COMPATIBILITY,
                twin.compatible() ? "CURRENT" : twin.reason(),
                twin.compatible() ? PreconditionResult.PASS : PreconditionResult.FAIL,
                twin.compatible() ? null : ChangePlanFailureCode.PLAN_TWIN_STALE.name(),
                "twin:" + context.intent().targetId()
        );
    }

    private PreconditionEvaluation evaluateProposal(EvaluationContext context) {
        return proposalRepository.findById(context.plan().getProposalId())
                .map(proposal -> {
                    if (!ProposalStatus.APPROVED.name().equals(proposal.getStatus())) {
                        return new PreconditionEvaluation(
                                PreconditionType.PROPOSAL_STILL_VALID,
                                proposal.getStatus(),
                                PreconditionResult.FAIL,
                                ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name(),
                                "proposal:" + proposal.getId()
                        );
                    }
                    ChangeProposalValidityService.ValidityResult validity = proposalValidityService.revalidate(proposal);
                    return new PreconditionEvaluation(
                            PreconditionType.PROPOSAL_STILL_VALID,
                            proposal.getStatus(),
                            validity.valid() ? PreconditionResult.PASS : PreconditionResult.FAIL,
                            validity.valid() ? null : ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name(),
                            "proposal:" + proposal.getId()
                    );
                })
                .orElse(new PreconditionEvaluation(
                        PreconditionType.PROPOSAL_STILL_VALID,
                        null,
                        PreconditionResult.FAIL,
                        ChangePlanFailureCode.PLAN_PROPOSAL_INVALID.name(),
                        "proposal:" + context.plan().getProposalId()
                ));
    }

    private PreconditionEvaluation evaluateTargetExists(EvaluationContext context) {
        boolean exists = cellRepository.findByCellId(context.intent().targetId()).isPresent();
        return new PreconditionEvaluation(
                PreconditionType.TARGET_EXISTS,
                context.intent().targetId(),
                exists ? PreconditionResult.PASS : PreconditionResult.FAIL,
                exists ? null : ChangePlanFailureCode.PLAN_TARGET_NOT_FOUND.name(),
                "cell:" + context.intent().targetId()
        );
    }

    private PreconditionEvaluation evaluateRollback(EvaluationContext context) {
        if (context.operations().isEmpty()) {
            return new PreconditionEvaluation(
                    PreconditionType.ROLLBACK_AVAILABLE,
                    null,
                    PreconditionResult.FAIL,
                    ChangePlanFailureCode.PLAN_ROLLBACK_UNAVAILABLE.name(),
                    "operations:empty"
            );
        }
        NetworkChangePlanRollbackOperationEntity rollback = context.rollback();
        boolean valid = rollbackService.validateRollback(context.operations().get(0), rollback);
        return new PreconditionEvaluation(
                PreconditionType.ROLLBACK_AVAILABLE,
                rollback == null ? null : rollback.getOperationType(),
                valid ? PreconditionResult.PASS : PreconditionResult.FAIL,
                valid ? null : ChangePlanFailureCode.PLAN_ROLLBACK_UNAVAILABLE.name(),
                "rollback:" + context.plan().getId()
        );
    }

    private PreconditionEvaluation evaluateDependencyGraph(EvaluationContext context) {
        List<ChangePlanDependencyService.DependencyEdge> edges = context.dependencies().stream()
                .map(d -> new ChangePlanDependencyService.DependencyEdge(d.getOperationId(), d.getDependsOnOperationId()))
                .toList();
        try {
            dependencyService.validateGraph(context.plan().getId(), context.operations(), edges);
            return new PreconditionEvaluation(
                    PreconditionType.DEPENDENCY_GRAPH_VALID,
                    "VALID",
                    PreconditionResult.PASS,
                    null,
                    "dependencies:" + context.plan().getId()
            );
        } catch (Exception ex) {
            return new PreconditionEvaluation(
                    PreconditionType.DEPENDENCY_GRAPH_VALID,
                    "INVALID",
                    PreconditionResult.FAIL,
                    ChangePlanFailureCode.PLAN_DEPENDENCY_INVALID.name(),
                    "dependencies:" + context.plan().getId()
            );
        }
    }

    private PreconditionEvaluation evaluateFingerprint(EvaluationContext context) {
        return new PreconditionEvaluation(
                PreconditionType.FINGERPRINT_CURRENT,
                context.plan().getFingerprint(),
                PreconditionResult.PASS,
                null,
                "fingerprint:" + context.plan().getId()
        );
    }

    private PreconditionEvaluation evaluateAuthorization(EvaluationContext context, boolean readinessPhase) {
        if (!readinessPhase) {
            return new PreconditionEvaluation(
                    PreconditionType.AUTHORIZATION_CURRENT,
                    null,
                    PreconditionResult.UNKNOWN,
                    ChangePlanFailureCode.PLAN_AUTHORIZATION_MISSING.name(),
                    "authorization:pending"
            );
        }
        if (context.plan().getAuthorizedAt() == null || context.plan().getAuthorizedFingerprint() == null) {
            return new PreconditionEvaluation(
                    PreconditionType.AUTHORIZATION_CURRENT,
                    null,
                    PreconditionResult.FAIL,
                    ChangePlanFailureCode.PLAN_AUTHORIZATION_MISSING.name(),
                    "authorization:" + context.plan().getId()
            );
        }
        boolean matches = context.plan().getAuthorizedFingerprint().equals(context.plan().getFingerprint());
        return new PreconditionEvaluation(
                PreconditionType.AUTHORIZATION_CURRENT,
                context.plan().getAuthorizedFingerprint(),
                matches ? PreconditionResult.PASS : PreconditionResult.STALE,
                matches ? null : ChangePlanFailureCode.PLAN_AUTHORIZATION_STALE.name(),
                "authorization:" + context.plan().getId()
        );
    }

    private SynchronizationPolicy resolvePolicy(NetworkChangePlanEntity plan) {
        return policyRegistry.policies().stream()
                .filter(p -> p.sourceSystem().equals(plan.getSourceSystem()))
                .findFirst()
                .orElse(policyRegistry.policies().get(0));
    }

    private static String expectedFor(PreconditionType type, ParameterChangeIntent intent) {
        return switch (type) {
            case EXPECTED_PARAMETER_VALUE -> intent.parameter() + "=" + intent.expectedCurrentValue();
            case NETWORK_KNOWLEDGE_CONFIDENCE -> "HIGH_OR_MEDIUM";
            case SOURCE_SYNCHRONIZATION_FRESHNESS -> "FRESH_OR_AGING";
            case NO_RELEVANT_DRIFT -> "NONE";
            case TWIN_COMPATIBILITY -> "CURRENT";
            case PROPOSAL_STILL_VALID -> "APPROVED";
            case TARGET_EXISTS -> intent.targetId();
            case ROLLBACK_AVAILABLE -> "REQUIRED";
            case DEPENDENCY_GRAPH_VALID -> "VALID";
            case FINGERPRINT_CURRENT -> "MATCH";
            case AUTHORIZATION_CURRENT -> "MATCH";
        };
    }
}
