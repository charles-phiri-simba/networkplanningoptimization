package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeGatewayAuthIT extends ProductionChangeITSupport {

    @Test
    void selfContainedGrantRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-SNIP-GATEWAY-CALLER-ID", "snip-npo-app");
        headers.setBearerAuth("not-a-durable-grant");
        String body;
        try {
            body = new RestTemplate().postForObject(
                    gatewayBaseUrl() + "/internal/v1/gateway/execute",
                    new HttpEntity<>(Map.of(
                            "grantId", UUID.randomUUID().toString(),
                            "productionChangeId", UUID.randomUUID().toString(),
                            "correlationId", "jwt-only"), headers),
                    String.class);
        } catch (HttpStatusCodeException ex) {
            body = ex.getResponseBodyAsString();
        }
        assertTrue(body.contains(ProductionReasonCode.PRODUCTION_GRANT_NOT_FOUND.name())
                || body.contains(ProductionReasonCode.PRODUCTION_GRANT_MISSING.name())
                || body.contains("GRANT"));
        assertEquals(0, mutationCount());
    }

    @Test
    void requestRejectsMutationPayloadAuthority() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-SNIP-GATEWAY-CALLER-ID", "snip-npo-app");
        String body;
        try {
            body = new RestTemplate().postForObject(
                    gatewayBaseUrl() + "/internal/v1/gateway/execute",
                    new HttpEntity<>(Map.of(
                            "grantId", UUID.randomUUID().toString(),
                            "productionChangeId", UUID.randomUUID().toString(),
                            "correlationId", "x",
                            "desiredValue", "99",
                            "endpoint", "https://enm.example"), headers),
                    String.class);
        } catch (HttpStatusCodeException ex) {
            body = ex.getResponseBodyAsString();
        }
        assertTrue(body.contains(ProductionReasonCode.PRODUCTION_INVALID_REQUEST.name())
                || body.contains("GRANT"));
        assertEquals(0, mutationCount());
    }
}
