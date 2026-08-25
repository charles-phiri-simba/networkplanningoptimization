package com.simba.snip.npo.twin;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TwinFreshnessEvaluatorTest {

    @Test
    void matchingFingerprintWithinWindowIsCurrent() {
        Instant synced = Instant.parse("2026-08-25T10:00:00Z");
        assertEquals(TwinFreshness.CURRENT, TwinFreshnessEvaluator.evaluate(
                synced, "fp-1", "fp-1", synced.plusSeconds(60), 24));
    }

    @Test
    void operationalFingerprintChangeIsStale() {
        Instant synced = Instant.parse("2026-08-25T10:00:00Z");
        assertEquals(TwinFreshness.STALE, TwinFreshnessEvaluator.evaluate(
                synced, "fp-1", "fp-2", synced.plusSeconds(60), 24));
    }

    @Test
    void ageAtOrBeyondThresholdIsExpired() {
        Instant synced = Instant.parse("2026-08-25T10:00:00Z");
        assertEquals(TwinFreshness.EXPIRED, TwinFreshnessEvaluator.evaluate(
                synced, "fp-1", "fp-1", synced.plusSeconds(24 * 3600), 24));
    }
}
