package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductionTerraformPlanSnapshotTest {

    @Test
    void terraformDoesNotEmbedWriteSecrets() throws IOException {
        Path infra = ProductionChangeSourcePaths.repoRoot().resolve("infra");
        if (!Files.exists(infra)) {
            return;
        }
        try (Stream<Path> files = Files.walk(infra)) {
            boolean secretValue = files.filter(p -> p.toString().endsWith(".tf")).anyMatch(path -> {
                try {
                    String text = Files.readString(path).toLowerCase(Locale.ROOT);
                    return text.contains("enm_write_password")
                            || text.contains("production_write_secret_value")
                            || (text.contains("azurerm_key_vault_secret") && text.contains("value = \""));
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(secretValue);
        }
    }
}
