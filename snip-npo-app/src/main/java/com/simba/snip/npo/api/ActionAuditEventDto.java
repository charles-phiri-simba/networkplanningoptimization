package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record ActionAuditEventDto(
        UUID id,
        String eventType,
        String actor,
        Instant occurredAt,
        String details
) {
}
