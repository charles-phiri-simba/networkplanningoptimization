package com.simba.snip.npo.integration.security;

public interface ConnectorCredentialProvider {

    CredentialHandle resolve(ConnectorIdentity identity);

    CredentialMetadata metadata(ConnectorIdentity identity);

    CredentialProviderType providerType();
}
