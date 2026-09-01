package com.simba.snip.npo.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IntegrationRuntimeIdentity {

    private final String instanceId;

    public IntegrationRuntimeIdentity(
            @Value("${snip.integration.instance-id:}") String configuredInstanceId
    ) {
        this.instanceId = configuredInstanceId == null || configuredInstanceId.isBlank()
                ? UUID.randomUUID().toString()
                : configuredInstanceId;
    }

    public String instanceId() {
        return instanceId;
    }
}
