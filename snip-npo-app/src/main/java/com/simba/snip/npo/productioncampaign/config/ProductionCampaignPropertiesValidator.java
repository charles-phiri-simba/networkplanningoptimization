package com.simba.snip.npo.productioncampaign.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class ProductionCampaignPropertiesValidator {

    private final ProductionCampaignProperties properties;

    public ProductionCampaignPropertiesValidator(ProductionCampaignProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validate() {
        properties.validate();
    }
}
