package com.simba.snip.npo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "snip.integration.enm")
public class EnmIntegrationProperties {

    private boolean enabled = true;
    private String connectorId = "ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER";
    private String implementationType = "SIMULATOR";
    private String accessMode = "READ_ONLY";
    private String environment = "INT";
    private String apiProfile = "ENM_SIMULATOR_V1";
    private String baseEndpoint = "";
    private int pageSize = 10;
    private int maxPages = 8;
    private int maxEntities = 32;
    private Duration requestTimeout = Duration.ofSeconds(2);
    private Duration overallExecutionTimeout = Duration.ofSeconds(30);
    private int maxAttempts = 3;
    private Duration initialBackoff = Duration.ofMillis(10);
    private Duration maxBackoff = Duration.ofMillis(100);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getImplementationType() {
        return implementationType;
    }

    public void setImplementationType(String implementationType) {
        this.implementationType = implementationType;
    }

    public String getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(String accessMode) {
        this.accessMode = accessMode;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getApiProfile() {
        return apiProfile;
    }

    public void setApiProfile(String apiProfile) {
        this.apiProfile = apiProfile;
    }

    public String getBaseEndpoint() {
        return baseEndpoint == null ? "" : baseEndpoint;
    }

    public void setBaseEndpoint(String baseEndpoint) {
        this.baseEndpoint = baseEndpoint == null ? "" : baseEndpoint;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getMaxEntities() {
        return maxEntities;
    }

    public void setMaxEntities(int maxEntities) {
        this.maxEntities = maxEntities;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getOverallExecutionTimeout() {
        return overallExecutionTimeout;
    }

    public void setOverallExecutionTimeout(Duration overallExecutionTimeout) {
        this.overallExecutionTimeout = overallExecutionTimeout;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public boolean productionSelected() {
        return "REAL".equalsIgnoreCase(implementationType);
    }
}
