package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EventConsumerProductionMutationIsolationTest {

    @Test
    void noProductionExecuteListener() throws IOException {
        Path main = ProductionChangeSourcePaths.appMainJava();
        try (Stream<Path> files = Files.walk(main)) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    boolean listener = source.contains("@KafkaListener")
                            || source.contains("@EventListener")
                            || source.contains("implements MessageListener");
                    if (!listener) {
                        return false;
                    }
                    String lower = source.toLowerCase(Locale.ROOT);
                    return lower.contains("productionwritegateway")
                            || lower.contains("executeproduction")
                            || lower.contains("/api/v1/production-changes");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender, "event listener must not invoke production execute");
        }
    }
}
