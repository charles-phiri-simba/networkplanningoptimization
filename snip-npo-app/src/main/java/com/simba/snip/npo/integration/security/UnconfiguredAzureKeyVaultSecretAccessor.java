package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;

public final class UnconfiguredAzureKeyVaultSecretAccessor implements AzureKeyVaultSecretAccessor {

    @Override
    public ResolvedVaultSecret get(AzureVaultCredentialReference reference) {
        throw new ConnectorSecurityException(
                ImportFailureCode.VAULT_UNAVAILABLE, "Azure Key Vault is not configured");
    }
}
