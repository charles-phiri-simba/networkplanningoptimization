package com.simba.snip.npo.planning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanningBoundsTest {

    @Test
    void constantsMatchSpecification() {
        assertEquals(4, PlanningBounds.MAX_CELLS_PER_SCENARIO);
        assertEquals(4, PlanningBounds.MAX_ALTERNATIVES_PER_SCENARIO);
        assertEquals(4, PlanningBounds.MAX_INTENTS_PER_ALTERNATIVE);
        assertEquals(16, PlanningBounds.MAX_SIMULATION_RUNS_PER_EVALUATION);
        assertEquals(100, PlanningBounds.MAX_SCENARIO_LIST);
    }
}
