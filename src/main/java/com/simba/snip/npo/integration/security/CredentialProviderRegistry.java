package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.config.ConnectorSecurityProperties;
import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class CredentialProviderRegistry {

    private final ConnectorSecurityProperties properties;
    private final Map<CredentialProviderType, ConnectorCredentialProvider> providers =
            new EnumMap<>(CredentialProviderType.class);

    public CredentialProviderRegistry(
            ConnectorSecurityProperties properties,
            List<ConnectorCredentialProvider> providers
    ) {
        this.properties = properties;
        for (ConnectorCredentialProvider provider : providers) {
            this.providers.put(provider.providerType(), provider);
        }
    }

    public ConnectorCredentialProvider require(CredentialProviderType type) {
        if (type == CredentialProviderType.LOCAL_DEVELOPMENT && properties.isProductionRuntime()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_UNAVAILABLE, "local credential provider is disabled in production");
        }
        ConnectorCredentialProvider provider = providers.get(type);
        if (provider == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential provider is not registered");
        }
        return provider;
    }
}
