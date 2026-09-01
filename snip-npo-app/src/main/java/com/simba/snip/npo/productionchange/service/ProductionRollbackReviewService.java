package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.domain.ReviewDecision;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionRollbackEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
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
public class ProductionRollbackReviewService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionExecutionRollbackRepository rollbackRepository;
    private final ProductionChangeAuditService auditService;
    private final Clock clock;

    public ProductionRollbackReviewService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionExecutionRollbackRepository rollbackRepository,
            ProductionChangeAuditService auditService,
            Clock clock
    ) {
        this.changeRepository = changeRepository;
        this.rollbackRepository = rollbackRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public ProductionNetworkChangeEntity review(
            UUID productionChangeId,
            ReviewDecision decision,
            ActorPrincipal reviewer
    ) {
        Instant now = clock.instant();
        ProductionNetworkChangeEntity change = changeRepository.lockById(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                        "production change not found"
                ));
        auditService.requireMutable(change);
        if (!ProductionChangeStatus.ROLLBACK_REQUESTED.name().equals(change.getStatus())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_ROLLBACK_BLOCKED,
                    "rollback is not awaiting review"
            );
        }
        ProductionExecutionRollbackEntity rollback = rollbackRepository
                .findFirstByProductionChangeIdOrderByCreatedAtDesc(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_ROLLBACK_BLOCKED,
                        "rollback request not found"
                ));
        rollback.setReviewerPrincipalId(reviewer.actorPrincipalId());
        rollback.setUpdatedAt(now);
        if (decision == ReviewDecision.REJECTED) {
            rollback.setStatus("REVIEW_REJECTED");
            change.setStatus(ProductionChangeStatus.RECOVERY_REQUIRED.name());
            change.setReasonCode(ProductionReasonCode.PRODUCTION_ROLLBACK_BLOCKED.name());
        } else {
            rollback.setStatus("REVIEWED");
            change.setStatus(ProductionChangeStatus.ROLLBACK_REVIEWED.name());
        }
        change.setUpdatedAt(now);
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_ROLLBACK_REVIEWED,
                reviewer.actorPrincipalId(),
                List.of(),
                Map.of("decision", decision.name())
        );
        return change;
    }
}
