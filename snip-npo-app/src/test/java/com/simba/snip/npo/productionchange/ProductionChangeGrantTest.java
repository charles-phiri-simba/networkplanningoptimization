package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.protocol.GrantStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionChangeGrantTest {

    @Test
    void grantStatusesComplete() {
        assertEquals(Set.of("ISSUED", "CONSUMED", "EXPIRED", "REVOKED"),
                Arrays.stream(GrantStatus.values()).map(Enum::name).collect(Collectors.toSet()));
    }
}
