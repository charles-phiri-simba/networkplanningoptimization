package com.simba.snip.npo.integration.security;

public record AzureVaultCredentialReference(
        String vaultUri,
        String secretName,
        String pinnedVersion,
        CredentialType credentialType
) {
    public AzureVaultCredentialReference {
        if (vaultUri == null || vaultUri.isBlank() || secretName == null || secretName.isBlank()) {
            throw new IllegalArgumentException("vault reference is incomplete");
        }
        if (credentialType == null) {
            credentialType = CredentialType.USERNAME_PASSWORD;
        }
        pinnedVersion = pinnedVersion == null || pinnedVersion.isBlank() ? null : pinnedVersion;
    }
}
