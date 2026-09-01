package com.simba.snip.npo.productionwritegateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "snip.production-change")
public class ProductionChangeGatewayProperties {

    private boolean enabled = false;
    private boolean globalExecutionEnabled = false;
    private boolean testTransportEnabled = false;
    private int maximumCellsPerExecution = 1;
    private int maximumParametersPerExecution = 1;
    private int maximumOperationsPerExecution = 1;
    private Duration maximumForwardGrantTtl = Duration.ofMinutes(5);
    private Duration maximumRollbackGrantTtl = Duration.ofMinutes(5);
    private int maximumChangesPerTargetPerHour = 6;
    private int maximumChangesPerCellPerDay = 3;
    private int maximumOutcomeUnknownBeforeSuspend = 3;
    private int maximumVerificationFailuresBeforeSuspend = 3;
    private boolean requireCurrentValueMatch = true;
    private boolean requireIndependentVerification = true;
    private boolean automaticRollbackEnabled = false;
    private List<String> permittedVendors = new ArrayList<>(List.of("ERICSSON"));
    private List<String> permittedPlatforms = new ArrayList<>(List.of("ENM"));
    private String minimumCertificationLevelForExecution = "L0";
    private boolean productionRuntime = false;
    private final FailureInjection failureInjection = new FailureInjection();
    private final Ssl ssl = new Ssl();
    private final Gateway gateway = new Gateway();
    private final Credentials credentials = new Credentials();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isGlobalExecutionEnabled() {
        return globalExecutionEnabled;
    }

    public void setGlobalExecutionEnabled(boolean globalExecutionEnabled) {
        this.globalExecutionEnabled = globalExecutionEnabled;
    }

    public boolean isTestTransportEnabled() {
        return testTransportEnabled;
    }

    public void setTestTransportEnabled(boolean testTransportEnabled) {
        this.testTransportEnabled = testTransportEnabled;
    }

    public int getMaximumCellsPerExecution() {
        return maximumCellsPerExecution;
    }

    public void setMaximumCellsPerExecution(int maximumCellsPerExecution) {
        this.maximumCellsPerExecution = maximumCellsPerExecution;
    }

    public int getMaximumParametersPerExecution() {
        return maximumParametersPerExecution;
    }

    public void setMaximumParametersPerExecution(int maximumParametersPerExecution) {
        this.maximumParametersPerExecution = maximumParametersPerExecution;
    }

    public int getMaximumOperationsPerExecution() {
        return maximumOperationsPerExecution;
    }

    public void setMaximumOperationsPerExecution(int maximumOperationsPerExecution) {
        this.maximumOperationsPerExecution = maximumOperationsPerExecution;
    }

    public Duration getMaximumForwardGrantTtl() {
        return maximumForwardGrantTtl;
    }

    public void setMaximumForwardGrantTtl(Duration maximumForwardGrantTtl) {
        this.maximumForwardGrantTtl = maximumForwardGrantTtl;
    }

    public Duration getMaximumRollbackGrantTtl() {
        return maximumRollbackGrantTtl;
    }

    public void setMaximumRollbackGrantTtl(Duration maximumRollbackGrantTtl) {
        this.maximumRollbackGrantTtl = maximumRollbackGrantTtl;
    }

    public int getMaximumChangesPerTargetPerHour() {
        return maximumChangesPerTargetPerHour;
    }

    public void setMaximumChangesPerTargetPerHour(int maximumChangesPerTargetPerHour) {
        this.maximumChangesPerTargetPerHour = maximumChangesPerTargetPerHour;
    }

    public int getMaximumChangesPerCellPerDay() {
        return maximumChangesPerCellPerDay;
    }

    public void setMaximumChangesPerCellPerDay(int maximumChangesPerCellPerDay) {
        this.maximumChangesPerCellPerDay = maximumChangesPerCellPerDay;
    }

    public int getMaximumOutcomeUnknownBeforeSuspend() {
        return maximumOutcomeUnknownBeforeSuspend;
    }

    public void setMaximumOutcomeUnknownBeforeSuspend(int maximumOutcomeUnknownBeforeSuspend) {
        this.maximumOutcomeUnknownBeforeSuspend = maximumOutcomeUnknownBeforeSuspend;
    }

    public int getMaximumVerificationFailuresBeforeSuspend() {
        return maximumVerificationFailuresBeforeSuspend;
    }

    public void setMaximumVerificationFailuresBeforeSuspend(int maximumVerificationFailuresBeforeSuspend) {
        this.maximumVerificationFailuresBeforeSuspend = maximumVerificationFailuresBeforeSuspend;
    }

    public boolean isRequireCurrentValueMatch() {
        return requireCurrentValueMatch;
    }

    public void setRequireCurrentValueMatch(boolean requireCurrentValueMatch) {
        this.requireCurrentValueMatch = requireCurrentValueMatch;
    }

    public boolean isRequireIndependentVerification() {
        return requireIndependentVerification;
    }

    public void setRequireIndependentVerification(boolean requireIndependentVerification) {
        this.requireIndependentVerification = requireIndependentVerification;
    }

    public boolean isAutomaticRollbackEnabled() {
        return automaticRollbackEnabled;
    }

    public void setAutomaticRollbackEnabled(boolean automaticRollbackEnabled) {
        this.automaticRollbackEnabled = automaticRollbackEnabled;
    }

    public List<String> getPermittedVendors() {
        return permittedVendors;
    }

    public void setPermittedVendors(List<String> permittedVendors) {
        this.permittedVendors = permittedVendors;
    }

    public List<String> getPermittedPlatforms() {
        return permittedPlatforms;
    }

    public void setPermittedPlatforms(List<String> permittedPlatforms) {
        this.permittedPlatforms = permittedPlatforms;
    }

    public String getMinimumCertificationLevelForExecution() {
        return minimumCertificationLevelForExecution;
    }

    public void setMinimumCertificationLevelForExecution(String minimumCertificationLevelForExecution) {
        this.minimumCertificationLevelForExecution = minimumCertificationLevelForExecution;
    }

    public boolean isProductionRuntime() {
        return productionRuntime;
    }

    public void setProductionRuntime(boolean productionRuntime) {
        this.productionRuntime = productionRuntime;
    }

    public FailureInjection getFailureInjection() {
        return failureInjection;
    }

    public Ssl getSsl() {
        return ssl;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Negative maxima are illegal. Zero remains legal and fails closed at
     * {@link com.simba.snip.npo.productionwritegateway.service.ProductionGatewayRateLimitEnforcementService}.
     */
    public void validateRateLimitMaxima() {
        if (maximumChangesPerTargetPerHour < 0 || maximumChangesPerCellPerDay < 0) {
            throw new IllegalStateException(
                    "snip.production-change rate-limit maxima must not be negative; zero fails closed");
        }
    }

    public static class FailureInjection {
        private boolean enabled = false;
        private String hook = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHook() {
            return hook;
        }

        public void setHook(String hook) {
            this.hook = hook;
        }
    }

    public static class Ssl {
        private boolean hostnameVerification = true;
        private boolean trustAll = false;

        public boolean isHostnameVerification() {
            return hostnameVerification;
        }

        public void setHostnameVerification(boolean hostnameVerification) {
            this.hostnameVerification = hostnameVerification;
        }

        public boolean isTrustAll() {
            return trustAll;
        }

        public void setTrustAll(boolean trustAll) {
            this.trustAll = trustAll;
        }
    }

    public static class Gateway {
        private List<String> allowedCallerIds = new ArrayList<>(List.of("snip-npo-app"));
        private String instanceId = "gateway-local";

        public List<String> getAllowedCallerIds() {
            return allowedCallerIds;
        }

        public void setAllowedCallerIds(List<String> allowedCallerIds) {
            this.allowedCallerIds = allowedCallerIds;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }
    }

    public static class Credentials {
        private boolean enabled = false;
        private String vaultUri = "";
        private String authentication = "WORKLOAD_IDENTITY";

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
