package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.CertificationLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeCertificationTest {

    @Test
    void level4NotActivatedByCode() throws IOException {
        ProductionChangeProperties properties = new ProductionChangeProperties();
        assertEquals(CertificationLevel.L0, properties.getMinimumCertificationLevelForExecution());
        assertNotEquals(CertificationLevel.L4, properties.getMinimumCertificationLevelForExecution());
        String yaml = Files.readString(ProductionChangeSourcePaths.appMainResources().resolve("application.yml"));
        assertTrue(yaml.contains("enabled: false"));
        assertFalseContainsLevel4Default(yaml);
        String gatewayYaml = Files.readString(ProductionChangeSourcePaths.repoRoot()
                .resolve("production-write-gateway/src/main/resources/application.yml"));
        assertTrue(gatewayYaml.contains("minimum-certification-level-for-execution: L0"));
        assertTrue(!gatewayYaml.contains("minimum-certification-level-for-execution: L4"));
    }

    private static void assertFalseContainsLevel4Default(String yaml) {
        assertTrue(!yaml.contains("minimum-certification-level-for-execution: L4"));
    }
}
