package com.simba.snip.npo.integration;

import java.time.Instant;
import java.util.UUID;

public record ImportLease(
        String leaseKey,
        String sourceSystem,
        String sourceScope,
        UUID ownerExecutionId,
        String ownerInstanceId,
        long fencingToken,
        Instant acquiredAt,
        Instant heartbeatAt,
        Instant expiresAt
) {
    public static String key(String sourceSystem, String sourceScope) {
        return sourceSystem + "/" + sourceScope;
    }
}
