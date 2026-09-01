package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionwritegateway.ProductionWriteGatewayApplication;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionWriteGatewayIsolationTest {

    @Test
    void independentMainClass() {
        assertEquals(
                "com.simba.snip.npo.productionwritegateway.ProductionWriteGatewayApplication",
                ProductionWriteGatewayApplication.class.getName()
        );
        assertFalse(ProductionWriteGatewayApplication.class.isAnnotationPresent(org.springframework.stereotype.Service.class));
    }

    @Test
    void productionTransportFailClosed() throws IOException {
        Path yaml = ProductionChangeSourcePaths.repoRoot()
                .resolve("production-write-gateway/src/main/resources/application.yml");
        String text = Files.readString(yaml);
        assertTrue(text.contains("enabled: false"));
        assertTrue(text.contains("test-transport-enabled: false"));
        Path unconfigured = ProductionChangeSourcePaths.gatewayMainJava().resolve(
                "com/simba/snip/npo/productionwritegateway/transport/UnconfiguredProductionEricssonWriteTransport.java");
        assertTrue(Files.exists(unconfigured));
        String source = Files.readString(unconfigured);
        assertTrue(source.contains("PRODUCTION_WRITE_TRANSPORT_NOT_CONFIGURED")
                || source.contains("PRODUCTION_TRANSPORT_NOT_CONFIGURED"));
        assertFalse(source.toLowerCase(Locale.ROOT).contains("http://"));
        assertFalse(source.toLowerCase(Locale.ROOT).contains("https://"));
    }

    @Test
    void gatewayNotScannedByApp() throws IOException {
        try (Stream<Path> files = Files.walk(ProductionChangeSourcePaths.appMainJava())) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("productionwritegateway.service")
                            || source.contains("EricssonEnmWriteAdapter")
                            || source.contains("EricssonWriteTransport");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender);
        }
    }
}
