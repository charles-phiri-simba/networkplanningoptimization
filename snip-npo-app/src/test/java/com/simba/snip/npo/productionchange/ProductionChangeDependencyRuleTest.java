package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeDependencyRuleTest {

    @Test
    void protocolNoKeyVault() throws IOException {
        String pom = Files.readString(ProductionChangeSourcePaths.protocolPom());
        assertFalse(pom.contains("azure-security-keyvault-secrets"));
        assertFalse(pom.contains("azure-identity"));
        assertFalse(pom.contains("production-write-gateway"));
        assertTrue(pom.contains("jackson-annotations"));
    }
}
