package com.simba.snip.npo.changeexecution.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ExecutionMetrics {

    private static final Logger log = LoggerFactory.getLogger(ExecutionMetrics.class);

    private final AtomicLong requested = new AtomicLong();
    private final AtomicLong admitted = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong started = new AtomicLong();
    private final AtomicLong verified = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong outcomeUnknown = new AtomicLong();
    private final AtomicLong rollbackRequested = new AtomicLong();
    private final AtomicLong rollbackVerified = new AtomicLong();
    private final AtomicLong manualIntervention = new AtomicLong();
    private final AtomicLong leaseAcquired = new AtomicLong();
    private final AtomicLong leaseRejected = new AtomicLong();

    public void incrementRequested() { requested.incrementAndGet(); log.debug("snip_change_execution_requested_total=1"); }
    public void incrementAdmitted() { admitted.incrementAndGet(); log.debug("snip_change_execution_admitted_total=1"); }
    public void incrementRejected() { rejected.incrementAndGet(); log.debug("snip_change_execution_rejected_total=1"); }
    public void incrementStarted() { started.incrementAndGet(); log.debug("snip_change_execution_started_total=1"); }
    public void incrementVerified() { verified.incrementAndGet(); log.debug("snip_change_execution_verified_total=1"); }
    public void incrementFailed() { failed.incrementAndGet(); log.debug("snip_change_execution_failed_total=1"); }
    public void incrementOutcomeUnknown() { outcomeUnknown.incrementAndGet(); log.debug("snip_change_execution_outcome_unknown_total=1"); }
    public void incrementRollbackRequested() { rollbackRequested.incrementAndGet(); log.debug("snip_change_execution_rollback_requested_total=1"); }
    public void incrementRollbackVerified() { rollbackVerified.incrementAndGet(); log.debug("snip_change_execution_rollback_verified_total=1"); }
    public void incrementManualIntervention() { manualIntervention.incrementAndGet(); log.debug("snip_change_execution_manual_intervention_total=1"); }
    public void incrementLeaseAcquired() { leaseAcquired.incrementAndGet(); }
    public void incrementLeaseRejected() { leaseRejected.incrementAndGet(); }

    public long requested() { return requested.get(); }
    public long admitted() { return admitted.get(); }
    public long rejected() { return rejected.get(); }
    public long started() { return started.get(); }
    public long verified() { return verified.get(); }
    public long failed() { return failed.get(); }
    public long outcomeUnknown() { return outcomeUnknown.get(); }
    public long rollbackRequested() { return rollbackRequested.get(); }
    public long rollbackVerified() { return rollbackVerified.get(); }
    public long manualIntervention() { return manualIntervention.get(); }
    public long leaseAcquired() { return leaseAcquired.get(); }
    public long leaseRejected() { return leaseRejected.get(); }
}
