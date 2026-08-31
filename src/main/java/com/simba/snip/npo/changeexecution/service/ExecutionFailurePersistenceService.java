package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.metrics.ExecutionMetrics;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ExecutionFailurePersistenceService {

    private final NetworkChangeExecutionRepository executionRepository;
    private final ExecutionAuditService auditService;
    private final ExecutionMetrics metrics;

    public ExecutionFailurePersistenceService(
            NetworkChangeExecutionRepository executionRepository,
            ExecutionAuditService auditService,
            ExecutionMetrics metrics
    ) {
        this.executionRepository = executionRepository;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistOutcomeUnknown(UUID executionId, String failureCode, String detail, Instant now) {
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        execution.markOutcomeUnknown(failureCode, detail, now);
        executionRepository.save(execution);
        metrics.incrementOutcomeUnknown();
        auditService.append(executionId, ExecutionAuditEventType.MUTATION_OUTCOME_UNKNOWN.name(), "system", detail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistVerificationFailed(UUID executionId, String failureCode, String detail, Instant now) {
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        execution.markVerificationFailed(failureCode, detail, now);
        executionRepository.save(execution);
        metrics.incrementFailed();
        auditService.append(executionId, ExecutionAuditEventType.VERIFICATION_FAILED.name(), "system", detail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistManualIntervention(UUID executionId, String detail, Instant now) {
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        execution.markManualIntervention(detail, now);
        executionRepository.save(execution);
        metrics.incrementManualIntervention();
        auditService.append(executionId, ExecutionAuditEventType.MANUAL_INTERVENTION_REQUIRED.name(), "system", detail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistExecutionFailed(UUID executionId, String failureCode, String detail, Instant now) {
        NetworkChangeExecutionEntity execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new DomainNotFoundException("changeExecution", executionId.toString()));
        execution.markExecutionFailed(failureCode, detail, now);
        executionRepository.save(execution);
        metrics.incrementFailed();
        auditService.append(executionId, ExecutionAuditEventType.MUTATION_FAILED.name(), "system", detail);
    }
}
