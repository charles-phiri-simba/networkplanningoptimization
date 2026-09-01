package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.ChangePlanException;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.ExecutionReadinessResult;
import com.simba.snip.npo.changeplanning.model.ParameterChangeIntent;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.changeplanning.model.PreconditionResult;
import com.simba.snip.npo.changeplanning.persist.ExecutionReadinessAssessmentEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanPreconditionEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import com.simba.snip.npo.changeplanning.repository.ExecutionReadinessAssessmentRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanOperationDependencyRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanOperationRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanPreconditionRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRollbackOperationRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChangePlanReadinessService {

    private static final List<String> READINESS_ELIGIBLE = List.of(
            PlanStatus.AUTHORIZED.name(),
            PlanStatus.READY_FOR_EXECUTION.name()
    );

    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangePlanOperationRepository operationRepository;
    private final NetworkChangePlanRollbackOperationRepository rollbackRepository;
    private final NetworkChangePlanPreconditionRepository preconditionRepository;
    private final NetworkChangePlanOperationDependencyRepository dependencyRepository;
    private final ExecutionReadinessAssessmentRepository assessmentRepository;
    private final ChangePlanValidityService validityService;
    private final ChangePlanPreconditionService preconditionService;
    private final ChangePlanSafetyService safetyService;
    private final ChangePlanAuditService auditService;
    private final ChangePlanMetrics metrics;
    private final Clock clock;

    public ChangePlanReadinessService(
            NetworkChangePlanRepository planRepository,
            NetworkChangePlanOperationRepository operationRepository,
            NetworkChangePlanRollbackOperationRepository rollbackRepository,
            NetworkChangePlanPreconditionRepository preconditionRepository,
            NetworkChangePlanOperationDependencyRepository dependencyRepository,
            ExecutionReadinessAssessmentRepository assessmentRepository,
            ChangePlanValidityService validityService,
            ChangePlanPreconditionService preconditionService,
            ChangePlanSafetyService safetyService,
            ChangePlanAuditService auditService,
            ChangePlanMetrics metrics,
            Clock clock
    ) {
        this.planRepository = planRepository;
        this.operationRepository = operationRepository;
        this.rollbackRepository = rollbackRepository;
        this.preconditionRepository = preconditionRepository;
        this.dependencyRepository = dependencyRepository;
        this.assessmentRepository = assessmentRepository;
        this.validityService = validityService;
        this.preconditionService = preconditionService;
        this.safetyService = safetyService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    public record ReadinessOutcome(
            ExecutionReadinessResult result,
            NetworkChangePlanEntity plan,
            ExecutionReadinessAssessmentEntity assessment
    ) {
    }

    @Transactional
    public ReadinessOutcome evaluate(UUID planId) {
        Instant now = clock.instant();
        metrics.incrementReadinessChecks();
        NetworkChangePlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new DomainNotFoundException("changePlan", planId.toString()));
        if (!READINESS_ELIGIBLE.contains(plan.getStatus())) {
            throw new ChangePlanException(ChangePlanFailureCode.INVALID_PLAN_STATE, plan.getStatus());
        }
        boolean wasReady = PlanStatus.READY_FOR_EXECUTION.name().equals(plan.getStatus());

        ChangePlanValidityService.ValidityResult validity = validityService.revalidate(plan);
        if (!validity.valid()) {
            throw new ChangePlanException(validity.failureCode(), validity.reason());
        }
        plan = planRepository.findById(planId).orElseThrow();
        if (PlanStatus.INVALIDATED.name().equals(plan.getStatus())) {
            throw new ChangePlanException(validity.failureCode() == null
                    ? ChangePlanFailureCode.PLAN_CURRENT_VALUE_MISMATCH
                    : validity.failureCode(), "invalidated");
        }

        List<String> reasonCodes = new ArrayList<>();
        ExecutionReadinessResult result = evaluateReadiness(plan, now, reasonCodes);
        ExecutionReadinessAssessmentEntity assessment = assessmentRepository.save(
                ExecutionReadinessAssessmentEntity.create(
                        UUID.randomUUID(),
                        planId,
                        now,
                        result.name(),
                        plan.getFingerprint(),
                        String.join(",", reasonCodes),
                        null
                )
        );
        auditService.append(planId, "PLAN_READINESS_EVALUATED", "system", result.name());
        try {
            if (result == ExecutionReadinessResult.READY) {
                if (!wasReady) {
                    plan.markReadyForExecution(now);
                }
                planRepository.save(plan);
                metrics.incrementReadinessReady();
                auditService.append(planId, "PLAN_READY", "system", plan.getFingerprint());
            } else {
                if (wasReady) {
                    plan.revertToAuthorized();
                    planRepository.save(plan);
                }
                metrics.incrementReadinessNotReady();
                auditService.append(planId, "PLAN_NOT_READY", "system", String.join(",", reasonCodes));
            }
        } catch (OptimisticLockingFailureException ex) {
            throw new ChangePlanException(ChangePlanFailureCode.CONCURRENT_PLAN_CONFLICT, "concurrent readiness conflict");
        }
        return new ReadinessOutcome(result, planRepository.findById(planId).orElseThrow(), assessment);
    }

    private ExecutionReadinessResult evaluateReadiness(
            NetworkChangePlanEntity plan,
            Instant now,
            List<String> reasonCodes
    ) {
        if (plan.getExpiresAt() != null && now.isAfter(plan.getExpiresAt())) {
            reasonCodes.add(ChangePlanFailureCode.PLAN_EXPIRED.name());
            return ExecutionReadinessResult.NOT_READY;
        }

        List<NetworkChangePlanOperationEntity> operations =
                operationRepository.findByPlanIdOrderBySequenceNumberAsc(plan.getId());
        List<NetworkChangePlanRollbackOperationEntity> rollbacks =
                rollbackRepository.findByPlanIdOrderBySequenceNumberAsc(plan.getId());
        ParameterChangeIntent intent = new ParameterChangeIntent(
                plan.getTargetEntityType(),
                plan.getTargetEntityId(),
                plan.getParameterName(),
                plan.getExpectedCurrentValue(),
                plan.getDesiredValue()
        );
        ChangePlanPreconditionService.EvaluationContext context = new ChangePlanPreconditionService.EvaluationContext(
                plan,
                intent,
                operations,
                dependencyRepository.findByPlanId(plan.getId()),
                rollbacks.isEmpty() ? null : rollbacks.get(0),
                true
        );

        List<NetworkChangePlanPreconditionEntity> preconditionEntities =
                preconditionRepository.findByPlanIdOrderBySequenceNumberAsc(plan.getId());
        List<ChangePlanPreconditionService.PreconditionEvaluation> evaluations =
                preconditionService.evaluateAtReadiness(context, now);
        preconditionService.applyEvaluations(preconditionEntities, evaluations, now);
        preconditionRepository.saveAll(preconditionEntities);

        ExecutionReadinessResult preconditionResult = aggregatePreconditionResults(evaluations, reasonCodes);
        if (preconditionResult != ExecutionReadinessResult.READY) {
            return preconditionResult;
        }

        ChangePlanSafetyService.SafetyEvaluation safety = safetyService.evaluateCreation(intent);
        if (!safety.pass()) {
            reasonCodes.add(safety.failureCode().name());
            return ExecutionReadinessResult.NOT_READY;
        }
        return ExecutionReadinessResult.READY;
    }

    private ExecutionReadinessResult aggregatePreconditionResults(
            List<ChangePlanPreconditionService.PreconditionEvaluation> evaluations,
            List<String> reasonCodes
    ) {
        ExecutionReadinessResult worst = ExecutionReadinessResult.READY;
        for (ChangePlanPreconditionService.PreconditionEvaluation evaluation : evaluations) {
            if (evaluation.result().countsAsPass()) {
                continue;
            }
            if (evaluation.reasonCode() != null) {
                reasonCodes.add(evaluation.reasonCode());
            } else {
                reasonCodes.add(evaluation.type().name() + "_" + evaluation.result().name());
            }
            worst = mergeReadinessResult(worst, mapPreconditionResult(evaluation.result()));
        }
        return worst;
    }

    private ExecutionReadinessResult mapPreconditionResult(PreconditionResult result) {
        return switch (result) {
            case PASS -> ExecutionReadinessResult.READY;
            case STALE -> ExecutionReadinessResult.STALE;
            case UNKNOWN -> ExecutionReadinessResult.UNKNOWN;
            case FAIL -> ExecutionReadinessResult.NOT_READY;
        };
    }

    private ExecutionReadinessResult mergeReadinessResult(
            ExecutionReadinessResult current,
            ExecutionReadinessResult candidate
    ) {
        if (current == ExecutionReadinessResult.READY) {
            return candidate;
        }
        if (candidate == ExecutionReadinessResult.UNKNOWN) {
            return ExecutionReadinessResult.UNKNOWN;
        }
        if (candidate == ExecutionReadinessResult.STALE && current != ExecutionReadinessResult.UNKNOWN) {
            return ExecutionReadinessResult.STALE;
        }
        return current;
    }
}
