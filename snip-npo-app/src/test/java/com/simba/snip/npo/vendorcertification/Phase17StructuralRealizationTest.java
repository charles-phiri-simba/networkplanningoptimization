package com.simba.snip.npo.vendorcertification;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17StructuralRealizationTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("structuralCases")
    void batchedStructuralProof(String evidenceId, String assertion) throws IOException {
        Path root = repoRoot();
        String v18 = Files.readString(root.resolve(
                "snip-npo-app/src/main/resources/db/migration/V18__phase17_certified_vendor_transport.sql"));
        String yaml = Files.readString(root.resolve("production-write-gateway/src/main/resources/application.yml"));
        String enm = Files.readString(root.resolve(
                "snip-npo-app/src/main/java/com/simba/snip/npo/integration/enm/EnmTransport.java"));
        switch (evidenceId) {
            case "T17-STR-001" -> assertFalse(v18.contains("DROP TABLE production_network_change"));
            case "T17-STR-002" -> assertTrue(Files.isDirectory(root.resolve("production-write-gateway")));
            case "T17-STR-003" -> assertFalse(containsAny(root.resolve("snip-npo-app/src/main/java"),
                    "EricssonWriteTransport", "enm.write.mutation"));
            case "T17-STR-004" -> {
                assertFalse(enm.toLowerCase(Locale.ROOT).contains("writemutation"));
                assertFalse(enm.contains("transmitMutation"));
            }
            case "T17-STR-005" -> assertTrue(Files.exists(root.resolve(
                    "production-write-gateway/src/main/java/com/simba/snip/npo/productionwritegateway/transport/UnconfiguredProductionEricssonWriteTransport.java")));
            case "T17-STR-006" -> {
                for (int i = 1; i <= 17; i++) {
                    int n = i;
                    try (Stream<Path> files = Files.list(root.resolve("snip-npo-app/src/main/resources/db/migration"))) {
                        assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("V" + n + "__")));
                    }
                }
            }
            case "T17-STR-007" -> {
                assertFalse(v18.toLowerCase(Locale.ROOT).contains("enm.ericsson.com"));
                assertFalse(v18.toLowerCase(Locale.ROOT).contains("secret_value"));
            }
            case "T17-STR-008" -> assertFalse(walkContains(root.resolve("snip-npo-app/src/main/java"),
                    path -> (path.contains("/agent/") || path.contains("/mcp/")) && path.contains("vendorcertification")));
            case "T17-STR-009" -> assertFalse(containsAny(root.resolve("snip-npo-app/src/main/java"),
                    "GatewayExecutionOrchestrator"));
            case "T17-STR-010" -> assertFalse(containsAny(root.resolve("production-write-gateway/src/main/java"),
                    "URLClassLoader", "ServiceLoader.load"));
            case "T17-STR-011" -> assertFalse(walkContains(root.resolve("snip-npo-app/src/main/java"),
                    path -> path.toLowerCase(Locale.ROOT).contains("nokia") && path.toLowerCase(Locale.ROOT).contains("write")));
            case "T17-STR-012" -> assertFalse(Files.exists(root.resolve(
                    "snip-npo-app/src/main/resources/db/migration/V19__phase18.sql")));
            case "T17-STR-013" -> assertFalse(containsAny(root.resolve("production-change-protocol/src/main/java"),
                    "com.azure"));
            case "T17-STR-014" -> assertFalse(v18.contains("level4"));
            case "T17-STR-015" -> {
                assertTrue(v18.contains("CHECK (certification_level IN ('L0','L1','L2','L3'))"));
                assertFalse(v18.contains("certification_level IN ('L0','L1','L2','L3','L4')"));
            }
            case "T17-STR-016" -> assertTrue(Files.exists(root.resolve(
                    "production-write-gateway/src/main/java/com/simba/snip/npo/productionwritegateway/vendortransport/CertifiedTransportResolver.java")));
            case "T17-STR-017" -> assertFalse(schedulerCallsExecute(root));
            case "T17-STR-018" -> {
                Path mcp = root.resolve("snip-npo-app/src/main/java/com/simba/snip/npo/mcp");
                assertFalse(Files.exists(mcp) && containsAny(mcp, "production-changes", "EXECUTE_PRODUCTION_CHANGE"));
            }
            case "T17-STR-019" -> {
                Path agent = root.resolve("snip-npo-app/src/main/java/com/simba/snip/npo/agent");
                assertFalse(Files.exists(agent) && containsAny(agent, "TRANSPORT_CERTIFY", "TARGET_ONBOARD_APPROVE"));
            }
            case "T17-STR-020" -> assertTrue(v18.contains("'MANUAL'") || yaml.contains("change-control")
                    || Files.exists(root.resolve(
                    "snip-npo-app/src/main/java/com/simba/snip/npo/productionchange/domain/ChangeControlMode.java"))
                    || containsAny(root.resolve("snip-npo-app/src/main/java/com/simba/snip/npo/productionchange"),
                    "MANUAL"));
            case "T17-STR-021" -> assertTrue(Files.exists(root.resolve(
                    "docs/architecture/SNIP-PHASE-17-CERTIFIED-VENDOR-WRITE-TRANSPORT-INTEGRATION-TARGET-ONBOARDING-PRODUCTION-OPERATIONAL-READINESS-ARCHITECTURE.md")));
            case "T17-STR-022" -> assertTrue(Files.exists(root.resolve(
                    "docs/implementation/SNIP-PHASE-17-CERTIFIED-VENDOR-WRITE-TRANSPORT-INTEGRATION-TARGET-ONBOARDING-PRODUCTION-OPERATIONAL-READINESS-SPECIFICATION.md")));
            case "T17-STR-023" -> {
                assertFalse(v18.toLowerCase(Locale.ROOT).contains("secret_value"));
                assertFalse(v18.toLowerCase(Locale.ROOT).contains("password"));
            }
            case "T17-STR-024" -> {
                assertFalse(v18.toLowerCase(Locale.ROOT).contains("http://"));
                assertFalse(v18.toLowerCase(Locale.ROOT).contains("netconf"));
            }
            case "T17-STR-025" -> assertTrue(Files.exists(root.resolve(
                    "production-write-gateway/src/main/java/com/simba/snip/npo/productionwritegateway/security/WriteCredentialHandle.java")));
            case "T17-STR-026" -> assertTrue(Files.exists(root.resolve(
                    "deploy/k8s/production-write-gateway-serviceaccount.yaml")));
            case "T17-STR-027" -> assertFalse(containsAny(root.resolve("snip-npo-app/src/main/java"),
                    "WriteCredentialHandle"));
            case "T17-STR-028" -> assertFalse(containsAny(
                    root.resolve("snip-npo-app/src/main/java/com/simba/snip/npo/vendorcertification/api"),
                    "/execute", "mutationCount"));
            case "T17-STR-029" -> {
                assertTrue(yaml.contains("enabled: false"));
                assertTrue(yaml.contains("global-execution-enabled: false"));
            }
            case "T17-STR-030" -> assertTrue(yaml.contains("test-transport-enabled: false"));
            default -> throw new IllegalArgumentException(evidenceId + " " + assertion);
        }
    }

    static Stream<Arguments> structuralCases() {
        return Stream.of(
                Arguments.of("T17-STR-001", "Phase16 tables not dropped"),
                Arguments.of("T17-STR-002", "gateway module directory exists"),
                Arguments.of("T17-STR-003", "app has no Ericsson write protocol client"),
                Arguments.of("T17-STR-004", "EnmTransport has no write methods"),
                Arguments.of("T17-STR-005", "UnconfiguredProductionEricssonWriteTransport present"),
                Arguments.of("T17-STR-006", "V1-V17 filenames exist"),
                Arguments.of("T17-STR-007", "no real hostname/secret seed in V18"),
                Arguments.of("T17-STR-008", "certification packages not in agent/mcp"),
                Arguments.of("T17-STR-009", "gateway orchestrator not an app bean"),
                Arguments.of("T17-STR-010", "no plugin/classloader transport load"),
                Arguments.of("T17-STR-011", "no Nokia write types"),
                Arguments.of("T17-STR-012", "no Phase18 artifacts"),
                Arguments.of("T17-STR-013", "protocol module has no Azure SDK"),
                Arguments.of("T17-STR-014", "no standing L4 column"),
                Arguments.of("T17-STR-015", "onboarding CHECK forbids L4"),
                Arguments.of("T17-STR-016", "SPI layering CertifiedTransportResolver preserved"),
                Arguments.of("T17-STR-017", "scheduler does not call production execute"),
                Arguments.of("T17-STR-018", "no MCP production mutation tool"),
                Arguments.of("T17-STR-019", "no Agent certify permission"),
                Arguments.of("T17-STR-020", "change-control remains MANUAL"),
                Arguments.of("T17-STR-021", "architecture document present for later SHA pin"),
                Arguments.of("T17-STR-022", "parent specification present"),
                Arguments.of("T17-STR-023", "no secret columns in V18"),
                Arguments.of("T17-STR-024", "no protocol URL/NETCONF constants"),
                Arguments.of("T17-STR-025", "gateway credential isolation class present"),
                Arguments.of("T17-STR-026", "write WI service account distinct"),
                Arguments.of("T17-STR-027", "app cannot resolve write secrets"),
                Arguments.of("T17-STR-028", "certification APIs cannot execute"),
                Arguments.of("T17-STR-029", "default production writes disabled"),
                Arguments.of("T17-STR-030", "test transport default disabled")
        );
    }

    private static boolean schedulerCallsExecute(Path root) throws IOException {
        Path scheduler = root.resolve(
                "snip-npo-app/src/main/java/com/simba/snip/npo/vendorcertification/service/Phase17CertificationExpiryScheduler.java");
        String text = Files.readString(scheduler);
        return text.contains("production-changes") || text.contains("executeProduction");
    }

    private static boolean containsAny(Path dir, String... needles) throws IOException {
        if (!Files.exists(dir)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(p -> p.toString().endsWith(".java")).anyMatch(p -> {
                try {
                    String text = Files.readString(p);
                    for (String needle : needles) {
                        if (text.contains(needle)) {
                            return true;
                        }
                    }
                    return false;
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
    }

    private static boolean walkContains(Path dir, java.util.function.Predicate<String> pathPredicate) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(p -> p.toString().endsWith(".java"))
                    .anyMatch(p -> pathPredicate.test(p.toString().replace('\\', '/')));
        }
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("snip-npo-app"))) {
            return cwd;
        }
        return cwd.getParent();
    }
}
