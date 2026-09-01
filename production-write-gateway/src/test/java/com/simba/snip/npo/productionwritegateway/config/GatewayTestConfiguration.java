package com.simba.snip.npo.productionwritegateway.config;

import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@Profile("test")
public class GatewayTestConfiguration {

    @Bean
    @Primary
    @ConditionalOnBean(ControlledTestEricssonWriteTransport.class)
    AtomicInteger mutationInvocationCounter(ControlledTestEricssonWriteTransport transport) {
        return transport.getMutationInvocationCounter();
    }
}
