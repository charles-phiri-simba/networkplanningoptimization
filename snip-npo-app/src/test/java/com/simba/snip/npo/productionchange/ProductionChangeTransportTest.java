package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeTransportTest {

    @Test
    void mutationHttpRetryDisabled() throws IOException {
        String gateway = Files.readString(ProductionChangeSourcePaths.gatewayMainJava().resolve(
                "com/simba/snip/npo/productionwritegateway/config/GatewayTransportConfig.java"));
        assertFalse(gateway.toLowerCase(Locale.ROOT).contains("retry"));
        assertFalse(gateway.contains("RestClient"), "gateway must not retain unused mutation RestClient");
        String client = Files.readString(ProductionChangeSourcePaths.appMainJava().resolve(
                "com/simba/snip/npo/productionchange/adapter/ProductionWriteGatewayClient.java"));
        assertTrue(client.contains("retries are disabled") || client.toLowerCase(Locale.ROOT).contains("retry"));
        assertFalse(client.contains("RetryInterceptor"));
    }
}
