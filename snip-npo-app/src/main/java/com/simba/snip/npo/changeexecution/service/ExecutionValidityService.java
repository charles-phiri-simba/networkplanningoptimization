package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.domain.target.ExecutionTargetDescriptor;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanOperationRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRollbackOperationRepository;
import com.simba.snip.npo.changeplanning.service.ChangePlanReadinessService;
import com.simba.snip.npo.changeplanning.service.ChangePlanValidityService;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ExecutionValidityService {

    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangePlanOperationRepository planOperationRepository;
    private final NetworkChangePlanRollbackOperationRepository rollbackRepository;
    private final ChangePlanValidityService planValidityService;
    private final ChangePlanReadinessService readinessService;

    public ExecutionValidityService(
            NetworkChangePlanRepository planRepository,
            NetworkChangePlanOperationRepository planOperationRepository,
            NetworkChangePlanRollbackOperationRepository rollbackRepository,
            ChangePlanValidityService planValidityService,
            ChangePlanReadinessService readinessService
    ) {
        this.planRepository = planRepository;
        this.planOperationRepository = planOperationRepository;
        this.rollbackRepository = rollbackRepository;
        this.planValidityService = planValidityService;
        this.readinessService = readinessService;
    }

    public record PlanContext(
            NetworkChangePlanEntity plan,
            List<NetworkChangePlanOperationEntity> planOperations,
            NetworkChangePlanRollbackOperationEntity rollback
    ) {
    }

    public PlanContext requireReadyPlan(UUID planId) {
        NetworkChangePlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new DomainNotFoundException("changePlan", planId.toString()));
        if (!PlanStatus.READY_FOR_EXECUTION.name().equals(plan.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_PLAN_NOT_READY, plan.getStatus());
        }
        ChangePlanValidityService.ValidityResult validity = planValidityService.revalidate(plan);
        if (!validity.valid()) {
            throw new ChangeExecutionException(mapPlanFailure(validity.failureCode()), validity.reason());
        }
        readinessService.evaluate(planId);
        NetworkChangePlanEntity refreshed = planRepository.findById(planId)
                .orElseThrow(() -> new DomainNotFoundException("changePlan", planId.toString()));
        if (!PlanStatus.READY_FOR_EXECUTION.name().equals(refreshed.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_PLAN_NOT_READY, refreshed.getStatus());
        }
        List<NetworkChangePlanOperationEntity> planOperations =
                planOperationRepository.findByPlanIdOrderBySequenceNumberAsc(planId);
        NetworkChangePlanRollbackOperationEntity rollback =
                rollbackRepository.findByPlanIdOrderBySequenceNumberAsc(planId).stream().findFirst().orElse(null);
        return new PlanContext(refreshed, planOperations, rollback);
    }

    public void validateScope(NetworkChangePlanEntity plan, ChangeExecutionProperties properties) {
        if (!"CELL".equals(plan.getTargetEntityType())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_TARGET_CAPABILITY_MISSING, "CELL only");
        }
        if (!SimulatableParameterRegistry.TX_POWER.equals(plan.getParameterName())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_TARGET_CAPABILITY_MISSING, "txPower only");
        }
        if (properties.getMaximumOperationCount() != 1) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_TARGET_CAPABILITY_MISSING, "single operation");
        }
    }

    public void ensureNoActiveExecutionConflict(NetworkChangeExecutionRepository executionRepository, UUID planId, String targetId, String cellId, String parameter) {
        executionRepository.findFirstByPlanIdAndStatusInOrderByCreatedAtDesc(planId, ExecutionStatus.ACTIVE_NAMES)
                .ifPresent(existing -> {
                    throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_CONFLICT, existing.getId().toString());
                });
        executionRepository.findFirstByExecutionTargetIdAndCellIdAndParameterNameAndStatusInOrderByCreatedAtDesc(
                targetId, cellId, parameter, ExecutionStatus.ACTIVE_NAMES
        ).ifPresent(existing -> {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_CONFLICT, existing.getId().toString());
        });
    }

    public ExecutionFailureCode mapPlanFailure(com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode code) {
        return switch (code) {
            case PLAN_EXPIRED -> ExecutionFailureCode.EXECUTION_PLAN_EXPIRED;
            case INVALID_PLAN_STATE -> ExecutionFailureCode.EXECUTION_PLAN_CANCELLED;
            case PLAN_PROPOSAL_INVALID, PLAN_PROPOSAL_NOT_APPROVED -> ExecutionFailureCode.EXECUTION_PLAN_INVALIDATED;
            case PLAN_CURRENT_VALUE_MISMATCH -> ExecutionFailureCode.EXECUTION_CURRENT_VALUE_MISMATCH;
            case PLAN_NETWORK_KNOWLEDGE_LOW -> ExecutionFailureCode.EXECUTION_KNOWLEDGE_LOW;
            case PLAN_NETWORK_KNOWLEDGE_UNKNOWN -> ExecutionFailureCode.EXECUTION_KNOWLEDGE_UNKNOWN;
            case PLAN_RELEVANT_DRIFT_PRESENT -> ExecutionFailureCode.EXECUTION_RELEVANT_DRIFT_PRESENT;
            case PLAN_AUTHORIZATION_MISSING -> ExecutionFailureCode.EXECUTION_AUTHORIZATION_MISSING;
            case PLAN_AUTHORIZATION_STALE -> ExecutionFailureCode.EXECUTION_AUTHORIZATION_STALE;
            default -> ExecutionFailureCode.EXECUTION_PLAN_NOT_READY;
        };
    }

    public List<NetworkChangeExecutionOperationEntity> toExecutionOperations(
            UUID executionId,
            List<NetworkChangePlanOperationEntity> planOperations,
            java.time.Instant now
    ) {
        return planOperations.stream()
                .map(op -> NetworkChangeExecutionOperationEntity.create(
                        UUID.randomUUID(),
                        executionId,
                        op.getSequenceNumber(),
                        op.getOperationType(),
                        op.getTargetEntityType(),
                        op.getTargetEntityId(),
                        op.getParameterName(),
                        op.getExpectedCurrentValue(),
                        op.getDesiredValue(),
                        now
                ))
                .toList();
    }

    public ExecutionTargetDescriptor requireTargetBinding(String executionTargetId, ExecutionTargetRegistry registry, ChangeExecutionProperties properties) {
        ExecutionTargetDescriptor descriptor = registry.require(executionTargetId);
        registry.requirePermitted(descriptor, properties);
        return descriptor;
    }
}
