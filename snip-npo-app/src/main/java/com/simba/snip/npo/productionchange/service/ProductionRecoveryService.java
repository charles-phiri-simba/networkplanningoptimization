package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionRecoveryEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionRecoveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionRecoveryService {

    private final ProductionExecutionRecoveryRepository recoveryRepository;
    private final ProductionFailurePersistenceService failurePersistenceService;
    private final ProductionChangeAuditService auditService;
    private final ProductionTargetHealthService healthService;
    private final Clock clock;

    public ProductionRecoveryService(
            ProductionExecutionRecoveryRepository recoveryRepository,
            ProductionFailurePersistenceService failurePersistenceService,
            ProductionChangeAuditService auditService,
            ProductionTargetHealthService healthService,
            Clock clock
    ) {
        this.recoveryRepository = recoveryRepository;
        this.failurePersistenceService = failurePersistenceService;
        this.auditService = auditService;
        this.healthService = healthService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionNetworkChangeEntity signal(
            ProductionNetworkChangeEntity change,
            ActorPrincipal actor,
            ProductionReasonCode reasonCode
    ) {
        recoveryRepository.save(ProductionExecutionRecoveryEntity.create(
                UUID.randomUUID(),
                change.getProductionChangeId(),
                ProductionChangeStatus.RECOVERY_REQUIRED.name(),
                reasonCode.name(),
                clock.instant()
        ));
        failurePersistenceService.persist(
                change.getProductionChangeId(),
                ProductionChangeStatus.RECOVERY_REQUIRED,
                reasonCode
        );
        change.setStatus(ProductionChangeStatus.RECOVERY_REQUIRED.name());
        change.setReasonCode(reasonCode.name());
        change.setUpdatedAt(clock.instant());
        if (reasonCode == ProductionReasonCode.PRODUCTION_MUTATION_OUTCOME_UNKNOWN
                || reasonCode == ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN) {
            healthService.recordOutcomeUnknown(change.getProductionTargetId(), actor);
        }
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_RECOVERY_REQUIRED,
                actor.actorPrincipalId(),
                List.of(reasonCode.name()),
                Map.of()
        );
        return change;
    }
}
