package com.simba.snip.npo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "snip.integration")
public class IntegrationRuntimeProperties {

    public static final String DEFAULT_SCOPE = "DEFAULT";

    private Duration leaseDuration = Duration.ofSeconds(30);
    private Duration heartbeatInterval = Duration.ofSeconds(5);
    private Duration executionTimeout = Duration.ofSeconds(120);
    private Duration fixtureReadDelay = Duration.ZERO;

    public void validate() {
        if (heartbeatInterval == null || heartbeatInterval.isZero() || heartbeatInterval.isNegative()) {
            throw new IllegalStateException("snip.integration.heartbeat-interval must be positive");
        }
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalStateException("snip.integration.lease-duration must be positive");
        }
        if (executionTimeout == null || executionTimeout.isZero() || executionTimeout.isNegative()) {
            throw new IllegalStateException("snip.integration.execution-timeout must be positive");
        }
        if (heartbeatInterval.compareTo(leaseDuration) >= 0) {
            throw new IllegalStateException("snip.integration.heartbeat-interval must be shorter than lease-duration");
        }
        if (heartbeatInterval.multipliedBy(2).compareTo(leaseDuration) > 0) {
            throw new IllegalStateException("snip.integration.heartbeat-interval must be meaningfully shorter than lease-duration");
        }
        if (fixtureReadDelay != null && fixtureReadDelay.isNegative()) {
            throw new IllegalStateException("snip.integration.fixture-read-delay must not be negative");
        }
    }

    public Duration getLeaseDuration() {
        return leaseDuration;
    }

    public void setLeaseDuration(Duration leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public Duration getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(Duration executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    public Duration getFixtureReadDelay() {
        return fixtureReadDelay == null ? Duration.ZERO : fixtureReadDelay;
    }

    public void setFixtureReadDelay(Duration fixtureReadDelay) {
        this.fixtureReadDelay = fixtureReadDelay;
    }
}
