package com.simba.snip.npo.vendorcertification.api;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.vendorcertification.domain.Phase17CertificationPermission;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import com.simba.snip.npo.vendorcertification.service.VendorInterfaceDefinitionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class VendorInterfaceController {

    private final Phase17SeparationOfDutiesPolicy sod;
    private final VendorInterfaceDefinitionService interfaces;

    public VendorInterfaceController(
            Phase17SeparationOfDutiesPolicy sod,
            VendorInterfaceDefinitionService interfaces
    ) {
        this.sod = sod;
        this.interfaces = interfaces;
    }

    @PostMapping(value = "/api/v1/vendor-interfaces", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> create(
            @RequestHeader(value = VendorCertificationController.ACTOR_HEADER, required = false) String actor,
            @RequestBody CreateInterfaceRequest request
    ) {
        sod.denyAgentOrMcp(actor);
        if (request.vendorCommand() != null || request.protocolPayload() != null || request.credentialValue() != null) {
            throw new Phase17Exception(Phase17DenialCode.P17_INTERFACE_UNRESOLVED, "raw vendor fields forbidden");
        }
        UUID id = interfaces.createDraft(actor, request.contentDigest(), request.documentationReference(),
                request.documentationVersion());
        return Map.of("id", id.toString(), "status", "DRAFT");
    }

    @GetMapping("/api/v1/vendor-interfaces/{id}")
    public Map<String, Object> get(
            @RequestHeader(value = VendorCertificationController.PERMISSION_HEADER, required = false) String permission,
            @PathVariable("id") UUID id
    ) {
        if (!Phase17CertificationPermission.VIEW_CERTIFICATION_STATUS.equals(permission)) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "VIEW_CERTIFICATION_STATUS required");
        }
        return Map.of("id", id.toString(), "executable", false, "level4", false);
    }

    public record CreateInterfaceRequest(
            String contentDigest,
            String documentationReference,
            String documentationVersion,
            String vendorCommand,
            String protocolPayload,
            String credentialValue
    ) {
    }
}
