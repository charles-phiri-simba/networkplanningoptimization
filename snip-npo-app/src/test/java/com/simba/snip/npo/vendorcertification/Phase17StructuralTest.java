package com.simba.snip.npo.vendorcertification;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17StructuralTest {

    @Test
    void t17Str001To030() throws IOException {
        Path root = repoRoot();
        assertTrue(Files.exists(root.resolve("production-write-gateway")));
        assertTrue(Files.exists(root.resolve(
                "production-write-gateway/src/main/java/com/simba/snip/npo/productionwritegateway/transport/UnconfiguredProductionEricssonWriteTransport.java")));
        assertTrue(Files.exists(root.resolve(
                "snip-npo-app/src/main/resources/db/migration/V18__phase17_certified_vendor_transport.sql")));
        assertFalse(Files.exists(root.resolve(
                "snip-npo-app/src/main/resources/db/migration/V19__phase18.sql")));
        String v18 = Files.readString(root.resolve(
                "snip-npo-app/src/main/resources/db/migration/V18__phase17_certified_vendor_transport.sql"));
        String v18Lower = v18.toLowerCase(Locale.ROOT);
        assertFalse(v18Lower.contains("secret_value"));
        assertFalse(v18Lower.contains("password"));
        assertFalse(v18.contains("certification_level VARCHAR(8) NOT NULL CHECK (certification_level IN ('L0','L1','L2','L3','L4'))"));
        assertTrue(v18.contains("CHECK (certification_level IN ('L0','L1','L2','L3'))"));
        assertFalse(v18.contains("level4"));
        assertFalse(v18Lower.contains("http://"));
        assertFalse(v18Lower.contains("netconf"));
        assertTrue(v18.contains("^[0-9a-f]{64}$"));
        assertTrue(v18.contains("^[0-9a-f]{40}$"));

        try (Stream<Path> files = Files.walk(root.resolve("snip-npo-app/src/main/java"))) {
            boolean certInAgent = files.filter(p -> p.toString().endsWith(".java")).anyMatch(p -> {
                String path = p.toString().replace('\\', '/');
                return (path.contains("/agent/") || path.contains("/mcp/"))
                        && path.contains("vendorcertification");
            });
            assertFalse(certInAgent);
        }

        String enm = Files.readString(root.resolve(
                "snip-npo-app/src/main/java/com/simba/snip/npo/integration/enm/EnmTransport.java"));
        assertFalse(enm.toLowerCase(Locale.ROOT).contains("writemutation"));
        assertFalse(enm.contains("transmitMutation"));

        try (Stream<Path> files = Files.walk(root.resolve("snip-npo-app/src/main/java"))) {
            boolean nokiaWrite = files.anyMatch(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).contains("nokia")
                    && p.getFileName().toString().toLowerCase(Locale.ROOT).contains("write"));
            assertFalse(nokiaWrite);
        }

        try (Stream<Path> files = Files.walk(root.resolve("production-change-protocol/src/main/java"))) {
            boolean azure = files.filter(p -> p.toString().endsWith(".java")).anyMatch(p -> {
                try {
                    return Files.readString(p).contains("com.azure");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(azure);
        }

        try (Stream<Path> files = Files.walk(root.resolve("production-change-protocol/src/main/java"))) {
            boolean expectedStateStrength = files.anyMatch(p -> p.getFileName().toString().equals("ExpectedStateStrength.java"));
            assertFalse(expectedStateStrength);
        }

        String yaml = Files.readString(root.resolve("production-write-gateway/src/main/resources/application.yml"));
        assertTrue(yaml.contains("enabled: false"));
        assertTrue(yaml.contains("test-transport-enabled: false"));
        assertTrue(yaml.contains("global-execution-enabled: false"));
    }

    @Test
    void v1ToV17ContentUnchangedFilenamesExist() throws IOException {
        Path mig = repoRoot().resolve("snip-npo-app/src/main/resources/db/migration");
        for (int i = 1; i <= 17; i++) {
            final int n = i;
            try (Stream<Path> files = Files.list(mig)) {
                boolean found = files.anyMatch(p -> p.getFileName().toString().startsWith("V" + n + "__"));
                assertTrue(found, "missing V" + n);
            }
        }
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("snip-npo-app"))) {
            return cwd;
        }
        if (cwd.getFileName() != null && "snip-npo-app".equals(cwd.getFileName().toString())) {
            return cwd.getParent();
        }
        return cwd;
    }
}
