package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.service.ProductionFingerprintService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionChangeFingerprintIT extends ProductionChangeITSupport {

    @Autowired
    ProductionFingerprintService fingerprintService;

    @Test
    void deterministicFingerprint() {
        String a = fingerprintService.computeTargetFingerprint(
                TARGET_ID, "ERICSSON", "ENM", "LAB", "ericsson-enm-write-l0", "1",
                "security-l0", "credential-profile-ref-l0", "L0", "READ_THEN_WRITE");
        String b = fingerprintService.computeTargetFingerprint(
                TARGET_ID, "ERICSSON", "ENM", "LAB", "ericsson-enm-write-l0", "1",
                "security-l0", "credential-profile-ref-l0", "L0", "READ_THEN_WRITE");
        assertEquals(a, b);
        assertEquals(64, a.length());
        ProductionChangeDto first = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        String hash = fingerprintService.operationBindingHash(
                CELL, "txPower", first.expectedValue(), first.desiredValue(), "FORWARD");
        assertEquals(hash, fingerprintService.operationBindingHash(
                CELL, "txPower", first.expectedValue(), first.desiredValue(), "FORWARD"));
        assertEquals(64, hash.length());
        BigDecimal unused = BigDecimal.ZERO;
        assertEquals(0, unused.intValue());
    }
}
