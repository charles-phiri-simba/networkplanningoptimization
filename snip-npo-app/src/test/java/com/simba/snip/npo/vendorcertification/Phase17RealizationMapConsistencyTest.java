package com.simba.snip.npo.vendorcertification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17RealizationMapConsistencyTest {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "NAMED_TEST",
            "PARAMETERIZED_TEST_CASE",
            "BATCHED_STRUCTURAL_PROOF",
            "BATCHED_DATABASE_PROOF",
            "BATCHED_SECURITY_PROOF",
            "INFRASTRUCTURE_ARTIFACT",
            "SOURCE_INSPECTION",
            "NOT_REALIZED"
    );

    @Test
    void realizationMapCoversFrozenLocalCatalogWithoutCatalogIndexProof() throws Exception {
        Path root = repoRoot();
        JsonNode doc = new ObjectMapper().readTree(Files.readString(
                root.resolve("docs/implementation/phase17-implementation-evidence-realization.json")));
        assertEquals(0, doc.get("catalogIndexOnlyCountedAsProof").asInt());
        assertEquals(253, doc.get("realized").asInt());
        assertEquals(0, doc.get("notRealized").asInt());
        assertEquals("ACCEPTABLE_ALIAS", doc.get("level4DenialCodeObservation").asText());

        JsonNode entries = doc.get("entries");
        assertEquals(253, entries.size());
        Set<String> ids = new TreeSet<>();
        int catalogIndex = 0;
        int notRealized = 0;
        for (JsonNode entry : entries) {
            String id = entry.get("evidenceId").asText();
            assertTrue(ids.add(id), "duplicate " + id);
            assertFalse(id.startsWith("EXT17"), id);
            String type = entry.get("realizationType").asText();
            assertTrue(ALLOWED_TYPES.contains(type), id + " " + type);
            assertFalse("CATALOG_INDEX_ONLY".equals(type), id);
            assertEquals("REALIZED", entry.get("status").asText(), id);
            assertTrue(Files.exists(root.resolve(entry.get("artifactPath").asText())), id);
            if ("NOT_REALIZED".equals(type) || "NOT_REALIZED".equals(entry.get("status").asText())) {
                notRealized++;
            }
            String method = entry.get("testMethod").asText();
            if (method.contains("Index") || method.toLowerCase().contains("catalogcount")) {
                catalogIndex++;
            }
        }
        assertEquals(0, notRealized);
        assertEquals(0, catalogIndex);
        assertEquals(expectedCatalog(), ids);
        assertEquals(158, doc.get("gateClassifications").size());
    }

    private static Set<String> expectedCatalog() {
        Set<String> ids = new HashSet<>();
        for (int i = 1; i <= 30; i++) {
            ids.add(String.format("T17-STR-%03d", i));
        }
        for (int i = 1; i <= 31; i++) {
            ids.add(String.format("T17-DB-%03d", i));
        }
        for (int i = 1; i <= 70; i++) {
            ids.add(String.format("T17-IMPL-%03d", i));
        }
        for (int i = 1; i <= 38; i++) {
            ids.add(String.format("T17-INT-%03d", i));
        }
        for (int i = 1; i <= 30; i++) {
            ids.add(String.format("T17-SEC-%03d", i));
        }
        for (int i = 1; i <= 8; i++) {
            ids.add(String.format("T17-INF-%03d", i));
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            ids.add("CS17-" + c);
        }
        for (int i = 1; i <= 20; i++) {
            ids.add(String.format("FI17-%03d", i));
        }
        return ids;
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return Files.exists(cwd.resolve("docs/implementation/phase17-implementation-evidence-realization.json"))
                ? cwd : cwd.getParent();
    }
}
