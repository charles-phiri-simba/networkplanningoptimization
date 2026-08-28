package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.config.SynchronizationProperties;
import com.simba.snip.npo.integration.security.ConnectorRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SynchronizationPolicyRegistry {

    private final SynchronizationProperties properties;
    private final ConnectorRegistry connectorRegistry;

    public SynchronizationPolicyRegistry(SynchronizationProperties properties, ConnectorRegistry connectorRegistry) {
        this.properties = properties;
        this.connectorRegistry = connectorRegistry;
    }

    public List<SynchronizationPolicy> policies() {
        return properties.validatedPolicies();
    }

    public Optional<SynchronizationPolicy> find(String sourceSystem, String sourceScope) {
        return policies().stream()
                .filter(policy -> policy.sourceSystem().equals(sourceSystem)
                        && policy.sourceScope().equals(sourceScope))
                .findFirst();
    }

    public Optional<SynchronizationPolicy> findByConnectorId(String connectorId) {
        return policies().stream()
                .filter(policy -> policy.connectorId().equals(connectorId))
                .findFirst();
    }

    public SynchronizationPolicy require(String sourceSystem, String sourceScope) {
        return find(sourceSystem, sourceScope)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no synchronization policy for " + sourceSystem + "/" + sourceScope));
    }

    public void validateConnectorBinding(SynchronizationPolicy policy) {
        connectorRegistry.require(policy.connectorId());
    }
}
