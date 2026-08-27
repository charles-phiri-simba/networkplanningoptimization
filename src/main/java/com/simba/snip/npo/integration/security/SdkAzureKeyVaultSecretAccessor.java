package com.simba.snip.npo.integration.security;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.identity.WorkloadIdentityCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.simba.snip.npo.config.ConnectorSecurityProperties;
import com.simba.snip.npo.integration.ImportFailureCode;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class SdkAzureKeyVaultSecretAccessor implements AzureKeyVaultSecretAccessor {

    private final SecretClient client;
    private final Duration timeout;

    public SdkAzureKeyVaultSecretAccessor(SecretClient client, Duration timeout) {
        this.client = client;
        this.timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
    }

    public static SdkAzureKeyVaultSecretAccessor create(ConnectorSecurityProperties properties) {
        ConnectorSecurityProperties.AzureKeyVault vault = properties.getAzureKeyVault();
        if (properties.isProductionRuntime() && vault.defaultAzureCredential()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_AUTHENTICATION_FAILED,
                    "production runtime must use Workload Identity");
        }
        com.azure.core.credential.TokenCredential credential;
        if (vault.workloadIdentity()) {
            WorkloadIdentityCredentialBuilder builder = new WorkloadIdentityCredentialBuilder();
            if (vault.getClientId() != null && !vault.getClientId().isBlank()) {
                builder.clientId(vault.getClientId());
            }
            credential = builder.build();
        } else if (vault.defaultAzureCredential() && !properties.isProductionRuntime()) {
            credential = new DefaultAzureCredentialBuilder().build();
        } else {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_AUTHENTICATION_FAILED,
                    "unsupported Azure authentication method");
        }
        SecretClient client = new SecretClientBuilder()
                .vaultUrl(vault.getVaultUri())
                .credential(credential)
                .buildClient();
        return new SdkAzureKeyVaultSecretAccessor(client, vault.getTimeout());
    }

    @Override
    public ResolvedVaultSecret get(AzureVaultCredentialReference reference) {
        try {
            KeyVaultSecret secret = CompletableFuture.supplyAsync(() -> {
                if (reference.pinnedVersion() == null) {
                    return client.getSecret(reference.secretName());
                }
                return client.getSecret(reference.secretName(), reference.pinnedVersion());
            }).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (secret == null || secret.getProperties() == null) {
                throw new ConnectorSecurityException(
                        ImportFailureCode.VAULT_SECRET_NOT_FOUND, "vault secret was not found");
            }
            Boolean enabled = secret.getProperties().isEnabled();
            if (enabled != null && !enabled) {
                throw new ConnectorSecurityException(
                        ImportFailureCode.VAULT_SECRET_DISABLED, "vault secret is disabled");
            }
            return new ResolvedVaultSecret(
                    secret.getName(),
                    secret.getProperties().getVersion(),
                    secret.getValue(),
                    true
            );
        } catch (ConnectorSecurityException ex) {
            throw ex;
        } catch (TimeoutException ex) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_UNAVAILABLE, "vault is unavailable", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_UNAVAILABLE, "vault is unavailable", ex);
        } catch (ExecutionException ex) {
            throw mapFailure(ex.getCause() == null ? ex : ex.getCause());
        } catch (RuntimeException ex) {
            throw mapFailure(ex);
        }
    }

    private static ConnectorSecurityException mapFailure(Throwable ex) {
        if (ex instanceof ConnectorSecurityException security) {
            return security;
        }
        if (ex instanceof ResourceNotFoundException) {
            return new ConnectorSecurityException(
                    ImportFailureCode.VAULT_SECRET_NOT_FOUND, "vault secret was not found", ex);
        }
        if (ex instanceof HttpResponseException http) {
            int status = http.getResponse() == null ? 0 : http.getResponse().getStatusCode();
            String azureCode = http.getMessage() == null ? "" : http.getMessage();
            if (status == 401) {
                return new ConnectorSecurityException(
                        ImportFailureCode.VAULT_AUTHENTICATION_FAILED, "vault authentication failed", http);
            }
            if (status == 403 && azureCode.contains("SecretDisabled")) {
                return new ConnectorSecurityException(
                        ImportFailureCode.VAULT_SECRET_DISABLED, "vault secret is disabled", http);
            }
            if (status == 403) {
                return new ConnectorSecurityException(
                        ImportFailureCode.VAULT_ACCESS_DENIED, "vault access denied", http);
            }
            return new ConnectorSecurityException(
                    ImportFailureCode.VAULT_UNAVAILABLE, "vault is unavailable", http);
        }
        String name = ex.getClass().getSimpleName();
        if (name.contains("Credential") || name.contains("Authentication")) {
            return new ConnectorSecurityException(
                    ImportFailureCode.VAULT_AUTHENTICATION_FAILED, "vault authentication failed", ex);
        }
        return new ConnectorSecurityException(
                ImportFailureCode.VAULT_UNAVAILABLE, "vault is unavailable", ex);
    }
}
