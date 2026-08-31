package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.target.ExecutionTargetDescriptor;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionRollbackEntity;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.metrics.ExecutionMetrics;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionOperationRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRollbackEntityRepository;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExecutionAdmissionService {

    private final ChangeExecutionProperties properties;
    private final ExecutionValidityService validityService;
    private final ExecutionTargetRegistry targetRegistry;
    private final ExecutionFingerprintService fingerprintService;
    private final NetworkChangeExecutionRepository executionRepository;
    private final NetworkChangeExecutionOperationRepository operationRepository;
    private final NetworkChangeExecutionRollbackEntityRepository rollbackRepository;
    private final ExecutionAuditService auditService;
    private final ExecutionMetrics metrics;
    private final Clock clock;

    public ExecutionAdmissionService(
            ChangeExecutionProperties properties,
            ExecutionValidityService validityService,
            ExecutionTargetRegistry targetRegistry,
            ExecutionFingerprintService fingerprintService,
            NetworkChangeExecutionRepository executionRepository,
            NetworkChangeExecutionOperationRepository operationRepository,
            NetworkChangeExecutionRollbackEntityRepository rollbackRepository,
            ExecutionAuditService auditService,
            ExecutionMetrics metrics,
            Clock clock
    ) {
        this.properties = properties;
        this.validityService = validityService;
        this.targetRegistry = targetRegistry;
        this.fingerprintService = fingerprintService;
        this.executionRepository = executionRepository;
        this.operationRepository = operationRepository;
        this.rollbackRepository = rollbackRepository;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangeExecutionEntity admit(UUID planId, String executionTargetId, String requestedBy) {
        if (!properties.isEnabled()) {
            throw new ChangeExecutionException(ExecutionFailureCode.CHANGE_EXECUTION_DISABLED, "disabled");
        }
        metrics.incrementRequested();
        Instant now = clock.instant();
        ExecutionValidityService.PlanContext planContext = validityService.requireReadyPlan(planId);
        NetworkChangePlanEntity plan = planContext.plan();
        validityService.validateScope(plan, properties);
        ExecutionTargetDescriptor target = validityService.requireTargetBinding(executionTargetId, targetRegistry, properties);
        validityService.ensureNoActiveExecutionConflict(
                executionRepository,
                planId,
                target.targetId(),
                plan.getTargetEntityId(),
                plan.getParameterName()
        );
        if (planContext.planOperations().size() != 1) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_TARGET_CAPABILITY_MISSING, "single operation required");
        }
        if (planContext.rollback() == null) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_PLAN_NOT_READY, "rollback required");
        }
        UUID executionId = UUID.randomUUID();
        List<NetworkChangeExecutionOperationEntity> operations = validityService.toExecutionOperations(
                executionId,
                planContext.planOperations(),
                now
        );
        String fingerprint = fingerprintService.compute(new ExecutionFingerprintService.FingerprintInput(
                plan,
                target,
                operations,
                planContext.rollback(),
                null,
                null
        ));
        NetworkChangeExecutionEntity execution = NetworkChangeExecutionEntity.createRequested(
                executionId,
                plan.getId(),
                plan.getPlanVersion(),
                plan.getFingerprint(),
                target.targetId(),
                target.targetType().name(),
                target.environment().name(),
                target.adapterProfileId(),
                target.capabilityProfileVersion(),
                plan.getTargetEntityId(),
                plan.getParameterName(),
                fingerprint,
                requestedBy,
                now
        );
        execution.setStatus("PRELIMINARY_ADMISSION_CHECKING");
        executionRepository.save(execution);
        operationRepository.saveAll(operations);
        rollbackRepository.save(NetworkChangeExecutionRollbackEntity.createPending(UUID.randomUUID(), executionId));
        execution.markAdmitted(now);
        executionRepository.save(execution);
        metrics.incrementAdmitted();
        auditService.append(executionId, ExecutionAuditEventType.PRELIMINARY_ADMISSION_PASSED.name(), requestedBy, fingerprint);
        return execution;
    }
}
