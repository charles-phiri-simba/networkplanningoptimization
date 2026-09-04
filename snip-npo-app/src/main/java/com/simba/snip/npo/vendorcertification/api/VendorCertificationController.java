package com.simba.snip.npo.vendorcertification.api;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.productionchange.protocol.TransportCertificationState;
import com.simba.snip.npo.vendorcertification.domain.Phase17CertificationPermission;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import com.simba.snip.npo.vendorcertification.service.TransportCertificationLifecycleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
public class VendorCertificationController {

    public static final String PERMISSION_HEADER = "X-SNIP-PHASE17-PERMISSION";
    public static final String ACTOR_HEADER = "X-SNIP-ACTOR-PRINCIPAL-ID";

    private final Phase17SeparationOfDutiesPolicy sod;
    private final TransportCertificationLifecycleService lifecycle;

    public VendorCertificationController(
            Phase17SeparationOfDutiesPolicy sod,
            TransportCertificationLifecycleService lifecycle
    ) {
        this.sod = sod;
        this.lifecycle = lifecycle;
    }

    @PostMapping(value = "/api/v1/transport-certifications/{id}/transition", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> transition(
            @RequestHeader(value = PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor,
            @PathVariable("id") UUID id,
            @RequestBody TransitionRequest request
    ) {
        sod.denyAgentOrMcp(actor);
        sod.requirePrincipal(actor, "actor");
        lifecycle.transition(
                id,
                request.from() == null ? null : TransportCertificationState.valueOf(request.from()),
                TransportCertificationState.valueOf(request.to()),
                actor,
                permission == null ? Set.of() : Set.of(permission.split(",")),
                request.securityCertifierPrincipalId()
        );
        return Map.of("status", "ACCEPTED");
    }

    @GetMapping("/api/v1/production-targets/{targetId}/readiness")
    public Map<String, Object> readiness(
            @RequestHeader(value = PERMISSION_HEADER, required = false) String permission,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor,
            @PathVariable("targetId") String targetId
    ) {
        if (permission != null && !permission.equals(Phase17CertificationPermission.VIEW_CERTIFICATION_STATUS)) {
            sod.requirePermission(permission, Phase17CertificationPermission.VIEW_CERTIFICATION_STATUS);
        }
        if (permission == null) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "VIEW default deny");
        }
        return Map.of("targetId", targetId, "level4", false, "executable", false);
    }

    @ExceptionHandler(Phase17Exception.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> deny(Phase17Exception ex) {
        return Map.of("code", ex.denialCode().name(), "message", ex.getMessage());
    }

    public record TransitionRequest(String from, String to, String securityCertifierPrincipalId) {
    }
}
