package com.simba.snip.npo.telemetry;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class TelemetryMetrics {

    private final AtomicLong eventsConsumed = new AtomicLong();
    private final AtomicLong eventsProjected = new AtomicLong();
    private final AtomicLong duplicatesIgnored = new AtomicLong();
    private final AtomicLong eventsDlq = new AtomicLong();
    private final AtomicLong lastProjectionLatencyMs = new AtomicLong();

    public void incrementConsumed() {
        eventsConsumed.incrementAndGet();
    }

    public void incrementProjected() {
        eventsProjected.incrementAndGet();
    }

    public void incrementDuplicatesIgnored() {
        duplicatesIgnored.incrementAndGet();
    }

    public void incrementDlq() {
        eventsDlq.incrementAndGet();
    }

    public void recordProjectionLatencyMs(long latencyMs) {
        lastProjectionLatencyMs.set(latencyMs);
    }

    public long getEventsConsumed() {
        return eventsConsumed.get();
    }

    public long getEventsProjected() {
        return eventsProjected.get();
    }

    public long getDuplicatesIgnored() {
        return duplicatesIgnored.get();
    }

    public long getEventsDlq() {
        return eventsDlq.get();
    }

    public long getLastProjectionLatencyMs() {
        return lastProjectionLatencyMs.get();
    }
}
