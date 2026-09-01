package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.config.ConnectorSecurityProperties;
import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AzureVaultReferenceRegistry {

    private final ConnectorSecurityProperties properties;
    private final Map<String, CredentialType> types = new ConcurrentHashMap<>();

    public AzureVaultReferenceRegistry(ConnectorSecurityProperties properties) {
        this.properties = properties;
    }

    public void bindType(String credentialRef, CredentialType type) {
        types.put(credentialRef, type);
    }

    public AzureVaultCredentialReference require(ConnectorIdentity identity) {
        ConnectorSecurityProperties.AzureKeyVault vault = properties.getAzureKeyVault();
        if (vault.getVaultUri() == null || vault.getVaultUri().isBlank()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_UNAVAILABLE, "Azure Key Vault is not configured");
        }
        String secretName = vault.getSecretNames().get(identity.credentialRef());
        if (secretName == null || secretName.isBlank()) {
            secretName = defaultSecretName(vault.getEnvironment(), identity);
        }
        String pinned = vault.getPinnedVersions().get(identity.credentialRef());
        CredentialType type = types.getOrDefault(identity.credentialRef(), CredentialType.USERNAME_PASSWORD);
        return new AzureVaultCredentialReference(vault.getVaultUri(), secretName, pinned, type);
    }

    public AzureVaultCredentialReference trust(String trustProfileId) {
        ConnectorSecurityProperties.AzureKeyVault vault = properties.getAzureKeyVault();
        String secretName = vault.getTrustSecretNames().get(trustProfileId);
        if (secretName == null || secretName.isBlank()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.TRUST_MATERIAL_RESOLUTION_FAILED, "trust material reference is missing");
        }
        if (vault.getVaultUri() == null || vault.getVaultUri().isBlank()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_UNAVAILABLE, "Azure Key Vault is not configured");
        }
        String pinned = vault.getPinnedVersions().get("trust:" + trustProfileId);
        return new AzureVaultCredentialReference(
                vault.getVaultUri(), secretName, pinned, CredentialType.CLIENT_CERTIFICATE);
    }

    public boolean hasTrustMaterial(String trustProfileId) {
        String secretName = properties.getAzureKeyVault().getTrustSecretNames().get(trustProfileId);
        return secretName != null && !secretName.isBlank();
    }

    private static String defaultSecretName(String environment, ConnectorIdentity identity) {
        String vendor = identity.vendor() == null ? "generic" : identity.vendor().name().toLowerCase();
        String env = environment == null || environment.isBlank() ? "int" : environment.toLowerCase();
        return "snip-" + env + "-" + vendor + "-inventory-reader";
    }
}
