package com.simba.snip.npo.vendorcertification.service;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import org.springframework.stereotype.Service;

@Service
public class VendorVersionCompatibilityService {

    public void requireExplicitPredicate(String observedVersion, String certifiedPredicate) {
        if (observedVersion == null || observedVersion.isBlank()) {
            throw new Phase17Exception(Phase17DenialCode.P17_VENDOR_VERSION_UNKNOWN, "unknown version");
        }
        if (certifiedPredicate == null || certifiedPredicate.isBlank()) {
            throw new Phase17Exception(Phase17DenialCode.P17_VENDOR_VERSION_MISMATCH, "no predicate");
        }
        if (certifiedPredicate.contains("..") || certifiedPredicate.toLowerCase().contains("semver")) {
            throw new Phase17Exception(Phase17DenialCode.P17_VENDOR_VERSION_MISMATCH, "no implicit SemVer");
        }
        if (!certifiedPredicate.equals(observedVersion) && !certifiedPredicate.equals("EXPLICIT:" + observedVersion)) {
            throw new Phase17Exception(Phase17DenialCode.P17_VENDOR_VERSION_MISMATCH, "out of certified scope");
        }
    }
}
