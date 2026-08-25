package com.simba.snip.npo.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalEnumerationNormalizerTest {

    @Test
    void vendorTechnologyAndDuplexAndStateNormalizeToSnipVocabulary() {
        assertEquals("NR", CanonicalEnumerationNormalizer.technology("NR").orElseThrow());
        assertEquals("NR", CanonicalEnumerationNormalizer.technology("5G-NR").orElseThrow());
        assertEquals("LTE", CanonicalEnumerationNormalizer.technology("EUTRAN").orElseThrow());
        assertTrue(CanonicalEnumerationNormalizer.technology("GSM").isEmpty());
        assertEquals("TDD", CanonicalEnumerationNormalizer.duplex("tdd").orElseThrow());
        assertEquals("FDD", CanonicalEnumerationNormalizer.duplex("FDD").orElseThrow());
        assertEquals("ACTIVE", CanonicalEnumerationNormalizer.operationalStatus("UNLOCKED").orElseThrow());
        assertEquals("ACTIVE", CanonicalEnumerationNormalizer.operationalStatus("enabled").orElseThrow());
        assertEquals("INACTIVE", CanonicalEnumerationNormalizer.operationalStatus("LOCKED").orElseThrow());
    }
}
