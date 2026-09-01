package com.simba.snip.npo.integration.security;

import java.time.Instant;

public record CredentialMetadata(
        String credentialRef,
        CredentialProviderType provider,
        CredentialType credentialType,
        String versionIdentifier,
        Instant resolvedAt,
        Instant expiresAt
) {
}
