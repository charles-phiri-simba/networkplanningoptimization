package com.simba.snip.npo.productionwritegateway.config;

import com.simba.snip.npo.productionwritegateway.transport.EricssonWriteTransport;
import com.simba.snip.npo.productionwritegateway.transport.UnconfiguredProductionEricssonWriteTransport;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class GatewayTransportConfig {

    public GatewayTransportConfig(ProductionChangeGatewayProperties properties) {
        properties.validateRateLimitMaxima();
    }

    @Bean
    @ConditionalOnMissingBean(EricssonWriteTransport.class)
    @ConditionalOnProperty(
            name = "snip.production-change.test-transport-enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public EricssonWriteTransport unconfiguredProductionEricssonWriteTransport() {
        return new UnconfiguredProductionEricssonWriteTransport();
    }

    @Bean
    @ConditionalOnBean(ControlledTestEricssonWriteTransport.class)
    @ConditionalOnMissingBean(name = "mutationInvocationCounter")
    public AtomicInteger mutationInvocationCounter(ControlledTestEricssonWriteTransport transport) {
        return transport.getMutationInvocationCounter();
    }
}
