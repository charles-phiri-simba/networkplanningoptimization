package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.action.CapabilityRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpProductionMutationIsolationTest {

    @Test
    void noProductionMutationTools() throws IOException {
        CapabilityRegistry.all().forEach(capability -> {
            String id = capability.capabilityId().toLowerCase(Locale.ROOT);
            assertFalse(id.contains("production"));
            assertFalse(id.contains("execute"));
            assertFalse(id.contains("apply"));
            assertFalse(id.contains("rollback"));
            assertFalse(id.contains("write"));
        });
        Path mcp = ProductionChangeSourcePaths.appMainJava().resolve("com/simba/snip/npo/mcp");
        assertTrue(Files.isDirectory(mcp));
        try (Stream<Path> files = Files.walk(mcp)) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path).toLowerCase(Locale.ROOT);
                    return source.contains("productionwritegateway")
                            || source.contains("production-changes")
                            || source.contains("execute_production")
                            || source.contains("ericssonwritetransport");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender, "MCP package registered a production mutation tool");
        }
    }
}
