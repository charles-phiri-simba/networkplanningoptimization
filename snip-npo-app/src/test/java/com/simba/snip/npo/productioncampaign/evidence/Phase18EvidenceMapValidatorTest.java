package com.simba.snip.npo.productioncampaign.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase18EvidenceMapValidatorTest {

    private final Phase18EvidenceMapValidator validator = new Phase18EvidenceMapValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path temp;

    @Test
    void missingGateFails() throws Exception {
        ObjectNode map = validSkeleton();
        ((ArrayNode) map.get("gates")).remove(0);
        assertInvalid(map, "missing G18");
    }

    @Test
    void duplicateGateFails() throws Exception {
        ObjectNode map = validSkeleton();
        ArrayNode gates = (ArrayNode) map.get("gates");
        gates.add(gates.get(0).deepCopy());
        assertInvalid(map, "duplicate G18");
    }

    @Test
    void unknownGateFails() throws Exception {
        ObjectNode map = validSkeleton();
        ObjectNode extra = mapper.createObjectNode();
        extra.put("id", "G18-999");
        extra.put("status", "PASS");
        extra.put("highRisk", false);
        extra.set("evidence", evidence("structural"));
        ((ArrayNode) map.get("gates")).add(extra);
        assertInvalid(map, "unknown G18");
    }

    @Test
    void missingThreatFails() throws Exception {
        ObjectNode map = validSkeleton();
        ((ArrayNode) map.get("threats")).remove(0);
        assertInvalid(map, "missing T18");
    }

    @Test
    void duplicateThreatFails() throws Exception {
        ObjectNode map = validSkeleton();
        ArrayNode threats = (ArrayNode) map.get("threats");
        threats.add(threats.get(0).deepCopy());
        assertInvalid(map, "duplicate T18");
    }

    @Test
    void unknownThreatFails() throws Exception {
        ObjectNode map = validSkeleton();
        ObjectNode extra = mapper.createObjectNode();
        extra.put("id", "T18-99");
        extra.set("gates", mapper.createArrayNode().add("G18-001"));
        ((ArrayNode) map.get("threats")).add(extra);
        assertInvalid(map, "unknown T18");
    }

    @Test
    void missingInvariantFails() throws Exception {
        ObjectNode map = validSkeleton();
        ((ArrayNode) map.get("invariants")).remove(0);
        assertInvalid(map, "missing I18");
    }

    @Test
    void duplicateInvariantFails() throws Exception {
        ObjectNode map = validSkeleton();
        ArrayNode invariants = (ArrayNode) map.get("invariants");
        invariants.add(invariants.get(0).deepCopy());
        assertInvalid(map, "duplicate I18");
    }

    @Test
    void unknownInvariantFails() throws Exception {
        ObjectNode map = validSkeleton();
        ObjectNode extra = mapper.createObjectNode();
        extra.put("id", "I18-99");
        extra.set("gates", mapper.createArrayNode().add("G18-001"));
        ((ArrayNode) map.get("invariants")).add(extra);
        assertInvalid(map, "unknown I18");
    }

    @Test
    void threatGateMappingDivergenceFails() throws Exception {
        ObjectNode map = validSkeleton();
        ArrayNode threats = (ArrayNode) map.get("threats");
        ArrayNode threatGates = (ArrayNode) threats.get(0).get("gates");
        threatGates.removeAll();
        threatGates.add("G18-001");
        assertInvalid(map, "threat -> gate mapping divergence");
    }

    @Test
    void invariantGateMappingDivergenceFails() throws Exception {
        ObjectNode map = validSkeleton();
        ArrayNode invariants = (ArrayNode) map.get("invariants");
        ArrayNode invariantGates = (ArrayNode) invariants.get(0).get("gates");
        invariantGates.removeAll();
        invariantGates.add("G18-254");
        assertInvalid(map, "invariant -> gate mapping divergence");
    }

    @Test
    void nonexistentEvidencePathFails() throws Exception {
        ObjectNode map = validSkeleton();
        ObjectNode ev = (ObjectNode) map.get("gates").get(0).get("evidence").get(0);
        ev.put("path", "does/not/exist.java");
        assertInvalid(map, "nonexistent evidence path");
    }

    @Test
    void nonexistentTestIdentifierFails() throws Exception {
        ObjectNode map = validSkeleton();
        ObjectNode ev = (ObjectNode) map.get("gates").get(0).get("evidence").get(0);
        ev.put("testIdentifier", "com.simba.snip.npo.DoesNotExist#missing");
        assertInvalid(map, "nonexistent referenced test identifier");
    }

    @Test
    void invalidStatusFails() throws Exception {
        ObjectNode map = validSkeleton();
        ((ObjectNode) map.get("gates").get(0)).put("status", "MAYBE");
        assertInvalid(map, "invalid status");
    }

    @Test
    void invalidEvidenceTypeFails() throws Exception {
        ObjectNode map = validSkeleton();
        ObjectNode ev = (ObjectNode) map.get("gates").get(0).get("evidence").get(0);
        ev.put("evidenceType", "anecdote");
        assertInvalid(map, "invalid evidence type");
    }

    @Test
    void highRiskStructuralOnlyPassFails() throws Exception {
        ObjectNode map = validSkeleton();
        ObjectNode gate = (ObjectNode) map.get("gates").get(2);
        gate.put("highRisk", true);
        gate.put("status", "PASS");
        gate.set("evidence", evidence("structural"));
        assertInvalid(map, "high-risk gate marked PASS with structural-only evidence");
    }

    @Test
    void historicalFrozenArtifactShaIsPreservedAndDistinctFromCanonicalTextSha() throws Exception {
        assertEquals(
                "19865a242141c6ab4f5e9233056eebada22dbc9c9b456b68676c736a07763a32",
                Phase18EvidenceMapValidator.HISTORICAL_FROZEN_ARTIFACT_SHA256);
        assertEquals(
                "08c9abaef3fec3cef4688690f2a9df2748c09229b97a9adbe56adbbf9600cad3",
                Phase18EvidenceMapValidator.CANONICAL_TEXT_SHA256);
        assertEquals(
                Phase18EvidenceMapValidator.HISTORICAL_FROZEN_ARTIFACT_SHA256,
                Phase18EvidenceMapValidator.FROZEN_ARCHITECTURE_SHA256);
        assertNotEquals(
                Phase18EvidenceMapValidator.HISTORICAL_FROZEN_ARTIFACT_SHA256,
                Phase18EvidenceMapValidator.CANONICAL_TEXT_SHA256);
        ObjectNode map = validSkeleton();
        assertEquals(
                Phase18EvidenceMapValidator.HISTORICAL_FROZEN_ARTIFACT_SHA256,
                map.path("architectureContentSha256").asText());
    }

    @Test
    void lfArchitectureRepresentationValidates() throws Exception {
        List<String> errors = validateAgainstTempArchitecture(toLf(realArchitectureBytes()));
        assertTrue(errors.isEmpty(), () -> String.join("\n", errors));
    }

    @Test
    void crlfArchitectureRepresentationValidates() throws Exception {
        List<String> errors = validateAgainstTempArchitecture(toCrlf(realArchitectureBytes()));
        assertTrue(errors.isEmpty(), () -> String.join("\n", errors));
    }

    @Test
    void lfAndCrlfCanonicalizeToIdenticalBytesAndHash() throws Exception {
        byte[] original = realArchitectureBytes();
        byte[] lf = toLf(original);
        byte[] crlf = toCrlf(original);
        byte[] loneCr = toLoneCr(original);
        assertNotEquals(0, lf.length);
        assertTrue(containsCrlf(crlf));
        byte[] canonicalLf = Phase18EvidenceMapValidator.canonicalizeFrozenText(lf);
        byte[] canonicalCrlf = Phase18EvidenceMapValidator.canonicalizeFrozenText(crlf);
        byte[] canonicalLoneCr = Phase18EvidenceMapValidator.canonicalizeFrozenText(loneCr);
        assertArrayEquals(canonicalLf, canonicalCrlf);
        assertArrayEquals(canonicalLf, canonicalLoneCr);
        assertEquals(
                Phase18EvidenceMapValidator.CANONICAL_TEXT_SHA256,
                Phase18EvidenceMapValidator.canonicalTextSha256(lf));
        assertEquals(
                Phase18EvidenceMapValidator.CANONICAL_TEXT_SHA256,
                Phase18EvidenceMapValidator.canonicalTextSha256(crlf));
        assertEquals(
                Phase18EvidenceMapValidator.canonicalTextSha256(lf),
                Phase18EvidenceMapValidator.canonicalTextSha256(crlf));
    }

    @Test
    void architectureByteMutationFails() throws Exception {
        byte[] bytes = realArchitectureBytes();
        bytes[Math.min(64, bytes.length - 1)] ^= 0x01;
        List<String> errors = validateAgainstTempArchitecture(bytes);
        assertTrue(errors.stream().anyMatch(e -> e.contains("architecture byte mutation")), errors.toString());
    }

    @Test
    void architectureCharacterMutationFails() throws Exception {
        byte[] lf = toLf(realArchitectureBytes());
        int idx = indexOfAsciiLetter(lf);
        lf[idx] = (byte) (lf[idx] == 'A' ? 'B' : 'A');
        List<String> errors = validateAgainstTempArchitecture(lf);
        assertTrue(errors.stream().anyMatch(e -> e.contains("architecture byte mutation")), errors.toString());
        assertNotEquals(
                Phase18EvidenceMapValidator.CANONICAL_TEXT_SHA256,
                Phase18EvidenceMapValidator.canonicalTextSha256(lf));
    }

    @Test
    void architectureDeletedCharacterFails() throws Exception {
        byte[] lf = toLf(realArchitectureBytes());
        int idx = indexOfAsciiLetter(lf);
        byte[] deleted = new byte[lf.length - 1];
        System.arraycopy(lf, 0, deleted, 0, idx);
        System.arraycopy(lf, idx + 1, deleted, idx, lf.length - idx - 1);
        List<String> errors = validateAgainstTempArchitecture(deleted);
        assertTrue(errors.stream().anyMatch(e -> e.contains("architecture byte mutation")), errors.toString());
    }

    @Test
    void architectureInsertedCharacterFails() throws Exception {
        byte[] lf = toLf(realArchitectureBytes());
        int idx = indexOfAsciiLetter(lf);
        byte[] inserted = new byte[lf.length + 1];
        System.arraycopy(lf, 0, inserted, 0, idx);
        inserted[idx] = 'X';
        System.arraycopy(lf, idx, inserted, idx + 1, lf.length - idx);
        List<String> errors = validateAgainstTempArchitecture(inserted);
        assertTrue(errors.stream().anyMatch(e -> e.contains("architecture byte mutation")), errors.toString());
    }

    @Test
    void architectureWhitespaceMutationOtherThanLineEndingFails() throws Exception {
        byte[] lf = toLf(realArchitectureBytes());
        int idx = indexOf(lf, (byte) ' ');
        lf[idx] = '\t';
        List<String> errors = validateAgainstTempArchitecture(lf);
        assertTrue(errors.stream().anyMatch(e -> e.contains("architecture byte mutation")), errors.toString());
    }

    @Test
    void architectureAndRootCopyCanonicalHashesMatch() throws Exception {
        Path repo = repoRoot();
        Path architecture = repo.resolve(
                "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md");
        Path rootCopy = repo.resolve(
                "docs/root-copies/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md");
        assertTrue(Files.exists(architecture));
        assertTrue(Files.exists(rootCopy));
        String architectureHash = Phase18EvidenceMapValidator.canonicalTextSha256(Files.readAllBytes(architecture));
        String rootCopyHash = Phase18EvidenceMapValidator.canonicalTextSha256(Files.readAllBytes(rootCopy));
        assertEquals(architectureHash, rootCopyHash);
        assertEquals(Phase18EvidenceMapValidator.CANONICAL_TEXT_SHA256, architectureHash);
        List<String> errors = validator.validate(
                repo,
                repo.resolve("docs/implementation/phase18-gate-evidence-map.json"),
                architecture);
        assertTrue(errors.isEmpty(), () -> String.join("\n", errors));
        assertFalse(errors.stream().anyMatch(e -> e.contains("root-copy")));
    }

    @Test
    void rootCopyLogicalDivergenceFails() throws Exception {
        Path repo = repoRoot();
        Path original = repo.resolve(
                "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md");
        Path isolated = temp.resolve("isolated-repo");
        Files.createDirectories(isolated.resolve("docs/root-copies"));
        Files.createDirectories(isolated.resolve("docs/implementation"));
        Files.createDirectories(isolated.resolve("docs/architecture"));
        byte[] architecture = Files.readAllBytes(original);
        byte[] diverged = toLf(architecture);
        int idx = indexOfAsciiLetter(diverged);
        diverged[idx] = (byte) (diverged[idx] == 'A' ? 'Z' : 'A');
        Files.write(isolated.resolve(
                "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md"),
                architecture);
        Files.write(isolated.resolve(
                "docs/root-copies/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md"),
                diverged);
        Files.copy(
                repo.resolve("docs/implementation/phase18-gate-evidence-map.json"),
                isolated.resolve("docs/implementation/phase18-gate-evidence-map.json"));
        List<String> errors = validator.validate(
                isolated,
                isolated.resolve("docs/implementation/phase18-gate-evidence-map.json"),
                isolated.resolve(
                        "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md"));
        assertTrue(errors.stream().anyMatch(e -> e.contains("architecture/root-copy logical equivalence mismatch")),
                errors.toString());
    }

    @Test
    void highRiskR3GateWithoutRequiredBehavioralClassFails() throws Exception {
        ObjectNode map = validSkeleton();
        ObjectNode gate = findGate(map, "G18-078");
        ArrayNode arr = mapper.createArrayNode();
        ObjectNode structural = mapper.createObjectNode();
        structural.put("evidenceType", "structural");
        structural.put("path", "snip-npo-app/src/main/resources/db/migration/V19__phase18_production_change_campaigns.sql");
        arr.add(structural);
        ObjectNode slot = mapper.createObjectNode();
        slot.put("evidenceType", "concurrency");
        slot.put("path", "snip-npo-app/src/test/java/com/simba/snip/npo/productioncampaign/CampaignMutationSlotConcurrencyIT.java");
        slot.put("testClass", "com.simba.snip.npo.productioncampaign.CampaignMutationSlotConcurrencyIT");
        slot.put("testIdentifier", "com.simba.snip.npo.productioncampaign.CampaignMutationSlotConcurrencyIT#forwardVersusRecoveryAcquisition");
        arr.add(slot);
        gate.set("evidence", arr);
        assertInvalid(map, "lacks required behavioral evidence class");
    }

    @Test
    void externalCertificationPassWithoutIndependentEvidenceFails() throws Exception {
        ObjectNode map = validSkeleton();
        ObjectNode gate = findGate(map, "G18-173");
        gate.put("status", "PASS");
        ObjectNode ev = mapper.createObjectNode();
        ev.put("evidenceType", "external_certification");
        ev.put("independentExternalEvidence", false);
        ev.put("path", "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md");
        ArrayNode arr = mapper.createArrayNode();
        arr.add(ev);
        gate.set("evidence", arr);
        List<String> errors = run(map);
        assertTrue(errors.stream().anyMatch(e -> e.contains("EXTERNAL_CERTIFICATION_NOT_EXECUTED")), errors.toString());
    }

    private void assertInvalid(ObjectNode map, String fragment) throws Exception {
        List<String> errors = run(map);
        assertFalse(errors.isEmpty(), "expected validator failure");
        assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains(fragment.toLowerCase())
                || e.contains(fragment)), errors.toString());
    }

    private List<String> run(ObjectNode map) throws Exception {
        Path mapPath = temp.resolve("map.json");
        Files.writeString(mapPath, mapper.writeValueAsString(map));
        Path repo = repoRoot();
        Path architecture = repo.resolve(
                "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md");
        try {
            return validator.validate(repo, mapPath, architecture);
        } catch (CampaignException ex) {
            assertEquals(ProductionReasonCode.EVIDENCE_MAP_INVALID, ex.reasonCode());
            return List.of(ex.getMessage());
        }
    }

    private ObjectNode validSkeleton() throws Exception {
        Path repo = repoRoot();
        Path real = repo.resolve("docs/implementation/phase18-gate-evidence-map.json");
        return (ObjectNode) mapper.readTree(Files.readString(real));
    }

    private ObjectNode findGate(ObjectNode map, String id) {
        for (var node : map.get("gates")) {
            if (id.equals(node.path("id").asText())) {
                return (ObjectNode) node;
            }
        }
        throw new IllegalStateException(id);
    }

    private ArrayNode evidence(String type) {
        ObjectNode ev = mapper.createObjectNode();
        ev.put("evidenceType", type);
        ev.put("path", "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md");
        ArrayNode arr = mapper.createArrayNode();
        arr.add(ev);
        return arr;
    }

    private List<String> validateAgainstTempArchitecture(byte[] architectureBytes) throws Exception {
        Path repo = repoRoot();
        Path architecture = temp.resolve("architecture-fixture.md");
        Files.write(architecture, architectureBytes);
        Path mapPath = temp.resolve("map.json");
        Files.writeString(mapPath, Files.readString(repo.resolve("docs/implementation/phase18-gate-evidence-map.json")));
        return validator.validate(repo, mapPath, architecture);
    }

    private static byte[] realArchitectureBytes() throws Exception {
        return Files.readAllBytes(repoRoot().resolve(
                "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md"));
    }

    private static byte[] toLf(byte[] raw) {
        return Phase18EvidenceMapValidator.canonicalizeFrozenText(raw);
    }

    private static byte[] toCrlf(byte[] raw) {
        byte[] lf = toLf(raw);
        ByteArrayOutputStream out = new ByteArrayOutputStream(lf.length + 4096);
        for (byte b : lf) {
            if (b == 0x0A) {
                out.write(0x0D);
                out.write(0x0A);
            } else {
                out.write(b);
            }
        }
        return out.toByteArray();
    }

    private static byte[] toLoneCr(byte[] raw) {
        byte[] lf = toLf(raw);
        ByteArrayOutputStream out = new ByteArrayOutputStream(lf.length);
        for (byte b : lf) {
            out.write(b == 0x0A ? 0x0D : b);
        }
        return out.toByteArray();
    }

    private static boolean containsCrlf(byte[] bytes) {
        for (int i = 0; i + 1 < bytes.length; i++) {
            if (bytes[i] == 0x0D && bytes[i + 1] == 0x0A) {
                return true;
            }
        }
        return false;
    }

    private static int indexOfAsciiLetter(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if ((b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z')) {
                return i;
            }
        }
        throw new IllegalStateException("no ASCII letter in fixture");
    }

    private static int indexOf(byte[] bytes, byte value) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == value) {
                return i;
            }
        }
        throw new IllegalStateException("value not found in fixture");
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("docs/architecture"))) {
            return cwd;
        }
        return cwd.getParent();
    }
}
