package com.simba.snip.npo.planning;

import com.simba.snip.npo.planning.service.PlanningFingerprintService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlanningEvaluationServiceTest {

    @Test
    void admissionCanonicalExcludesOutcomeAndFailure() {
        PlanningFingerprintService service = new PlanningFingerprintService();
        String canonical = service.admissionCanonical("abc", List.of(
                new PlanningFingerprintService.AdmissionLine(
                        1,
                        "CELL-001",
                        UUID.fromString("00000000-0000-4000-8000-0000000000aa"),
                        1,
                        new BigDecimal("46.00"),
                        "cfg",
                        "snip.synthetic.cell-parameter.v1",
                        "1.0"
                )
        ));
        assertEquals("""
                schema=planning.admission.v1
                intentFingerprint=abc
                item.1.CELL-001.twinId=00000000-0000-4000-8000-0000000000aa
                item.1.CELL-001.twinVersion=1
                item.1.CELL-001.baseline=46
                item.1.CELL-001.config=cfg
                item.1.CELL-001.modelId=snip.synthetic.cell-parameter.v1
                item.1.CELL-001.modelVersion=1.0""", canonical);
        assertFalse(canonical.contains("outcome"));
        assertFalse(canonical.contains("failureCode"));
    }
}
