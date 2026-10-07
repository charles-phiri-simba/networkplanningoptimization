package com.simba.snip.npo.demo;

import com.simba.snip.npo.changeexecution.config.ChangeExecutionProperties;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoSafetyGuardTest {

    @Test
    void productionChangeEnabledFailsClosed() {
        ProductionChangeProperties production = new ProductionChangeProperties();
        production.setEnabled(true);
        ChangeExecutionProperties execution = new ChangeExecutionProperties();
        execution.setEnabled(true);
        SnipProperties snip = new SnipProperties();
        snip.setKafkaEnabled(false);
        DemoSafetyGuard guard = new DemoSafetyGuard(production, execution, snip);
        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::assertSafe);
        assertTrue(ex.getMessage().contains("production-change"));
    }

    @Test
    void kafkaEnabledFailsClosed() {
        ProductionChangeProperties production = new ProductionChangeProperties();
        ChangeExecutionProperties execution = new ChangeExecutionProperties();
        execution.setEnabled(true);
        SnipProperties snip = new SnipProperties();
        snip.setKafkaEnabled(true);
        DemoSafetyGuard guard = new DemoSafetyGuard(production, execution, snip);
        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::assertSafe);
        assertTrue(ex.getMessage().contains("Kafka"));
    }

    @Test
    void sandboxDisabledFailsClosed() {
        ProductionChangeProperties production = new ProductionChangeProperties();
        ChangeExecutionProperties execution = new ChangeExecutionProperties();
        execution.setEnabled(false);
        SnipProperties snip = new SnipProperties();
        DemoSafetyGuard guard = new DemoSafetyGuard(production, execution, snip);
        assertThrows(IllegalStateException.class, guard::assertSafe);
    }

    @Test
    void demoSafeDefaultsPass() {
        ProductionChangeProperties production = new ProductionChangeProperties();
        ChangeExecutionProperties execution = new ChangeExecutionProperties();
        execution.setEnabled(true);
        SnipProperties snip = new SnipProperties();
        snip.setKafkaEnabled(false);
        DemoSafetyGuard guard = new DemoSafetyGuard(production, execution, snip);
        assertDoesNotThrow(guard::assertSafe);
    }
}
