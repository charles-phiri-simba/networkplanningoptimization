package com.simba.snip.npo.integration;

import com.simba.snip.npo.config.IntegrationRuntimeProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ImportRuntimeRecovery implements ApplicationRunner {

    private final IntegrationRuntimeProperties properties;
    private final ImportLeaseService leaseService;

    public ImportRuntimeRecovery(IntegrationRuntimeProperties properties, ImportLeaseService leaseService) {
        this.properties = properties;
        this.leaseService = leaseService;
        this.properties.validate();
    }

    @Override
    public void run(ApplicationArguments args) {
        leaseService.recoverExpired();
    }
}
