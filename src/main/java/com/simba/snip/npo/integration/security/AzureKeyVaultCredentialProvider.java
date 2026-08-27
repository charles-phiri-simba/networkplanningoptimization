package com.simba.snip.npo.integration.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.config.ConnectorSecurityProperties;
import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;

@Component
public class AzureKeyVaultCredentialProvider implements ConnectorCredentialProvider {

    private final ConnectorSecurityProperties properties;
    private final AzureKeyVaultSecretAccessor accessor;
    private final AzureVaultReferenceRegistry references;
    private final ConnectorSecurityMetrics metrics;
    private final ObjectMapper objectMapper;
    private volatile String lastVersion;

    public AzureKeyVaultCredentialProvider(
            ConnectorSecurityProperties properties,
            AzureKeyVaultSecretAccessor accessor,
            AzureVaultReferenceRegistry references,
            ConnectorSecurityMetrics metrics,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.accessor = accessor;
        this.references = references;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    @Override
    public CredentialProviderType providerType() {
        return CredentialProviderType.AZURE_KEY_VAULT;
    }

    @Override
    public CredentialHandle resolve(ConnectorIdentity identity) {
        if (properties.isProductionRuntime() && properties.isLocalCredentialsEnabled()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_UNAVAILABLE,
                    "local credential provider is disabled in production");
        }
        AzureVaultCredentialReference reference = references.require(identity);
        try {
            ResolvedVaultSecret secret = accessor.get(reference);
            if (secret.value() == null || secret.value().isBlank()) {
                throw new ConnectorSecurityException(
                        ImportFailureCode.VAULT_SECRET_NOT_FOUND, "vault secret was not found");
            }
            if (lastVersion != null && !lastVersion.equals(secret.version())) {
                metrics.incrementCredentialVersionChangesObserved();
            }
            lastVersion = secret.version();
            metrics.incrementVaultCredentialResolutions();
            CredentialMetadata metadata = new CredentialMetadata(
                    identity.credentialRef(),
                    CredentialProviderType.AZURE_KEY_VAULT,
                    reference.credentialType(),
                    secret.version(),
                    Instant.now(),
                    null
            );
            return parse(reference.credentialType(), metadata, secret.value());
        } catch (ConnectorSecurityException ex) {
            metrics.incrementVaultFailure(ex.failureCode());
            throw ex;
        }
    }

    @Override
    public CredentialMetadata metadata(ConnectorIdentity identity) {
        AzureVaultCredentialReference reference = references.require(identity);
        return new CredentialMetadata(
                identity.credentialRef(),
                CredentialProviderType.AZURE_KEY_VAULT,
                reference.credentialType(),
                lastVersion,
                Instant.now(),
                null
        );
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

    public boolean workloadIdentityConfigured() {
        return configured() && properties.getAzureKeyVault().workloadIdentity();
    }

    private CredentialHandle parse(CredentialType type, CredentialMetadata metadata, String payload) {
        try {
            if (type == CredentialType.CLIENT_CERTIFICATE) {
                JsonNode node = objectMapper.readTree(payload);
                byte[] certificate = Base64.getDecoder().decode(text(node, "certificateDerBase64"));
                byte[] key = Base64.getDecoder().decode(text(node, "privateKeyPkcs8Base64"));
                if (node.hasNonNull("username") && node.hasNonNull("password")) {
                    return CredentialHandle.basicPlusMtls(
                            metadata,
                            text(node, "username"),
                            text(node, "password").toCharArray(),
                            certificate,
                            key
                    );
                }
                return CredentialHandle.clientCertificate(metadata, certificate, key);
            }
            JsonNode node = objectMapper.readTree(payload);
            return CredentialHandle.usernamePassword(
                    metadata,
                    text(node, "username"),
                    text(node, "password").toCharArray()
            );
        } catch (ConnectorSecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "vault secret payload is invalid", ex);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "vault secret payload is invalid");
        }
        return value.asText();
    }
}
