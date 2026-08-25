package com.simba.snip.npo.integration.security;

public record ConnectorSecurityContext(
        ConnectorIdentity connectorIdentity,
        CredentialHandle credentialHandle,
        ConnectorAuthorizationProfile authorizationProfile,
        ConnectorTrustProfile trustProfile,
        ConnectorNetworkPolicy networkPolicy
) {
    @Override
    public String toString() {
        return "ConnectorSecurityContext[connectorId="
                + (connectorIdentity == null ? null : connectorIdentity.connectorId())
                + ", credentialRef="
                + (connectorIdentity == null ? null : connectorIdentity.credentialRef())
                + "]";
    }
}
