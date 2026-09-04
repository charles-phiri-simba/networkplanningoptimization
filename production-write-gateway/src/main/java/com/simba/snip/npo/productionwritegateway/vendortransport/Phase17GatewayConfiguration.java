package com.simba.snip.npo.productionwritegateway.vendortransport;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class Phase17GatewayConfiguration {

    @Bean
    @ConditionalOnMissingBean(RuntimeTransportArtifactIdentityProvider.class)
    public RuntimeTransportArtifactIdentityProvider packagedRuntimeTransportArtifactIdentityProvider() {
        return new PackagedRuntimeTransportArtifactIdentityProvider();
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock phase17UtcClock() {
        return Clock.systemUTC();
    }
}
