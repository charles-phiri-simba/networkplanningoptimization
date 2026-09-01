package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionwritegateway.transport.ControlledTestEricssonWriteTransport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionChangeMutationCounterIT extends ProductionChangeITSupport {

    @Test
    void assertExactMutationCounts() {
        ProductionChangeDto deny = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.OBSERVE_MISMATCH);
        http.exchange(
                "/api/v1/production-changes/" + deny.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(0, mutationCount());

        testTransport().reset();
        ProductionChangeDto happy = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(happy);
        executeExpectingOk(happy.productionChangeId());
        assertEquals(1, mutationCount());

        testTransport().reset();
        ProductionChangeDto ambiguous = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(ambiguous);
        testTransport().setFailureMode(ControlledTestEricssonWriteTransport.FailureMode.RESPONSE_LOST);
        http.exchange(
                "/api/v1/production-changes/" + ambiguous.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(1, mutationCount());
    }
}
