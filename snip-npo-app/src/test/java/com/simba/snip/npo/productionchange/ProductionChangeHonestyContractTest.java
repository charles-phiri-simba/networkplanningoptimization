package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeHonestyContractTest {

    @Test
    void noExactOnceClaim() throws IOException {
        assertTrue(ArraysContainsOutcomeUnknown());
        scanForForbiddenClaim("exactly-once");
        scanForForbiddenClaim("exact-once");
        scanForForbiddenClaim("exactly once");
    }

    @Test
    void noDistributedAcidClaim() throws IOException {
        scanForForbiddenClaim("distributed acid");
        scanForForbiddenClaim("distributed ACID");
    }

    private static boolean ArraysContainsOutcomeUnknown() {
        return java.util.Arrays.stream(MutationOutcome.values())
                .anyMatch(v -> v.name().equals("OUTCOME_UNKNOWN"));
    }

    private static void scanForForbiddenClaim(String phrase) throws IOException {
        String needle = phrase.toLowerCase(Locale.ROOT);
        for (Path root : java.util.List.of(
                ProductionChangeSourcePaths.appMainJava().resolve("com/simba/snip/npo/productionchange"),
                ProductionChangeSourcePaths.gatewayMainJava())) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                boolean claimed = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                    try {
                        String source = Files.readString(path).toLowerCase(Locale.ROOT);
                        if (source.contains("do not claim") || source.contains("must not claim")
                                || source.contains("no claim")) {
                            return false;
                        }
                        return source.contains("\"" + needle + "\"")
                                || source.contains("guarantees " + needle)
                                || source.contains("provides " + needle);
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
                assertFalse(claimed, "forbidden honesty claim '" + phrase + "' in " + root);
            }
        }
    }
}
