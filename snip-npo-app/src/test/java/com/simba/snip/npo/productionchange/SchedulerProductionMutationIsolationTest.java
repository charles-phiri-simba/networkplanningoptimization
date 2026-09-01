package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SchedulerProductionMutationIsolationTest {

    @Test
    void noProductionExecuteScheduled() throws IOException {
        Path main = ProductionChangeSourcePaths.appMainJava();
        try (Stream<Path> files = Files.walk(main)) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    if (!source.contains("@Scheduled")) {
                        return false;
                    }
                    String lower = source.toLowerCase(Locale.ROOT);
                    return lower.contains("productionchange")
                            || lower.contains("productionwritegateway")
                            || lower.contains("executeproduction")
                            || lower.contains("/api/v1/production-changes");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender, "@Scheduled method must not invoke production execute");
        }
    }
}
