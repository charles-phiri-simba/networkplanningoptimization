package com.simba.snip.npo.changeexecution.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChangeExecutionConfiguration {

    private final ChangeExecutionProperties properties;

    public ChangeExecutionConfiguration(ChangeExecutionProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validateProperties() {
        properties.validate();
    }
}
