package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class ProductionGatewayEgressAllowlistTest {

    @Test
    void appNetworkPolicyNoVendorEgress() throws IOException {
        new ProductionChangeInfraValidationTest().appNetworkPolicyNoVendorEgress();
        new ProductionChangeInfraValidationTest().gatewayNetworkPolicyRestrictedEgress();
    }
}
