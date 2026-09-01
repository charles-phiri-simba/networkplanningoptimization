package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionCredentialIsolationTest {

    @Test
    void appNoWriteCredentialProvider() throws IOException {
        Path appMain = ProductionChangeSourcePaths.appMainJava();
        try (Stream<Path> files = Files.walk(appMain)) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                String name = path.getFileName().toString();
                if (name.equals("ProductionWriteCredentialProvider.java")
                        || name.equals("ProductionCredentialResolutionService.java")) {
                    return true;
                }
                try {
                    String source = Files.readString(path);
                    return source.contains("class ProductionWriteCredentialProvider")
                            || source.contains("class ProductionCredentialResolutionService");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender, "app main sources must not own write credential resolution");
        }
        try (Stream<Path> files = Files.walk(appMain)) {
            boolean importOffender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path).toLowerCase(Locale.ROOT);
                    return source.contains("productionwritecredentialprovider")
                            || source.contains("productioncredentialresolutionservice");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(importOffender);
        }
    }

    @Test
    void gatewayOwnsCredentialService() {
        Path gatewayService = ProductionChangeSourcePaths.gatewayMainJava().resolve(
                "com/simba/snip/npo/productionwritegateway/service/ProductionCredentialResolutionService.java");
        assertTrue(Files.exists(gatewayService));
    }
}
