package com.simba.snip.npo.changeexecution.service;

import com.simba.snip.npo.changeexecution.audit.ExecutionAuditService;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeexecution.domain.ExecutionAuditEventType;
import com.simba.snip.npo.changeexecution.domain.RecoveryStatus;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionRecoveryEntity;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRecoveryRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ExecutionRecoveryService {

    private final ChangeExecutionProperties properties;
    private final NetworkChangeExecutionRepository executionRepository;
    private final NetworkChangeExecutionRecoveryRepository recoveryRepository;
    private final ExecutionAuditService auditService;
    private final Clock clock;

    public ExecutionRecoveryService(
            ChangeExecutionProperties properties,
            NetworkChangeExecutionRepository executionRepository,
            NetworkChangeExecutionRecoveryRepository recoveryRepository,
            ExecutionAuditService auditService,
            Clock clock
    ) {
        this.properties = properties;
        this.executionRepository = executionRepository;
        this.recoveryRepository = recoveryRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangeExecutionRecoveryEntity evaluateRecovery(NetworkChangeExecutionEntity execution, String reasonCodes) {
        Instant now = clock.instant();
        execution.markRecoveryRequired(now);
        executionRepository.save(execution);
        boolean rollbackEligible = !properties.isAutomaticRollbackEnabled();
        NetworkChangeExecutionRecoveryEntity recovery = NetworkChangeExecutionRecoveryEntity.create(
                UUID.randomUUID(),
                execution.getId(),
                RecoveryStatus.REQUIRED.name(),
                rollbackEligible,
                reasonCodes,
                "deterministic recovery evaluation",
                now
        );
        recoveryRepository.save(recovery);
        auditService.append(execution.getId(), ExecutionAuditEventType.RECOVERY_REQUIRED.name(), "system", reasonCodes);
        return recovery;
    }
}
