package com.simba.snip.npo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snip.integration.security")
public class ConnectorSecurityProperties {

    private boolean localCredentialsEnabled = false;
    private AzureKeyVault azureKeyVault = new AzureKeyVault();

    public boolean isLocalCredentialsEnabled() {
        return localCredentialsEnabled;
    }

    public void setLocalCredentialsEnabled(boolean localCredentialsEnabled) {
        this.localCredentialsEnabled = localCredentialsEnabled;
    }

    public AzureKeyVault getAzureKeyVault() {
        return azureKeyVault;
    }

    public void setAzureKeyVault(AzureKeyVault azureKeyVault) {
        this.azureKeyVault = azureKeyVault;
    }

    public static class AzureKeyVault {
        private boolean enabled = false;
        private String vaultUri = "";
        private String authentication = "MANAGED_IDENTITY";

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
    }
}
