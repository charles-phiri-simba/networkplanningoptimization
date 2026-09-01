package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.adapter.spi.AuthorizedExecutionOperation;
import com.simba.snip.npo.changeexecution.adapter.spi.AuthorizedRollbackOperation;
import com.simba.snip.npo.changeexecution.adapter.spi.ExecutionContext;
import com.simba.snip.npo.changeexecution.adapter.spi.ExecutionObservationAdapter;
import com.simba.snip.npo.changeexecution.adapter.spi.ObservationResult;
import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.domain.AttemptDirection;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.VerificationOutcome;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAttemptEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionVerificationEntity;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionAttemptRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionOperationRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionVerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExecutionVerificationService {

    private final ExecutionTargetRegistry targetRegistry;
    private final NetworkChangeExecutionOperationRepository operationRepository;
    private final NetworkChangeExecutionAttemptRepository attemptRepository;
    private final NetworkChangeExecutionVerificationRepository verificationRepository;
    private final ExecutionAuditService auditService;
    private final Clock clock;

    public ExecutionVerificationService(
            ExecutionTargetRegistry targetRegistry,
            NetworkChangeExecutionOperationRepository operationRepository,
            NetworkChangeExecutionAttemptRepository attemptRepository,
            NetworkChangeExecutionVerificationRepository verificationRepository,
            ExecutionAuditService auditService,
            Clock clock
    ) {
        this.targetRegistry = targetRegistry;
        this.operationRepository = operationRepository;
        this.attemptRepository = attemptRepository;
        this.verificationRepository = verificationRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public VerificationResult verifyForward(NetworkChangeExecutionEntity execution, UUID attemptId) {
        auditService.append(execution.getId(), ExecutionAuditEventType.VERIFICATION_STARTED.name(), "system", "forward");
        NetworkChangeExecutionOperationEntity operation = singleOperation(execution.getId());
        ExecutionObservationAdapter adapter = targetRegistry.requireObservationAdapter(execution.getExecutionTargetId());
        ExecutionContext context = new ExecutionContext(
                execution.getId().toString(),
                execution.getExecutionTargetId(),
                execution.getCellId(),
                operation.getParameterName(),
                execution.getFencingToken() == null ? 0L : execution.getFencingToken(),
                execution.getRequestedAt()
        );
        AuthorizedExecutionOperation authorized = new AuthorizedExecutionOperation(
                operation.getOperationType(),
                operation.getTargetEntityType(),
                operation.getTargetEntityId(),
                operation.getParameterName(),
                operation.getExpectedCurrentValue(),
                operation.getDesiredValue()
        );
        NetworkChangeExecutionAttemptEntity attempt = requireAttempt(
                execution.getId(), attemptId, AttemptDirection.FORWARD);
        ObservationResult observation = adapter.verifyForward(
                authorized, context, attempt.getTargetRevisionAfter());
        persistVerification(execution.getId(), attemptId, AttemptDirection.FORWARD.name(), observation, operation.getDesiredValue());
        return new VerificationResult(observation);
    }

    @Transactional
    public VerificationResult verifyRollback(
            NetworkChangeExecutionEntity execution,
            UUID attemptId,
            AuthorizedRollbackOperation rollbackOperation
    ) {
        auditService.append(execution.getId(), ExecutionAuditEventType.VERIFICATION_STARTED.name(), "system", "rollback");
        ExecutionObservationAdapter adapter = targetRegistry.requireObservationAdapter(execution.getExecutionTargetId());
        ExecutionContext context = new ExecutionContext(
                execution.getId().toString(),
                execution.getExecutionTargetId(),
                execution.getCellId(),
                rollbackOperation.parameterName(),
                execution.getFencingToken() == null ? 0L : execution.getFencingToken(),
                execution.getRequestedAt()
        );
        NetworkChangeExecutionAttemptEntity attempt = requireAttempt(
                execution.getId(), attemptId, AttemptDirection.ROLLBACK);
        ObservationResult observation = adapter.verifyRollback(
                rollbackOperation, context, attempt.getTargetRevisionAfter());
        persistVerification(execution.getId(), attemptId, AttemptDirection.ROLLBACK.name(), observation, rollbackOperation.desiredValue());
        return new VerificationResult(observation);
    }

    private void persistVerification(
            UUID executionId,
            UUID attemptId,
            String direction,
            ObservationResult observation,
            String expectedValue
    ) {
        verificationRepository.save(NetworkChangeExecutionVerificationEntity.create(
                UUID.randomUUID(),
                executionId,
                attemptId,
                direction,
                observation.outcome().name(),
                observation.observedValue(),
                expectedValue,
                observation.targetRevision(),
                observation.reasonCode(),
                observation.evidenceSummary(),
                observation.observedAt() == null ? clock.instant() : observation.observedAt()
        ));
    }

    private NetworkChangeExecutionOperationEntity singleOperation(UUID executionId) {
        List<NetworkChangeExecutionOperationEntity> operations =
                operationRepository.findByExecutionIdOrderBySequenceNumberAsc(executionId);
        return operations.get(0);
    }

    private NetworkChangeExecutionAttemptEntity requireAttempt(
            UUID executionId,
            UUID attemptId,
            AttemptDirection direction
    ) {
        if (attemptId != null) {
            NetworkChangeExecutionAttemptEntity attempt = attemptRepository.findById(attemptId)
                    .orElseThrow(() -> new IllegalStateException("execution attempt not found"));
            if (!executionId.equals(attempt.getExecutionId())
                    || !direction.name().equals(attempt.getDirection())) {
                throw new IllegalStateException("execution attempt does not match verification");
            }
            return attempt;
        }
        return attemptRepository
                .findFirstByExecutionIdAndDirectionOrderByAttemptNumberDesc(executionId, direction.name())
                .orElseThrow(() -> new IllegalStateException("execution attempt not found"));
    }

    public record VerificationResult(ObservationResult observation) {
        public boolean verified() {
            return observation.outcome() == VerificationOutcome.VERIFIED;
        }
    }
}
