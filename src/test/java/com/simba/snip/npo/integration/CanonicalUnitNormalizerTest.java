package com.simba.snip.npo.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalUnitNormalizerTest {

    @Test
    void ericssonTenthsBecomeDbmAndNokiaDirectDbmIsUnchanged() {
        assertEquals(46.0d, CanonicalUnitNormalizer.txPowerToDbm(460.0d, PowerUnit.TENTHS_DBM));
        assertEquals(43.0d, CanonicalUnitNormalizer.txPowerToDbm(430.0d, PowerUnit.TENTHS_DBM));
        assertEquals(46.0d, CanonicalUnitNormalizer.txPowerToDbm(46.0d, PowerUnit.DBM));
        assertEquals("46", CanonicalUnitNormalizer.formatDbm(46.0d));
        assertTrue(CanonicalUnitNormalizer.inOperationalRange(46.0d));
        assertTrue(CanonicalUnitNormalizer.inOperationalRange(20.0d));
        assertTrue(CanonicalUnitNormalizer.inOperationalRange(50.0d));
        assertFalse(CanonicalUnitNormalizer.inOperationalRange(0.5d));
        assertFalse(CanonicalUnitNormalizer.inOperationalRange(60.0d));
    }
}
