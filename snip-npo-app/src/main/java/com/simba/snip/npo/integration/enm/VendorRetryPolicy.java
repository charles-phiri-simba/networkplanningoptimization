package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.ImportFailureCode;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class VendorRetryPolicy {

    private final int maxAttempts;
    private final Duration initialBackoff;
    private final Duration maxBackoff;

    public VendorRetryPolicy(int maxAttempts, Duration initialBackoff, Duration maxBackoff) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoff = initialBackoff == null ? Duration.ZERO : initialBackoff;
        this.maxBackoff = maxBackoff == null ? this.initialBackoff : maxBackoff;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public boolean retryable(ImportFailureCode code) {
        return code == ImportFailureCode.VENDOR_UNAVAILABLE
                || code == ImportFailureCode.VENDOR_RATE_LIMITED
                || code == ImportFailureCode.VENDOR_TIMEOUT;
    }

    public Duration backoff(int attempt, Duration retryAfter) {
        if (retryAfter != null && !retryAfter.isNegative() && !retryAfter.isZero()) {
            return retryAfter.compareTo(maxBackoff) > 0 ? maxBackoff : retryAfter;
        }
        long millis = initialBackoff.toMillis() * (1L << Math.min(attempt, 8));
        millis = Math.min(millis, maxBackoff.toMillis());
        long jitter = millis <= 0 ? 0 : ThreadLocalRandom.current().nextLong(Math.max(1, millis / 5 + 1));
        return Duration.ofMillis(millis + jitter);
    }
}
