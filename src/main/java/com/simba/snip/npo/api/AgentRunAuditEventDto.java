package com.simba.snip.npo.api;

import java.time.Instant;
import java.util.UUID;

public record AgentRunAuditEventDto(
        UUID id,
        String eventType,
        String agentId,
        Instant occurredAt,
        String summary
) {
}
