package com.simba.snip.npo.integration;

import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ImportFaultInjector {

    private final AtomicBoolean failNextCanonicalCommit = new AtomicBoolean();
    private final AtomicReference<CountDownLatch> leaseHeld = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> holdUntil = new AtomicReference<>();

    public void failNextCanonicalCommit() {
        failNextCanonicalCommit.set(true);
    }

    public void armLeaseHeld(CountDownLatch latch) {
        leaseHeld.set(latch);
    }

    public void armHoldUntil(CountDownLatch latch) {
        holdUntil.set(latch);
    }

    public void signalLeaseHeld() {
        CountDownLatch latch = leaseHeld.getAndSet(null);
        if (latch != null) {
            latch.countDown();
        }
    }

    public void awaitHold() {
        CountDownLatch latch = holdUntil.get();
        if (latch == null) {
            return;
        }
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ImportRuntimeException(ImportFailureCode.EXECUTION_TIMEOUT, "lease hold interrupted", ex);
        }
    }

    public void maybeFailCanonicalCommit(String sourceSnapshotId) {
        if (failNextCanonicalCommit.compareAndSet(true, false)) {
            throw new ImportRuntimeException(
                    ImportFailureCode.DATABASE_COMMIT_FAILED,
                    "forced canonical commit failure for " + sourceSnapshotId
            );
        }
    }
}
