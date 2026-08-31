package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.adapter.spi.AuthorizedRollbackOperation;
import com.simba.snip.npo.changeexecution.adapter.spi.MutationResult;
import com.simba.snip.npo.changeexecution.adapter.simulator.SimulatorExecutionStateStore;
import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.domain.AttemptOutcome;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.domain.VerificationOutcome;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionRollbackEntity;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.metrics.ExecutionMetrics;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRollbackEntityRepository;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRollbackOperationRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RollbackExecutionService {

    private final NetworkChangeExecutionRepository executionRepository;
    private final NetworkChangeExecutionRollbackEntityRepository rollbackRepository;
    private final NetworkChangePlanRollbackOperationRepository planRollbackRepository;
    private final RollbackAuthorizationService rollbackAuthorizationService;
    private final ChangeOperationExecutionService operationExecutionService;
    private final ExecutionVerificationService verificationService;
    private final ExecutionLeaseService leaseService;
    private final SimulatorExecutionStateStore simulatorStateStore;
    private final ExecutionFailurePersistenceService failurePersistenceService;
    private final ExecutionAuditService auditService;
    private final ExecutionMetrics metrics;
    private final Clock clock;

    public RollbackExecutionService(
            NetworkChangeExecutionRepository executionRepository,
            NetworkChangeExecutionRollbackEntityRepository rollbackRepository,
            NetworkChangePlanRollbackOperationRepository planRollbackRepository,
            RollbackAuthorizationService rollbackAuthorizationService,
            ChangeOperationExecutionService operationExecutionService,
            ExecutionVerificationService verificationService,
            ExecutionLeaseService leaseService,
            SimulatorExecutionStateStore simulatorStateStore,
            ExecutionFailurePersistenceService failurePersistenceService,
            ExecutionAuditService auditService,
            ExecutionMetrics metrics,
            Clock clock
    ) {
        this.executionRepository = executionRepository;
        this.rollbackRepository = rollbackRepository;
        this.planRollbackRepository = planRollbackRepository;
        this.rollbackAuthorizationService = rollbackAuthorizationService;
        this.operationExecutionService = operationExecutionService;
        this.verificationService = verificationService;
        this.leaseService = leaseService;
        this.simulatorStateStore = simulatorStateStore;
        this.failurePersistenceService = failurePersistenceService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    public NetworkChangeExecutionEntity executeRollback(UUID executionId) {
        Instant now = clock.instant();
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        if (!ExecutionStatus.ROLLBACK_AUTHORIZED.name().equals(execution.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.ROLLBACK_AUTHORIZATION_MISSING, execution.getStatus());
        }
        NetworkChangeExecutionRollbackEntity rollback = rollbackRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ChangeExecutionException(ExecutionFailureCode.ROLLBACK_AUTHORIZATION_MISSING, "rollback missing"));
        rollbackAuthorizationService.requireCurrentRollbackAuthorization(execution, rollback);
        NetworkChangePlanRollbackOperationEntity planRollback = planRollbackRepository
                .findByPlanIdOrderBySequenceNumberAsc(execution.getPlanId()).stream().findFirst()
                .orElseThrow(() -> new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, "plan rollback missing"));
        AuthorizedRollbackOperation authorizedRollback = new AuthorizedRollbackOperation(
                planRollback.getOperationType(),
                planRollback.getTargetEntityType(),
                planRollback.getTargetEntityId(),
                planRollback.getParameterName(),
                planRollback.getExpectedCurrentValue(),
                planRollback.getDesiredValue()
        );
        ExecutionLeaseService.ExecutionLease lease = leaseService.acquire(
                execution.getExecutionTargetId(),
                execution.getCellId(),
                execution.getParameterName(),
                execution.getId()
        ).orElseThrow(() -> new ChangeExecutionException(ExecutionFailureCode.EXECUTION_LEASE_UNAVAILABLE, "lease unavailable"));
        try {
            leaseService.assertOwnership(lease);
            assertRollbackCurrentMatches(execution, authorizedRollback);
            execution.markLeaseAcquired(lease.leaseKey(), lease.fencingToken(), now);
            execution.setStatus(ExecutionStatus.ROLLING_BACK.name());
            execution = executionRepository.save(execution);
            rollback.markExecuting(now);
            rollback = rollbackRepository.save(rollback);
            ChangeOperationExecutionService.MutationAttemptResult attemptResult =
                    operationExecutionService.executeRollback(execution, authorizedRollback);
            MutationResult mutation = attemptResult.mutationResult();
            if (mutation.outcome() == AttemptOutcome.REJECTED) {
                failurePersistenceService.persistManualIntervention(executionId, mutation.failureDetailSafe(), now);
                NetworkChangeExecutionRollbackEntity failedRollback = rollbackRepository.findByExecutionId(executionId).orElseThrow();
                failedRollback.markFailed(mutation.failureCode(), mutation.failureDetailSafe(), now);
                rollbackRepository.save(failedRollback);
                return executionRepository.findById(executionId).orElseThrow();
            }
            if (mutation.outcome() == AttemptOutcome.OUTCOME_UNKNOWN) {
                execution.setStatus(ExecutionStatus.ROLLBACK_OUTCOME_UNKNOWN.name());
                execution.markRollbackStatus("OUTCOME_UNKNOWN");
                executionRepository.save(execution);
                rollback.markOutcomeUnknown(mutation.failureCode(), mutation.failureDetailSafe(), now);
                rollbackRepository.save(rollback);
                auditService.append(executionId, ExecutionAuditEventType.ROLLBACK_OUTCOME_UNKNOWN.name(), "system", mutation.failureDetailSafe());
                return executionRepository.findById(executionId).orElseThrow();
            }
            rollback.markApplied(now);
            rollback = rollbackRepository.save(rollback);
            execution.setStatus(ExecutionStatus.ROLLBACK_APPLIED.name());
            execution.markRollbackStatus("APPLIED");
            execution = executionRepository.save(execution);
            auditService.append(executionId, ExecutionAuditEventType.ROLLBACK_APPLIED.name(), "system", null);
            ExecutionVerificationService.VerificationResult verification =
                    verificationService.verifyRollback(execution, attemptResult.attemptId(), authorizedRollback);
            if (verification.verified()) {
                execution.markRolledBack(now);
                executionRepository.save(execution);
                rollback.markVerified(now);
                rollbackRepository.save(rollback);
                metrics.incrementRollbackVerified();
                auditService.append(executionId, ExecutionAuditEventType.ROLLBACK_VERIFIED.name(), "system", null);
            } else if (verification.observation().outcome() == VerificationOutcome.MISMATCH) {
                failurePersistenceService.persistManualIntervention(executionId, "rollback verification mismatch", now);
                NetworkChangeExecutionRollbackEntity failedRollback = rollbackRepository.findByExecutionId(executionId).orElseThrow();
                failedRollback.markFailed("ROLLBACK_VERIFICATION_FAILED", "rollback verification mismatch", now);
                rollbackRepository.save(failedRollback);
            }
            return executionRepository.findById(executionId).orElseThrow();
        } finally {
            leaseService.release(lease);
            executionRepository.findById(executionId).ifPresent(entity -> {
                entity.clearLease();
                executionRepository.save(entity);
            });
        }
    }

    public NetworkChangeExecutionEntity verifyRollbackOutcome(UUID executionId) {
        Instant now = clock.instant();
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        if (!ExecutionStatus.ROLLBACK_OUTCOME_UNKNOWN.name().equals(execution.getStatus())
                && !ExecutionStatus.ROLLBACK_APPLIED.name().equals(execution.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, execution.getStatus());
        }
        NetworkChangeExecutionRollbackEntity rollback = rollbackRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ChangeExecutionException(
                        ExecutionFailureCode.INVALID_EXECUTION_STATE, "rollback record missing"));
        NetworkChangePlanRollbackOperationEntity planRollback = planRollbackRepository
                .findByPlanIdOrderBySequenceNumberAsc(execution.getPlanId()).stream().findFirst()
                .orElseThrow(() -> new ChangeExecutionException(
                        ExecutionFailureCode.INVALID_EXECUTION_STATE, "plan rollback missing"));
        AuthorizedRollbackOperation operation = new AuthorizedRollbackOperation(
                planRollback.getOperationType(),
                planRollback.getTargetEntityType(),
                planRollback.getTargetEntityId(),
                planRollback.getParameterName(),
                planRollback.getExpectedCurrentValue(),
                planRollback.getDesiredValue()
        );
        ExecutionVerificationService.VerificationResult verification =
                verificationService.verifyRollback(execution, null, operation);
        if (verification.verified()) {
            execution.markRolledBack(now);
            executionRepository.save(execution);
            rollback.markVerified(now);
            rollbackRepository.save(rollback);
            metrics.incrementRollbackVerified();
            auditService.append(executionId, ExecutionAuditEventType.ROLLBACK_VERIFIED.name(), "system", null);
        } else {
            failurePersistenceService.persistManualIntervention(
                    executionId,
                    "rollback outcome could not be verified safely: " + verification.observation().outcome(),
                    now
            );
            NetworkChangeExecutionRollbackEntity failed = rollbackRepository.findByExecutionId(executionId).orElseThrow();
            failed.markFailed(
                    verification.observation().reasonCode() == null
                            ? "ROLLBACK_VERIFICATION_FAILED"
                            : verification.observation().reasonCode(),
                    verification.observation().evidenceSummary(),
                    now
            );
            rollbackRepository.save(failed);
        }
        return executionRepository.findById(executionId).orElseThrow();
    }

    private void assertRollbackCurrentMatches(NetworkChangeExecutionEntity execution, AuthorizedRollbackOperation rollbackOperation) {
        simulatorStateStore.initializeIfAbsent(
                execution.getExecutionTargetId(),
                execution.getCellId(),
                rollbackOperation.parameterName(),
                rollbackOperation.expectedCurrentValue()
        );
        String actual = simulatorStateStore.read(
                execution.getExecutionTargetId(),
                execution.getCellId(),
                rollbackOperation.parameterName()
        ).map(SimulatorExecutionStateStore.CellState::value).orElse(null);
        if (actual == null || !valuesEqual(actual, rollbackOperation.expectedCurrentValue())) {
            throw new ChangeExecutionException(ExecutionFailureCode.ROLLBACK_CURRENT_VALUE_MISMATCH, actual);
        }
    }

    private boolean valuesEqual(String left, String right) {
        try {
            return new BigDecimal(left.strip()).compareTo(new BigDecimal(right.strip())) == 0;
        } catch (NumberFormatException ex) {
            return left.strip().equals(right.strip());
        }
    }
}
