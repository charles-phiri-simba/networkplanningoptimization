package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChangePlanInvalidationPersistenceService {

    private final NetworkChangePlanRepository planRepository;
    private final ChangePlanAuditService auditService;
    private final ChangePlanMetrics metrics;

    public ChangePlanInvalidationPersistenceService(
            NetworkChangePlanRepository planRepository,
            ChangePlanAuditService auditService,
            ChangePlanMetrics metrics
    ) {
        this.planRepository = planRepository;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistInvalidation(UUID planId, String invalidationReason, Instant invalidatedAt, String auditDetail) {
        NetworkChangePlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new DomainNotFoundException("changePlan", planId.toString()));
        if (PlanStatus.INVALIDATED.name().equals(plan.getStatus())) {
            return;
        }
        plan.markInvalidated(invalidationReason, invalidatedAt);
        planRepository.save(plan);
        auditService.append(planId, "PLAN_INVALIDATED", "system", auditDetail);
        metrics.incrementPlansInvalidated();
    }
}
