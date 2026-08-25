package com.simba.snip.npo.integration.security;

import java.util.List;
import java.util.Set;

public record ConnectorNetworkPolicy(
        String networkPolicyId,
        List<String> allowedHostnames,
        Set<Integer> allowedPorts,
        boolean httpsOnly,
        boolean allowRedirects
) {
    public ConnectorNetworkPolicy {
        allowedHostnames = allowedHostnames == null ? List.of() : List.copyOf(allowedHostnames);
        allowedPorts = allowedPorts == null ? Set.of() : Set.copyOf(allowedPorts);
        if (!httpsOnly) {
            throw new IllegalArgumentException("httpsOnly must be true");
        }
        if (allowRedirects) {
            throw new IllegalArgumentException("redirects are disabled");
        }
    }
}
