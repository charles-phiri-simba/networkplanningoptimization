package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangePackageBoundaryTest {

    @Test
    void packagesExist() {
        Path root = ProductionChangeSourcePaths.appMainJava().resolve("com/simba/snip/npo/productionchange");
        for (String leaf : List.of(
                "api", "domain", "entity", "repository", "service", "security",
                "policy", "audit", "metrics", "adapter", "config", "exception")) {
            assertTrue(Files.isDirectory(root.resolve(leaf)), leaf);
        }
        Path gateway = ProductionChangeSourcePaths.gatewayMainJava()
                .resolve("com/simba/snip/npo/productionwritegateway");
        assertTrue(Files.isDirectory(gateway));
        assertTrue(Files.isDirectory(gateway.resolve("adapter")));
        assertTrue(Files.isDirectory(gateway.resolve("transport")));
    }
}
