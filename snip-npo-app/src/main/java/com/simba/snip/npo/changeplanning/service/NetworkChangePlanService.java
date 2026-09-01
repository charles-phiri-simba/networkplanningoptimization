package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.ChangePlanException;
import com.simba.snip.npo.changeplanning.config.ChangePlanningProperties;
import com.simba.snip.npo.changeplanning.model.ChangeImpactLevel;
import com.simba.snip.npo.changeplanning.model.ChangePlanAuditEventType;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanPreconditionEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanOperationDependencyRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanOperationRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanPreconditionRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRollbackOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NetworkChangePlanService {

    private final ChangePlanningProperties properties;
    private final ChangePlanEligibilityService eligibilityService;
    private final ChangePlanOperationBuilder operationBuilder;
    private final ChangePlanDependencyService dependencyService;
    private final ChangePlanRollbackService rollbackService;
    private final ChangePlanPreconditionService preconditionService;
    private final ChangePlanFingerprintService fingerprintService;
    private final ChangeImpactAssessmentService impactAssessmentService;
    private final ChangePlanSafetyService safetyService;
    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangePlanOperationRepository operationRepository;
    private final NetworkChangePlanRollbackOperationRepository rollbackRepository;
    private final NetworkChangePlanPreconditionRepository preconditionRepository;
    private final NetworkChangePlanOperationDependencyRepository dependencyRepository;
    private final ChangePlanAuditService auditService;
    private final ChangePlanMetrics metrics;
    private final Clock clock;

    public NetworkChangePlanService(
            ChangePlanningProperties properties,
            ChangePlanEligibilityService eligibilityService,
            ChangePlanOperationBuilder operationBuilder,
            ChangePlanDependencyService dependencyService,
            ChangePlanRollbackService rollbackService,
            ChangePlanPreconditionService preconditionService,
            ChangePlanFingerprintService fingerprintService,
            ChangeImpactAssessmentService impactAssessmentService,
            ChangePlanSafetyService safetyService,
            NetworkChangePlanRepository planRepository,
            NetworkChangePlanOperationRepository operationRepository,
            NetworkChangePlanRollbackOperationRepository rollbackRepository,
            NetworkChangePlanPreconditionRepository preconditionRepository,
            NetworkChangePlanOperationDependencyRepository dependencyRepository,
            ChangePlanAuditService auditService,
            ChangePlanMetrics metrics,
            Clock clock
    ) {
        this.properties = properties;
        this.eligibilityService = eligibilityService;
        this.operationBuilder = operationBuilder;
        this.dependencyService = dependencyService;
        this.rollbackService = rollbackService;
        this.preconditionService = preconditionService;
        this.fingerprintService = fingerprintService;
        this.impactAssessmentService = impactAssessmentService;
        this.safetyService = safetyService;
        this.planRepository = planRepository;
        this.operationRepository = operationRepository;
        this.rollbackRepository = rollbackRepository;
        this.preconditionRepository = preconditionRepository;
        this.dependencyRepository = dependencyRepository;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangePlanEntity createPlan(UUID proposalId, String createdBy) {
        if (!properties.isEnabled()) {
            throw new ChangePlanException(
                    com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode.CHANGE_PLANNING_DISABLED,
                    "disabled"
            );
        }
        Instant now = clock.instant();
        ChangePlanEligibilityService.EligibilityResult eligibility = eligibilityService.evaluate(proposalId);
        UUID planId = UUID.randomUUID();
        auditService.append(planId, ChangePlanAuditEventType.PLAN_CREATED.name(), createdBy, proposalId.toString());
        auditService.append(planId, ChangePlanAuditEventType.PLAN_VALIDATION_STARTED.name(), createdBy, null);

        ParameterChangeIntent intent = eligibility.intent();
        NetworkChangePlanOperationEntity operation = operationBuilder.buildForwardOperation(planId, intent, now);
        operationBuilder.enforceOperationCount(List.of(operation));
        List<ChangePlanDependencyService.DependencyEdge> dependencies = List.of();
        dependencyService.validateGraph(planId, List.of(operation), dependencies);

        NetworkChangePlanRollbackOperationEntity rollback = null;
        if (properties.isRequireRollback()) {
            rollback = rollbackService.buildRollback(planId, intent, now);
        }

        ChangePlanSafetyService.SafetyEvaluation safety = safetyService.evaluateCreation(intent);
        if (!safety.pass()) {
            NetworkChangePlanEntity blocked = persistBlockedPlan(
                    planId, eligibility, intent, now, createdBy, safety.failureCode().name());
            metrics.incrementPlansBlocked();
            auditService.append(planId, ChangePlanAuditEventType.PLAN_BLOCKED.name(), "system", safety.reason());
            return blocked;
        }
        if (rollback != null) {
            ChangePlanSafetyService.SafetyEvaluation rollbackSafety = safetyService.evaluateRollback(operation, rollback);
            if (!rollbackSafety.pass()) {
                NetworkChangePlanEntity blocked = persistBlockedPlan(
                        planId, eligibility, intent, now, createdBy, rollbackSafety.failureCode().name());
                metrics.incrementPlansBlocked();
                auditService.append(planId, ChangePlanAuditEventType.PLAN_BLOCKED.name(), "system", rollbackSafety.reason());
                return blocked;
            }
        }

        List<ChangePlanFingerprintService.PreconditionDefinition> preconditionDefs =
                ChangePlanPreconditionService.defaultDefinitions(intent);
        String fingerprint = fingerprintService.compute(new ChangePlanFingerprintService.FingerprintInput(
                proposalId,
                intent,
                List.of(operation),
                dependencies,
                preconditionDefs,
                rollback,
                eligibility.proposal().getSourceSynchronizationExecutionId(),
                eligibility.proposal().getSourceSnapshotId()
        ));
        ChangeImpactLevel impact = impactAssessmentService.assess(intent);

        NetworkChangePlanEntity plan = NetworkChangePlanEntity.createDraft(
                planId,
                proposalId,
                eligibility.rankOneCandidate().getId(),
                intent.targetType(),
                intent.targetId(),
                intent.parameter(),
                intent.expectedCurrentValue(),
                intent.desiredValue(),
                fingerprint,
                eligibility.proposal().getSourceSystem(),
                eligibility.proposal().getSourceSnapshotId(),
                eligibility.proposal().getSourceSynchronizationExecutionId(),
                eligibility.knowledge().getConfidence(),
                eligibility.knowledge().getReasonCodes(),
                impact.name(),
                eligibility.proposal().getRiskLevel(),
                eligibility.proposal().getRiskReasonCodes(),
                createdBy,
                now,
                now.plus(properties.getValidityDuration())
        );
        plan.setStatus(PlanStatus.READY_FOR_REVIEW.name());
        planRepository.save(plan);
        operationRepository.save(operation);
        if (rollback != null) {
            rollbackRepository.save(rollback);
        }
        dependencyRepository.saveAll(dependencyService.toEntities(planId, dependencies));
        List<NetworkChangePlanPreconditionEntity> preconditions =
                ChangePlanPreconditionService.createPersisted(planId, preconditionDefs, now);
        ChangePlanPreconditionService.EvaluationContext evaluationContext =
                new ChangePlanPreconditionService.EvaluationContext(
                        plan,
                        intent,
                        List.of(operation),
                        List.of(),
                        rollback,
                        false
                );
        List<ChangePlanPreconditionService.PreconditionEvaluation> evaluations =
                preconditionService.evaluateAtCreation(evaluationContext, now);
        preconditionService.applyEvaluations(preconditions, evaluations, now);
        preconditionRepository.saveAll(preconditions);

        metrics.incrementPlansCreated();
        auditService.append(planId, ChangePlanAuditEventType.PLAN_VALIDATED.name(), "system", fingerprint);
        auditService.append(planId, ChangePlanAuditEventType.PLAN_SAFETY_EVALUATED.name(), "system", impact.name());
        return plan;
    }

    private NetworkChangePlanEntity persistBlockedPlan(
            UUID planId,
            ChangePlanEligibilityService.EligibilityResult eligibility,
            ParameterChangeIntent intent,
            Instant now,
            String createdBy,
            String reason
    ) {
        NetworkChangePlanEntity plan = NetworkChangePlanEntity.createDraft(
                planId,
                eligibility.proposal().getId(),
                eligibility.rankOneCandidate().getId(),
                intent.targetType(),
                intent.targetId(),
                intent.parameter(),
                intent.expectedCurrentValue(),
                intent.desiredValue(),
                "BLOCKED",
                eligibility.proposal().getSourceSystem(),
                eligibility.proposal().getSourceSnapshotId(),
                eligibility.proposal().getSourceSynchronizationExecutionId(),
                eligibility.knowledge().getConfidence(),
                eligibility.knowledge().getReasonCodes(),
                ChangeImpactLevel.MINIMAL.name(),
                eligibility.proposal().getRiskLevel(),
                eligibility.proposal().getRiskReasonCodes(),
                createdBy,
                now,
                now.plus(properties.getValidityDuration())
        );
        plan.markBlocked();
        return planRepository.save(plan);
    }
}
