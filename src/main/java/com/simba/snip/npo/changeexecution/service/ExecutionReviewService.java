package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ExecutionReviewService {

    private final ChangeExecutionProperties properties;
    private final NetworkChangeExecutionRepository executionRepository;
    private final ExecutionAuditService auditService;
    private final Clock clock;

    public ExecutionReviewService(
            ChangeExecutionProperties properties,
            NetworkChangeExecutionRepository executionRepository,
            ExecutionAuditService auditService,
            Clock clock
    ) {
        this.properties = properties;
        this.executionRepository = executionRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangeExecutionEntity review(UUID executionId, String reviewer, String comment) {
        if (properties.isRequireExecutionReview()) {
            // mandatory review enforced by controller permission
        }
        Instant now = clock.instant();
        NetworkChangeExecutionEntity execution = requireReviewable(executionId);
        if (!ExecutionStatus.READY_FOR_REVIEW.name().equals(execution.getStatus())) {
            throw new ChangeExecutionException(ExecutionFailureCode.INVALID_EXECUTION_STATE, execution.getStatus());
        }
        try {
            execution.markReviewed(reviewer, now);
            executionRepository.save(execution);
            auditService.append(executionId, ExecutionAuditEventType.EXECUTION_REVIEWED.name(), reviewer, comment);
            return execution;
        } catch (OptimisticLockingFailureException ex) {
            throw new ChangeExecutionException(ExecutionFailureCode.CONCURRENT_EXECUTION_CONFLICT, "concurrent review conflict");
        }
    }

    private NetworkChangeExecutionEntity requireReviewable(UUID executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
    }
}
