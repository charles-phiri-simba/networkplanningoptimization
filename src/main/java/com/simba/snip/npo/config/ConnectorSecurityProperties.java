package com.simba.snip.npo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "snip.integration.security")
public class ConnectorSecurityProperties {

    public static final String AUTH_WORKLOAD_IDENTITY = "WORKLOAD_IDENTITY";
    public static final String AUTH_MANAGED_IDENTITY = "MANAGED_IDENTITY";
    public static final String AUTH_DEFAULT_AZURE = "DEFAULT_AZURE_CREDENTIAL";

    private boolean localCredentialsEnabled = false;
    private boolean productionRuntime = false;
    private boolean networkPolicyConfigured = false;
    private boolean enableMockConnectors = false;
    private String mockVendorHost = "";
    private AzureKeyVault azureKeyVault = new AzureKeyVault();

    public boolean isLocalCredentialsEnabled() {
        return localCredentialsEnabled;
    }

    public void setLocalCredentialsEnabled(boolean localCredentialsEnabled) {
        this.localCredentialsEnabled = localCredentialsEnabled;
    }

    public boolean isProductionRuntime() {
        return productionRuntime;
    }

    public void setProductionRuntime(boolean productionRuntime) {
        this.productionRuntime = productionRuntime;
    }

    public boolean isNetworkPolicyConfigured() {
        return networkPolicyConfigured;
    }

    public void setNetworkPolicyConfigured(boolean networkPolicyConfigured) {
        this.networkPolicyConfigured = networkPolicyConfigured;
    }

    public boolean isEnableMockConnectors() {
        return enableMockConnectors;
    }

    public void setEnableMockConnectors(boolean enableMockConnectors) {
        this.enableMockConnectors = enableMockConnectors;
    }

    public String getMockVendorHost() {
        return mockVendorHost;
    }

    public void setMockVendorHost(String mockVendorHost) {
        this.mockVendorHost = mockVendorHost == null ? "" : mockVendorHost;
    }

    public AzureKeyVault getAzureKeyVault() {
        return azureKeyVault;
    }

    public void setAzureKeyVault(AzureKeyVault azureKeyVault) {
        this.azureKeyVault = azureKeyVault;
    }

    public boolean localFallbackProhibited() {
        return productionRuntime || azureKeyVault.isEnabled();
    }

    public static class AzureKeyVault {
        private boolean enabled = false;
        private String vaultUri = "";
        private String authentication = AUTH_WORKLOAD_IDENTITY;
        private String environment = "INT";
        private String clientId = "";
        private Duration timeout = Duration.ofSeconds(5);
        private Map<String, String> secretNames = new LinkedHashMap<>();
        private Map<String, String> trustSecretNames = new LinkedHashMap<>();
        private Map<String, String> pinnedVersions = new LinkedHashMap<>();
        private boolean useSdkClient = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getVaultUri() {
            return vaultUri;
        }

        public void setVaultUri(String vaultUri) {
            this.vaultUri = vaultUri;
        }

        public String getAuthentication() {
            return authentication;
        }

        public void setAuthentication(String authentication) {
            this.authentication = authentication;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Map<String, String> getSecretNames() {
            return secretNames;
        }

        public void setSecretNames(Map<String, String> secretNames) {
            this.secretNames = secretNames == null ? new LinkedHashMap<>() : secretNames;
        }

        public Map<String, String> getTrustSecretNames() {
            return trustSecretNames;
        }

        public void setTrustSecretNames(Map<String, String> trustSecretNames) {
            this.trustSecretNames = trustSecretNames == null ? new LinkedHashMap<>() : trustSecretNames;
        }

        public Map<String, String> getPinnedVersions() {
            return pinnedVersions;
        }

        public void setPinnedVersions(Map<String, String> pinnedVersions) {
            this.pinnedVersions = pinnedVersions == null ? new LinkedHashMap<>() : pinnedVersions;
        }

        public boolean isUseSdkClient() {
            return useSdkClient;
        }

        public void setUseSdkClient(boolean useSdkClient) {
            this.useSdkClient = useSdkClient;
        }

        public boolean workloadIdentity() {
            return AUTH_WORKLOAD_IDENTITY.equalsIgnoreCase(authentication)
                    || AUTH_MANAGED_IDENTITY.equalsIgnoreCase(authentication);
        }

        public boolean defaultAzureCredential() {
            return AUTH_DEFAULT_AZURE.equalsIgnoreCase(authentication);
        }
    }
}
