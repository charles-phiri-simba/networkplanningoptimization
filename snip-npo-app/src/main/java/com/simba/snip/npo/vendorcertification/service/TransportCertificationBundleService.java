package com.simba.snip.npo.vendorcertification.service;

import com.simba.snip.npo.productionchange.protocol.Phase17BundleDigest;
import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.vendorcertification.audit.Phase17CertificationAuditService;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import org.springframework.stereotype.Service;

@Service
public class TransportCertificationBundleService {

    private final Phase17SeparationOfDutiesPolicy sod;
    private final Phase17CertificationAuditService audit;

    public TransportCertificationBundleService(
            Phase17SeparationOfDutiesPolicy sod,
            Phase17CertificationAuditService audit
    ) {
        this.sod = sod;
        this.audit = audit;
    }

    public String digest(Phase17BundleDigest.BundleDigestInput input, String actor) {
        sod.requirePrincipal(actor, "certifier");
        sod.denyAgentOrMcp(actor);
        try {
            String digest = Phase17BundleDigest.digest(input);
            audit.append("BUNDLE", input.bundleId().toString(), String.valueOf(input.versionNo()),
                    "ARTIFACT_BOUND", actor, "{\"digest\":\"" + digest + "\"}");
            return digest;
        } catch (IllegalArgumentException ex) {
            throw new Phase17Exception(Phase17DenialCode.P17_BUNDLE_INVALID, ex.getMessage());
        }
    }
}
