package com.simba.snip.npo.productionwritegateway;

import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
@EnableConfigurationProperties(ProductionChangeGatewayProperties.class)
public class ProductionWriteGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductionWriteGatewayApplication.class, args);
    }
}
