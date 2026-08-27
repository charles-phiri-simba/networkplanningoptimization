package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Component
public class ConnectorTrustMaterialProvider {

    private final AzureKeyVaultSecretAccessor accessor;
    private final AzureVaultReferenceRegistry references;
    private final ConnectorSecurityMetrics metrics;

    public ConnectorTrustMaterialProvider(
            AzureKeyVaultSecretAccessor accessor,
            AzureVaultReferenceRegistry references,
            ConnectorSecurityMetrics metrics
    ) {
        this.accessor = accessor;
        this.references = references;
        this.metrics = metrics;
    }

    public ConnectorTrustProfile resolve(ConnectorTrustProfile profile) {
        if (profile == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.TRUST_MATERIAL_RESOLUTION_FAILED, "trust profile is missing");
        }
        if (profile.trustMode() != TrustMode.CUSTOM_CA) {
            return profile;
        }
        if (profile.customTrustedCertificates() != null && !profile.customTrustedCertificates().isEmpty()) {
            return profile;
        }
        if (!references.hasTrustMaterial(profile.trustProfileId())) {
            return profile;
        }
        try {
            ResolvedVaultSecret secret = accessor.get(references.trust(profile.trustProfileId()));
            List<X509Certificate> certificates = parseCertificates(secret.value());
            if (certificates.isEmpty()) {
                throw new ConnectorSecurityException(
                        ImportFailureCode.TRUST_MATERIAL_RESOLUTION_FAILED, "trust material is empty");
            }
            return new ConnectorTrustProfile(
                    profile.trustProfileId(),
                    profile.trustMode(),
                    certificates,
                    profile.strictHostnameVerification(),
                    profile.allowedServerNames(),
                    profile.clientCertificateCredentialRef()
            );
        } catch (ConnectorSecurityException ex) {
            if (ex.failureCode() == ImportFailureCode.TRUST_MATERIAL_RESOLUTION_FAILED) {
                throw ex;
            }
            throw new ConnectorSecurityException(
                    ImportFailureCode.TRUST_MATERIAL_RESOLUTION_FAILED, "trust material resolution failed", ex);
        }
    }

    static List<X509Certificate> parseCertificates(String payload) {
        try {
            List<X509Certificate> certificates = new ArrayList<>();
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            if (payload.contains("BEGIN CERTIFICATE")) {
                String[] parts = payload.split("-----END CERTIFICATE-----");
                for (String part : parts) {
                    String pem = part.trim();
                    if (pem.isEmpty()) {
                        continue;
                    }
                    pem = pem + "\n-----END CERTIFICATE-----\n";
                    certificates.add((X509Certificate) factory.generateCertificate(
                            new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII))));
                }
                return certificates;
            }
            byte[] der = Base64.getDecoder().decode(payload.replaceAll("\\s", ""));
            certificates.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der)));
            return certificates;
        } catch (Exception ex) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.TRUST_MATERIAL_RESOLUTION_FAILED, "trust material is invalid", ex);
        }
    }

    public static String fingerprintLabel(String version) {
        return version == null ? "" : version.toLowerCase(Locale.ROOT);
    }
}
