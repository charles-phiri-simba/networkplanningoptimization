package com.simba.snip.npo.productionchange.adapter;

import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteRequest;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteResponse;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * App→gateway client. HTTP retries are disabled. Request body is grantId, productionChangeId, correlationId only.
 */
@Service
public class ProductionWriteGatewayClient {

    private final ProductionChangeProperties properties;
    private final RestClient restClient;
    private final ObjectProvider<ProductionWriteGatewayDelegate> inProcessDelegate;

    public ProductionWriteGatewayClient(
            ProductionChangeProperties properties,
            RestClient productionWriteGatewayRestClient,
            ObjectProvider<ProductionWriteGatewayDelegate> inProcessDelegate
    ) {
        this.properties = properties;
        this.restClient = productionWriteGatewayRestClient;
        this.inProcessDelegate = inProcessDelegate;
    }

    public GatewayExecuteResponse execute(GatewayExecuteRequest request) {
        return invoke("/internal/v1/gateway/execute", request);
    }

    public GatewayExecuteResponse executeRollback(GatewayExecuteRequest request) {
        return invoke("/internal/v1/gateway/rollback-execute", request);
    }

    private GatewayExecuteResponse invoke(String path, GatewayExecuteRequest request) {
        ProductionWriteGatewayDelegate delegate = inProcessDelegate.getIfAvailable();
        if (delegate != null) {
            return delegate.execute(request);
        }
        if (!properties.gatewayUrlConfigured()) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_GATEWAY_UNAVAILABLE,
                    "gateway-base-url is missing; execute denied"
            );
        }
        String url = properties.getGatewayBaseUrl().strip().replaceAll("/$", "") + path;
        try {
            GatewayExecuteResponse response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-SNIP-GATEWAY-CALLER-ID", properties.getInstanceId())
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                    })
                    .body(GatewayExecuteResponse.class);
            if (response == null) {
                throw new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_GATEWAY_UNAVAILABLE,
                        "gateway returned an empty response"
                );
            }
            return response;
        } catch (ProductionChangeException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_GATEWAY_UNAVAILABLE,
                    "gateway call failed: " + ex.getStatusCode() + " " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_GATEWAY_UNAVAILABLE,
                    "gateway call failed",
                    ex
            );
        }
    }
}
