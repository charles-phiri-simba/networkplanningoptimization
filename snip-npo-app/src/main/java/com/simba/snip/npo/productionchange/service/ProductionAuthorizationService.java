package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.AuthorizationRecordStatus;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionChangeAuthorizationEntity;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.policy.ProductionSeparationOfDutiesPolicy;
import com.simba.snip.npo.productionchange.protocol.GrantStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionChangeAuthorizationRepository;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionGrantRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionAuthorizationService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionChangeAuthorizationRepository authorizationRepository;
    private final ProductionExecutionGrantRepository grantRepository;
    private final ProductionTargetRegistry targetRegistry;
    private final ProductionBindingAssembler bindingAssembler;
    private final ProductionFingerprintService fingerprintService;
    private final ProductionChangeControlService changeControlService;
    private final ProductionSeparationOfDutiesPolicy sodPolicy;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;

    public ProductionAuthorizationService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionChangeAuthorizationRepository authorizationRepository,
            ProductionExecutionGrantRepository grantRepository,
            ProductionTargetRegistry targetRegistry,
            ProductionBindingAssembler bindingAssembler,
            ProductionFingerprintService fingerprintService,
            ProductionChangeControlService changeControlService,
            ProductionSeparationOfDutiesPolicy sodPolicy,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            Clock clock
    ) {
        this.changeRepository = changeRepository;
        this.authorizationRepository = authorizationRepository;
        this.grantRepository = grantRepository;
        this.targetRegistry = targetRegistry;
        this.bindingAssembler = bindingAssembler;
        this.fingerprintService = fingerprintService;
        this.changeControlService = changeControlService;
        this.sodPolicy = sodPolicy;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public ProductionNetworkChangeEntity authorize(UUID productionChangeId, ActorPrincipal authorizer) {
        Instant now = clock.instant();
        ProductionNetworkChangeEntity change = changeRepository.lockById(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                        "production change not found"
                ));
        auditService.requireMutable(change);
        if (!ProductionChangeStatus.REVIEWED.name().equals(change.getStatus())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                    "production change must be REVIEWED before authorization"
            );
        }
        sodPolicy.requesterMustNotAuthorize(change.getRequesterPrincipalId(), authorizer.actorPrincipalId());
        sodPolicy.reviewerMustNotAuthorize(change.getReviewerPrincipalId(), authorizer.actorPrincipalId());
        changeControlService.requireCurrent(change.getProductionChangeId(), now);
        ProductionNetworkTargetEntity target = targetRegistry.require(change.getProductionTargetId());
        bindingAssembler.requireTargetEligible(target);
        ProductionBindingAssembler.UpstreamBinding binding =
                bindingAssembler.requireVerifiedPhase15(change.getPhase15ExecutionId());
        int nextGeneration = change.getAuthorizationGeneration() + 1;
        String fingerprint = fingerprintService.compute(bindingAssembler.fingerprintInput(
                change,
                target,
                binding,
                change.getChangeControlReference(),
                nextGeneration
        ));
        change.setAuthorizationGeneration(nextGeneration);
        change.setProductionFingerprint(fingerprint);
        change.setAuthorizerPrincipalId(authorizer.actorPrincipalId());
        change.setStatus(ProductionChangeStatus.AUTHORIZED.name());
        change.setUpdatedAt(now);
        authorizationRepository.save(ProductionChangeAuthorizationEntity.create(
                UUID.randomUUID(),
                change.getProductionChangeId(),
                authorizer.actorPrincipalId(),
                nextGeneration,
                fingerprint,
                AuthorizationRecordStatus.ACTIVE.name(),
                now,
                binding.execution().getExecutionWindowClosesAt()
        ));
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_AUTHORIZED,
                authorizer.actorPrincipalId(),
                List.of(),
                Map.of("authorizationGeneration", nextGeneration)
        );
        metrics.incrementAuthorizations("AUTHORIZED");
        return change;
    }

    @Transactional
    public void markStale(ProductionNetworkChangeEntity change, ActorPrincipal actor, ProductionReasonCode reasonCode) {
        Instant now = clock.instant();
        for (ProductionChangeAuthorizationEntity authorization :
                authorizationRepository.findByProductionChangeIdAndStatus(change.getProductionChangeId(), AuthorizationRecordStatus.ACTIVE.name())) {
            authorization.setStatus(AuthorizationRecordStatus.STALE.name());
        }
        for (ProductionExecutionGrantEntity grant :
                grantRepository.findByProductionChangeIdAndStatus(change.getProductionChangeId(), GrantStatus.ISSUED.name())) {
            grant.setStatus(GrantStatus.REVOKED.name());
            metrics.incrementGrantRevoked();
            auditService.append(
                    change.getProductionChangeId(),
                    ProductionAuditEventType.PRODUCTION_GRANT_REVOKED,
                    actor.actorPrincipalId(),
                    List.of(ProductionReasonCode.PRODUCTION_GRANT_REVOKED.name()),
                    Map.of()
            );
        }
        change.setStatus(ProductionChangeStatus.AUTHORIZATION_STALE.name());
        change.setReasonCode(reasonCode.name());
        change.setUpdatedAt(now);
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_AUTHORIZATION_STALE,
                actor.actorPrincipalId(),
                List.of(reasonCode.name()),
                Map.of()
        );
        metrics.incrementAuthorizations("STALE");
    }

    public ProductionChangeAuthorizationEntity requireActive(UUID productionChangeId, String currentFingerprint) {
        ProductionChangeAuthorizationEntity authorization = authorizationRepository
                .findFirstByProductionChangeIdAndStatusOrderByAuthorizedAtDesc(
                        productionChangeId,
                        AuthorizationRecordStatus.ACTIVE.name()
                )
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_AUTHORIZATION_MISSING,
                        "production authorization is missing"
                ));
        if (!currentFingerprint.equals(authorization.getProductionFingerprint())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_AUTHORIZATION_STALE,
                    "production authorization is stale"
            );
        }
        return authorization;
    }
}
