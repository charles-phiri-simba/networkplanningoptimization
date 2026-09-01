package com.simba.snip.npo.integration.security;

public record CredentialReference(
        String credentialRef,
        CredentialProviderType provider,
        String secretIdentifier,
        CredentialType credentialType
) {
}
