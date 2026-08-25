package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.config.ConnectorSecurityProperties;
import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

@Component
public class AzureKeyVaultCredentialProvider implements ConnectorCredentialProvider {

    private final ConnectorSecurityProperties properties;

    public AzureKeyVaultCredentialProvider(ConnectorSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public CredentialProviderType providerType() {
        return CredentialProviderType.AZURE_KEY_VAULT;
    }

    @Override
    public CredentialHandle resolve(ConnectorIdentity identity) {
        throw unavailable();
    }

    @Override
    public CredentialMetadata metadata(ConnectorIdentity identity) {
        throw unavailable();
    }

    public String vaultUri() {
        return properties.getAzureKeyVault().getVaultUri();
    }

    public String authentication() {
        return properties.getAzureKeyVault().getAuthentication();
    }

    public boolean configured() {
        return properties.getAzureKeyVault().isEnabled()
                && properties.getAzureKeyVault().getVaultUri() != null
                && !properties.getAzureKeyVault().getVaultUri().isBlank();
    }

    private ConnectorSecurityException unavailable() {
        return new ConnectorSecurityException(
                ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED,
                "Azure Key Vault is not connected in Phase 9"
        );
    }
}
