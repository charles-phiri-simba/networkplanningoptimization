package com.simba.snip.npo.twin;

import com.simba.snip.npo.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatableParameterRegistryTest {

    @Test
    void txPowerIsEnabledAndBounded() {
        SimulatableParameterDefinition definition =
                SimulatableParameterRegistry.requireEnabled("txPower", TwinScopeType.CELL);
        assertEquals("dBm", definition.unit());
        assertEquals(new BigDecimal("20"), definition.minValue());
        assertEquals(new BigDecimal("50"), definition.maxValue());
        SimulatableParameterRegistry.requireInRange(definition, new BigDecimal("46"));
    }

    @Test
    void electricalTiltAndUnknownAndOutOfRangeAreRejected() {
        DomainValidationException tilt = assertThrows(DomainValidationException.class,
                () -> SimulatableParameterRegistry.requireEnabled("electricalTilt", TwinScopeType.CELL));
        assertTrue(tilt.getMessage().contains("unsupported parameter"));
        assertThrows(DomainValidationException.class,
                () -> SimulatableParameterRegistry.requireEnabled("pci", TwinScopeType.CELL));
        SimulatableParameterDefinition definition =
                SimulatableParameterRegistry.requireEnabled("txPower", TwinScopeType.CELL);
        assertThrows(DomainValidationException.class,
                () -> SimulatableParameterRegistry.requireInRange(definition, new BigDecimal("5")));
        assertThrows(DomainValidationException.class,
                () -> SimulatableParameterRegistry.requireInRange(definition, new BigDecimal("60")));
    }
}
