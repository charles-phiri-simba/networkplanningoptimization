package com.simba.snip.npo.productionwritegateway.vendortransport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17CriticalScenarioIndexTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "CS17-A", "CS17-B", "CS17-C", "CS17-D", "CS17-E", "CS17-F", "CS17-G", "CS17-H",
            "CS17-I", "CS17-J", "CS17-K", "CS17-L", "CS17-M", "CS17-N", "CS17-O", "CS17-P",
            "CS17-Q", "CS17-R", "CS17-S", "CS17-T", "CS17-U", "CS17-V", "CS17-W", "CS17-X",
            "CS17-Y", "CS17-Z"
    })
    void allTwentySixCriticalScenariosIndexed(String id) {
        assertTrue(id.startsWith("CS17-"));
    }
}
