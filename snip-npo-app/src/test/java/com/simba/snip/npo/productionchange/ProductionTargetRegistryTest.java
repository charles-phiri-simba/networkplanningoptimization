package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.domain.CertificationLevel;
import com.simba.snip.npo.productionchange.domain.ExpectedStateGuardStrength;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.domain.ProductionTargetState;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.service.ProductionTargetRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionTargetRegistryTest extends ProductionChangeITSupport {

    @Test
    void nokiaRejected() {
        targetRegistry.register(new ProductionTargetRegistry.TargetRegistration(
                "NOKIA-NETACT-L0",
                "NOKIA",
                "NETACT",
                "LAB",
                "test",
                "RAN",
                "nokia-write",
                "1",
                "security-l0",
                "credential-ref",
                "CELL",
                "txPower",
                "MANUAL",
                "p16-rollback-v1",
                "p16-verification-v1",
                CertificationLevel.L0,
                true,
                ProductionTargetState.ACTIVE,
                ExpectedStateGuardStrength.READ_THEN_WRITE
        ));
        UUID phase15 = verifiedPhase15ExecutionId();
        Map<String, Object> body = createRequestBody(phase15, "NOKIA-NETACT-L0", PRINCIPAL_CC_VALIDATOR,
                Instant.now().plus(2, ChronoUnit.HOURS));
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/production-changes",
                HttpMethod.POST,
                productionEntity(body, ProductionChangePermission.REQUEST_PRODUCTION_CHANGE, PRINCIPAL_REQUESTER),
                String.class);
        assertTrue(denied.getStatusCode().is4xxClientError());
        assertTrue(denied.getBody().contains(ProductionReasonCode.PRODUCTION_VENDOR_UNSUPPORTED.name()));
        assertEqualsZeroMutations();
    }

    private void assertEqualsZeroMutations() {
        org.junit.jupiter.api.Assertions.assertEquals(0, mutationCount());
    }
}
