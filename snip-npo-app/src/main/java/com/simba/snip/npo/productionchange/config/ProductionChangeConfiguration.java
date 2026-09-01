package com.simba.snip.npo.productionchange.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ProductionChangeConfiguration {

    private final ProductionChangeProperties properties;

    public ProductionChangeConfiguration(ProductionChangeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validateProperties() {
        properties.validate();
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock productionChangeClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry productionChangeMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public RestClient productionWriteGatewayRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        RestClient.Builder builder = RestClient.builder().requestFactory(factory);
        if (properties.gatewayUrlConfigured()) {
            builder.baseUrl(properties.getGatewayBaseUrl().strip());
        }
        return builder.build();
    }

    /**
     * Prevents the ordinary SNIP process from component-scanning the write-gateway package
     * if that module is present on a test classpath.
     */
    @Bean
    public TypeExcludeFilter productionWriteGatewayTypeExcludeFilter() {
        return new TypeExcludeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
                return metadataReader.getClassMetadata().getClassName()
                        .startsWith("com.simba.snip.npo.productionwritegateway.");
            }

            @Override
            public boolean equals(Object obj) {
                return obj != null && getClass() == obj.getClass();
            }

            @Override
            public int hashCode() {
                return getClass().hashCode();
            }
        };
    }
}
