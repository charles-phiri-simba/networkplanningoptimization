package com.simba.snip.npo.integration;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalValidatorTest {

    @Test
    void missingParentAndSelfNeighbourAreRejected() {
        SourceSnapshot source = new SourceSnapshot(
                "snap",
                "ERICSSON_FIXTURE",
                Vendor.ERICSSON,
                "ERICSSON_FIXTURE_V1",
                Instant.parse("2026-08-25T10:00:00Z"),
                true,
                List.of(),
                List.of(),
                List.of(new SourceCell(
                        "CELLMO", "dn", "CELL-X", "GNB-MISSING", "x", "NR", "n78",
                        1, 1, 40, "TDD", "UNLOCKED")),
                List.of(new SourceConfiguration("cfg", "dn", "CELL-X", "txPower", 460.0d, PowerUnit.TENTHS_DBM)),
                List.of(new SourceNeighbour("rel", "dn", "CELL-X", "CELL-X", "INTRA_FREQUENCY", "UNLOCKED"))
        );
        CanonicalNormalizer.NormalizeResult normalized = new CanonicalNormalizer().normalize(source);
        List<ValidationIssue> issues = new CanonicalValidator().validateAndFilter(normalized.snapshot(), normalized.issues());
        assertTrue(issues.stream().anyMatch(i -> i.reasonCode() == RejectionReasonCode.MISSING_PARENT));
        assertTrue(issues.stream().anyMatch(i -> i.reasonCode() == RejectionReasonCode.MALFORMED_RELATIONSHIP));
        assertTrue(normalized.snapshot().cells().isEmpty());
        assertTrue(normalized.snapshot().neighbours().isEmpty());
    }
}
