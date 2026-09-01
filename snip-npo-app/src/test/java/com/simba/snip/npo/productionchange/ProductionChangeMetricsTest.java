package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductionChangeMetricsTest extends ProductionChangeITSupport {

    @Autowired
    ProductionChangeMetrics metrics;
    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void forbiddenLabelsAbsent() {
        metrics.incrementRequests("REQUESTED");
        metrics.incrementAttempts("DENIED");
        meterRegistry.getMeters().forEach(meter -> {
            meter.getId().getTags().forEach(tag -> {
                String key = tag.getKey().toLowerCase();
                assertFalse(key.contains("cellid") || key.equals("cellid"));
                assertFalse(key.contains("grantid"));
                assertFalse(key.contains("productionchangeid"));
                assertFalse(key.contains("userid"));
                assertFalse(key.contains("fingerprint"));
                assertFalse(key.contains("endpoint"));
            });
        });
        assertNotNull(metrics);
    }
}
