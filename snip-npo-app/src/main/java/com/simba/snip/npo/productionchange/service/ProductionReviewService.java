package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.domain.ReviewDecision;
import com.simba.snip.npo.productionchange.entity.ProductionChangeReviewEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionChangeReviewRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionReviewService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionChangeReviewRepository reviewRepository;
    private final ProductionChangeAuditService auditService;
    private final Clock clock;

    public ProductionReviewService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionChangeReviewRepository reviewRepository,
            ProductionChangeAuditService auditService,
            Clock clock
    ) {
        this.changeRepository = changeRepository;
        this.reviewRepository = reviewRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public ProductionNetworkChangeEntity review(
            UUID productionChangeId,
            ReviewDecision decision,
            List<String> reasonCodes,
            ActorPrincipal reviewer
    ) {
        Instant now = clock.instant();
        ProductionNetworkChangeEntity change = changeRepository.lockById(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                        "production change not found"
                ));
        auditService.requireMutable(change);
        if (!ProductionChangeStatus.READY_FOR_REVIEW.name().equals(change.getStatus())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                    "production change is not READY_FOR_REVIEW"
            );
        }
        reviewRepository.save(ProductionChangeReviewEntity.create(
                UUID.randomUUID(),
                change.getProductionChangeId(),
                reviewer.actorPrincipalId(),
                decision.name(),
                reasonCodes == null ? null : String.join(",", reasonCodes),
                now,
                change.getProductionFingerprint()
        ));
        change.setReviewerPrincipalId(reviewer.actorPrincipalId());
        change.setUpdatedAt(now);
        if (decision == ReviewDecision.REJECTED) {
            change.setStatus(ProductionChangeStatus.REVIEW_REJECTED.name());
            change.setReasonCode(ProductionReasonCode.PRODUCTION_POLICY_DENY.name());
            auditService.append(
                    change.getProductionChangeId(),
                    ProductionAuditEventType.PRODUCTION_REVIEW_REJECTED,
                    reviewer.actorPrincipalId(),
                    reasonCodes == null ? List.of() : reasonCodes,
                    Map.of()
            );
        } else {
            change.setStatus(ProductionChangeStatus.REVIEWED.name());
            auditService.append(
                    change.getProductionChangeId(),
                    ProductionAuditEventType.PRODUCTION_REVIEW_APPROVED,
                    reviewer.actorPrincipalId(),
                    List.of(),
                    Map.of()
            );
        }
        return change;
    }
}
