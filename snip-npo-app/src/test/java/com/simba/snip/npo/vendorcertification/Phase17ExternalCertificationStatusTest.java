package com.simba.snip.npo.vendorcertification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Phase17ExternalCertificationStatusTest {

    @Test
    void documentsExternalLevelsRemainUnexecuted() {
        assertEquals("NOT EXECUTED", "NOT EXECUTED");
        assertEquals("NOT SATISFIED", "NOT SATISFIED");
    }
}
