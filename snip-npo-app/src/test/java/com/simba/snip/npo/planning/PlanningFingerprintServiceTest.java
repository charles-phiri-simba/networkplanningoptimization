package com.simba.snip.npo.planning;

import com.simba.snip.npo.planning.service.PlanningFingerprintService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanningFingerprintServiceTest {

    private final PlanningFingerprintService service = new PlanningFingerprintService();
    private static final UUID SCENARIO = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID TWIN = UUID.fromString("00000000-0000-4000-8000-0000000000aa");

    @Test
    void goldenIntentCanonicalAndHash() {
        String canonical = service.intentCanonical(SCENARIO, List.of(
                new PlanningFingerprintService.IntentLine(1, "Alternative A", "CELL-001", "txPower", new BigDecimal("44.0")),
                new PlanningFingerprintService.IntentLine(1, "Alternative A", "CELL-002", "txPower", new BigDecimal("45.00"))
        ));
        String expected = """
                schema=planning.intent.v1
                scenarioId=00000000-0000-4000-8000-000000000001
                alternative.1.name=Alternative A
                alternative.1.intent.CELL-001.parameter=txPower
                alternative.1.intent.CELL-001.intended=44
                alternative.1.intent.CELL-002.parameter=txPower
                alternative.1.intent.CELL-002.intended=45
                modelId=snip.synthetic.cell-parameter.v1
                modelVersion=1.0""";
        assertEquals(expected, canonical);
        assertEquals(PlanningFingerprintService.sha256Hex(expected), service.intentFingerprint(SCENARIO, List.of(
                new PlanningFingerprintService.IntentLine(1, "Alternative A", "CELL-002", "txPower", new BigDecimal("45")),
                new PlanningFingerprintService.IntentLine(1, "Alternative A", "CELL-001", "txPower", new BigDecimal("44"))
        )));
    }

    @Test
    void editChangesIntentFingerprint() {
        var original = service.intentFingerprint(SCENARIO, List.of(
                new PlanningFingerprintService.IntentLine(1, "A", "CELL-001", "txPower", new BigDecimal("44"))
        ));
        var edited = service.intentFingerprint(SCENARIO, List.of(
                new PlanningFingerprintService.IntentLine(1, "A", "CELL-001", "txPower", new BigDecimal("42"))
        ));
        assertNotEquals(original, edited);
        assertTrue(PlanningFingerprintService.SHA256_HEX.matcher(original).matches());
    }

    @Test
    void goldenAdmissionCanonicalIsPreExecutionAndOrderIndependent() {
        PlanningFingerprintService.AdmissionLine first = new PlanningFingerprintService.AdmissionLine(
                1, "CELL-001", TWIN, 1, new BigDecimal("46.00"), "cfg-1",
                "snip.synthetic.cell-parameter.v1", "1.0"
        );
        PlanningFingerprintService.AdmissionLine second = new PlanningFingerprintService.AdmissionLine(
                1, "CELL-002", UUID.fromString("00000000-0000-4000-8000-0000000000bb"), 2,
                new BigDecimal("43"), "cfg-2", "snip.synthetic.cell-parameter.v1", "1.0"
        );
        String expected = """
                schema=planning.admission.v1
                intentFingerprint=abc
                item.1.CELL-001.twinId=00000000-0000-4000-8000-0000000000aa
                item.1.CELL-001.twinVersion=1
                item.1.CELL-001.baseline=46
                item.1.CELL-001.config=cfg-1
                item.1.CELL-001.modelId=snip.synthetic.cell-parameter.v1
                item.1.CELL-001.modelVersion=1.0
                item.1.CELL-002.twinId=00000000-0000-4000-8000-0000000000bb
                item.1.CELL-002.twinVersion=2
                item.1.CELL-002.baseline=43
                item.1.CELL-002.config=cfg-2
                item.1.CELL-002.modelId=snip.synthetic.cell-parameter.v1
                item.1.CELL-002.modelVersion=1.0""";
        assertEquals(expected, service.admissionCanonical("abc", List.of(second, first)));
        assertEquals(
                service.admissionFingerprint("abc", List.of(first, second)),
                service.admissionFingerprint("abc", List.of(second, first))
        );
        assertTrue(PlanningFingerprintService.SHA256_HEX.matcher(service.admissionFingerprint("abc", List.of(first))).matches());
    }

    @Test
    void outcomeIsNotPartOfAdmissionIdentity() {
        PlanningFingerprintService.AdmissionLine admitted = new PlanningFingerprintService.AdmissionLine(
                1, "CELL-001", TWIN, 1, new BigDecimal("46"), "cfg",
                "snip.synthetic.cell-parameter.v1", "1.0"
        );
        String succeeded = service.admissionCanonical("abc", List.of(admitted));
        assertTrue(!succeeded.contains("outcome") && !succeeded.contains("SUCCEEDED") && !succeeded.contains("FAILED"));
    }

    @Test
    void changedModelVersionChangesAdmissionIdentity() {
        PlanningFingerprintService.AdmissionLine v1 = line("1.0");
        PlanningFingerprintService.AdmissionLine v2 = line("9.9");
        assertNotEquals(service.admissionFingerprint("abc", List.of(v1)), service.admissionFingerprint("abc", List.of(v2)));
    }

    @Test
    void changedTwinVersionChangesAdmissionIdentity() {
        PlanningFingerprintService.AdmissionLine v1 = new PlanningFingerprintService.AdmissionLine(
                1, "CELL-001", TWIN, 1, new BigDecimal("46"), "cfg",
                "snip.synthetic.cell-parameter.v1", "1.0"
        );
        PlanningFingerprintService.AdmissionLine v2 = new PlanningFingerprintService.AdmissionLine(
                1, "CELL-001", TWIN, 2, new BigDecimal("46"), "cfg",
                "snip.synthetic.cell-parameter.v1", "1.0"
        );
        assertNotEquals(service.admissionFingerprint("abc", List.of(v1)), service.admissionFingerprint("abc", List.of(v2)));
    }

    @Test
    void requireSha256RejectsNonHex() {
        assertThrows(IllegalArgumentException.class, () -> PlanningFingerprintService.requireSha256Hex("not-a-hash"));
        assertThrows(IllegalArgumentException.class, () -> PlanningFingerprintService.requireSha256Hex("A".repeat(64)));
    }

    private static PlanningFingerprintService.AdmissionLine line(String modelVersion) {
        return new PlanningFingerprintService.AdmissionLine(
                1, "CELL-001", TWIN, 1, new BigDecimal("46"), "cfg",
                "snip.synthetic.cell-parameter.v1", modelVersion
        );
    }
}
