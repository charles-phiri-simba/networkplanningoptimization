package com.simba.snip.npo.integration;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IntegrationRuntimeIdentity {

    private final String instanceId = UUID.randomUUID().toString();

    public String instanceId() {
        return instanceId;
    }
}
