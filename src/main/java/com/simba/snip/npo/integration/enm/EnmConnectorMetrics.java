package com.simba.snip.npo.integration.enm;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class EnmConnectorMetrics {

    private final AtomicLong connectorSessions = new AtomicLong();
    private final AtomicLong connectorSessionFailures = new AtomicLong();
    private final AtomicLong vendorRequests = new AtomicLong();
    private final AtomicLong vendorRequestFailures = new AtomicLong();
    private final AtomicLong vendorThrottles = new AtomicLong();
    private final AtomicLong vendorRetries = new AtomicLong();
    private final AtomicLong pagesRead = new AtomicLong();
    private final AtomicLong entitiesRead = new AtomicLong();
    private final AtomicLong snapshotPartial = new AtomicLong();
    private final AtomicLong snapshotFailed = new AtomicLong();
    private final AtomicLong leaseLostDuringAcquisition = new AtomicLong();
    private final AtomicLong connectorCancellation = new AtomicLong();

    public void incrementSessions() {
        connectorSessions.incrementAndGet();
    }

    public void incrementSessionFailures() {
        connectorSessionFailures.incrementAndGet();
    }

    public void incrementVendorRequests() {
        vendorRequests.incrementAndGet();
    }

    public void incrementVendorRequestFailures() {
        vendorRequestFailures.incrementAndGet();
    }

    public void incrementThrottles() {
        vendorThrottles.incrementAndGet();
    }

    public void incrementRetries() {
        vendorRetries.incrementAndGet();
    }

    public void incrementPagesRead() {
        pagesRead.incrementAndGet();
    }

    public void recordPages(int pages, int entities) {
        pagesRead.addAndGet(pages);
        entitiesRead.addAndGet(entities);
    }

    public void incrementSnapshotPartial() {
        snapshotPartial.incrementAndGet();
    }

    public void incrementSnapshotFailed() {
        snapshotFailed.incrementAndGet();
    }

    public void incrementLeaseLostDuringAcquisition() {
        leaseLostDuringAcquisition.incrementAndGet();
    }

    public void incrementCancellation() {
        connectorCancellation.incrementAndGet();
    }

    public long connectorSessions() {
        return connectorSessions.get();
    }

    public long vendorRetries() {
        return vendorRetries.get();
    }

    public long vendorThrottles() {
        return vendorThrottles.get();
    }

    public long snapshotPartial() {
        return snapshotPartial.get();
    }

    public long snapshotFailed() {
        return snapshotFailed.get();
    }

    public long connectorCancellation() {
        return connectorCancellation.get();
    }

    public long leaseLostDuringAcquisition() {
        return leaseLostDuringAcquisition.get();
    }

    public long pagesRead() {
        return pagesRead.get();
    }

    public long entitiesRead() {
        return entitiesRead.get();
    }
}
