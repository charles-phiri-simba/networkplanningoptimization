package com.simba.snip.npo.changeplanning.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ChangePlanMetrics {

    private static final Logger log = LoggerFactory.getLogger(ChangePlanMetrics.class);

    private final AtomicLong plansCreated = new AtomicLong();
    private final AtomicLong plansBlocked = new AtomicLong();
    private final AtomicLong plansReviewed = new AtomicLong();
    private final AtomicLong plansAuthorized = new AtomicLong();
    private final AtomicLong plansInvalidated = new AtomicLong();
    private final AtomicLong plansCancelled = new AtomicLong();
    private final AtomicLong readinessChecks = new AtomicLong();
    private final AtomicLong readinessReady = new AtomicLong();
    private final AtomicLong readinessNotReady = new AtomicLong();

    public void incrementPlansCreated() {
        plansCreated.incrementAndGet();
        log.debug("plans_created_total=1");
    }

    public void incrementPlansBlocked() {
        plansBlocked.incrementAndGet();
        log.debug("plans_blocked_total=1");
    }

    public void incrementPlansReviewed() {
        plansReviewed.incrementAndGet();
        log.debug("plans_reviewed_total=1");
    }

    public void incrementPlansAuthorized() {
        plansAuthorized.incrementAndGet();
        log.debug("plans_authorized_total=1");
    }

    public void incrementPlansInvalidated() {
        plansInvalidated.incrementAndGet();
        log.debug("plans_invalidated_total=1");
    }

    public void incrementPlansCancelled() {
        plansCancelled.incrementAndGet();
        log.debug("plans_cancelled_total=1");
    }

    public void incrementReadinessChecks() {
        readinessChecks.incrementAndGet();
        log.debug("readiness_checks_total=1");
    }

    public void incrementReadinessReady() {
        readinessReady.incrementAndGet();
        log.debug("readiness_ready_total=1");
    }

    public void incrementReadinessNotReady() {
        readinessNotReady.incrementAndGet();
        log.debug("readiness_not_ready_total=1");
    }

    public long plansCreated() { return plansCreated.get(); }
    public long plansBlocked() { return plansBlocked.get(); }
    public long plansReviewed() { return plansReviewed.get(); }
    public long plansAuthorized() { return plansAuthorized.get(); }
    public long plansInvalidated() { return plansInvalidated.get(); }
    public long plansCancelled() { return plansCancelled.get(); }
    public long readinessChecks() { return readinessChecks.get(); }
    public long readinessReady() { return readinessReady.get(); }
    public long readinessNotReady() { return readinessNotReady.get(); }
}
