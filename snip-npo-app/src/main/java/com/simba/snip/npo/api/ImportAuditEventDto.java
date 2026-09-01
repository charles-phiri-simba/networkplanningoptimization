package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record ImportAuditEventDto(
        UUID id,
        String eventType,
        Instant occurredAt,
        String details
) {
}
