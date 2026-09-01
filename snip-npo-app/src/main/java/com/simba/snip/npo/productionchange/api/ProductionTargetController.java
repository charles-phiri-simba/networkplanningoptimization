package com.simba.snip.npo.productionchange.api;

import com.simba.snip.npo.productionchange.security.ProductionChangeAuthorizer;
import com.simba.snip.npo.productionchange.service.ProductionTargetAdministrationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ProductionTargetController {

    private final ProductionChangeAuthorizer authorizer;
    private final ProductionTargetAdministrationService administrationService;

    public ProductionTargetController(
            ProductionChangeAuthorizer authorizer,
            ProductionTargetAdministrationService administrationService
    ) {
        this.authorizer = authorizer;
        this.administrationService = administrationService;
    }

    @PostMapping("/api/v1/production-targets/{id}/suspend")
    public Map<String, String> suspend(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") String id
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireAdministerTarget();
        var target = administrationService.suspend(id, authorizer.requireActor());
        Map<String, String> body = new LinkedHashMap<>();
        body.put("targetId", target.getTargetId());
        body.put("targetState", target.getTargetState());
        return body;
    }

    @PostMapping("/api/v1/production-targets/{id}/resume")
    public Map<String, String> resume(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") String id
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireAdministerTarget();
        var target = administrationService.resume(id, authorizer.requireActor());
        Map<String, String> body = new LinkedHashMap<>();
        body.put("targetId", target.getTargetId());
        body.put("targetState", target.getTargetState());
        return body;
    }
}
