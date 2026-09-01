package com.simba.snip.npo.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.integration.ericsson.EricssonFixtureAdapter;
import com.simba.snip.npo.integration.nokia.NokiaFixtureAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkSourceAdapterRegistryTest {

    private static ObjectMapper mapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void registryMapsEricssonAndNokiaAndRejectsUnknown() {
        EricssonFixtureAdapter ericsson = new EricssonFixtureAdapter(mapper());
        NokiaFixtureAdapter nokia = new NokiaFixtureAdapter(mapper());
        NetworkSourceAdapterRegistry registry = new NetworkSourceAdapterRegistry(List.of(ericsson, nokia));
        assertEquals(ericsson, registry.require(Vendor.ERICSSON));
        assertEquals(nokia, registry.require(Vendor.NOKIA));
        assertEquals("ERICSSON_FIXTURE", ericsson.sourceSystem());
        assertEquals("ERICSSON_FIXTURE_V1", ericsson.schemaVersion());
        assertEquals("NOKIA_FIXTURE", nokia.sourceSystem());
        assertEquals("NOKIA_FIXTURE_V1", nokia.schemaVersion());
    }

    @Test
    void adaptersReadMateriallyDifferentNormalSnapshots() {
        EricssonFixtureAdapter ericsson = new EricssonFixtureAdapter(mapper());
        NokiaFixtureAdapter nokia = new NokiaFixtureAdapter(mapper());
        SourceSnapshot er = ericsson.readSnapshot(FixtureKind.NORMAL);
        SourceSnapshot nk = nokia.readSnapshot(FixtureKind.NORMAL);
        assertEquals("er-snap-normal-001", er.sourceSnapshotId());
        assertEquals("nk-snap-normal-001", nk.sourceSnapshotId());
        assertEquals(PowerUnit.TENTHS_DBM, er.configurations().get(0).sourceUnit());
        assertEquals(PowerUnit.DBM, nk.configurations().get(0).sourceUnit());
        assertEquals(460.0d, er.configurations().get(0).sourceValue());
        assertEquals(46.0d, nk.configurations().get(0).sourceValue());
        assertTrue(er.cells().stream().anyMatch(c -> "CELL-E001".equals(c.canonicalCellId())));
        assertTrue(nk.cells().stream().anyMatch(c -> "CELL-N001".equals(c.canonicalCellId())));
        assertThrows(DomainValidationException.class, () -> nokia.readSnapshot(FixtureKind.UPDATE));
    }
}
