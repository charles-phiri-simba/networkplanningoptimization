package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeFailureTaxonomyTest {

    @Test
    void specificReasonCodesNoCatchRetry() throws IOException {
        Set<String> codes = Arrays.stream(ProductionReasonCode.values()).map(Enum::name).collect(Collectors.toSet());
        assertTrue(codes.contains("PRODUCTION_VENDOR_STATE_MISMATCH"));
        assertTrue(codes.contains("PRODUCTION_OUTCOME_UNKNOWN"));
        assertTrue(codes.contains("PRODUCTION_GRANT_ALREADY_CONSUMED"));
        String orchestrator = Files.readString(ProductionChangeSourcePaths.gatewayMainJava().resolve(
                "com/simba/snip/npo/productionwritegateway/service/GatewayExecutionOrchestrator.java"));
        assertFalse(orchestrator.toLowerCase(Locale.ROOT).contains("catch (exception")
                && orchestrator.toLowerCase(Locale.ROOT).contains("retry"));
        assertFalse(orchestrator.contains("for (int retry"));
        String client = Files.readString(ProductionChangeSourcePaths.appMainJava().resolve(
                "com/simba/snip/npo/productionchange/adapter/ProductionWriteGatewayClient.java"));
        assertFalse(client.contains("RetryTemplate"));
    }
}
