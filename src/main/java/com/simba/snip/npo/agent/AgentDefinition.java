package com.simba.snip.npo.agent;

import java.util.Set;

public record AgentDefinition(
        String agentId,
        AgentRole role,
        String description,
        boolean enabled,
        Set<AgentServiceKind> allowedServices,
        String modelProfile,
        double temperature,
        int maxOutputTokens,
        long timeoutMs,
        int maxCalls
) {
}
