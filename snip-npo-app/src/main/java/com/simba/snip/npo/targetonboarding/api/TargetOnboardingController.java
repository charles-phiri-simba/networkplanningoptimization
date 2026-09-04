package com.simba.snip.npo.targetonboarding.api;

import com.simba.snip.npo.targetonboarding.service.ProductionTargetOnboardingService;
import com.simba.snip.npo.vendorcertification.domain.Phase17CertificationPermission;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class TargetOnboardingController {

    public static final String ACTOR_HEADER = "X-SNIP-ACTOR-PRINCIPAL-ID";
    public static final String PERMISSION_HEADER = "X-SNIP-PHASE17-PERMISSION";

    private final Phase17SeparationOfDutiesPolicy sod;
    private final ProductionTargetOnboardingService onboarding;

    public TargetOnboardingController(
            Phase17SeparationOfDutiesPolicy sod,
            ProductionTargetOnboardingService onboarding
    ) {
        this.sod = sod;
        this.onboarding = onboarding;
    }

    @PostMapping(value = "/api/v1/target-onboardings", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> create(
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor,
            @RequestHeader(value = PERMISSION_HEADER, required = false) String permission,
            @RequestBody CreateRequest request
    ) {
        sod.denyAgentOrMcp(actor);
        sod.requirePrincipal(actor, "creator");
        sod.requirePermission(permission, Phase17CertificationPermission.TARGET_ONBOARD_CREATE);
        onboarding.denyStandingL4(request.certificationLevel());
        if (request.status() != null || request.health() != null || request.endpointOverride() != null
                || request.credentialValue() != null || request.vendorPayload() != null
                || "APPROVED".equals(request.certificationLevel())
                || "PRODUCTION_REGISTERED".equals(request.certificationLevel())
                || "L4".equals(request.certificationLevel())) {
            throw new Phase17Exception(
                    com.simba.snip.npo.productionchange.protocol.Phase17DenialCode.P17_ENDPOINT_IDENTITY_MISMATCH,
                    "execution-time override forbidden");
        }
        return onboarding.create(new ProductionTargetOnboardingService.CreateCommand(
                actor,
                request.productionTargetId(),
                request.certificationLevel(),
                request.interfaceDefinitionVersionId(),
                request.transportProfileVersionId(),
                request.artifactDigest(),
                request.capabilityCertVersionId(),
                request.securityCertVersionId(),
                request.credentialProfileVersionId(),
                request.tlsProfileVersionId(),
                request.networkPolicyProfileVersionId(),
                request.endpointProfileVersionId(),
                request.bundleVersionId(),
                request.vendorSoftwareVersion(),
                request.environment()
        ));
    }

    @PostMapping("/api/v1/target-onboardings/{id}/review")
    public Map<String, String> review(
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor,
            @RequestHeader(value = PERMISSION_HEADER, required = false) String permission,
            @PathVariable("id") UUID id
    ) {
        sod.denyAgentOrMcp(actor);
        sod.requirePermission(permission, Phase17CertificationPermission.TARGET_ONBOARD_REVIEW);
        sod.requirePrincipal(actor, "reviewer");
        return onboarding.review(id, actor);
    }

    @PostMapping("/api/v1/target-onboardings/{id}/approve")
    public Map<String, String> approve(
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor,
            @RequestHeader(value = PERMISSION_HEADER, required = false) String permission,
            @PathVariable("id") UUID id
    ) {
        sod.denyAgentOrMcp(actor);
        sod.requirePermission(permission, Phase17CertificationPermission.TARGET_ONBOARD_APPROVE);
        sod.requirePrincipal(actor, "approver");
        return onboarding.approve(id, actor);
    }

    @ExceptionHandler(Phase17Exception.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> deny(Phase17Exception ex) {
        return Map.of("code", ex.denialCode().name(), "message", ex.getMessage());
    }

    public record CreateRequest(
            String productionTargetId,
            String certificationLevel,
            String status,
            String health,
            String endpointOverride,
            String credentialValue,
            String vendorPayload,
            UUID interfaceDefinitionVersionId,
            UUID transportProfileVersionId,
            String artifactDigest,
            UUID capabilityCertVersionId,
            UUID securityCertVersionId,
            UUID credentialProfileVersionId,
            UUID tlsProfileVersionId,
            UUID networkPolicyProfileVersionId,
            UUID endpointProfileVersionId,
            UUID bundleVersionId,
            String vendorSoftwareVersion,
            String environment
    ) {
    }
}
