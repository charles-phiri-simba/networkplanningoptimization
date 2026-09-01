package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeConfigTest {

    @Test
    void scopeExpansionRejected() {
        ProductionChangeProperties properties = new ProductionChangeProperties();
        properties.setMaximumCellsPerExecution(2);
        assertThrows(DomainValidationException.class, properties::validate);
        properties = new ProductionChangeProperties();
        properties.setMaximumParametersPerExecution(2);
        assertThrows(DomainValidationException.class, properties::validate);
        properties = new ProductionChangeProperties();
        properties.setMaximumOperationsPerExecution(2);
        assertThrows(DomainValidationException.class, properties::validate);
    }

    @Test
    void defaultEnabledFalse() throws IOException {
        ProductionChangeProperties properties = new ProductionChangeProperties();
        assertFalse(properties.isEnabled());
        assertFalse(properties.isGlobalExecutionEnabled());
        String yaml = Files.readString(ProductionChangeSourcePaths.appMainResources().resolve("application.yml"));
        assertTrue(yaml.contains("enabled: false"));
        assertTrue(yaml.contains("global-execution-enabled: false"));
    }
}
