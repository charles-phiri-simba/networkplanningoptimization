package com.simba.snip.npo.productionchange.domain;

import java.time.Instant;
import java.util.UUID;

public record LeaseHandle(
        UUID leaseId,
        String productionTargetId,
        String cellId,
        String parameter,
        String holderId,
        long fencingToken,
        Instant acquiredAt,
        Instant expiresAt
) {
}
