package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.ChangePlanException;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import com.simba.snip.npo.changeplanning.model.PlanStatus;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanReviewEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanReviewRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChangePlanGovernanceService {

    private static final List<String> CANCELLABLE = List.of(
            PlanStatus.DRAFT.name(),
            PlanStatus.VALIDATING.name(),
            PlanStatus.PLANNED.name(),
            PlanStatus.SAFETY_EVALUATING.name(),
            PlanStatus.READY_FOR_REVIEW.name(),
            PlanStatus.AUTHORIZED.name(),
            PlanStatus.READY_FOR_EXECUTION.name()
    );

    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangePlanReviewRepository reviewRepository;
    private final ChangePlanValidityService validityService;
    private final ChangePlanFingerprintService fingerprintService;
    private final ChangePlanAuditService auditService;
    private final ChangePlanMetrics metrics;
    private final Clock clock;

    public ChangePlanGovernanceService(
            NetworkChangePlanRepository planRepository,
            NetworkChangePlanReviewRepository reviewRepository,
            ChangePlanValidityService validityService,
            ChangePlanFingerprintService fingerprintService,
            ChangePlanAuditService auditService,
            ChangePlanMetrics metrics,
            Clock clock
    ) {
        this.planRepository = planRepository;
        this.reviewRepository = reviewRepository;
        this.validityService = validityService;
        this.fingerprintService = fingerprintService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangePlanEntity review(UUID planId, String reviewer, String comment) {
        Instant now = clock.instant();
        NetworkChangePlanEntity plan = requireReviewable(planId);
        ChangePlanValidityService.ValidityResult validity = validityService.revalidate(plan);
        if (!validity.valid()) {
            throw new ChangePlanException(validity.failureCode(), validity.reason());
        }
        if (!PlanStatus.READY_FOR_REVIEW.name().equals(plan.getStatus())) {
            throw new ChangePlanException(ChangePlanFailureCode.INVALID_PLAN_STATE, plan.getStatus());
        }
        try {
            plan.markReviewed(reviewer, now);
            planRepository.save(plan);
            reviewRepository.save(NetworkChangePlanReviewEntity.create(
                    UUID.randomUUID(),
                    planId,
                    reviewer,
                    comment,
                    plan.getVersion(),
                    now
            ));
            metrics.incrementPlansReviewed();
            auditService.append(planId, "PLAN_REVIEWED", reviewer, comment);
            return plan;
        } catch (OptimisticLockingFailureException ex) {
            throw new ChangePlanException(ChangePlanFailureCode.CONCURRENT_PLAN_CONFLICT, "concurrent review conflict");
        }
    }

    @Transactional
    public NetworkChangePlanEntity authorize(UUID planId, String authorizer) {
        Instant now = clock.instant();
        NetworkChangePlanEntity plan = requireAuthorizable(planId);
        ChangePlanValidityService.ValidityResult validity = validityService.revalidate(plan);
        if (!validity.valid()) {
            throw new ChangePlanException(validity.failureCode(), validity.reason());
        }
        if (!PlanStatus.READY_FOR_REVIEW.name().equals(plan.getStatus())) {
            throw new ChangePlanException(ChangePlanFailureCode.INVALID_PLAN_STATE, plan.getStatus());
        }
        if (plan.getReviewedAt() == null) {
            throw new ChangePlanException(ChangePlanFailureCode.INVALID_PLAN_STATE, "review required");
        }
        if (reviewRepository.findByPlanIdOrderByReviewedAtAsc(planId).isEmpty()) {
            throw new ChangePlanException(ChangePlanFailureCode.INVALID_PLAN_STATE, "review record required");
        }
        String currentFingerprint = plan.getFingerprint();
        try {
            plan.markAuthorized(authorizer, currentFingerprint, now);
            planRepository.save(plan);
            metrics.incrementPlansAuthorized();
            auditService.append(planId, "PLAN_AUTHORIZED", authorizer, currentFingerprint);
            return plan;
        } catch (OptimisticLockingFailureException ex) {
            throw new ChangePlanException(ChangePlanFailureCode.CONCURRENT_PLAN_CONFLICT, "concurrent authorize conflict");
        }
    }

    @Transactional
    public NetworkChangePlanEntity cancel(UUID planId, String actor, String reason) {
        Instant now = clock.instant();
        NetworkChangePlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new DomainNotFoundException("changePlan", planId.toString()));
        if (!CANCELLABLE.contains(plan.getStatus())) {
            throw new ChangePlanException(ChangePlanFailureCode.INVALID_PLAN_STATE, plan.getStatus());
        }
        try {
            plan.markCancelled(actor, now);
            planRepository.save(plan);
            metrics.incrementPlansCancelled();
            auditService.append(planId, "PLAN_CANCELLED", actor, reason);
            return plan;
        } catch (OptimisticLockingFailureException ex) {
            throw new ChangePlanException(ChangePlanFailureCode.CONCURRENT_PLAN_CONFLICT, "concurrent cancel conflict");
        }
    }

    private NetworkChangePlanEntity requireReviewable(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new DomainNotFoundException("changePlan", planId.toString()));
    }

    private NetworkChangePlanEntity requireAuthorizable(UUID planId) {
        NetworkChangePlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new DomainNotFoundException("changePlan", planId.toString()));
        if (PlanStatus.AUTHORIZED.name().equals(plan.getStatus())
                || PlanStatus.READY_FOR_EXECUTION.name().equals(plan.getStatus())) {
            throw new ChangePlanException(ChangePlanFailureCode.INVALID_PLAN_STATE, plan.getStatus());
        }
        return plan;
    }
}
