package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.exception.GatewayFailureInjectionException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

@Component
public class ProductionGatewayFailureInjector {

    private final ProductionChangeGatewayProperties properties;
    private final Environment environment;
    private volatile FailureInjectionPoint nextHook;

    public ProductionGatewayFailureInjector(
            ProductionChangeGatewayProperties properties,
            Environment environment
    ) {
        this.properties = properties;
        this.environment = environment;
    }

    public void setNextHook(FailureInjectionPoint point) {
        this.nextHook = point;
    }

    public void inject(FailureInjectionPoint point) {
        if (!injectionAllowed()) {
            return;
        }
        FailureInjectionPoint configured = nextHook;
        if (configured == null) {
            String hook = properties.getFailureInjection().getHook();
            if (hook == null || hook.isBlank()) {
                return;
            }
            try {
                configured = FailureInjectionPoint.valueOf(hook.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return;
            }
        }
        if (configured == point) {
            nextHook = null;
            throw new GatewayFailureInjectionException(point);
        }
    }

    private boolean injectionAllowed() {
        if (Boolean.parseBoolean(environment.getProperty("snip.integration.security.production-runtime", "false"))) {
            return false;
        }
        if (properties.isProductionRuntime()) {
            return false;
        }
        boolean prodProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
        if (prodProfile) {
            return false;
        }
        boolean testProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("test"));
        if (!testProfile) {
            return false;
        }
        return properties.getFailureInjection().isEnabled();
    }
}
