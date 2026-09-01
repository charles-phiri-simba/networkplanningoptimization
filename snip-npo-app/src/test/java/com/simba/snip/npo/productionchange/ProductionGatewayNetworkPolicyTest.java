package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class ProductionGatewayNetworkPolicyTest {

    @Test
    void gatewayNetworkPolicyRestrictedEgress() throws IOException {
        new ProductionChangeInfraValidationTest().gatewayNetworkPolicyRestrictedEgress();
        new ProductionChangeInfraValidationTest().appNetworkPolicyNoVendorEgress();
    }
}
