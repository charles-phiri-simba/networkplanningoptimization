package com.simba.snip.npo.demo;

import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Fail-closed demo startup checks. Runs after Flyway (Spring Boot lifecycle) and before
 * {@link DemoBootstrapRunner}. Production-change cannot be enabled on the demo profile.
 */
@Component
@Profile("demo")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DemoSafetyGuard {

    private static final Logger log = LoggerFactory.getLogger(DemoSafetyGuard.class);

    private final ProductionChangeProperties productionChange;
    private final ChangeExecutionProperties changeExecution;
    private final SnipProperties snip;

    public DemoSafetyGuard(
            ProductionChangeProperties productionChange,
            ChangeExecutionProperties changeExecution,
            SnipProperties snip
    ) {
        this.productionChange = productionChange;
        this.changeExecution = changeExecution;
        this.snip = snip;
    }

    @PostConstruct
    public void assertSafe() {
        if (productionChange.isEnabled() || productionChange.isGlobalExecutionEnabled()) {
            throw new IllegalStateException(
                    "SNIP demo startup refused: snip.production-change.enabled and "
                            + "global-execution-enabled must be false");
        }
        if (!changeExecution.isEnabled()) {
            throw new IllegalStateException(
                    "SNIP demo startup refused: snip.change-execution.enabled must be true "
                            + "in the demo profile");
        }
        if (snip.isKafkaEnabled()) {
            throw new IllegalStateException("SNIP demo startup refused: Kafka must remain disabled");
        }
        log.info("SNIP demo safety guard passed production-change=false kafka=false sandbox=true");
    }
}
