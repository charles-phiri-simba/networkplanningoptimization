package com.simba.snip.npo.productionchange.api;

import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ReviewDecision;
import com.simba.snip.npo.productionchange.security.ProductionChangeAuthorizer;
import com.simba.snip.npo.productionchange.service.ProductionChangeControlService;
import com.simba.snip.npo.productionchange.service.ProductionChangeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ProductionChangeController {

    private final ProductionChangeAuthorizer authorizer;
    private final ProductionChangeService productionChangeService;

    public ProductionChangeController(
            ProductionChangeAuthorizer authorizer,
            ProductionChangeService productionChangeService
    ) {
        this.authorizer = authorizer;
        this.productionChangeService = productionChangeService;
    }

    @PostMapping(value = "/api/v1/production-changes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductionChangeDto create(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @RequestBody CreateProductionChangeRequest request
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireRequest();
        ActorPrincipal actor = authorizer.requireActor();
        ChangeControlReferenceDto cc = request.changeControlReference();
        return ProductionChangeMapper.toDto(productionChangeService.create(
                request.phase15ExecutionId(),
                request.productionTargetId(),
                cc == null ? null : new ProductionChangeControlService.ChangeControlReference(
                        cc.system(),
                        cc.reference(),
                        cc.status(),
                        cc.validatedByPrincipalId(),
                        cc.validatedAt(),
                        cc.validUntil()
                ),
                actor
        ));
    }

    @GetMapping("/api/v1/production-changes")
    public List<ProductionChangeDto> list(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireView();
        return productionChangeService.list().stream().map(ProductionChangeMapper::toDto).toList();
    }

    @GetMapping("/api/v1/production-changes/{id}")
    public ProductionChangeDto get(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") UUID id
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireViewOrEvidence();
        return ProductionChangeMapper.toDto(productionChangeService.require(id));
    }

    @GetMapping("/api/v1/production-changes/{id}/evidence")
    public Map<String, Object> evidence(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") UUID id
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireViewOrEvidence();
        return productionChangeService.evidence(id);
    }

    @PostMapping(value = "/api/v1/production-changes/{id}/review", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductionChangeDto review(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") UUID id,
            @RequestBody ReviewProductionChangeRequest request
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireReview();
        return ProductionChangeMapper.toDto(productionChangeService.review(
                id,
                ReviewDecision.valueOf(request.decision() == null ? "APPROVED" : request.decision()),
                request.reasonCodes(),
                authorizer.requireActor()
        ));
    }

    @PostMapping(value = "/api/v1/production-changes/{id}/authorize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductionChangeDto authorize(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) AuthorizeProductionChangeRequest request
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireAuthorize();
        return ProductionChangeMapper.toDto(productionChangeService.authorize(id, authorizer.requireActor()));
    }

    @PostMapping(value = "/api/v1/production-changes/{id}/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductionChangeDto execute(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ExecuteProductionChangeRequest request
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireExecute();
        return ProductionChangeMapper.toDto(productionChangeService.execute(id, authorizer.requireActor()));
    }

    @PostMapping("/api/v1/production-changes/{id}/rollback/request")
    public ProductionChangeDto rollbackRequest(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") UUID id
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireRollbackRequest();
        return ProductionChangeMapper.toDto(productionChangeService.requestRollback(id, authorizer.requireActor()));
    }

    @PostMapping(value = "/api/v1/production-changes/{id}/rollback/review", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductionChangeDto rollbackReview(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") UUID id,
            @RequestBody ReviewProductionChangeRequest request
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireRollbackReview();
        return ProductionChangeMapper.toDto(productionChangeService.reviewRollback(
                id,
                ReviewDecision.valueOf(request.decision() == null ? "APPROVED" : request.decision()),
                authorizer.requireActor()
        ));
    }

    @PostMapping("/api/v1/production-changes/{id}/rollback/authorize")
    public ProductionChangeDto rollbackAuthorize(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") UUID id
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireRollbackAuthorize();
        return ProductionChangeMapper.toDto(productionChangeService.authorizeRollback(id, authorizer.requireActor()));
    }

    @PostMapping("/api/v1/production-changes/{id}/rollback/execute")
    public ProductionChangeDto rollbackExecute(
            @RequestHeader(value = ProductionChangeAuthorizer.HEADER, required = false) String permission,
            @RequestHeader(value = ProductionChangeAuthorizer.ACTOR_HEADER, required = false) String actorPrincipalId,
            @PathVariable("id") UUID id
    ) {
        authorizer.bindRequest(permission, actorPrincipalId);
        authorizer.requireRollbackExecute();
        return ProductionChangeMapper.toDto(productionChangeService.executeRollback(id, authorizer.requireActor()));
    }
}
