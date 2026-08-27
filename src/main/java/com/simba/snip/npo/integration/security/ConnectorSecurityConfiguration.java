package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.config.ConnectorSecurityProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConnectorSecurityConfiguration {

    @Bean
    public Clock connectorClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(AzureKeyVaultSecretAccessor.class)
    public AzureKeyVaultSecretAccessor azureKeyVaultSecretAccessor(ConnectorSecurityProperties properties) {
        if (properties.getAzureKeyVault().isEnabled() && properties.getAzureKeyVault().isUseSdkClient()) {
            return SdkAzureKeyVaultSecretAccessor.create(properties);
        }
        return new UnconfiguredAzureKeyVaultSecretAccessor();
    }

    @Bean
    public ApplicationRunner connectorProductionSecurityGuard(ConnectorSecurityProperties properties) {
        return args -> {
            if (!properties.isProductionRuntime()) {
                return;
            }
            if (properties.isLocalCredentialsEnabled()) {
                throw new IllegalStateException("local credential provider is disabled in production");
            }
            if (!properties.getAzureKeyVault().isEnabled() || !properties.getAzureKeyVault().workloadIdentity()) {
                throw new IllegalStateException("production runtime requires Workload Identity Key Vault");
            }
            if (properties.getAzureKeyVault().getVaultUri() == null
                    || properties.getAzureKeyVault().getVaultUri().isBlank()) {
                throw new IllegalStateException("production runtime requires a Key Vault URI");
            }
            if (properties.getAzureKeyVault().defaultAzureCredential()) {
                throw new IllegalStateException("DefaultAzureCredential is not permitted in production");
            }
        };
    }
}
