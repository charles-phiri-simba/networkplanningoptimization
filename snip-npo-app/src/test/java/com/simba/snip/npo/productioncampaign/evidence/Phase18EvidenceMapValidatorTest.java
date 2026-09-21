package com.simba.snip.npo.productioncampaign.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void architectureByteMutationFails() throws Exception {
        Path repo = repoRoot();
        Path original = repo.resolve(
                "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md");
        byte[] bytes = Files.readAllBytes(original);
        bytes[Math.min(64, bytes.length - 1)] ^= 0x01;
        Path mutated = temp.resolve("mutated-architecture.md");
        Files.write(mutated, bytes);
        Path mapPath = temp.resolve("map.json");
        Files.writeString(mapPath, Files.readString(repo.resolve("docs/implementation/phase18-gate-evidence-map.json")));
        List<String> errors = validator.validate(repo, mapPath, mutated);
        assertTrue(errors.stream().anyMatch(e -> e.contains("architecture byte mutation")), errors.toString());
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

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("docs/architecture"))) {
            return cwd;
        }
        return cwd.getParent();
    }
}
