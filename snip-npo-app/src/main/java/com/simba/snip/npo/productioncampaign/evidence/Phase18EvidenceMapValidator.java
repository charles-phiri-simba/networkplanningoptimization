package com.simba.snip.npo.productioncampaign.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Phase18EvidenceMapValidator {

    /**
     * Historical working-tree SHA-256 of the frozen architecture on a CRLF checkout.
     * Preserved as the evidence-map {@code architectureContentSha256} identity.
     * Not used to hash checkout bytes.
     */
    public static final String HISTORICAL_FROZEN_ARTIFACT_SHA256 =
            "19865a242141c6ab4f5e9233056eebada22dbc9c9b456b68676c736a07763a32";

    /**
     * SHA-256 of the frozen architecture after deterministic text canonicalization
     * (UTF-8 bytes; CRLF and lone CR mapped to LF; no trim or rewrite).
     */
    public static final String CANONICAL_TEXT_SHA256 =
            "08c9abaef3fec3cef4688690f2a9df2748c09229b97a9adbe56adbbf9600cad3";

    /** Evidence-map field identity; equals {@link #HISTORICAL_FROZEN_ARTIFACT_SHA256}. */
    public static final String FROZEN_ARCHITECTURE_SHA256 = HISTORICAL_FROZEN_ARTIFACT_SHA256;
    public static final String FROZEN_ARCHITECTURE_BASELINE = "f38a62ad0e3f80522a95830322079b1380289719";

    static final Set<String> STATUSES = Set.of("PASS", "FAIL", "NOT_EXECUTED", "NOT_APPLICABLE");
    static final Set<String> EVIDENCE_TYPES = Set.of(
            "structural", "behavioral", "database", "integration", "security",
            "concurrency", "failure_injection", "external_certification"
    );
    static final Set<String> EXTERNAL_GATES = Set.of(
            "G18-169", "G18-170", "G18-171", "G18-172", "G18-173"
    );
    private static final Pattern TEST_ID = Pattern.compile("^([A-Za-z0-9_.]+)#([A-Za-z0-9_]+)$");
    private static final Map<String, List<String>> HIGH_RISK_REQUIRED_TEST_CLASSES = Map.ofEntries(
            Map.entry("G18-078", List.of("CampaignOriginGatewayPreflightIT")),
            Map.entry("G18-141", List.of("CampaignP17InvalidationCascadeIT")),
            Map.entry("G18-142", List.of("CampaignP17InvalidationCascadeIT")),
            Map.entry("G18-143", List.of("CampaignP17InvalidationCascadeIT")),
            Map.entry("G18-144", List.of("CampaignP17InvalidationCascadeIT")),
            Map.entry("G18-145", List.of("CampaignP17InvalidationCascadeIT")),
            Map.entry("G18-146", List.of("CampaignP17InvalidationCascadeIT")),
            Map.entry("G18-189", List.of("CampaignP17InvalidationCascadeIT")),
            Map.entry("G18-191", List.of("CampaignReleaseFingerprintTest", "CampaignReleaseMembershipIT")),
            Map.entry("G18-203", List.of("CampaignToP16HandoffIT")),
            Map.entry("G18-204", List.of("CampaignToP16HandoffIT")),
            Map.entry("G18-219", List.of("CampaignResumptionGovernanceIT")),
            Map.entry("G18-243", List.of("CampaignOriginRollbackGuardIT"))
    );

    private final ObjectMapper mapper = new ObjectMapper();

    public List<String> validate(Path repoRoot, Path mapPath, Path architecturePath) {
        List<String> errors = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(Files.readString(mapPath));
            Phase18ArchitectureCatalog.Catalog catalog = Phase18ArchitectureCatalog.parse(architecturePath);
            if (root.path("phase").asInt(-1) != 18) {
                errors.add("phase must be 18");
            }
            if (!FROZEN_ARCHITECTURE_BASELINE.equals(root.path("architectureBaseline").asText())) {
                errors.add("architectureBaseline mismatch");
            }
            if (!FROZEN_ARCHITECTURE_SHA256.equalsIgnoreCase(root.path("architectureContentSha256").asText())) {
                errors.add("architectureContentSha256 mismatch");
            }
            JsonNode gates = root.get("gates");
            JsonNode threats = root.get("threats");
            JsonNode invariants = root.get("invariants");
            if (gates == null || !gates.isArray()) {
                errors.add("gates[] required");
                return errors;
            }
            if (threats == null || !threats.isArray()) {
                errors.add("threats[] required");
                return errors;
            }
            if (invariants == null || !invariants.isArray()) {
                errors.add("invariants[] required");
                return errors;
            }
            validateCatalogue(gates, "id", "G18-", Phase18ArchitectureCatalog.GATE_COUNT, catalog.gates(), errors, "G18");
            validateCatalogue(threats, "id", "T18-", Phase18ArchitectureCatalog.THREAT_COUNT, catalog.threats(), errors, "T18");
            validateCatalogue(invariants, "id", "I18-", Phase18ArchitectureCatalog.INVARIANT_COUNT, catalog.invariants(), errors, "I18");
            validateMappings(threats, catalog.threatToGates(), errors, "threat");
            validateMappings(invariants, catalog.invariantToGates(), errors, "invariant");
            validateArchitectureBytes(repoRoot, architecturePath, errors);
            validateGateEvidence(repoRoot, gates, errors);
        } catch (CampaignException ex) {
            errors.add(ex.reasonCode().name() + ": " + ex.getMessage());
        } catch (Exception ex) {
            errors.add("EVIDENCE_MAP_INVALID: " + ex.getMessage());
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        return List.of();
    }

    public void validateOrThrow(Path repoRoot, Path mapPath, Path architecturePath) {
        List<String> errors = validate(repoRoot, mapPath, architecturePath);
        if (!errors.isEmpty()) {
            throw new CampaignException(
                    ProductionReasonCode.EVIDENCE_MAP_INVALID,
                    String.join("; ", errors)
            );
        }
    }

    private void validateCatalogue(
            JsonNode array,
            String idField,
            String prefix,
            int expectedCount,
            Set<String> architectureIds,
            List<String> errors,
            String label
    ) {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> duplicates = new TreeSet<>();
        Set<String> unknown = new TreeSet<>();
        for (JsonNode node : array) {
            String id = node.path(idField).asText("");
            if (!seen.add(id)) {
                duplicates.add(id);
            }
            if (!architectureIds.contains(id)) {
                unknown.add(id);
            }
        }
        Set<String> missing = new TreeSet<>(architectureIds);
        missing.removeAll(seen);
        if (seen.size() != expectedCount || array.size() != expectedCount) {
            errors.add(label + " count expected " + expectedCount + " actual " + array.size());
        }
        if (!missing.isEmpty()) {
            errors.add("missing " + label + ": " + missing);
        }
        if (!duplicates.isEmpty()) {
            errors.add("duplicate " + label + ": " + duplicates);
        }
        if (!unknown.isEmpty()) {
            errors.add("unknown " + label + ": " + unknown);
        }
    }

    private void validateMappings(
            JsonNode array,
            Map<String, List<String>> architecture,
            List<String> errors,
            String kind
    ) {
        for (JsonNode node : array) {
            String id = node.path("id").asText();
            List<String> expected = architecture.get(id);
            if (expected == null) {
                errors.add(kind + " mapping missing from architecture for " + id);
                continue;
            }
            List<String> actual = new ArrayList<>();
            JsonNode gates = node.get("gates");
            if (gates == null || !gates.isArray()) {
                errors.add(kind + " " + id + " missing gates[]");
                continue;
            }
            gates.forEach(g -> actual.add(g.asText()));
            if (!expected.equals(actual)) {
                errors.add(kind + " -> gate mapping divergence for " + id + " expected " + expected + " actual " + actual);
            }
        }
        if (array.size() != architecture.size()) {
            errors.add(kind + " mapping count divergence");
        }
    }

    private void validateGateEvidence(Path repoRoot, JsonNode gates, List<String> errors) {
        for (JsonNode gate : gates) {
            String id = gate.path("id").asText();
            String status = gate.path("status").asText();
            if (!STATUSES.contains(status)) {
                errors.add("invalid status for " + id + ": " + status);
            }
            boolean highRisk = gate.path("highRisk").asBoolean(false);
            JsonNode evidence = gate.get("evidence");
            if (evidence == null || !evidence.isArray() || evidence.isEmpty()) {
                errors.add("evidence required for " + id);
                continue;
            }
            Set<String> types = new HashSet<>();
            boolean independentExternal = false;
            for (JsonNode ev : evidence) {
                String type = ev.path("evidenceType").asText().toLowerCase(Locale.ROOT);
                if (!EVIDENCE_TYPES.contains(type)) {
                    errors.add("invalid evidence type for " + id + ": " + type);
                }
                types.add(type);
                String path = ev.path("path").asText(null);
                if (path != null && !path.isBlank() && !Files.exists(repoRoot.resolve(path))) {
                    errors.add("nonexistent evidence path for " + id + ": " + path);
                }
                String testId = ev.path("testIdentifier").asText(null);
                if (testId != null && !testId.isBlank() && !testExists(repoRoot, testId)) {
                    errors.add("nonexistent referenced test identifier for " + id + ": " + testId);
                }
                if ("external_certification".equals(type) && ev.path("independentExternalEvidence").asBoolean(false)) {
                    independentExternal = true;
                }
                if ("PASS".equals(status) && "external_certification".equals(type) && !ev.path("independentExternalEvidence").asBoolean(false)) {
                    errors.add("EXTERNAL_CERTIFICATION_NOT_EXECUTED for " + id);
                }
            }
            if (highRisk && "PASS".equals(status) && types.size() == 1 && types.contains("structural")) {
                errors.add("high-risk gate marked PASS with structural-only evidence: " + id);
            }
            if (highRisk && "PASS".equals(status)) {
                boolean hasBehavioralTest = false;
                for (JsonNode ev : evidence) {
                    String type = ev.path("evidenceType").asText("").toLowerCase(Locale.ROOT);
                    String testId = ev.path("testIdentifier").asText("");
                    if ((type.equals("behavioral") || type.equals("integration") || type.equals("database")
                            || type.equals("security") || type.equals("concurrency") || type.equals("failure_injection"))
                            && TEST_ID.matcher(testId).matches()) {
                        hasBehavioralTest = true;
                        break;
                    }
                }
                if (!hasBehavioralTest) {
                    errors.add("high-risk PASS requires a behavioral/integration testIdentifier: " + id);
                }
            }
            if (EXTERNAL_GATES.contains(id) && !"NOT_EXECUTED".equals(status) && !independentExternal) {
                errors.add("EXTERNAL_CERTIFICATION_NOT_EXECUTED for " + id);
            }
            if (highRisk && "PASS".equals(status)) {
                List<String> required = HIGH_RISK_REQUIRED_TEST_CLASSES.get(id);
                if (required != null) {
                    for (String requiredClass : required) {
                        boolean cited = false;
                        for (JsonNode ev : evidence) {
                            String testId = ev.path("testIdentifier").asText("");
                            String type = ev.path("evidenceType").asText("").toLowerCase(Locale.ROOT);
                            if (testId.contains(requiredClass)
                                    && (type.equals("behavioral") || type.equals("integration")
                                    || type.equals("database") || type.equals("security")
                                    || type.equals("concurrency") || type.equals("failure_injection"))) {
                                cited = true;
                                break;
                            }
                        }
                        if (!cited) {
                            errors.add("high-risk PASS for " + id + " lacks required behavioral evidence class "
                                    + requiredClass);
                        }
                    }
                }
            }
        }
    }

    /**
     * Canonical frozen-text bytes: UTF-8 as stored, CRLF and lone CR mapped to LF.
     * No locale transform, no whitespace trim, no Markdown rewrite.
     */
    public static byte[] canonicalizeFrozenText(byte[] raw) {
        if (raw == null) {
            throw new IllegalArgumentException("frozen text bytes required");
        }
        byte[] out = new byte[raw.length];
        int n = 0;
        for (int i = 0; i < raw.length; i++) {
            byte b = raw[i];
            if (b == 0x0D) {
                out[n++] = 0x0A;
                if (i + 1 < raw.length && raw[i + 1] == 0x0A) {
                    i++;
                }
            } else {
                out[n++] = b;
            }
        }
        return n == raw.length ? out : Arrays.copyOf(out, n);
    }

    public static String canonicalTextSha256(byte[] raw) {
        return Sha256Hex.hashBytes(canonicalizeFrozenText(raw));
    }

    private void validateArchitectureBytes(Path repoRoot, Path architecturePath, List<String> errors) {
        try {
            byte[] architectureBytes = Files.readAllBytes(architecturePath);
            String actual = canonicalTextSha256(architectureBytes);
            if (!CANONICAL_TEXT_SHA256.equalsIgnoreCase(actual)) {
                errors.add("architecture byte mutation / SHA mismatch expected canonical "
                        + CANONICAL_TEXT_SHA256 + " actual " + actual
                        + " (historical frozen artifact SHA " + HISTORICAL_FROZEN_ARTIFACT_SHA256 + ")");
            }
            Path rootCopy = repoRoot.resolve(
                    "docs/root-copies/" + architecturePath.getFileName());
            if (Files.exists(rootCopy)) {
                String copyHash = canonicalTextSha256(Files.readAllBytes(rootCopy));
                if (!actual.equalsIgnoreCase(copyHash)) {
                    errors.add("architecture/root-copy logical equivalence mismatch");
                }
            }
        } catch (Exception ex) {
            errors.add("architecture bytes unreadable: " + ex.getMessage());
        }
    }

    private boolean testExists(Path repoRoot, String testId) {
        Matcher m = TEST_ID.matcher(testId);
        if (!m.matches()) {
            return false;
        }
        String className = m.group(1);
        String method = m.group(2);
        String relative = className.replace('.', '/') + ".java";
        Path[] candidates = new Path[] {
                repoRoot.resolve("snip-npo-app/src/test/java").resolve(relative),
                repoRoot.resolve("production-write-gateway/src/test/java").resolve(relative),
                repoRoot.resolve("production-change-protocol/src/test/java").resolve(relative)
        };
        Pattern annotated = Pattern.compile(
                "(?:@(?:Test|ParameterizedTest)\\b[\\s\\S]{0,800}?)(?:public\\s+)?void\\s+"
                        + Pattern.quote(method)
                        + "\\s*\\("
        );
        for (Path file : candidates) {
            if (!Files.exists(file)) {
                continue;
            }
            try {
                String src = Files.readString(file);
                if (annotated.matcher(src).find()) {
                    return true;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }
}
