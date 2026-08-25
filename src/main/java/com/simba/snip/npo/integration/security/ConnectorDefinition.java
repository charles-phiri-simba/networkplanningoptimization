package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.Vendor;

import java.util.Set;

public record ConnectorDefinition(
        String connectorId,
        Vendor vendor,
        String sourceSystem,
        String sourceScope,
        String endpointRef,
        String inventoryPath,
        String credentialRef,
        String trustProfileId,
        String authorizationProfileId,
        String networkPolicyId,
        AuthenticationMethod authenticationMethod,
        CredentialProviderType credentialProvider,
        Set<ConnectorCapability> requiredCapabilities,
        boolean enabled,
        ConnectorMode mode
) {
    public static final String ERICSSON_ENM_INT_INVENTORY_READER = "ERICSSON_ENM_INT_INVENTORY_READER";
    public static final String NOKIA_NETACT_INT_INVENTORY_READER = "NOKIA_NETACT_INT_INVENTORY_READER";

    public ConnectorDefinition {
        requiredCapabilities = requiredCapabilities == null ? Set.of() : Set.copyOf(requiredCapabilities);
        if (inventoryPath == null || !inventoryPath.startsWith("/")) {
            throw new IllegalArgumentException("inventoryPath must be a fixed absolute path");
        }
    }

    public ConnectorIdentity identity() {
        return new ConnectorIdentity(
                connectorId,
                sourceSystem,
                vendor,
                environmentFromId(),
                purposeFromId(),
                credentialRef,
                trustProfileId,
                authorizationProfileId,
                networkPolicyId,
                enabled
        );
    }

    private String environmentFromId() {
        return connectorId.contains("_PROD_") ? "PROD" : "INT";
    }

    private String purposeFromId() {
        return "INVENTORY_READER";
    }
}
