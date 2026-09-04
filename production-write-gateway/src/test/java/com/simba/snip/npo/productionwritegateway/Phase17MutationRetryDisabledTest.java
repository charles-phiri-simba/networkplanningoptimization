package com.simba.snip.npo.productionwritegateway;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17MutationRetryDisabledTest {

    @Test
    void t17Impl066MutationPathRetriesDisabled() throws IOException {
        Path gatewayMain = repoRoot().resolve("production-write-gateway/src/main");
        try (Stream<Path> files = Files.walk(gatewayMain)) {
            files.filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".yml")).forEach(path -> {
                try {
                    String text = Files.readString(path);
                    String lower = text.toLowerCase(Locale.ROOT);
                    assertFalse(lower.contains("@retryable"), path.toString());
                    assertFalse(lower.contains("retrytemplate"), path.toString());
                    assertFalse(lower.contains("resilience4j.retry"), path.toString());
                    assertFalse(lower.contains("retrywhen"), path.toString());
                    assertFalse(text.contains("Retry.maxAttempts"), path.toString());
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
        String adapter = Files.readString(gatewayMain.resolve(
                "java/com/simba/snip/npo/productionwritegateway/adapter/EricssonEnmWriteAdapter.java"));
        assertFalse(adapter.toLowerCase(Locale.ROOT).contains("retry"));
        String unconfigured = Files.readString(gatewayMain.resolve(
                "java/com/simba/snip/npo/productionwritegateway/transport/UnconfiguredProductionEricssonWriteTransport.java"));
        assertFalse(unconfigured.toLowerCase(Locale.ROOT).contains("retry"));
        assertTrue(Files.exists(gatewayMain.resolve("resources/META-INF/snip-transport-artifact.json")));
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("production-write-gateway"))) {
            return cwd;
        }
        return cwd.getParent();
    }
}
