package com.simba.snip.npo.agent;

import com.simba.snip.npo.domain.DomainValidationException;
import org.springframework.stereotype.Component;

@Component
public class AgentPermissionGuard {

    private final AgentRegistry registry;

    public AgentPermissionGuard(AgentRegistry registry) {
        this.registry = registry;
    }

    public void assertAllowed(String agentId, AgentServiceKind service) {
        AgentDefinition definition = registry.requireEnabled(agentId);
        if (!definition.allowedServices().contains(service)) {
            throw new DomainValidationException(agentId + " is not permitted to use " + service);
        }
    }
}
