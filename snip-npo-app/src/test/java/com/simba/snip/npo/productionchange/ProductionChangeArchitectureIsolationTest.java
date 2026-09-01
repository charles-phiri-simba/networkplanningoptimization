package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeArchitectureIsolationTest {

    @Test
    void appDoesNotImplementVendorWriteTransport() throws IOException {
        Path app = ProductionChangeSourcePaths.appMainJava().resolve("com/simba/snip/npo/productionchange");
        assertTrue(Files.isDirectory(app));
        try (Stream<Path> files = Files.walk(app)) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path).toLowerCase(Locale.ROOT);
                    return source.contains("implements ericssonwritetransport")
                            || source.contains("class ericssonenmwriteadapter")
                            || source.contains("netconf")
                            || source.contains("bulkcm");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender);
        }
    }

    @Test
    void agentsMcpSchedulerIsolatedFromGateway() throws IOException {
        for (String leaf : new String[] {"agent", "mcp"}) {
            Path dir = ProductionChangeSourcePaths.appMainJava().resolve("com/simba/snip/npo/" + leaf);
            try (Stream<Path> files = Files.walk(dir)) {
                boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                    try {
                        return Files.readString(path).contains("productionwritegateway");
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
                assertFalse(offender, leaf + " imported productionwritegateway");
            }
        }
    }
}
