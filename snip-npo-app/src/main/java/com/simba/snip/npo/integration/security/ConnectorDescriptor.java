package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.Vendor;

import java.util.Set;

public record ConnectorDescriptor(
        String connectorId,
        Vendor vendor,
        String platform,
        String environment,
        ConnectorImplementationType implementationType,
        ConnectorAccessMode accessMode,
        Set<ConnectorCapability> capabilities
) {
    public ConnectorDescriptor {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }

    public boolean readOnly() {
        return accessMode == ConnectorAccessMode.READ_ONLY
                && capabilities.stream().noneMatch(ConnectorCapability::mutatesNetwork);
    }
}
