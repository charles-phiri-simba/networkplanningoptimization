package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionRollbackEntity;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.metrics.ExecutionMetrics;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRollbackEntityRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RollbackReviewService {

    private final ChangeExecutionProperties properties;
    private final NetworkChangeExecutionRepository executionRepository;
    private final NetworkChangeExecutionRollbackEntityRepository rollbackRepository;
    private final ExecutionAuditService auditService;
    private final ExecutionMetrics metrics;
    private final Clock clock;

    public RollbackReviewService(
            ChangeExecutionProperties properties,
            NetworkChangeExecutionRepository executionRepository,
            NetworkChangeExecutionRollbackEntityRepository rollbackRepository,
            ExecutionAuditService auditService,
            ExecutionMetrics metrics,
            Clock clock
    ) {
        this.properties = properties;
        this.executionRepository = executionRepository;
        this.rollbackRepository = rollbackRepository;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangeExecutionEntity requestRollback(UUID executionId, String requester) {
        Instant now = clock.instant();
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        if (!ExecutionStatus.RECOVERY_REQUIRED.name().equals(execution.getStatus())
                && !ExecutionStatus.VERIFICATION_FAILED.name().equals(execution.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, execution.getStatus());
        }
        NetworkChangeExecutionRollbackEntity rollback = rollbackRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, "rollback record missing"));
        rollback.markRequested(requester, now);
        rollbackRepository.save(rollback);
        execution.setStatus(ExecutionStatus.ROLLBACK_REQUESTED.name());
        execution.markRollbackStatus("REQUESTED");
        executionRepository.save(execution);
        metrics.incrementRollbackRequested();
        auditService.append(executionId, ExecutionAuditEventType.ROLLBACK_REQUESTED.name(), requester, null);
        return execution;
    }

    @Transactional
    public NetworkChangeExecutionRollbackEntity reviewRollback(UUID executionId, String reviewer, String comment) {
        if (properties.isRequireRollbackReview()) {
            // enforced by controller permission
        }
        Instant now = clock.instant();
        requireRecovery(executionId);
        NetworkChangeExecutionRollbackEntity rollback = rollbackRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, "rollback record missing"));
        if (!"REQUESTED".equals(rollback.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, rollback.getStatus());
        }
        rollback.markReviewed(reviewer, now);
        rollbackRepository.save(rollback);
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        execution.setStatus(ExecutionStatus.ROLLBACK_REVIEWED.name());
        execution.markRollbackStatus("REVIEWED");
        executionRepository.save(execution);
        auditService.append(executionId, ExecutionAuditEventType.ROLLBACK_REVIEWED.name(), reviewer, comment);
        return rollback;
    }

    private NetworkChangeExecutionEntity requireRecovery(UUID executionId) {
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        if (!ExecutionStatus.RECOVERY_REQUIRED.name().equals(execution.getStatus())
                && !ExecutionStatus.ROLLBACK_REQUESTED.name().equals(execution.getStatus())
                && !ExecutionStatus.ROLLBACK_REVIEWED.name().equals(execution.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, execution.getStatus());
        }
        return execution;
    }
}
