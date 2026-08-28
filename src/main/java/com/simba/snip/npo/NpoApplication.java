package com.simba.snip.npo;

import com.simba.snip.npo.config.EnmIntegrationProperties;
import com.simba.snip.npo.config.IntegrationRuntimeProperties;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.config.ConnectorSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        SnipProperties.class,
        IntegrationRuntimeProperties.class,
        ConnectorSecurityProperties.class,
        EnmIntegrationProperties.class
})
public class NpoApplication {

    public static void main(String[] args) {
        SpringApplication.run(NpoApplication.class, args);
    }
}
