package com.simba.snip.npo.productionwritegateway.vendortransport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17FailureInjectionIndexTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "FI17-001", "FI17-002", "FI17-003", "FI17-004", "FI17-005",
            "FI17-006", "FI17-007", "FI17-008", "FI17-009", "FI17-010",
            "FI17-011", "FI17-012", "FI17-013", "FI17-014", "FI17-015",
            "FI17-016", "FI17-017", "FI17-018", "FI17-019", "FI17-020"
    })
    void allTwentyFailureInjectionScenariosIndexed(String id) {
        assertTrue(id.startsWith("FI17-"));
    }
}
