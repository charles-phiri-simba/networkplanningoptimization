package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.adapter.spi.MutationResult;
import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.AttemptOutcome;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.domain.VerificationOutcome;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.metrics.ExecutionMetrics;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionOperationRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class NetworkChangeExecutionService {

    private final ChangeExecutionProperties properties;
    private final NetworkChangeExecutionRepository executionRepository;
    private final NetworkChangeExecutionOperationRepository operationRepository;
    private final ExecutionAdmissionService admissionService;
    private final ExecutionReviewService reviewService;
    private final ExecutionAuthorizationService authorizationService;
    private final ExecutionFinalPreflightService finalPreflightService;
    private final ChangeOperationExecutionService operationExecutionService;
    private final ExecutionVerificationService verificationService;
    private final ExecutionLeaseService leaseService;
    private final ExecutionRecoveryService recoveryService;
    private final ExecutionFailurePersistenceService failurePersistenceService;
    private final RollbackReviewService rollbackReviewService;
    private final RollbackAuthorizationService rollbackAuthorizationService;
    private final RollbackExecutionService rollbackExecutionService;
    private final ExecutionAuditService auditService;
    private final ExecutionMetrics metrics;
    private final Clock clock;

    public NetworkChangeExecutionService(
            ChangeExecutionProperties properties,
            NetworkChangeExecutionRepository executionRepository,
            NetworkChangeExecutionOperationRepository operationRepository,
            ExecutionAdmissionService admissionService,
            ExecutionReviewService reviewService,
            ExecutionAuthorizationService authorizationService,
            ExecutionFinalPreflightService finalPreflightService,
            ChangeOperationExecutionService operationExecutionService,
            ExecutionVerificationService verificationService,
            ExecutionLeaseService leaseService,
            ExecutionRecoveryService recoveryService,
            ExecutionFailurePersistenceService failurePersistenceService,
            RollbackReviewService rollbackReviewService,
            RollbackAuthorizationService rollbackAuthorizationService,
            RollbackExecutionService rollbackExecutionService,
            ExecutionAuditService auditService,
            ExecutionMetrics metrics,
            Clock clock
    ) {
        this.properties = properties;
        this.executionRepository = executionRepository;
        this.operationRepository = operationRepository;
        this.admissionService = admissionService;
        this.reviewService = reviewService;
        this.authorizationService = authorizationService;
        this.finalPreflightService = finalPreflightService;
        this.operationExecutionService = operationExecutionService;
        this.verificationService = verificationService;
        this.leaseService = leaseService;
        this.recoveryService = recoveryService;
        this.failurePersistenceService = failurePersistenceService;
        this.rollbackReviewService = rollbackReviewService;
        this.rollbackAuthorizationService = rollbackAuthorizationService;
        this.rollbackExecutionService = rollbackExecutionService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangeExecutionEntity requestExecution(UUID planId, String executionTargetId, String requestedBy) {
        NetworkChangeExecutionEntity execution = admissionService.admit(planId, executionTargetId, requestedBy);
        auditService.append(execution.getId(), ExecutionAuditEventType.EXECUTION_REQUESTED.name(), requestedBy, planId.toString());
        return execution;
    }

    @Transactional
    public NetworkChangeExecutionEntity review(UUID executionId, String reviewer, String comment) {
        return reviewService.review(executionId, reviewer, comment);
    }

    @Transactional
    public NetworkChangeExecutionEntity authorize(UUID executionId, String authorizer) {
        return authorizationService.authorize(executionId, authorizer);
    }

    public NetworkChangeExecutionEntity execute(UUID executionId) {
        Instant now = clock.instant();
        NetworkChangeExecutionEntity execution = requireExecutable(executionId);
        if (ExecutionStatus.VERIFIED.name().equals(execution.getStatus())) {
            return execution;
        }
        ExecutionLeaseService.ExecutionLease lease = leaseService.acquire(
                execution.getExecutionTargetId(),
                execution.getCellId(),
                execution.getParameterName(),
                execution.getId()
        ).orElseThrow(() -> new ChangeExecutionException(ExecutionFailureCode.EXECUTION_LEASE_UNAVAILABLE, "lease unavailable"));
        try {
            leaseService.assertOwnership(lease);
            execution.markLeaseAcquired(lease.leaseKey(), lease.fencingToken(), now);
            execution.setStatus(ExecutionStatus.FINAL_PREFLIGHT_CHECKING.name());
            execution = executionRepository.save(execution);
            finalPreflightService.runForwardPreflight(execution);
            execution.markExecuting(now);
            execution = executionRepository.save(execution);
            ChangeOperationExecutionService.MutationAttemptResult attemptResult = operationExecutionService.executeForward(execution);
            MutationResult mutation = attemptResult.mutationResult();
            handleForwardMutationResult(execution, mutation, attemptResult.attemptId(), now);
            return executionRepository.findById(executionId).orElseThrow();
        } catch (OptimisticLockingFailureException ex) {
            throw new ChangeExecutionException(
                    ExecutionFailureCode.CONCURRENT_EXECUTION_CONFLICT,
                    "concurrent execute conflict: " + ex.getMessage());
        } finally {
            leaseService.release(lease);
            executionRepository.findById(executionId).ifPresent(entity -> {
                entity.clearLease();
                executionRepository.save(entity);
            });
        }
    }

    public NetworkChangeExecutionEntity verify(UUID executionId) {
        Instant now = clock.instant();
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        boolean ambiguousForwardOutcome =
                ExecutionStatus.EXECUTION_OUTCOME_UNKNOWN.name().equals(execution.getStatus());
        if (ExecutionStatus.ROLLBACK_OUTCOME_UNKNOWN.name().equals(execution.getStatus())
                || ExecutionStatus.ROLLBACK_APPLIED.name().equals(execution.getStatus())) {
            return rollbackExecutionService.verifyRollbackOutcome(executionId);
        }
        if (!ExecutionStatus.valueOf(execution.getStatus()).allowsVerify()) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, execution.getStatus());
        }
        if (execution.getLeaseKey() != null && execution.getFencingToken() != null) {
            ExecutionLeaseService.ExecutionLease lease = new ExecutionLeaseService.ExecutionLease(
                    execution.getLeaseKey(),
                    execution.getExecutionTargetId(),
                    execution.getCellId(),
                    execution.getParameterName(),
                    execution.getId(),
                    properties.getInstanceId(),
                    execution.getFencingToken(),
                    now,
                    now,
                    now.plus(properties.getLeaseDuration())
            );
            leaseService.assertOwnership(lease);
        }
        execution.markVerifying(now);
        execution = executionRepository.save(execution);
        ExecutionVerificationService.VerificationResult verification =
                verificationService.verifyForward(execution, null);
        return applyForwardVerification(execution, verification, now, ambiguousForwardOutcome);
    }

    @Transactional
    public NetworkChangeExecutionEntity cancel(UUID executionId, String actor, String reason) {
        Instant now = clock.instant();
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        if (!ExecutionStatus.valueOf(execution.getStatus()).allowsCancelBeforeMutation()) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, execution.getStatus());
        }
        execution.markCancelled(reason, now);
        executionRepository.save(execution);
        auditService.append(executionId, ExecutionAuditEventType.EXECUTION_CANCELLED.name(), actor, reason);
        return execution;
    }

    @Transactional
    public NetworkChangeExecutionEntity requestRollback(UUID executionId, String requester) {
        rollbackReviewService.requestRollback(executionId, requester);
        return executionRepository.findById(executionId).orElseThrow();
    }

    @Transactional
    public NetworkChangeExecutionEntity reviewRollback(UUID executionId, String reviewer, String comment) {
        rollbackReviewService.reviewRollback(executionId, reviewer, comment);
        return executionRepository.findById(executionId).orElseThrow();
    }

    @Transactional
    public NetworkChangeExecutionEntity authorizeRollback(UUID executionId, String authorizer) {
        rollbackAuthorizationService.authorizeRollback(executionId, authorizer);
        return executionRepository.findById(executionId).orElseThrow();
    }

    @Transactional
    public NetworkChangeExecutionEntity executeRollback(UUID executionId) {
        return rollbackExecutionService.executeRollback(executionId);
    }

    private NetworkChangeExecutionEntity requireExecutable(UUID executionId) {
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        if (!ExecutionStatus.valueOf(execution.getStatus()).allowsExecute()
                && !ExecutionStatus.VERIFIED.name().equals(execution.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, execution.getStatus());
        }
        return execution;
    }

    private void handleForwardMutationResult(
            NetworkChangeExecutionEntity execution,
            MutationResult mutation,
            UUID attemptId,
            Instant now
    ) {
        UUID executionId = execution.getId();
        if (mutation.outcome() == AttemptOutcome.REJECTED) {
            persistFailureIndependently(() -> failurePersistenceService.persistExecutionFailed(
                    executionId,
                    mutation.failureCode(),
                    mutation.failureDetailSafe(),
                    now
            ));
            return;
        }
        if (mutation.outcome() == AttemptOutcome.TIMEOUT) {
            persistFailureIndependently(() -> failurePersistenceService.persistExecutionFailed(
                    executionId,
                    mutation.failureCode(),
                    mutation.failureDetailSafe(),
                    now
            ));
            return;
        }
        if (mutation.outcome() == AttemptOutcome.OUTCOME_UNKNOWN) {
            persistFailureIndependently(() -> failurePersistenceService.persistOutcomeUnknown(
                    executionId,
                    mutation.failureCode(),
                    mutation.failureDetailSafe(),
                    now
            ));
            return;
        }
        execution.markApplied(now);
        execution = executionRepository.save(execution);
        auditService.append(execution.getId(), ExecutionAuditEventType.MUTATION_APPLIED.name(), "system", null);
        ExecutionVerificationService.VerificationResult verification =
                verificationService.verifyForward(execution, attemptId);
        applyForwardVerification(execution, verification, now, false);
    }

    private NetworkChangeExecutionEntity applyForwardVerification(
            NetworkChangeExecutionEntity execution,
            ExecutionVerificationService.VerificationResult verification,
            Instant now,
            boolean ambiguousForwardOutcome
    ) {
        if (verification.verified()) {
            NetworkChangeExecutionEntity current = executionRepository.findById(execution.getId()).orElseThrow();
            current.markVerified(VerificationOutcome.VERIFIED.name(), now);
            executionRepository.save(current);
            metrics.incrementVerified();
            auditService.append(execution.getId(), ExecutionAuditEventType.VERIFICATION_SUCCEEDED.name(), "system", null);
            auditService.append(execution.getId(), ExecutionAuditEventType.EXECUTION_COMPLETED.name(), "system", null);
            return execution;
        }
        if (verification.observation().outcome() == VerificationOutcome.MISMATCH) {
            if (ambiguousForwardOutcome) {
                String expected = operationRepository
                        .findByExecutionIdOrderBySequenceNumberAsc(execution.getId())
                        .get(0).getExpectedCurrentValue();
                String detail = valuesEqual(verification.observation().observedValue(), expected)
                        ? "ambiguous forward outcome resolved to pre-change value; automatic retry prohibited"
                        : "ambiguous forward outcome resolved to a third value";
                persistFailureIndependently(() -> failurePersistenceService.persistManualIntervention(
                        execution.getId(),
                        detail,
                        now
                ));
                return executionRepository.findById(execution.getId()).orElseThrow();
            }
            persistFailureIndependently(() -> failurePersistenceService.persistVerificationFailed(
                    execution.getId(),
                    "EXECUTION_VERIFICATION_MISMATCH",
                    verification.observation().evidenceSummary(),
                    now
            ));
            recoveryService.evaluateRecovery(
                    executionRepository.findById(execution.getId()).orElseThrow(),
                    "EXECUTION_VERIFICATION_MISMATCH"
            );
            return executionRepository.findById(execution.getId()).orElseThrow();
        }
        persistFailureIndependently(() -> failurePersistenceService.persistVerificationFailed(
                execution.getId(),
                verification.observation().reasonCode() == null
                        ? "EXECUTION_VERIFICATION_UNKNOWN"
                        : verification.observation().reasonCode(),
                verification.observation().evidenceSummary(),
                now
        ));
        return executionRepository.findById(execution.getId()).orElseThrow();
    }

    private void persistFailureIndependently(Runnable persistAction) {
        persistAction.run();
    }

    private boolean valuesEqual(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        try {
            return new java.math.BigDecimal(left.strip())
                    .compareTo(new java.math.BigDecimal(right.strip())) == 0;
        } catch (NumberFormatException ex) {
            return left.strip().equals(right.strip());
        }
    }
}
