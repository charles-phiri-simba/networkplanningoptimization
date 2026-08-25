package com.simba.snip.npo.integration.security;

import java.security.cert.X509Certificate;
import java.util.List;

public record ConnectorTrustProfile(
        String trustProfileId,
        TrustMode trustMode,
        List<X509Certificate> customTrustedCertificates,
        boolean strictHostnameVerification,
        List<String> allowedServerNames,
        String clientCertificateCredentialRef
) {
    public ConnectorTrustProfile {
        customTrustedCertificates = customTrustedCertificates == null ? List.of() : List.copyOf(customTrustedCertificates);
        allowedServerNames = allowedServerNames == null ? List.of() : List.copyOf(allowedServerNames);
        if (!strictHostnameVerification) {
            throw new IllegalArgumentException("hostname verification must be strict");
        }
    }
}
