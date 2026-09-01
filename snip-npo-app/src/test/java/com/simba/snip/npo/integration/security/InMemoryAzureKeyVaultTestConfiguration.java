package com.simba.snip.npo.integration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class InMemoryAzureKeyVaultTestConfiguration {

    @Bean
    @Primary
    public AzureKeyVaultSecretAccessor inMemoryAzureKeyVaultSecretAccessor() {
        return new InMemoryAzureKeyVaultSecretAccessor();
    }
}
