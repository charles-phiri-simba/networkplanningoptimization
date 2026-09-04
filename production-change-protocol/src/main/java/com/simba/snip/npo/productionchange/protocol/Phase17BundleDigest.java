package com.simba.snip.npo.productionchange.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Canonical bundle content-digest. UTF-8 name + uint32 BE length + value.
 * NULL length is 0xFFFFFFFF. Empty string length is 0.
 */
public final class Phase17BundleDigest {

    private static final int NULL_LEN = 0xFFFFFFFF;

    private Phase17BundleDigest() {
    }

    public static String digest(BundleDigestInput input) {
        requireLowerHex(input.artifactDigest(), 64, "artifact_digest");
        requireLowerHex(input.sourceBaselineSha(), 40, "source_baseline_sha");
        requireLowerHex(input.activeEvidenceSetDigest(), 64, "active_evidence_set_digest");
        byte[] canonical = canonicalize(input);
        return Sha256Hex.hashBytes(canonical);
    }

    public static String evidenceSetDigest(List<UUID> activeEvidenceIds) {
        List<String> sorted = new ArrayList<>();
        for (UUID id : activeEvidenceIds) {
            sorted.add(id.toString().toLowerCase(Locale.ROOT));
        }
        sorted.sort(Comparator.naturalOrder());
        return Sha256Hex.hash(String.join(",", sorted));
    }

    public static byte[] canonicalize(BundleDigestInput input) {
        List<byte[]> chunks = new ArrayList<>();
        chunks.add(field("bundle_id", uuid(input.bundleId())));
        chunks.add(field("version_no", intUtf8(input.versionNo())));
        chunks.add(field("vendor", utf8(input.vendor())));
        chunks.add(field("platform", utf8(input.platform())));
        chunks.add(field("interface_definition_version_id", uuid(input.interfaceDefinitionVersionId())));
        chunks.add(field("interface_approval_id", uuid(input.interfaceApprovalId())));
        chunks.add(field("transport_profile_version_id", uuid(input.transportProfileVersionId())));
        chunks.add(field("artifact_digest", utf8(input.artifactDigest())));
        chunks.add(field("transport_implementation_version", utf8(input.transportImplementationVersion())));
        chunks.add(field("source_baseline_sha", utf8(input.sourceBaselineSha())));
        chunks.add(field("vendor_version_predicate", utf8(input.vendorVersionPredicate())));
        chunks.add(field("capability_cert_version_id", uuid(input.capabilityCertVersionId())));
        chunks.add(field("security_cert_version_id", uuid(input.securityCertVersionId())));
        chunks.add(field("credential_profile_version_id", uuid(input.credentialProfileVersionId())));
        chunks.add(field("tls_profile_version_id", uuid(input.tlsProfileVersionId())));
        chunks.add(field("network_policy_profile_version_id", uuid(input.networkPolicyProfileVersionId())));
        chunks.add(field("endpoint_profile_version_id", uuid(input.endpointProfileVersionId())));
        chunks.add(field("target_class", utf8(input.targetClass())));
        chunks.add(field("active_evidence_set_digest", utf8(input.activeEvidenceSetDigest())));
        int total = 0;
        for (byte[] c : chunks) {
            total += c.length;
        }
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, out, pos, c.length);
            pos += c.length;
        }
        return out;
    }

    private static void requireLowerHex(String value, int len, String name) {
        if (value == null || !value.matches("^[0-9a-f]{" + len + "}$")) {
            throw new IllegalArgumentException("invalid " + name);
        }
    }

    private static byte[] uuid(UUID id) {
        return id == null ? null : utf8(id.toString().toLowerCase(Locale.ROOT));
    }

    private static byte[] intUtf8(int value) {
        return utf8(Integer.toString(value));
    }

    private static byte[] utf8(String value) {
        return value == null ? null : value.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] field(String name, byte[] value) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(nameBytes.length + 4 + (value == null ? 0 : value.length));
        buf.put(nameBytes);
        if (value == null) {
            buf.putInt(NULL_LEN);
        } else {
            buf.putInt(value.length);
            buf.put(value);
        }
        return buf.array();
    }

    public record BundleDigestInput(
            UUID bundleId,
            int versionNo,
            String vendor,
            String platform,
            UUID interfaceDefinitionVersionId,
            UUID interfaceApprovalId,
            UUID transportProfileVersionId,
            String artifactDigest,
            String transportImplementationVersion,
            String sourceBaselineSha,
            String vendorVersionPredicate,
            UUID capabilityCertVersionId,
            UUID securityCertVersionId,
            UUID credentialProfileVersionId,
            UUID tlsProfileVersionId,
            UUID networkPolicyProfileVersionId,
            UUID endpointProfileVersionId,
            String targetClass,
            String activeEvidenceSetDigest
    ) {
    }
}
