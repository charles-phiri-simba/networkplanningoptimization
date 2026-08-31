package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.AuthorizationType;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAuthorizationEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionOperationRepository;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionAuthorizationRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRollbackOperationRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ExecutionAuthorizationService {

    private final ChangeExecutionProperties properties;
    private final NetworkChangeExecutionRepository executionRepository;
    private final NetworkChangeExecutionOperationRepository operationRepository;
    private final NetworkChangeExecutionAuthorizationRepository authorizationRepository;
    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangePlanRollbackOperationRepository rollbackRepository;
    private final ExecutionTargetRegistry targetRegistry;
    private final ExecutionFingerprintService fingerprintService;
    private final ExecutionAuditService auditService;
    private final Clock clock;

    public ExecutionAuthorizationService(
            ChangeExecutionProperties properties,
            NetworkChangeExecutionRepository executionRepository,
            NetworkChangeExecutionOperationRepository operationRepository,
            NetworkChangeExecutionAuthorizationRepository authorizationRepository,
            NetworkChangePlanRepository planRepository,
            NetworkChangePlanRollbackOperationRepository rollbackRepository,
            ExecutionTargetRegistry targetRegistry,
            ExecutionFingerprintService fingerprintService,
            ExecutionAuditService auditService,
            Clock clock
    ) {
        this.properties = properties;
        this.executionRepository = executionRepository;
        this.operationRepository = operationRepository;
        this.authorizationRepository = authorizationRepository;
        this.planRepository = planRepository;
        this.rollbackRepository = rollbackRepository;
        this.targetRegistry = targetRegistry;
        this.fingerprintService = fingerprintService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangeExecutionEntity authorize(UUID executionId, String authorizer) {
        if (properties.isRequireExecutionAuthorization()) {
            // mandatory authorization enforced by controller permission
        }
        Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        NetworkChangeExecutionEntity execution = requireAuthorizable(executionId);
        if (!ExecutionStatus.READY_FOR_EXECUTION_AUTHORIZATION.name().equals(execution.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, execution.getStatus());
        }
        if (execution.getReviewedAt() == null) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, "review required");
        }
        Instant windowOpens = now;
        Instant windowCloses = now.plus(properties.getExecutionWindowDuration());
        var plan = planRepository.findById(execution.getPlanId())
                .orElseThrow(() -> new DomainNotFoundException("changePlan", execution.getPlanId().toString()));
        var target = targetRegistry.require(execution.getExecutionTargetId());
        var rollback = rollbackRepository.findByPlanIdOrderBySequenceNumberAsc(execution.getPlanId())
                .stream().findFirst().orElse(null);
        String currentFingerprint = fingerprintService.compute(new ExecutionFingerprintService.FingerprintInput(
                plan,
                target,
                operationRepository.findByExecutionIdOrderBySequenceNumberAsc(executionId),
                rollback,
                windowOpens,
                windowCloses
        ));
        try {
            execution.setExecutionFingerprint(currentFingerprint);
            execution.markAuthorized(authorizer, currentFingerprint, windowOpens, windowCloses, now);
            executionRepository.save(execution);
            authorizationRepository.save(NetworkChangeExecutionAuthorizationEntity.create(
                    UUID.randomUUID(),
                    executionId,
                    AuthorizationType.EXECUTION.name(),
                    authorizer,
                    execution.getExecutionFingerprint(),
                    execution.getVersion(),
                    now
            ));
            auditService.append(executionId, ExecutionAuditEventType.EXECUTION_AUTHORIZED.name(), authorizer, execution.getExecutionFingerprint());
            return execution;
        } catch (OptimisticLockingFailureException ex) {
            throw new ChangeExecutionException(ExecutionFailureCode.CONCURRENT_EXECUTION_CONFLICT, "concurrent authorize conflict");
        }
    }

    public void requireCurrentAuthorization(NetworkChangeExecutionEntity execution) {
        if (execution.getAuthorizedExecutionFingerprint() == null) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_AUTHORIZATION_MISSING, "authorization missing");
        }
        if (!execution.getAuthorizedExecutionFingerprint().equals(execution.getExecutionFingerprint())) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_AUTHORIZATION_STALE, "authorization stale");
        }
    }

    private NetworkChangeExecutionEntity requireAuthorizable(UUID executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
    }
}
