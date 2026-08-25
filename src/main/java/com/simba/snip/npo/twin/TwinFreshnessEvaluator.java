package com.simba.snip.npo.twin;

import java.time.Duration;
import java.time.Instant;

/**
 * Deterministic Twin freshness.
 *
 * <pre>
 * EXPIRED if now − synchronizedAt ≥ configured expired hours
 * STALE   else if current operational fingerprint ≠ snapshot fingerprint
 * CURRENT otherwise
 * </pre>
 *
 * Fingerprint covers txPower (value + effectiveFrom) and each current KPI (value + observedAt).
 * A newer operational or telemetry observation therefore makes the Twin STALE.
 * Simulation is allowed only for CURRENT. STALE/EXPIRED require explicit resynchronization.
 * This evaluator never resynchronizes.
 */
public final class TwinFreshnessEvaluator {

    private TwinFreshnessEvaluator() {
    }

    public static TwinFreshness evaluate(
            Instant synchronizedAt,
            String snapshotFingerprint,
            String currentFingerprint,
            Instant now,
            int expiredHours
    ) {
        if (synchronizedAt == null || now == null) {
            return TwinFreshness.EXPIRED;
        }
        if (!now.isBefore(synchronizedAt.plus(Duration.ofHours(expiredHours)))) {
            return TwinFreshness.EXPIRED;
        }
        if (snapshotFingerprint == null || !snapshotFingerprint.equals(currentFingerprint)) {
            return TwinFreshness.STALE;
        }
        return TwinFreshness.CURRENT;
    }
}
