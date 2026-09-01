package com.simba.snip.npo.productionchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeGateTraceabilityValidationTest {

    private static final Pattern EVIDENCE_ROW = Pattern.compile(
            "^\\| (P16-E\\d{3}) \\|.*?\\| (STRUCTURAL|BEHAVIORAL|INTEGRATION|INFRASTRUCTURE|EXTERNAL_CERTIFICATION) \\|",
            Pattern.MULTILINE);
    private static final Pattern HIGH_RISK_EVIDENCE = Pattern.compile("P16-E\\d{3}");
    private static final Set<String> STRUCTURAL_ONLY_ALLOWED = Set.of("G16-010", "G16-014");

    @Test
    void allGatesHaveEvidence() throws Exception {
        JsonNode root = loadJson();
        assertTrue(root.isArray());
        assertEquals(154, root.size());
        Set<String> gates = new TreeSet<>();
        int evidenceRefs = 0;
        for (JsonNode node : root) {
            String gate = node.get("gate").asText();
            assertTrue(gate.matches("G16-\\d{3}"));
            assertTrue(gates.add(gate), "duplicate " + gate);
            JsonNode ids = node.get("evidenceIds");
            assertTrue(ids.isArray() && ids.size() >= 1, gate + " missing evidence");
            evidenceRefs += ids.size();
            JsonNode types = node.get("evidenceTypes");
            assertTrue(types.isArray() && types.size() >= 1, gate + " missing types");
        }
        assertEquals(154, gates.size());
        for (int i = 1; i <= 154; i++) {
            assertTrue(gates.contains("G16-" + String.format("%03d", i)), "missing G16-" + String.format("%03d", i));
        }
        assertTrue(evidenceRefs >= 154);

        Map<String, String> catalogTypes = catalogEvidenceTypes();
        assertEquals(173, catalogTypes.size());
        for (JsonNode node : root) {
            for (JsonNode id : node.get("evidenceIds")) {
                assertTrue(catalogTypes.containsKey(id.asText()), "unknown evidence " + id.asText());
            }
        }

        String spec = specText();
        assertFalse(spec.contains("| GENERIC |"));
        assertTrue(spec.contains("GENERIC_PLACEHOLDERS=0"));
        assertTrue(spec.toLowerCase(Locale.ROOT).contains("generic placeholder evidence is forbidden"));
        long placeholders = catalogTypes.keySet().stream()
                .filter(id -> id.contains("PLACEHOLDER") || id.contains("TBD"))
                .count();
        assertEquals(0, placeholders);

        Set<String> highRiskGates = highRiskGatesFromSpec(spec, jsonEvidenceIndex(root));
        for (String gate : highRiskGates) {
            JsonNode node = findGate(root, gate);
            Set<String> types = new LinkedHashSet<>();
            node.get("evidenceTypes").forEach(t -> types.add(t.asText()));
            boolean structuralOnly = types.size() == 1 && types.contains("STRUCTURAL");
            if (STRUCTURAL_ONLY_ALLOWED.contains(gate)) {
                continue;
            }
            assertFalse(structuralOnly, gate + " is high-risk but STRUCTURAL-only: " + types);
        }
    }

    @Test
    void jsonMapMatchesMarkdown() throws Exception {
        JsonNode root = loadJson();
        Map<String, List<String>> json = new LinkedHashMap<>();
        for (JsonNode node : root) {
            List<String> ids = new ArrayList<>();
            node.get("evidenceIds").forEach(id -> ids.add(id.asText()));
            json.put(node.get("gate").asText(), ids);
        }
        String spec = specText();
        int section54 = spec.indexOf("## 54. Architecture gate traceability matrix");
        int section55 = spec.indexOf("## 55. Implementation evidence catalog");
        assertTrue(section54 > 0 && section55 > section54);
        String table = spec.substring(section54, section55);
        Map<String, List<String>> markdown = new LinkedHashMap<>();
        for (String line : table.split("\n")) {
            if (!line.startsWith("| G16-")) {
                continue;
            }
            String[] cols = line.split("\\|", -1);
            if (cols.length < 7) {
                continue;
            }
            String gate = cols[1].trim();
            List<String> ids = new ArrayList<>();
            for (String id : cols[5].split(",")) {
                ids.add(id.trim());
            }
            markdown.put(gate, ids);
        }
        assertEquals(154, markdown.size());
        assertEquals(markdown.keySet(), json.keySet());
        for (Map.Entry<String, List<String>> entry : markdown.entrySet()) {
            assertEquals(entry.getValue(), json.get(entry.getKey()), entry.getKey());
        }
        String architecture = Files.readString(ProductionChangeSourcePaths.repoRoot().resolve(
                "docs/architecture/SNIP-PHASE-16-VENDOR-WRITE-INTEGRATION-SECURITY-PRODUCTION-CHANGE-CONTROL-CONTROLLED-REAL-NETWORK-EXECUTION-ARCHITECTURE.md"));
        assertTrue(architecture.contains("ae9c13d") || architecture.contains("Phase 15"));
        assertTrue(architecture.contains("0cb1223"));
    }

    private static JsonNode loadJson() throws IOException {
        Path path = ProductionChangeSourcePaths.repoRoot()
                .resolve("docs/implementation/phase16-gate-evidence-map.json");
        assertTrue(Files.exists(path));
        return new ObjectMapper().readTree(path.toFile());
    }

    private static String specText() throws IOException {
        return Files.readString(ProductionChangeSourcePaths.repoRoot().resolve(
                "docs/implementation/SNIP-PHASE-16-VENDOR-WRITE-INTEGRATION-SECURITY-PRODUCTION-CHANGE-CONTROL-CONTROLLED-REAL-NETWORK-EXECUTION-SPECIFICATION.md"));
    }

    private static Map<String, String> catalogEvidenceTypes() throws IOException {
        String spec = specText();
        int start = spec.indexOf("## 55. Implementation evidence catalog");
        int end = spec.indexOf("### 55.1 Evidence totals");
        Matcher matcher = EVIDENCE_ROW.matcher(spec.substring(start, end));
        Map<String, String> types = new LinkedHashMap<>();
        while (matcher.find()) {
            types.put(matcher.group(1), matcher.group(2));
        }
        return types;
    }

    private static Map<String, JsonNode> jsonEvidenceIndex(JsonNode root) {
        Map<String, JsonNode> byGate = new LinkedHashMap<>();
        root.forEach(node -> byGate.put(node.get("gate").asText(), node));
        return byGate;
    }

    private static JsonNode findGate(JsonNode root, String gate) {
        for (JsonNode node : root) {
            if (gate.equals(node.get("gate").asText())) {
                return node;
            }
        }
        throw new AssertionError("missing gate " + gate);
    }

    private static Set<String> highRiskGatesFromSpec(String spec, Map<String, JsonNode> byGate) {
        int start = spec.indexOf("### 55.2 High-risk runtime proof index");
        int end = spec.indexOf("## 56. Critical scenarios");
        Matcher matcher = HIGH_RISK_EVIDENCE.matcher(spec.substring(start, end));
        Set<String> evidence = new LinkedHashSet<>();
        while (matcher.find()) {
            evidence.add(matcher.group());
        }
        Set<String> gates = new TreeSet<>();
        byGate.forEach((gate, node) -> {
            for (JsonNode id : node.get("evidenceIds")) {
                if (evidence.contains(id.asText())) {
                    gates.add(gate);
                }
            }
        });
        return gates;
    }
}
