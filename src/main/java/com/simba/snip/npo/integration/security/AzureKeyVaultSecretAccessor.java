package com.simba.snip.npo.integration.security;

public interface AzureKeyVaultSecretAccessor {

    ResolvedVaultSecret get(AzureVaultCredentialReference reference);
}
