package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionchange.entity.ProductionGatewayAttemptEntity;
import com.simba.snip.npo.productionchange.entity.ProductionGatewayEvidenceEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.protocol.GatewayAttemptStatus;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteResponse;
import com.simba.snip.npo.productionchange.protocol.GrantStatus;
import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionGrantRepository;
import com.simba.snip.npo.productionchange.repository.ProductionGatewayAttemptRepository;
import com.simba.snip.npo.productionchange.repository.ProductionGatewayEvidenceRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductionExecutionSyncService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionExecutionGrantRepository grantRepository;
    private final ProductionGatewayAttemptRepository attemptRepository;
    private final ProductionGatewayEvidenceRepository evidenceRepository;
    private final ProductionVerificationSyncService verificationSyncService;
    private final ProductionRecoveryService recoveryService;
    private final ProductionFailurePersistenceService failurePersistenceService;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;

    public ProductionExecutionSyncService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionExecutionGrantRepository grantRepository,
            ProductionGatewayAttemptRepository attemptRepository,
            ProductionGatewayEvidenceRepository evidenceRepository,
            ProductionVerificationSyncService verificationSyncService,
            ProductionRecoveryService recoveryService,
            ProductionFailurePersistenceService failurePersistenceService,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            Clock clock
    ) {
        this.changeRepository = changeRepository;
        this.grantRepository = grantRepository;
        this.attemptRepository = attemptRepository;
        this.evidenceRepository = evidenceRepository;
        this.verificationSyncService = verificationSyncService;
        this.recoveryService = recoveryService;
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
        Optional<ProductionExecutionGrantEntity> grant = response == null
                ? Optional.empty()
                : grantRepository.findById(response.grantId());
        Optional<ProductionGatewayAttemptEntity> attempt = attemptRepository
                .findFirstByProductionChangeIdOrderByStartedAtDesc(productionChangeId);
        if (grant.filter(g -> GrantStatus.CONSUMED.name().equals(g.getStatus())).isPresent() && attempt.isEmpty()) {
            failurePersistenceService.apply(
                    change,
                    ProductionChangeStatus.CONSUMED_PRE_SEND_RECOVERY_REQUIRED,
                    ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN,
                    clock.instant()
            );
            return change;
        }
        if (attempt.isEmpty()) {
            metrics.incrementAttempts("NO_ATTEMPT");
            return applyGatewayDenyIfPresent(change, response, actor);
        }
        ProductionGatewayAttemptEntity latest = attempt.get();
        List<ProductionGatewayEvidenceEntity> evidence = evidenceRepository.findByAttemptIdOrderByProducedAtAsc(latest.getAttemptId());
        GatewayAttemptStatus attemptStatus = GatewayAttemptStatus.valueOf(latest.getStatus());
        metrics.incrementAttempts(attemptStatus.name());
        return switch (attemptStatus) {
            case VERIFIED -> verificationSyncService.applyVerified(change, latest, evidence, actor);
            case VERIFICATION_FAILED -> verificationSyncService.applyVerificationFailed(change, latest, evidence, actor);
            case OUTCOME_UNKNOWN, MAY_HAVE_SENT -> applyOutcomeUnknown(change, latest, evidence, actor);
            case VENDOR_ACCEPTED -> applyVendorAccepted(change, evidence, actor);
            case VENDOR_REJECTED -> applyRejected(change, actor);
            case RECOVERY_REQUIRED -> recoveryService.signal(change, actor, ProductionReasonCode.PRODUCTION_VERIFICATION_MISMATCH);
            case MANUAL_INTERVENTION_REQUIRED -> applyManual(change, actor);
            default -> applyGatewayDenyIfPresent(change, response, actor);
        };
    }

    private ProductionNetworkChangeEntity applyVendorAccepted(
            ProductionNetworkChangeEntity change,
            List<ProductionGatewayEvidenceEntity> evidence,
            ActorPrincipal actor
    ) {
        if (evidence.isEmpty()) {
            return change;
        }
        change.setStatus(ProductionChangeStatus.VERIFYING.name());
        change.setUpdatedAt(clock.instant());
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_MUTATION_VENDOR_ACCEPTED,
                actor.actorPrincipalId(),
                List.of(),
                Map.of("evidenceCount", evidence.size())
        );
        return change;
    }

    private ProductionNetworkChangeEntity applyOutcomeUnknown(
            ProductionNetworkChangeEntity change,
            ProductionGatewayAttemptEntity attempt,
            List<ProductionGatewayEvidenceEntity> evidence,
            ActorPrincipal actor
    ) {
        if (evidence.isEmpty() && MutationOutcome.OUTCOME_UNKNOWN.name().equals(attempt.getMutationOutcome())) {
            failurePersistenceService.apply(
                    change,
                    ProductionChangeStatus.OUTCOME_UNKNOWN,
                    ProductionReasonCode.PRODUCTION_MUTATION_OUTCOME_UNKNOWN,
                    clock.instant()
            );
            metrics.incrementOutcomeUnknown();
            auditService.append(
                    change.getProductionChangeId(),
                    ProductionAuditEventType.PRODUCTION_MUTATION_OUTCOME_UNKNOWN,
                    actor.actorPrincipalId(),
                    List.of(ProductionReasonCode.PRODUCTION_MUTATION_OUTCOME_UNKNOWN.name()),
                    Map.of()
            );
            return recoveryService.signal(change, actor, ProductionReasonCode.PRODUCTION_MUTATION_OUTCOME_UNKNOWN);
        }
        if (!evidence.isEmpty() && MutationOutcome.OUTCOME_UNKNOWN.name().equals(attempt.getMutationOutcome())) {
            change.setStatus(ProductionChangeStatus.OUTCOME_UNKNOWN.name());
            change.setReasonCode(ProductionReasonCode.PRODUCTION_MUTATION_OUTCOME_UNKNOWN.name());
            change.setUpdatedAt(clock.instant());
            metrics.incrementOutcomeUnknown();
        }
        return change;
    }

    private ProductionNetworkChangeEntity applyGatewayDenyIfPresent(
            ProductionNetworkChangeEntity change,
            GatewayExecuteResponse response,
            ActorPrincipal actor
    ) {
        if (response == null || response.reasonCode() == null || response.reasonCode().isBlank()) {
            return change;
        }
        ProductionReasonCode reason;
        try {
            reason = ProductionReasonCode.valueOf(response.reasonCode());
        } catch (IllegalArgumentException ex) {
            reason = ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED;
        }
        if (reason == ProductionReasonCode.PRODUCTION_OUTCOME_UNKNOWN
                || reason == ProductionReasonCode.PRODUCTION_OUTCOME_UNRESOLVED) {
            return change;
        }
        ProductionChangeStatus denied = ProductionChangeStatus.EXECUTE_DENIED;
        if (reason == ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED) {
            denied = ProductionChangeStatus.PREFLIGHT_DENIED;
        }
        failurePersistenceService.persist(change.getProductionChangeId(), denied, reason);
        throw new ProductionChangeException(reason, "gateway denied execute: " + reason.name());
    }

    private ProductionNetworkChangeEntity applyRejected(ProductionNetworkChangeEntity change, ActorPrincipal actor) {
        change.setStatus(ProductionChangeStatus.EXECUTE_DENIED.name());
        change.setReasonCode(ProductionReasonCode.PRODUCTION_VENDOR_REJECTION.name());
        change.setUpdatedAt(clock.instant());
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_MUTATION_REJECTED,
                actor.actorPrincipalId(),
                List.of(ProductionReasonCode.PRODUCTION_VENDOR_REJECTION.name()),
                Map.of()
        );
        return change;
    }

    private ProductionNetworkChangeEntity applyManual(ProductionNetworkChangeEntity change, ActorPrincipal actor) {
        failurePersistenceService.apply(
                change,
                ProductionChangeStatus.MANUAL_INTERVENTION_REQUIRED,
                ProductionReasonCode.PRODUCTION_MANUAL_INTERVENTION_REQUIRED,
                clock.instant()
        );
        metrics.incrementManualIntervention();
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_MANUAL_INTERVENTION_REQUIRED,
                actor.actorPrincipalId(),
                List.of(ProductionReasonCode.PRODUCTION_MANUAL_INTERVENTION_REQUIRED.name()),
                Map.of()
        );
        return change;
    }
}
