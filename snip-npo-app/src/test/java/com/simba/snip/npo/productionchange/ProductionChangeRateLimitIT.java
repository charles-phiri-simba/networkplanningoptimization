package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.service.ProductionRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeRateLimitIT extends ProductionChangeITSupport {

    @Autowired
    ProductionRateLimitService rateLimitService;
    @Autowired
    ProductionChangeProperties properties;

    @Test
    void sharedRateLimitAcrossGateways() {
        String target = TARGET_ID;
        int max = properties.getMaximumChangesPerTargetPerHour();
        int denied = 0;
        for (int i = 0; i < max + 2; i++) {
            try {
                rateLimitService.consumeTargetHour(target);
            } catch (Exception ex) {
                denied++;
                assertTrue(ex.toString().contains("RATE") || ex.getMessage().contains("rate"));
            }
        }
        assertTrue(denied >= 1);
        assertEquals(0, mutationCount());
    }
}
