package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionVerificationEntity;
import com.simba.snip.npo.productionchange.entity.ProductionGatewayAttemptEntity;
import com.simba.snip.npo.productionchange.entity.ProductionGatewayEvidenceEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionVerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionVerificationSyncService {

    private final ProductionExecutionVerificationRepository verificationRepository;
    private final ProductionRecoveryService recoveryService;
    private final ProductionFailurePersistenceService failurePersistenceService;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final ProductionTargetHealthService healthService;
    private final Clock clock;

    public ProductionVerificationSyncService(
            ProductionExecutionVerificationRepository verificationRepository,
            ProductionRecoveryService recoveryService,
            ProductionFailurePersistenceService failurePersistenceService,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            ProductionTargetHealthService healthService,
            Clock clock
    ) {
        this.verificationRepository = verificationRepository;
        this.recoveryService = recoveryService;
        this.failurePersistenceService = failurePersistenceService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.healthService = healthService;
        this.clock = clock;
    }

    @Transactional
    public ProductionNetworkChangeEntity applyVerified(
            ProductionNetworkChangeEntity change,
            ProductionGatewayAttemptEntity attempt,
            List<ProductionGatewayEvidenceEntity> evidence,
            ActorPrincipal actor
    ) {
        boolean hasVerificationEvidence = evidence.stream()
                .anyMatch(row -> "VERIFICATION".equals(row.getEvidenceType()) || "READBACK".equals(row.getEvidenceType()));
        if (!hasVerificationEvidence) {
            return change;
        }
        verificationRepository.save(ProductionExecutionVerificationEntity.create(
                UUID.randomUUID(),
                change.getProductionChangeId(),
                attempt.getAttemptId(),
                "VERIFIED",
                change.getDesiredValue(),
                change.getDesiredValue(),
                clock.instant()
        ));
        change.setStatus(ProductionChangeStatus.VERIFIED.name());
        change.setReasonCode(null);
        change.setUpdatedAt(clock.instant());
        metrics.incrementVerified();
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_VERIFIED,
                actor.actorPrincipalId(),
                List.of(),
                Map.of("evidenceCount", evidence.size())
        );
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_NETWORK_SYNCHRONIZATION_REQUIRED,
                actor.actorPrincipalId(),
                List.of(),
                Map.of("canonicalMutation", false)
        );
        return change;
    }

    @Transactional
    public ProductionNetworkChangeEntity applyVerificationFailed(
            ProductionNetworkChangeEntity change,
            ProductionGatewayAttemptEntity attempt,
            List<ProductionGatewayEvidenceEntity> evidence,
            ActorPrincipal actor
    ) {
        if (evidence.isEmpty()) {
            return change;
        }
        verificationRepository.save(ProductionExecutionVerificationEntity.create(
                UUID.randomUUID(),
                change.getProductionChangeId(),
                attempt.getAttemptId(),
                "FAILED",
                null,
                change.getDesiredValue(),
                clock.instant()
        ));
        failurePersistenceService.apply(
                change,
                ProductionChangeStatus.VERIFICATION_FAILED,
                ProductionReasonCode.PRODUCTION_VERIFICATION_MISMATCH,
                clock.instant()
        );
        metrics.incrementVerificationFailures();
        healthService.recordVerificationFailure(change.getProductionTargetId(), actor);
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_VERIFICATION_FAILED,
                actor.actorPrincipalId(),
                List.of(ProductionReasonCode.PRODUCTION_VERIFICATION_MISMATCH.name()),
                Map.of()
        );
        return recoveryService.signal(change, actor, ProductionReasonCode.PRODUCTION_VERIFICATION_MISMATCH);
    }
}
