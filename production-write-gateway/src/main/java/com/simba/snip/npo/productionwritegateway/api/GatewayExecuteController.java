package com.simba.snip.npo.productionwritegateway.api;

import com.simba.snip.npo.productionchange.protocol.GatewayExecuteRequest;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteResponse;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionwritegateway.security.GatewayCallerAuthenticator;
import com.simba.snip.npo.productionwritegateway.service.GatewayExecutionOrchestrator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/gateway")
public class GatewayExecuteController {

    private final GatewayExecutionOrchestrator orchestrator;

    public GatewayExecuteController(GatewayExecutionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping(path = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GatewayExecuteResponse execute(
            @RequestBody GatewayExecuteRequest request,
            @RequestHeader(value = GatewayCallerAuthenticator.CALLER_HEADER, required = false) String callerId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return orchestrator.execute(request, callerId, authorization, GrantType.FORWARD);
    }

    @PostMapping(path = "/rollback-execute", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GatewayExecuteResponse rollbackExecute(
            @RequestBody GatewayExecuteRequest request,
            @RequestHeader(value = GatewayCallerAuthenticator.CALLER_HEADER, required = false) String callerId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return orchestrator.execute(request, callerId, authorization, GrantType.ROLLBACK);
    }
}
