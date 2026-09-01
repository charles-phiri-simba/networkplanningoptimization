package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.domain.ProductionTargetState;
import com.simba.snip.npo.productionchange.entity.ProductionChangeAuthorizationEntity;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.protocol.GrantStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionChangeAuthorizationRepository;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionGrantRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkTargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ProductionTargetAdministrationService {

    private final ProductionNetworkTargetRepository targetRepository;
    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionChangeAuthorizationRepository authorizationRepository;
    private final ProductionExecutionGrantRepository grantRepository;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;

    public ProductionTargetAdministrationService(
            ProductionNetworkTargetRepository targetRepository,
            ProductionNetworkChangeRepository changeRepository,
            ProductionChangeAuthorizationRepository authorizationRepository,
            ProductionExecutionGrantRepository grantRepository,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            Clock clock
    ) {
        this.targetRepository = targetRepository;
        this.changeRepository = changeRepository;
        this.authorizationRepository = authorizationRepository;
        this.grantRepository = grantRepository;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionNetworkTargetEntity suspend(String targetId, ActorPrincipal actor) {
        Instant now = clock.instant();
        ProductionNetworkTargetEntity target = targetRepository.lockById(targetId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_TARGET_NOT_FOUND,
                        "production target not found"
                ));
        String before = target.getTargetFingerprint();
        target.setTargetState(ProductionTargetState.SUSPENDED.name());
        target.setUpdatedAt(now);
        staleAuthorizationsAndRevokeGrants(targetId, actor, now);
        metrics.incrementTargetSuspensions();
        changeRepository.findAll().stream()
                .filter(change -> targetId.equals(change.getProductionTargetId()))
                .forEach(change -> auditService.append(
                        change.getProductionChangeId(),
                        ProductionAuditEventType.PRODUCTION_TARGET_SUSPENDED,
                        actor.actorPrincipalId(),
                        List.of(ProductionReasonCode.PRODUCTION_TARGET_SUSPENDED.name()),
                        Map.of("beforeFingerprint", before, "targetId", targetId)
                ));
        return target;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionNetworkTargetEntity resume(String targetId, ActorPrincipal actor) {
        Instant now = clock.instant();
        ProductionNetworkTargetEntity target = targetRepository.lockById(targetId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_TARGET_NOT_FOUND,
                        "production target not found"
                ));
        if (ProductionTargetState.DISABLED.name().equals(target.getTargetState())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_TARGET_DISABLED,
                    "disabled targets cannot be resumed automatically; re-provision required"
            );
        }
        target.setTargetState(ProductionTargetState.ACTIVE.name());
        target.setUpdatedAt(now);
        changeRepository.findAll().stream()
                .filter(change -> targetId.equals(change.getProductionTargetId()))
                .forEach(change -> auditService.append(
                        change.getProductionChangeId(),
                        ProductionAuditEventType.PRODUCTION_TARGET_RESUMED,
                        actor.actorPrincipalId(),
                        List.of(),
                        Map.of("targetId", targetId)
                ));
        return target;
    }

    private void staleAuthorizationsAndRevokeGrants(String targetId, ActorPrincipal actor, Instant now) {
        for (ProductionNetworkChangeEntity change : changeRepository.findAll()) {
            if (!targetId.equals(change.getProductionTargetId())) {
                continue;
            }
            for (ProductionChangeAuthorizationEntity authorization :
                    authorizationRepository.findByProductionChangeIdAndStatus(change.getProductionChangeId(), "ACTIVE")) {
                authorization.setStatus("STALE");
            }
            if (ProductionChangeStatus.AUTHORIZED.name().equals(change.getStatus())) {
                change.setStatus(ProductionChangeStatus.AUTHORIZATION_STALE.name());
                change.setReasonCode(ProductionReasonCode.PRODUCTION_AUTHORIZATION_STALE.name());
                change.setUpdatedAt(now);
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
                        Map.of("grantType", grant.getGrantType())
                );
            }
        }
    }
}
