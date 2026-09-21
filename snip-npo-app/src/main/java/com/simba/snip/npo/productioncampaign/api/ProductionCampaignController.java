package com.simba.snip.npo.productioncampaign.api;

import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.entity.ProductionChangeCampaignEntity;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.security.HeaderAuthenticatedActorProvider;
import com.simba.snip.npo.productioncampaign.service.CampaignRecoveryService;
import com.simba.snip.npo.productioncampaign.service.CampaignResumptionService;
import com.simba.snip.npo.productioncampaign.service.ProductionChangeCampaignService;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ProductionCampaignController {

    private final HeaderAuthenticatedActorProvider actorProvider;
    private final ProductionChangeCampaignService campaignService;
    private final CampaignRecoveryService recoveryService;
    private final CampaignResumptionService resumptionService;

    public ProductionCampaignController(
            HeaderAuthenticatedActorProvider actorProvider,
            ProductionChangeCampaignService campaignService,
            CampaignRecoveryService recoveryService,
            CampaignResumptionService resumptionService
    ) {
        this.actorProvider = actorProvider;
        this.campaignService = campaignService;
        this.recoveryService = recoveryService;
        this.resumptionService = resumptionService;
    }

    @PostMapping(value = "/api/v1/production-campaigns", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductionCampaignDto create(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateProductionCampaignRequest request
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        var actor = actorProvider.requireHuman(CampaignPermission.CAMPAIGN_CREATE);
        if (request.getItems() == null) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE, "items required");
        }
        List<ProductionChangeCampaignService.PlannedItem> items = request.getItems().stream()
                .map(item -> new ProductionChangeCampaignService.PlannedItem(
                        item.getPhase14PlanId(),
                        item.getPhase14PlanFingerprint(),
                        item.getPhase15ExecutionId(),
                        item.getPhase15ExecutionFingerprint(),
                        item.getCellId(),
                        item.getExpectedValue(),
                        item.getDesiredValue(),
                        item.getRollbackValue(),
                        item.getUnit(),
                        item.getObjectType(),
                        item.getParameter()
                ))
                .toList();
        ProductionChangeCampaignEntity created = campaignService.create(
                request.getProductionTargetId(),
                request.getObjective(),
                request.getChangeControlReference(),
                items,
                actor,
                idempotencyKey
        );
        return ProductionCampaignMapper.toDto(created);
    }

    @GetMapping("/api/v1/production-campaigns/{id}")
    public ProductionCampaignDto get(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        actorProvider.requireHuman(CampaignPermission.CAMPAIGN_VIEW);
        return ProductionCampaignMapper.toDto(campaignService.require(id));
    }

    @PostMapping(value = "/api/v1/production-campaigns/{id}/commands", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductionCampaignDto command(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable("id") UUID id,
            @RequestBody CampaignCommandRequest request
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        CampaignPermission required = permissionFor(request.getTrigger());
        var actor = actorProvider.requireHuman(required);
        return ProductionCampaignMapper.toDto(campaignService.transition(id, request.getTrigger(), actor, idempotencyKey));
    }

    @PostMapping("/api/v1/production-campaigns/{id}/recovery/request")
    public void recoveryRequest(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        recoveryService.request(id, actorProvider.requireHuman(CampaignPermission.CAMPAIGN_RECOVERY_REQUEST));
    }

    @PostMapping("/api/v1/production-campaigns/{id}/recovery/review")
    public void recoveryReview(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        recoveryService.startReview(id, actorProvider.requireHuman(CampaignPermission.CAMPAIGN_RECOVERY_REVIEW));
    }

    @PostMapping("/api/v1/production-campaigns/{id}/recovery/authorize")
    public void recoveryAuthorize(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        recoveryService.authorize(id, actorProvider.requireHuman(CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE));
    }

    @PostMapping("/api/v1/production-campaigns/{id}/resumption/request")
    public void resumptionRequest(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        CampaignPermission required = CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW;
        if (CampaignPermission.CAMPAIGN_PAUSE.name().equals(permission)) {
            required = CampaignPermission.CAMPAIGN_PAUSE;
        }
        resumptionService.request(id, actorProvider.requireHuman(required));
    }

    @PostMapping("/api/v1/production-campaigns/{id}/resumption/review")
    public void resumptionReview(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        resumptionService.startReview(id, actorProvider.requireHuman(CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
    }

    @PostMapping("/api/v1/production-campaigns/{id}/resumption/approve")
    public void resumptionApprove(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        resumptionService.approveReview(id, actorProvider.requireHuman(CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
    }

    @PostMapping("/api/v1/production-campaigns/{id}/resumption/reject")
    public void resumptionReject(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        resumptionService.rejectReview(id, actorProvider.requireHuman(CampaignPermission.CAMPAIGN_RESUMPTION_REVIEW));
    }

    @PostMapping("/api/v1/production-campaigns/{id}/resumption/authorize")
    public void resumptionAuthorize(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        resumptionService.authorize(id, actorProvider.requireHuman(CampaignPermission.CAMPAIGN_RESUMPTION_AUTHORIZE));
    }

    @PostMapping("/api/v1/production-campaigns/{id}/resumption/effective")
    public String resumptionEffective(
            @RequestHeader(value = HeaderAuthenticatedActorProvider.PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_HEADER, required = false) String actorId,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.SOURCE_HEADER, required = false) String source,
            @RequestHeader(value = HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, required = false) String actorType,
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ResumptionEffectiveRequest request
    ) {
        actorProvider.bindRequest(permission, actorId, source, actorType);
        throw new CampaignException(
                ProductionReasonCode.SYSTEM_TRANSITION_DENIED,
                "MAKE_EFFECTIVE is a SYSTEM transition and cannot be invoked over HTTP"
        );
    }

    private static CampaignPermission permissionFor(String trigger) {
        if (trigger == null || trigger.isBlank()) {
            throw new CampaignException(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, "trigger is required");
        }
        return switch (trigger) {
            case "SUBMIT_FOR_REVIEW" -> CampaignPermission.CAMPAIGN_CREATE;
            case "REVIEW_APPROVE", "REVIEW_RETURN" -> CampaignPermission.CAMPAIGN_REVIEW;
            case "AUTHORIZE_CAMPAIGN" -> CampaignPermission.CAMPAIGN_AUTHORIZE;
            case "RELEASE_CANARY", "RELEASE_STANDARD_COHORT", "RELEASE_COHORT" -> CampaignPermission.RELEASE_COHORT;
            case "PAUSE_CAMPAIGN", "OPERATOR_PAUSE" -> CampaignPermission.CAMPAIGN_PAUSE;
            case "ABORT_CAMPAIGN" -> CampaignPermission.CAMPAIGN_ABORT;
            case "EVALUATE_READY", "CANARY_ITEMS_ENTER_OBSERVING", "CANARY_HEALTH_AND_SAFETY_PASS",
                 "CANARY_TERMINALLY_ACCOUNTED", "COHORT_ITEMS_ENTER_OBSERVING", "COHORT_HEALTH_AND_SAFETY_PASS",
                 "MORE_AUTHORIZED_COHORTS_REMAIN", "LAST_AUTHORIZED_COHORT_ACCOUNTED", "COMPLETE_CAMPAIGN",
                 "SAFETY_SUSPEND", "MATERIAL_INVALIDATION", "UNRESOLVED_MUTATION_OUTCOME",
                 "DETERMINED_RECOVERY_REQUIRED" -> throw new CampaignException(
                    ProductionReasonCode.SYSTEM_TRANSITION_DENIED,
                    "HTTP clients cannot invoke internal SYSTEM campaign transitions: " + trigger
            );
            default -> throw new CampaignException(
                    ProductionReasonCode.SYSTEM_TRANSITION_DENIED,
                    "unknown or internal campaign trigger is not externally invocable: " + trigger
            );
        };
    }
}
