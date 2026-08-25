package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.integration.FixtureKind;
import com.simba.snip.npo.integration.NetworkImportService;
import com.simba.snip.npo.integration.Vendor;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(classes = NpoApplication.class)
@TestPropertySource(properties = {
        "snip.integration.lease-duration=5s",
        "snip.integration.heartbeat-interval=1s",
        "snip.integration.execution-timeout=200ms",
        "snip.integration.fixture-read-delay=1500ms"
})
class IntegrationRuntimeTimeoutTest extends AbstractPostgresIT {

    @Autowired
    private NetworkImportService importService;

    @Test
    void delayedImportTimesOutAndDoesNotCompleteLater() {
        NetworkImportBatchEntity timedOut = importService.importVendor(Vendor.ERICSSON, FixtureKind.TIMEOUT, true);
        assertEquals("TIMED_OUT", timedOut.getStatus());
        assertEquals("EXECUTION_TIMEOUT", timedOut.getFailureCode());
        assertEquals(Boolean.TRUE, timedOut.getRetryable());
        assertNotEquals("COMPLETED", timedOut.getStatus());
    }
}
