package com.simba.snip.npo.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.simba.snip.npo.integration.ericsson.EricssonFixtureAdapter;
import com.simba.snip.npo.integration.nokia.NokiaFixtureAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalNormalizationEquivalenceTest {

    @Test
    void equivalentEricssonAndNokiaRecordsNormalizeToTheSameCanonicalFields() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        CanonicalNormalizer normalizer = new CanonicalNormalizer();
        SourceSnapshot ericsson = new EricssonFixtureAdapter(mapper).readSnapshot(FixtureKind.NORMAL);
        SourceSnapshot nokia = new NokiaFixtureAdapter(mapper).readSnapshot(FixtureKind.NORMAL);
        CanonicalSnapshot er = normalizer.normalize(ericsson).snapshot();
        CanonicalSnapshot nk = normalizer.normalize(nokia).snapshot();
        CanonicalCell erCell = er.cells().stream().filter(c -> "CELL-E001".equals(c.canonicalCellId())).findFirst().orElseThrow();
        CanonicalCell nkCell = nk.cells().stream().filter(c -> "CELL-N001".equals(c.canonicalCellId())).findFirst().orElseThrow();
        assertEquals("NR", erCell.technology());
        assertEquals("NR", nkCell.technology());
        assertEquals("TDD", erCell.duplexMode());
        assertEquals("TDD", nkCell.duplexMode());
        assertEquals("ACTIVE", erCell.status());
        assertEquals("ACTIVE", nkCell.status());
        CanonicalCellConfiguration erTx = er.configurations().stream()
                .filter(c -> "CELL-E001".equals(c.canonicalCellId())).findFirst().orElseThrow();
        CanonicalCellConfiguration nkTx = nk.configurations().stream()
                .filter(c -> "CELL-N001".equals(c.canonicalCellId())).findFirst().orElseThrow();
        assertEquals(46.0d, erTx.txPowerDbm());
        assertEquals(46.0d, nkTx.txPowerDbm());
        assertEquals("dBm", erTx.unit());
        assertEquals("dBm", nkTx.unit());
        assertTrue(normalizer.normalize(ericsson).issues().isEmpty());
        assertTrue(normalizer.normalize(nokia).issues().isEmpty());
    }
}
