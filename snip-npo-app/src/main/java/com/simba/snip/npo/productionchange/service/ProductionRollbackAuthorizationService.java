package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionRollbackEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.policy.ProductionSeparationOfDutiesPolicy;
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
public class ProductionRollbackAuthorizationService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionExecutionRollbackRepository rollbackRepository;
    private final ProductionSeparationOfDutiesPolicy sodPolicy;
    private final ProductionChangeAuditService auditService;
    private final Clock clock;

    public ProductionRollbackAuthorizationService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionExecutionRollbackRepository rollbackRepository,
            ProductionSeparationOfDutiesPolicy sodPolicy,
            ProductionChangeAuditService auditService,
            Clock clock
    ) {
        this.changeRepository = changeRepository;
        this.rollbackRepository = rollbackRepository;
        this.sodPolicy = sodPolicy;
        this.auditService = auditService;
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
        if (!ProductionChangeStatus.ROLLBACK_REVIEWED.name().equals(change.getStatus())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_ROLLBACK_AUTHORIZATION_MISSING,
                    "rollback is not reviewed"
            );
        }
        ProductionExecutionRollbackEntity rollback = rollbackRepository
                .findFirstByProductionChangeIdOrderByCreatedAtDesc(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_ROLLBACK_AUTHORIZATION_MISSING,
                        "rollback request not found"
                ));
        sodPolicy.requesterMustNotAuthorize(rollback.getRequesterPrincipalId(), authorizer.actorPrincipalId());
        sodPolicy.reviewerMustNotAuthorize(rollback.getReviewerPrincipalId(), authorizer.actorPrincipalId());
        int generation = rollback.getAuthorizationGeneration() == null ? 1 : rollback.getAuthorizationGeneration() + 1;
        rollback.setAuthorizerPrincipalId(authorizer.actorPrincipalId());
        rollback.setAuthorizationGeneration(generation);
        rollback.setStatus("AUTHORIZED");
        rollback.setUpdatedAt(now);
        change.setStatus(ProductionChangeStatus.ROLLBACK_AUTHORIZED.name());
        change.setUpdatedAt(now);
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_ROLLBACK_AUTHORIZED,
                authorizer.actorPrincipalId(),
                List.of(),
                Map.of("authorizationGeneration", generation)
        );
        return change;
    }
}
