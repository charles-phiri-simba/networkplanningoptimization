package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.AuthorizationType;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.domain.target.ExecutionTargetDescriptor;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAuthorizationEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionRollbackEntity;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionAuthorizationRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRollbackEntityRepository;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRollbackOperationRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RollbackAuthorizationService {

    private final ChangeExecutionProperties properties;
    private final NetworkChangeExecutionRepository executionRepository;
    private final NetworkChangeExecutionRollbackEntityRepository rollbackRepository;
    private final NetworkChangeExecutionAuthorizationRepository authorizationRepository;
    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangePlanRollbackOperationRepository planRollbackRepository;
    private final ExecutionTargetRegistry targetRegistry;
    private final ExecutionFingerprintService fingerprintService;
    private final ExecutionAuditService auditService;
    private final Clock clock;

    public RollbackAuthorizationService(
            ChangeExecutionProperties properties,
            NetworkChangeExecutionRepository executionRepository,
            NetworkChangeExecutionRollbackEntityRepository rollbackRepository,
            NetworkChangeExecutionAuthorizationRepository authorizationRepository,
            NetworkChangePlanRepository planRepository,
            NetworkChangePlanRollbackOperationRepository planRollbackRepository,
            ExecutionTargetRegistry targetRegistry,
            ExecutionFingerprintService fingerprintService,
            ExecutionAuditService auditService,
            Clock clock
    ) {
        this.properties = properties;
        this.executionRepository = executionRepository;
        this.rollbackRepository = rollbackRepository;
        this.authorizationRepository = authorizationRepository;
        this.planRepository = planRepository;
        this.planRollbackRepository = planRollbackRepository;
        this.targetRegistry = targetRegistry;
        this.fingerprintService = fingerprintService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangeExecutionRollbackEntity authorizeRollback(UUID executionId, String authorizer) {
        if (properties.isRequireRollbackAuthorization()) {
            // enforced by controller permission
        }
        Instant now = clock.instant();
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        NetworkChangeExecutionRollbackEntity rollback = rollbackRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, "rollback record missing"));
        if (!"REVIEWED".equals(rollback.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.ROLLBACK_AUTHORIZATION_MISSING, rollback.getStatus());
        }
        NetworkChangePlanEntity plan = planRepository.findById(execution.getPlanId())
                .orElseThrow(() -> new DomainNotFoundException("changePlan", execution.getPlanId().toString()));
        NetworkChangePlanRollbackOperationEntity planRollback = planRollbackRepository
                .findByPlanIdOrderBySequenceNumberAsc(plan.getId()).stream().findFirst()
                .orElseThrow(() -> new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, "plan rollback missing"));
        ExecutionTargetDescriptor target = targetRegistry.require(execution.getExecutionTargetId());
        String fingerprint = fingerprintService.computeRollbackFingerprint(executionId, plan, target, planRollback);
        rollback.markAuthorized(authorizer, fingerprint, now);
        rollbackRepository.save(rollback);
        execution.setStatus(ExecutionStatus.ROLLBACK_AUTHORIZED.name());
        execution.markRollbackStatus("AUTHORIZED");
        executionRepository.save(execution);
        authorizationRepository.save(NetworkChangeExecutionAuthorizationEntity.create(
                UUID.randomUUID(),
                executionId,
                AuthorizationType.ROLLBACK.name(),
                authorizer,
                fingerprint,
                execution.getVersion(),
                now
        ));
        auditService.append(executionId, ExecutionAuditEventType.ROLLBACK_AUTHORIZED.name(), authorizer, fingerprint);
        return rollback;
    }

    public void requireCurrentRollbackAuthorization(
            NetworkChangeExecutionEntity execution,
            NetworkChangeExecutionRollbackEntity rollback
    ) {
        if (rollback.getAuthorizedRollbackFingerprint() == null) {
            throw new ChangeExecutionException(ExecutionFailureCode.ROLLBACK_AUTHORIZATION_MISSING, "missing");
        }
        NetworkChangePlanEntity plan = planRepository.findById(execution.getPlanId())
                .orElseThrow(() -> new DomainNotFoundException("changePlan", execution.getPlanId().toString()));
        NetworkChangePlanRollbackOperationEntity planRollback = planRollbackRepository
                .findByPlanIdOrderBySequenceNumberAsc(plan.getId()).stream().findFirst()
                .orElseThrow(() -> new ChangeExecutionException(
                        ExecutionFailureCode.INVALID_EXECUTION_STATE, "plan rollback missing"));
        ExecutionTargetDescriptor target = targetRegistry.require(execution.getExecutionTargetId());
        String current = fingerprintService.computeRollbackFingerprint(
                execution.getId(), plan, target, planRollback);
        if (!current.equals(rollback.getAuthorizedRollbackFingerprint())) {
            throw new ChangeExecutionException(ExecutionFailureCode.ROLLBACK_AUTHORIZATION_STALE, "rollback authorization stale");
        }
    }
}
