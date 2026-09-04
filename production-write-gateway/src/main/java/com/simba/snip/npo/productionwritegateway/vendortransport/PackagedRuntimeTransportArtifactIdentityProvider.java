package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simba.snip.npo.productionchange.protocol.RuntimeArtifactIdentity;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Runtime identity is the SHA-256 of canonical packaged manifest bytes,
 * never a self-asserted artifactDigest field.
 */
public class PackagedRuntimeTransportArtifactIdentityProvider implements RuntimeTransportArtifactIdentityProvider {

    public static final String RESOURCE = "/META-INF/snip-transport-artifact.json";
    public static final String CLAIMED_DIGEST_PROPERTY = "snip.transport.claimed-artifact-digest";
    public static final String CLAIMED_DIGEST_ENV = "SNIP_TRANSPORT_CLAIMED_ARTIFACT_DIGEST";

    private final RuntimeArtifactIdentity identity;

    public PackagedRuntimeTransportArtifactIdentityProvider() {
        this(loadClasspathBytes());
    }

    public PackagedRuntimeTransportArtifactIdentityProvider(byte[] packagedBytes) {
        this.identity = fromPackagedBytes(packagedBytes);
    }

    static byte[] loadClasspathBytes() {
        try (InputStream in = PackagedRuntimeTransportArtifactIdentityProvider.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("missing packaged transport artifact identity");
            }
            return in.readAllBytes();
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("malformed packaged transport artifact identity", ex);
        }
    }

    static RuntimeArtifactIdentity fromPackagedBytes(byte[] packagedBytes) {
        if (packagedBytes == null || packagedBytes.length == 0) {
            throw new IllegalStateException("missing packaged transport artifact identity");
        }
        try {
            JsonNode node = new ObjectMapper().readTree(packagedBytes);
            if (node == null || !node.isObject()) {
                throw new IllegalStateException("malformed packaged transport artifact identity");
            }
            byte[] canonical = canonicalize((ObjectNode) node);
            String computed = Sha256Hex.hashBytes(canonical);
            String claimedOverride = claimedDigestOverride();
            if (claimedOverride != null && !claimedOverride.equals(computed)) {
                throw new IllegalStateException("claimed artifact digest does not match packaged bytes");
            }
            return new RuntimeArtifactIdentity(
                    computed,
                    text(node, "transportImplementationVersion"),
                    text(node, "sourceBaselineSha"),
                    node.hasNonNull("containerImageDigest") ? node.get("containerImageDigest").asText() : null
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("malformed packaged transport artifact identity", ex);
        }
    }

    static byte[] canonicalize(ObjectNode node) {
        TreeMap<String, String> fields = new TreeMap<>();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if ("artifactDigest".equals(name)) {
                continue;
            }
            JsonNode value = node.get(name);
            if (value != null && value.isValueNode() && !value.isNull()) {
                fields.put(name, value.asText());
            }
        }
        StringBuilder canonical = new StringBuilder();
        for (var entry : fields.entrySet()) {
            canonical.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return canonical.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String claimedDigestOverride() {
        String env = System.getenv(CLAIMED_DIGEST_ENV);
        if (env != null && !env.isBlank()) {
            return env.strip();
        }
        return System.getProperty(CLAIMED_DIGEST_PROPERTY);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    @Override
    public RuntimeArtifactIdentity currentIdentity() {
        return identity;
    }
}
