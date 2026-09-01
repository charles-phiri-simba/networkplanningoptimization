package com.simba.snip.npo.integration;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CanonicalSnapshotHasherTest {

    private final CanonicalSnapshotHasher hasher = new CanonicalSnapshotHasher();

    @Test
    void equivalentContentProducesTheSameHashAndIgnoresCaptureTime() {
        CanonicalSnapshot first = snapshot("SITE-A", 46.0d, Instant.parse("2026-08-25T10:00:00Z"));
        CanonicalSnapshot second = snapshot("SITE-A", 46.0d, Instant.parse("2026-08-25T11:00:00Z"));
        assertEquals(hasher.hash(first), hasher.hash(second));
        assertEquals(64, hasher.hash(first).length());
    }

    @Test
    void changedCanonicalContentChangesTheHash() {
        CanonicalSnapshot first = snapshot("SITE-A", 46.0d, Instant.parse("2026-08-25T10:00:00Z"));
        CanonicalSnapshot second = snapshot("SITE-A", 44.0d, Instant.parse("2026-08-25T10:00:00Z"));
        assertNotEquals(hasher.hash(first), hasher.hash(second));
    }

    private static CanonicalSnapshot snapshot(String siteId, double txPower, Instant capturedAt) {
        SourceSnapshot source = new SourceSnapshot(
                "snap-hash-001",
                "ERICSSON_FIXTURE",
                Vendor.ERICSSON,
                "ERICSSON_FIXTURE_V1",
                capturedAt,
                true,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        CanonicalSnapshot canonical = new CanonicalSnapshot(source);
        canonical.sites().add(new CanonicalSite("ME", "dn", siteId, "Site", -26.2d, 28.0d, "ACTIVE"));
        canonical.configurations().add(new CanonicalCellConfiguration(
                "cfg", "dn", "CELL-A", "txPower", txPower, "dBm"));
        return canonical;
    }
}
