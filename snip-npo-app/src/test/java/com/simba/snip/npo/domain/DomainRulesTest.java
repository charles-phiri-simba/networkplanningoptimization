package com.simba.snip.npo.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainRulesTest {

    @Test
    void domainIdMustBePresentAndBounded() {
        assertEquals("CELL-001", DomainRules.requireDomainId(" CELL-001 ", "cellId"));
        assertThrows(DomainValidationException.class, () -> DomainRules.requireDomainId(" ", "cellId"));
        assertThrows(DomainValidationException.class, () -> DomainRules.requireDomainId("x".repeat(65), "cellId"));
    }

    @Test
    void neighbourSourceCannotEqualTarget() {
        DomainRules.requireDistinctCells("CELL-001", "CELL-002");
        assertThrows(DomainValidationException.class, () -> DomainRules.requireDistinctCells("CELL-001", "CELL-001"));
    }
}
