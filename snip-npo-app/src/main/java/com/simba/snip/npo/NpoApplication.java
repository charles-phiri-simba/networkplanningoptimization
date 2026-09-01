package com.simba.snip.npo;

import com.simba.snip.npo.config.EnmIntegrationProperties;
import com.simba.snip.npo.config.IntegrationRuntimeProperties;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.config.ConnectorSecurityProperties;
import com.simba.snip.npo.changeintelligence.config.ChangeIntelligenceProperties;
import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.changeplanning.config.ChangePlanningProperties;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.simba\\.snip\\.npo\\.productionwritegateway\\..*"))
@EntityScan(basePackages = {
        "com.simba.snip.npo.persist",
        "com.simba.snip.npo.changeintelligence.persist",
        "com.simba.snip.npo.changeplanning.persist",
        "com.simba.snip.npo.changeexecution.entity",
        "com.simba.snip.npo.productionchange.entity",
        "com.simba.snip.npo.integration"
})
@EnableJpaRepositories(basePackages = {
        "com.simba.snip.npo.persist",
        "com.simba.snip.npo.context",
        "com.simba.snip.npo.changeintelligence.repository",
        "com.simba.snip.npo.changeplanning.repository",
        "com.simba.snip.npo.changeexecution.repository",
        "com.simba.snip.npo.productionchange.repository"
})
@EnableScheduling
@EnableConfigurationProperties({
        SnipProperties.class,
        IntegrationRuntimeProperties.class,
        ConnectorSecurityProperties.class,
        EnmIntegrationProperties.class,
        com.simba.snip.npo.config.SynchronizationProperties.class,
        ChangeIntelligenceProperties.class,
        ChangePlanningProperties.class,
        ChangeExecutionProperties.class,
        com.simba.snip.npo.changeplanning.config.ChangePlanningProperties.class,
        ProductionChangeProperties.class
})
public class NpoApplication {

    public static void main(String[] args) {
        SpringApplication.run(NpoApplication.class, args);
    }
}
