package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ProductionChangeMutationOutcomeIT extends ProductionChangeITSupport {

    @Test
    void outcomesRemainDistinct() {
        Set<String> names = Arrays.stream(MutationOutcome.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("NOT_SENT", "REJECTED", "VENDOR_ACCEPTED", "OUTCOME_UNKNOWN"), names);
        assertNotEquals(MutationOutcome.NOT_SENT, MutationOutcome.OUTCOME_UNKNOWN);
        assertNotEquals(MutationOutcome.VENDOR_ACCEPTED, MutationOutcome.OUTCOME_UNKNOWN);
        assertEquals(4, MutationOutcome.values().length);
        assertEquals(0, mutationCount());
    }
}
