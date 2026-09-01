package com.simba.snip.npo.changeexecution.api;

import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAttemptEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAuthorizationEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionRecoveryEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionRollbackEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionVerificationEntity;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionAttemptRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionAuthorizationRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionOperationRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRecoveryRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRollbackEntityRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionVerificationRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChangeExecutionQueryService {

    private final NetworkChangeExecutionRepository executionRepository;
    private final NetworkChangeExecutionOperationRepository operationRepository;
    private final NetworkChangeExecutionAttemptRepository attemptRepository;
    private final NetworkChangeExecutionAuthorizationRepository authorizationRepository;
    private final NetworkChangeExecutionVerificationRepository verificationRepository;
    private final NetworkChangeExecutionRecoveryRepository recoveryRepository;
    private final NetworkChangeExecutionRollbackEntityRepository rollbackRepository;
    private final ExecutionAuditService auditService;

    public ChangeExecutionQueryService(
            NetworkChangeExecutionRepository executionRepository,
            NetworkChangeExecutionOperationRepository operationRepository,
            NetworkChangeExecutionAttemptRepository attemptRepository,
            NetworkChangeExecutionAuthorizationRepository authorizationRepository,
            NetworkChangeExecutionVerificationRepository verificationRepository,
            NetworkChangeExecutionRecoveryRepository recoveryRepository,
            NetworkChangeExecutionRollbackEntityRepository rollbackRepository,
            ExecutionAuditService auditService
    ) {
        this.executionRepository = executionRepository;
        this.operationRepository = operationRepository;
        this.attemptRepository = attemptRepository;
        this.authorizationRepository = authorizationRepository;
        this.verificationRepository = verificationRepository;
        this.recoveryRepository = recoveryRepository;
        this.rollbackRepository = rollbackRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ExecutionSummaryDto> list() {
        return executionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ExecutionDetailDto require(UUID executionId) {
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        return toDetail(execution);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> evidence(UUID executionId) {
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("executionId", execution.getId().toString());
        evidence.put("planId", execution.getPlanId().toString());
        evidence.put("executionFingerprint", execution.getExecutionFingerprint());
        evidence.put("authorizedExecutionFingerprint", execution.getAuthorizedExecutionFingerprint());
        evidence.put("operations", operationRepository.findByExecutionIdOrderBySequenceNumberAsc(executionId).stream()
                .map(op -> Map.of(
                        "sequenceNumber", op.getSequenceNumber(),
                        "operationType", op.getOperationType(),
                        "expectedCurrentValue", op.getExpectedCurrentValue(),
                        "desiredValue", op.getDesiredValue()
                ))
                .toList());
        evidence.put("attempts", attemptRepository.findByExecutionIdOrderByAttemptNumberAsc(executionId).stream()
                .map(this::attemptEvidence)
                .toList());
        evidence.put("authorizations", authorizationRepository.findByExecutionIdOrderByAuthorizedAtAsc(executionId).stream()
                .map(this::authorizationEvidence)
                .toList());
        evidence.put("verifications", verificationRepository.findByExecutionIdOrderByObservedAtAsc(executionId).stream()
                .map(this::verificationEvidence)
                .toList());
        evidence.put("recoveries", recoveryRepository.findByExecutionIdOrderByEvaluatedAtAsc(executionId).stream()
                .map(this::recoveryEvidence)
                .toList());
        rollbackRepository.findByExecutionId(executionId).ifPresent(rollback -> evidence.put("rollback", rollbackEvidence(rollback)));
        evidence.put("auditEvents", auditService.list(executionId).stream()
                .map(event -> Map.of(
                        "eventType", event.getEventType(),
                        "actor", event.getActor(),
                        "details", event.getDetails(),
                        "occurredAt", event.getOccurredAt().toString()
                ))
                .toList());
        return evidence;
    }

    private ExecutionSummaryDto toSummary(NetworkChangeExecutionEntity execution) {
        return new ExecutionSummaryDto(
                execution.getId(),
                execution.getPlanId(),
                execution.getStatus(),
                execution.getExecutionTargetId(),
                execution.getCellId(),
                execution.getParameterName(),
                execution.getExecutionFingerprint(),
                execution.getRequestedAt(),
                execution.getAuthorizedAt(),
                execution.getCompletedAt()
        );
    }

    private ExecutionDetailDto toDetail(NetworkChangeExecutionEntity execution) {
        List<ExecutionOperationDto> operations = operationRepository.findByExecutionIdOrderBySequenceNumberAsc(execution.getId())
                .stream()
                .map(this::toOperationDto)
                .toList();
        return new ExecutionDetailDto(
                execution.getId(),
                execution.getPlanId(),
                execution.getPlanVersion(),
                execution.getPlanFingerprint(),
                execution.getExecutionTargetId(),
                execution.getExecutionTargetType(),
                execution.getExecutionTargetEnvironment(),
                execution.getAdapterProfileId(),
                execution.getCapabilityProfileVersion(),
                execution.getCellId(),
                execution.getParameterName(),
                execution.getExecutionFingerprint(),
                execution.getAuthorizedExecutionFingerprint(),
                execution.getStatus(),
                execution.getRequestedBy(),
                execution.getRequestedAt(),
                execution.getReviewedBy(),
                execution.getReviewedAt(),
                execution.getAuthorizedBy(),
                execution.getAuthorizedAt(),
                execution.getExecutionWindowOpensAt(),
                execution.getExecutionWindowClosesAt(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getFailureCode(),
                execution.getFailureDetailSafe(),
                execution.getVerificationStatus(),
                execution.getRecoveryStatus(),
                execution.getRollbackStatus(),
                operations
        );
    }

    private ExecutionOperationDto toOperationDto(NetworkChangeExecutionOperationEntity operation) {
        return new ExecutionOperationDto(
                operation.getSequenceNumber(),
                operation.getOperationType(),
                operation.getTargetEntityType(),
                operation.getTargetEntityId(),
                operation.getParameterName(),
                operation.getExpectedCurrentValue(),
                operation.getDesiredValue()
        );
    }

    private Map<String, Object> attemptEvidence(NetworkChangeExecutionAttemptEntity attempt) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("attemptNumber", attempt.getAttemptNumber());
        map.put("direction", attempt.getDirection());
        map.put("outcome", attempt.getOutcome());
        map.put("failureCode", attempt.getFailureCode());
        map.put("startedAt", attempt.getStartedAt().toString());
        if (attempt.getCompletedAt() != null) {
            map.put("completedAt", attempt.getCompletedAt().toString());
        }
        return map;
    }

    private Map<String, Object> authorizationEvidence(NetworkChangeExecutionAuthorizationEntity authorization) {
        return Map.of(
                "authorizationType", authorization.getAuthorizationType(),
                "authorizer", authorization.getAuthorizer(),
                "authorizedFingerprint", authorization.getAuthorizedFingerprint(),
                "authorizedAt", authorization.getAuthorizedAt().toString()
        );
    }

    private Map<String, Object> verificationEvidence(NetworkChangeExecutionVerificationEntity verification) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("direction", verification.getDirection());
        map.put("outcome", verification.getOutcome());
        map.put("observedValue", verification.getObservedValue());
        map.put("expectedValue", verification.getExpectedValue());
        map.put("observedAt", verification.getObservedAt().toString());
        return map;
    }

    private Map<String, Object> recoveryEvidence(NetworkChangeExecutionRecoveryEntity recovery) {
        return Map.of(
                "recoveryStatus", recovery.getRecoveryStatus(),
                "rollbackEligible", recovery.isRollbackEligible(),
                "reasonCodes", recovery.getReasonCodes(),
                "evaluatedAt", recovery.getEvaluatedAt().toString()
        );
    }

    private Map<String, Object> rollbackEvidence(NetworkChangeExecutionRollbackEntity rollback) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", rollback.getStatus());
        map.put("authorizedRollbackFingerprint", rollback.getAuthorizedRollbackFingerprint());
        if (rollback.getRequestedAt() != null) {
            map.put("requestedAt", rollback.getRequestedAt().toString());
        }
        if (rollback.getAuthorizedAt() != null) {
            map.put("authorizedAt", rollback.getAuthorizedAt().toString());
        }
        return map;
    }
}
