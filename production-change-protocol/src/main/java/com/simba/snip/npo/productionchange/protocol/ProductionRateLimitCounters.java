package com.simba.snip.npo.productionchange.protocol;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Shared durable rate-limit counter identifiers used by the app grant issuer and every gateway replica.
 */
public final class ProductionRateLimitCounters {

    public static final String TARGET_HOUR = "TARGET_HOUR";
    public static final String CELL_DAY = "CELL_DAY";
    public static final String ACTOR_HOUR = "ACTOR_HOUR";

    private ProductionRateLimitCounters() {
    }

    public static Instant align(Instant now, Duration window) {
        if (window.equals(Duration.ofHours(1))) {
            return now.truncatedTo(ChronoUnit.HOURS);
        }
        if (window.equals(Duration.ofDays(1))) {
            return now.truncatedTo(ChronoUnit.DAYS);
        }
        return now.truncatedTo(ChronoUnit.MINUTES);
    }

    public static String counterId(String scopeType, String scopeKey, Instant windowStart) {
        return scopeType + ":" + scopeKey + ":" + windowStart.toEpochMilli();
    }
}
