package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record ActionPolicyDto(
        UUID id,
        String decision,
        String policyId,
        String reason,
        Instant evaluatedAt
) {
}
