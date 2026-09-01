package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeModuleBoundaryTest {

    private static final List<String> PACKAGES = List.of(
            "api", "domain", "entity", "repository", "service", "security",
            "policy", "audit", "metrics", "adapter", "config", "exception"
    );

    @Test
    void package_api_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("api");
    }

    @Test
    void package_domain_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("domain");
    }

    @Test
    void package_entity_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("entity");
    }

    @Test
    void package_repository_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("repository");
    }

    @Test
    void package_service_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("service");
    }

    @Test
    void package_security_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("security");
    }

    @Test
    void package_policy_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("policy");
    }

    @Test
    void package_audit_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("audit");
    }

    @Test
    void package_metrics_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("metrics");
    }

    @Test
    void package_adapter_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("adapter");
    }

    @Test
    void package_config_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("config");
    }

    @Test
    void package_exception_under_productionchange() throws IOException {
        assertPackagePresentWithoutWriteTransport("exception");
    }

    @Test
    void parentAggregatorModules() throws IOException {
        String pom = Files.readString(ProductionChangeSourcePaths.repoRoot().resolve("pom.xml"));
        assertTrue(pom.contains("<packaging>pom</packaging>"));
        assertTrue(pom.contains("<module>production-change-protocol</module>"));
        assertTrue(pom.contains("<module>production-write-gateway</module>"));
        assertTrue(pom.contains("<module>snip-npo-app</module>"));
        int moduleCount = pom.split("<module>", -1).length - 1;
        assertTrue(moduleCount == 3, "exactly three Phase 16 modules declared, found " + moduleCount);
    }

    private static void assertPackagePresentWithoutWriteTransport(String leaf) throws IOException {
        Path dir = ProductionChangeSourcePaths.appMainJava()
                .resolve("com/simba/snip/npo/productionchange/" + leaf);
        assertTrue(Files.isDirectory(dir), "missing package " + leaf);
        try (Stream<Path> files = Files.walk(dir)) {
            boolean writeTransport = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path).toLowerCase(Locale.ROOT);
                    return source.contains("ericssonwritetransport")
                            || source.contains("vendornetworkwriteadapter")
                            || source.contains("controlledtestericssonwritetransport")
                            || source.contains("productionwritegateway.transport");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(writeTransport, "write transport type leaked into app package " + leaf);
        }
    }
}
