package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.domain.ProductionRollbackStatus;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionRollbackEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionRollbackRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionRollbackRequestService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionExecutionRollbackRepository rollbackRepository;
    private final ProductionFingerprintService fingerprintService;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;

    public ProductionRollbackRequestService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionExecutionRollbackRepository rollbackRepository,
            ProductionFingerprintService fingerprintService,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            Clock clock
    ) {
        this.changeRepository = changeRepository;
        this.rollbackRepository = rollbackRepository;
        this.fingerprintService = fingerprintService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public ProductionNetworkChangeEntity request(UUID productionChangeId, ActorPrincipal requester) {
        Instant now = clock.instant();
        ProductionNetworkChangeEntity change = changeRepository.lockById(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                        "production change not found"
                ));
        auditService.requireMutable(change);
        if (!ProductionChangeStatus.VERIFIED.name().equals(change.getStatus())
                && !ProductionChangeStatus.VERIFICATION_FAILED.name().equals(change.getStatus())
                && !ProductionChangeStatus.RECOVERY_REQUIRED.name().equals(change.getStatus())
                && !ProductionChangeStatus.NETWORK_SYNCHRONIZATION_REQUIRED.name().equals(change.getStatus())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_ROLLBACK_BLOCKED,
                    "rollback is not available in the current lifecycle state"
            );
        }
        if (change.getRollbackDesiredValue() == null || change.getRollbackExpectedValue() == null) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_ROLLBACK_BLOCKED,
                    "rollback values must come from persisted phase 14 state"
            );
        }
        String rollbackFingerprint = fingerprintService.operationBindingHash(
                change.getCellId(),
                change.getParameter(),
                change.getRollbackExpectedValue(),
                change.getRollbackDesiredValue(),
                "ROLLBACK"
        );
        rollbackRepository.save(ProductionExecutionRollbackEntity.createRequested(
                UUID.randomUUID(),
                change.getProductionChangeId(),
                requester.actorPrincipalId(),
                rollbackFingerprint,
                now
        ));
        change.setStatus(ProductionChangeStatus.ROLLBACK_REQUESTED.name());
        change.setUpdatedAt(now);
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_ROLLBACK_REQUESTED,
                requester.actorPrincipalId(),
                List.of(),
                Map.of("status", ProductionRollbackStatus.REQUESTED.name())
        );
        metrics.incrementRollbacks("REQUESTED");
        return change;
    }
}
