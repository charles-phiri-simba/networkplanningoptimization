package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionGatewayAttemptEntity;
import com.simba.snip.npo.productionchange.entity.ProductionGatewayEvidenceEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.protocol.GatewayAttemptStatus;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteResponse;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionGatewayAttemptRepository;
import com.simba.snip.npo.productionchange.repository.ProductionGatewayEvidenceRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionRollbackSyncService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionGatewayAttemptRepository attemptRepository;
    private final ProductionGatewayEvidenceRepository evidenceRepository;
    private final ProductionFailurePersistenceService failurePersistenceService;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;

    public ProductionRollbackSyncService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionGatewayAttemptRepository attemptRepository,
            ProductionGatewayEvidenceRepository evidenceRepository,
            ProductionFailurePersistenceService failurePersistenceService,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            Clock clock
    ) {
        this.changeRepository = changeRepository;
        this.attemptRepository = attemptRepository;
        this.evidenceRepository = evidenceRepository;
        this.failurePersistenceService = failurePersistenceService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public ProductionNetworkChangeEntity syncFromDurableEvidence(
            UUID productionChangeId,
            GatewayExecuteResponse response,
            ActorPrincipal actor
    ) {
        ProductionNetworkChangeEntity change = changeRepository.findById(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                        "production change not found"
                ));
        ProductionGatewayAttemptEntity attempt = null;
        if (response != null && response.attemptId() != null) {
            attempt = attemptRepository.findById(response.attemptId()).orElse(null);
        }
        if (attempt == null && response != null && response.grantId() != null) {
            attempt = attemptRepository.findFirstByGrantIdOrderByStartedAtDesc(response.grantId()).orElse(null);
        }
        if (attempt == null) {
            attempt = attemptRepository.findFirstByProductionChangeIdOrderByStartedAtDesc(productionChangeId)
                    .orElse(null);
        }
        GatewayAttemptStatus status = attempt == null
                ? (response == null ? null : response.attemptStatus())
                : GatewayAttemptStatus.valueOf(attempt.getStatus());
        if (status == GatewayAttemptStatus.VERIFIED) {
            List<ProductionGatewayEvidenceEntity> evidence = attempt == null
                    ? List.of()
                    : evidenceRepository.findByAttemptIdOrderByProducedAtAsc(attempt.getAttemptId());
            boolean hasRollbackEvidence = evidence.stream().anyMatch(row ->
                    "VERIFICATION".equals(row.getEvidenceType()) || "READBACK".equals(row.getEvidenceType()));
            if (!hasRollbackEvidence) {
                return change;
            }
            change.setStatus(ProductionChangeStatus.ROLLED_BACK.name());
            change.setUpdatedAt(clock.instant());
            metrics.incrementRollbacks("ROLLED_BACK");
            auditService.append(
                    change.getProductionChangeId(),
                    ProductionAuditEventType.PRODUCTION_ROLLED_BACK,
                    actor.actorPrincipalId(),
                    List.of(),
                    Map.of("evidenceCount", evidence.size())
            );
            return change;
        }
        if (status == GatewayAttemptStatus.OUTCOME_UNKNOWN || status == GatewayAttemptStatus.MAY_HAVE_SENT) {
            failurePersistenceService.apply(
                    change,
                    ProductionChangeStatus.ROLLBACK_OUTCOME_UNKNOWN,
                    ProductionReasonCode.PRODUCTION_ROLLBACK_OUTCOME_UNKNOWN,
                    clock.instant()
            );
            metrics.incrementRollbacks("OUTCOME_UNKNOWN");
            return change;
        }
        if (status == GatewayAttemptStatus.MANUAL_INTERVENTION_REQUIRED) {
            failurePersistenceService.apply(
                    change,
                    ProductionChangeStatus.MANUAL_INTERVENTION_REQUIRED,
                    ProductionReasonCode.PRODUCTION_MANUAL_INTERVENTION_REQUIRED,
                    clock.instant()
            );
            return change;
        }
        if (response != null && response.reasonCode() != null && !response.reasonCode().isBlank()) {
            ProductionReasonCode reason;
            try {
                reason = ProductionReasonCode.valueOf(response.reasonCode());
            } catch (IllegalArgumentException ex) {
                reason = ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED;
            }
            if (reason == ProductionReasonCode.PRODUCTION_EXPECTED_STATE_MISMATCH
                    || reason == ProductionReasonCode.PRODUCTION_VENDOR_STATE_MISMATCH
                    || reason == ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED
                    || reason == ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED
                    || reason == ProductionReasonCode.PRODUCTION_KILL_SWITCH_DENY) {
                failurePersistenceService.persist(
                        change.getProductionChangeId(),
                        ProductionChangeStatus.EXECUTE_DENIED,
                        reason
                );
                throw new ProductionChangeException(reason, "gateway denied rollback: " + reason.name());
            }
        }
        return change;
    }
}
