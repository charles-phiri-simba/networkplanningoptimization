package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeExpectedStateIT extends ProductionChangeITSupport {

    @Test
    void observationBeforeMutationOrdering() throws IOException {
        String orchestrator = Files.readString(ProductionChangeSourcePaths.gatewayMainJava().resolve(
                "com/simba/snip/npo/productionwritegateway/service/GatewayExecutionOrchestrator.java"));
        int observe = orchestrator.indexOf("observeExpected");
        int mutate = orchestrator.indexOf("invokeMutation");
        assertTrue(observe >= 0 && mutate > observe);
        ProductionChangeDto authorized = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_MISMATCH);
        executeProductionChange(authorized.productionChangeId());
        assertEquals(0, mutationCount());
    }
}
