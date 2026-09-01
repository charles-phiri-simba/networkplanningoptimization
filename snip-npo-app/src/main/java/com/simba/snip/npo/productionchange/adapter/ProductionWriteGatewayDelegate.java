package com.simba.snip.npo.productionchange.adapter;

import com.simba.snip.npo.productionchange.protocol.GatewayExecuteRequest;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteResponse;

/**
 * Optional in-process gateway for tests. Production runtime uses HTTP.
 * Implementations MUST NOT live in the write-gateway package scanned by the app.
 */
public interface ProductionWriteGatewayDelegate {

    GatewayExecuteResponse execute(GatewayExecuteRequest request);
}
