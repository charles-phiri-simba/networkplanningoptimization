package com.simba.snip.npo.integration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConnectorSecurityConfiguration {

    @Bean
    public Clock connectorClock() {
        return Clock.systemUTC();
    }
}
