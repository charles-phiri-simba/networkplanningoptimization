package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import com.simba.snip.npo.productionwritegateway.metrics.ProductionGatewayMetrics;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductionKillSwitchEnforcementService {

    private final ProductionChangeGatewayProperties properties;
    private final ProductionGatewayMetrics metrics;

    public ProductionKillSwitchEnforcementService(
            ProductionChangeGatewayProperties properties,
            ProductionGatewayMetrics metrics
    ) {
        this.properties = properties;
        this.metrics = metrics;
    }

    public void assertEnabled(UUID grantId, UUID productionChangeId) {
        if (!properties.isEnabled() || !properties.isGlobalExecutionEnabled()) {
            metrics.incrementKillSwitchDenials();
            throw GatewayDeniedException.deny(
                    ProductionReasonCode.PRODUCTION_KILL_SWITCH_DENY, grantId, productionChangeId);
        }
    }

    public boolean bothFlagsEnabled() {
        return properties.isEnabled() && properties.isGlobalExecutionEnabled();
    }
}
