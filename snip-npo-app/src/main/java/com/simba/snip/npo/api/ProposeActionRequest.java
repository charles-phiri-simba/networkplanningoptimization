package com.simba.snip.npo.api;

import java.util.Map;
import java.util.UUID;

public record ProposeActionRequest(
        String actionType,
        String capabilityId,
        String targetType,
        String targetId,
        Map<String, Object> parameters,
        String rationale,
        String proposedBy
) {
}
