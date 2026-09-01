package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.LeaseHandle;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.protocol.GrantStatus;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionGrantRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionExecutionGrantService {

    private final ProductionExecutionGrantRepository grantRepository;
    private final ProductionChangeProperties properties;
    private final ProductionFingerprintService fingerprintService;
    private final ProductionRateLimitService rateLimitService;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;

    public ProductionExecutionGrantService(
            ProductionExecutionGrantRepository grantRepository,
            ProductionChangeProperties properties,
            ProductionFingerprintService fingerprintService,
            ProductionRateLimitService rateLimitService,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            Clock clock
    ) {
        this.grantRepository = grantRepository;
        this.properties = properties;
        this.fingerprintService = fingerprintService;
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionExecutionGrantEntity issue(
            ProductionNetworkChangeEntity change,
            LeaseHandle lease,
            GrantType grantType,
            ActorPrincipal actor
    ) {
        auditService.requireMutable(change);
        Instant now = clock.instant();
        if (grantRepository.findFirstByProductionChangeIdAndGrantTypeAndStatus(
                change.getProductionChangeId(),
                grantType.name(),
                GrantStatus.ISSUED.name()
        ).isPresent()) {
            metrics.incrementGrantIssuanceDenied(ProductionReasonCode.PRODUCTION_GRANT_ACTIVE_CONFLICT);
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_GRANT_ACTIVE_CONFLICT,
                    "an active " + grantType + " grant already exists"
            );
        }
        long issuedOnTarget = grantRepository.countByTargetIdAndStatus(change.getProductionTargetId(), GrantStatus.ISSUED.name());
        if (issuedOnTarget >= properties.getMaximumConcurrentIssuedGrantsPerTarget()) {
            metrics.incrementGrantIssuanceDenied(ProductionReasonCode.PRODUCTION_GRANT_ISSUANCE_LIMIT_EXCEEDED);
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_GRANT_ISSUANCE_LIMIT_EXCEEDED,
                    "concurrent ISSUED grants for target exceeded"
            );
        }
        rateLimitService.consumeTargetHour(change.getProductionTargetId());
        rateLimitService.consumeCellDay(change.getCellId());
        rateLimitService.consumeActorHour(actor.actorPrincipalId());
        BigDecimal expected = grantType == GrantType.ROLLBACK ? change.getRollbackExpectedValue() : change.getExpectedValue();
        BigDecimal desired = grantType == GrantType.ROLLBACK ? change.getRollbackDesiredValue() : change.getDesiredValue();
        String bindingHash = fingerprintService.operationBindingHash(
                change.getCellId(),
                change.getParameter(),
                expected,
                desired,
                grantType.name()
        );
        Instant expiresAt = now.plus(grantType == GrantType.ROLLBACK
                ? properties.getMaximumRollbackGrantTtl()
                : properties.getMaximumForwardGrantTtl());
        try {
            ProductionExecutionGrantEntity grant = grantRepository.save(ProductionExecutionGrantEntity.issue(
                    UUID.randomUUID(),
                    change.getProductionChangeId(),
                    change.getPhase15ExecutionId(),
                    change.getProductionTargetId(),
                    grantType.name(),
                    change.getProductionFingerprint(),
                    change.getAuthorizationGeneration(),
                    lease.fencingToken(),
                    bindingHash,
                    now,
                    expiresAt
            ));
            auditService.append(
                    change.getProductionChangeId(),
                    ProductionAuditEventType.PRODUCTION_GRANT_ISSUED,
                    actor.actorPrincipalId(),
                    List.of(),
                    Map.of("grantType", grantType.name())
            );
            metrics.incrementGrantIssuance("ISSUED");
            return grant;
        } catch (DataIntegrityViolationException ex) {
            metrics.incrementGrantIssuanceDenied(ProductionReasonCode.PRODUCTION_GRANT_ACTIVE_CONFLICT);
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_GRANT_ACTIVE_CONFLICT,
                    "active grant unique constraint rejected issuance",
                    ex
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeIssued(UUID productionChangeId, ActorPrincipal actor) {
        for (ProductionExecutionGrantEntity grant :
                grantRepository.findByProductionChangeIdAndStatus(productionChangeId, GrantStatus.ISSUED.name())) {
            grant.setStatus(GrantStatus.REVOKED.name());
            metrics.incrementGrantRevoked();
            auditService.append(
                    productionChangeId,
                    ProductionAuditEventType.PRODUCTION_GRANT_REVOKED,
                    actor.actorPrincipalId(),
                    List.of(ProductionReasonCode.PRODUCTION_GRANT_REVOKED.name()),
                    Map.of("grantType", grant.getGrantType())
            );
        }
    }
}
