package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record ActionApprovalDto(
        UUID id,
        String decision,
        String decidedBy,
        Instant decidedAt,
        String comment
) {
}
