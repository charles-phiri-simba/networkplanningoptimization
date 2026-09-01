package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.CertificationLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductionCertificationLevelDefaultTest {

    @Test
    void defaultCertificationIsL0() throws IOException {
        assertEquals(CertificationLevel.L0, new ProductionChangeProperties().getMinimumCertificationLevelForExecution());
        new ProductionChangeInfraValidationTest().defaultEnabledFalseInYaml();
        assertFalse(new ProductionChangeProperties().isEnabled());
    }
}
