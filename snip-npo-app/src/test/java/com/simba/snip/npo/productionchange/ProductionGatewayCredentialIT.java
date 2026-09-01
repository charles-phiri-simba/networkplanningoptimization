package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionwritegateway.security.TestOnlyProductionCredentialResolver;
import com.simba.snip.npo.productionwritegateway.service.ProductionCredentialResolutionService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionGatewayCredentialIT extends ProductionChangeITSupport {

    @Test
    void credentialAfterConsumeAndPreflight() {
        ProductionCredentialResolutionService service =
                GATEWAY_CTX.getBean(ProductionCredentialResolutionService.class);
        int before = service.getCredentialResolutionCount();
        service.resolveAfterPreflight("credential-profile-ref-l0", UUID.randomUUID(), UUID.randomUUID());
        assertTrue(service.getCredentialResolutionCount() > before);
        assertEquals(0, mutationCount());
    }

    @Test
    void credentialFailureZeroMutation() {
        TestOnlyProductionCredentialResolver resolver =
                GATEWAY_CTX.getBean(TestOnlyProductionCredentialResolver.class);
        resolver.failNext();
        ProductionCredentialResolutionService service =
                GATEWAY_CTX.getBean(ProductionCredentialResolutionService.class);
        assertThrows(Exception.class, () ->
                service.resolveAfterPreflight("credential-profile-ref-l0", UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(0, mutationCount());
        resolver.reset();
    }

    @Test
    void noOldVersionFallback() {
        TestOnlyProductionCredentialResolver resolver =
                GATEWAY_CTX.getBean(TestOnlyProductionCredentialResolver.class);
        assertThrows(Exception.class, () -> resolver.resolveVersion("credential-profile-ref-l0", "1"));
        assertEquals(0, mutationCount());
    }

    @Test
    void readCredentialCannotSubstituteWrite() {
        ProductionCredentialResolutionService service =
                GATEWAY_CTX.getBean(ProductionCredentialResolutionService.class);
        assertThrows(Exception.class, () ->
                service.resolveAfterPreflight("ericsson-enm-int-inventory-reader", UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(0, mutationCount());
    }
}
