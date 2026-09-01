package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectorEndpointRegistry {

    private final Map<String, ConnectorEndpoint> endpoints = new ConcurrentHashMap<>();

    public void register(ConnectorEndpoint endpoint) {
        endpoints.put(endpoint.endpointRef(), endpoint);
    }

    public ConnectorEndpoint require(String endpointRef) {
        ConnectorEndpoint endpoint = endpoints.get(endpointRef);
        if (endpoint == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.NETWORK_POLICY_DENIED, "endpoint is not registered");
        }
        return endpoint;
    }
}
