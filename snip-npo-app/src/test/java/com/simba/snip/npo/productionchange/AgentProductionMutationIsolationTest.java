package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentProductionMutationIsolationTest {

    @Test
    void noWriteAdapterImports() throws IOException {
        Path agent = ProductionChangeSourcePaths.appMainJava().resolve("com/simba/snip/npo/agent");
        assertTrue(Files.isDirectory(agent));
        try (Stream<Path> files = Files.walk(agent)) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path).toLowerCase(Locale.ROOT);
                    return source.contains("productionwritegateway")
                            || source.contains("ericssonenmwriteadapter")
                            || source.contains("vendornetworkwriteadapter")
                            || source.contains("ericssonwritetransport")
                            || source.contains("controlledtestericssonwritetransport");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender, "agent package imported production write adapter/transport");
        }
    }
}
