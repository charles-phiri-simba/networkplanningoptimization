package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.adapter.spi.AuthorizedExecutionOperation;
import com.simba.snip.npo.changeexecution.adapter.spi.AuthorizedRollbackOperation;
import com.simba.snip.npo.changeexecution.adapter.spi.ExecutionContext;
import com.simba.snip.npo.changeexecution.adapter.spi.ExecutionMutationAdapter;
import com.simba.snip.npo.changeexecution.adapter.spi.MutationResult;
import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.AttemptDirection;
import com.simba.snip.npo.changeexecution.domain.AttemptOutcome;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAttemptEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.metrics.ExecutionMetrics;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionAttemptRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChangeOperationExecutionService {

    private final ChangeExecutionProperties properties;
    private final ExecutionTargetRegistry targetRegistry;
    private final NetworkChangeExecutionOperationRepository operationRepository;
    private final NetworkChangeExecutionAttemptRepository attemptRepository;
    private final ExecutionAuditService auditService;
    private final ExecutionMetrics metrics;
    private final Clock clock;

    public ChangeOperationExecutionService(
            ChangeExecutionProperties properties,
            ExecutionTargetRegistry targetRegistry,
            NetworkChangeExecutionOperationRepository operationRepository,
            NetworkChangeExecutionAttemptRepository attemptRepository,
            ExecutionAuditService auditService,
            ExecutionMetrics metrics,
            Clock clock
    ) {
        this.properties = properties;
        this.targetRegistry = targetRegistry;
        this.operationRepository = operationRepository;
        this.attemptRepository = attemptRepository;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public MutationAttemptResult executeForward(NetworkChangeExecutionEntity execution) {
        ensureForwardAttemptAllowed(execution.getId());
        List<NetworkChangeExecutionOperationEntity> operations =
                operationRepository.findByExecutionIdOrderBySequenceNumberAsc(execution.getId());
        NetworkChangeExecutionOperationEntity operation = operations.get(0);
        ExecutionMutationAdapter adapter = targetRegistry.requireMutationAdapter(execution.getExecutionTargetId());
        ExecutionContext context = new ExecutionContext(
                execution.getId().toString(),
                execution.getExecutionTargetId(),
                execution.getCellId(),
                operation.getParameterName(),
                execution.getFencingToken() == null ? 0L : execution.getFencingToken(),
                execution.getRequestedAt()
        );
        Instant now = clock.instant();
        NetworkChangeExecutionAttemptEntity attempt = NetworkChangeExecutionAttemptEntity.start(
                UUID.randomUUID(),
                execution.getId(),
                1,
                AttemptDirection.FORWARD.name(),
                null,
                now
        );
        auditService.append(execution.getId(), ExecutionAuditEventType.MUTATION_STARTED.name(), "system", null);
        metrics.incrementStarted();
        AuthorizedExecutionOperation authorized = new AuthorizedExecutionOperation(
                operation.getOperationType(),
                operation.getTargetEntityType(),
                operation.getTargetEntityId(),
                operation.getParameterName(),
                operation.getExpectedCurrentValue(),
                operation.getDesiredValue()
        );
        MutationResult result = adapter.execute(authorized, context);
        attempt.complete(
                result.outcome().name(),
                result.failureCode(),
                result.failureDetailSafe(),
                result.targetRevisionAfter(),
                clock.instant()
        );
        attemptRepository.save(attempt);
        return new MutationAttemptResult(result, attempt.getId());
    }

    @Transactional
    public MutationAttemptResult executeRollback(
            NetworkChangeExecutionEntity execution,
            AuthorizedRollbackOperation rollbackOperation
    ) {
        long rollbackAttempts = attemptRepository.countByExecutionIdAndDirection(execution.getId(), AttemptDirection.ROLLBACK.name());
        if (rollbackAttempts >= 1) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_ALREADY_TERMINAL, "rollback attempt limit reached");
        }
        ExecutionMutationAdapter adapter = targetRegistry.requireMutationAdapter(execution.getExecutionTargetId());
        ExecutionContext context = new ExecutionContext(
                execution.getId().toString(),
                execution.getExecutionTargetId(),
                execution.getCellId(),
                rollbackOperation.parameterName(),
                execution.getFencingToken() == null ? 0L : execution.getFencingToken(),
                execution.getRequestedAt()
        );
        Instant now = clock.instant();
        NetworkChangeExecutionAttemptEntity attempt = NetworkChangeExecutionAttemptEntity.start(
                UUID.randomUUID(),
                execution.getId(),
                1,
                AttemptDirection.ROLLBACK.name(),
                null,
                now
        );
        auditService.append(execution.getId(), ExecutionAuditEventType.ROLLBACK_STARTED.name(), "system", null);
        MutationResult result = adapter.rollback(rollbackOperation, context);
        attempt.complete(
                result.outcome().name(),
                result.failureCode(),
                result.failureDetailSafe(),
                result.targetRevisionAfter(),
                clock.instant()
        );
        attemptRepository.save(attempt);
        return new MutationAttemptResult(result, attempt.getId());
    }

    private void ensureForwardAttemptAllowed(UUID executionId) {
        long forwardAttempts = attemptRepository.countByExecutionIdAndDirection(executionId, AttemptDirection.FORWARD.name());
        if (forwardAttempts >= properties.getMaximumForwardAttempts()) {
            throw new ChangeExecutionException(ExecutionFailureCode.EXECUTION_ALREADY_TERMINAL, "forward attempt limit reached");
        }
    }

    public record MutationAttemptResult(MutationResult mutationResult, UUID attemptId) {
        public boolean applied() {
            return mutationResult.outcome() == AttemptOutcome.APPLIED;
        }

        public boolean outcomeUnknown() {
            return mutationResult.outcome() == AttemptOutcome.OUTCOME_UNKNOWN;
        }
    }
}
