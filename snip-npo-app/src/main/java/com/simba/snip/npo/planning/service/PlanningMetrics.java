package com.simba.snip.npo.planning.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class PlanningMetrics {

    private static final Logger log = LoggerFactory.getLogger(PlanningMetrics.class);

    private final AtomicLong scenariosCreated = new AtomicLong();
    private final AtomicLong scenariosUpdated = new AtomicLong();
    private final AtomicLong evaluationsStarted = new AtomicLong();
    private final AtomicLong evaluationsSucceeded = new AtomicLong();
    private final AtomicLong evaluationsPartial = new AtomicLong();
    private final AtomicLong evaluationsFailed = new AtomicLong();
    private final AtomicLong itemsSucceeded = new AtomicLong();
    private final AtomicLong itemsFailed = new AtomicLong();
    private final AtomicLong runsReused = new AtomicLong();
    private final AtomicLong inProgressConflicts = new AtomicLong();

    public void incrementScenariosCreated() {
        scenariosCreated.incrementAndGet();
        log.info("planningScenariosCreated=1");
    }

    public void incrementScenariosUpdated() {
        scenariosUpdated.incrementAndGet();
        log.info("planningScenariosUpdated=1");
    }

    public void incrementEvaluationsStarted() {
        evaluationsStarted.incrementAndGet();
        log.info("planningEvaluationsStarted=1");
    }

    public void incrementEvaluationsSucceeded() {
        evaluationsSucceeded.incrementAndGet();
        log.info("planningEvaluationsSucceeded=1");
    }

    public void incrementEvaluationsPartial() {
        evaluationsPartial.incrementAndGet();
        log.info("planningEvaluationsPartial=1");
    }

    public void incrementEvaluationsFailed() {
        evaluationsFailed.incrementAndGet();
        log.info("planningEvaluationsFailed=1");
    }

    public void incrementItemsSucceeded() {
        itemsSucceeded.incrementAndGet();
        log.info("planningEvaluationItemsSucceeded=1");
    }

    public void incrementItemsFailed() {
        itemsFailed.incrementAndGet();
        log.info("planningEvaluationItemsFailed=1");
    }

    public void incrementRunsReused() {
        runsReused.incrementAndGet();
        log.info("planningEvaluationRunsReused=1");
    }

    public void incrementInProgressConflicts() {
        inProgressConflicts.incrementAndGet();
        log.info("planningEvaluationInProgressConflicts=1");
    }
}
