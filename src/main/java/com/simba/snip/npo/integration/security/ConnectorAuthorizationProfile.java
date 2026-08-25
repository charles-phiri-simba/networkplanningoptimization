package com.simba.snip.npo.integration.security;

import java.util.EnumSet;
import java.util.Set;

public record ConnectorAuthorizationProfile(
        String authorizationProfileId,
        Set<ConnectorCapability> allowedCapabilities
) {
    public static final String READ_ONLY_NETWORK_INVENTORY = "READ_ONLY_NETWORK_INVENTORY";

    public static final Set<ConnectorCapability> READ_INVENTORY_CAPABILITIES = EnumSet.of(
            ConnectorCapability.READ_SITE,
            ConnectorCapability.READ_GNB,
            ConnectorCapability.READ_CELL,
            ConnectorCapability.READ_CONFIGURATION,
            ConnectorCapability.READ_NEIGHBOURS
    );

    public ConnectorAuthorizationProfile {
        allowedCapabilities = allowedCapabilities == null
                ? Set.of()
                : Set.copyOf(allowedCapabilities);
    }

    public static ConnectorAuthorizationProfile readOnlyNetworkInventory() {
        return new ConnectorAuthorizationProfile(READ_ONLY_NETWORK_INVENTORY, READ_INVENTORY_CAPABILITIES);
    }

    public boolean allowsAll(Set<ConnectorCapability> required) {
        return required != null && allowedCapabilities.containsAll(required);
    }
}
