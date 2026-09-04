package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.RuntimeArtifactIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackagedRuntimeTransportArtifactIdentityProviderTest {

    @Test
    void packagedAMatchesPackagedA() {
        RuntimeArtifactIdentity a = new PackagedRuntimeTransportArtifactIdentityProvider().currentIdentity();
        RuntimeArtifactIdentity b = new PackagedRuntimeTransportArtifactIdentityProvider().currentIdentity();
        assertEquals(a.artifactDigest(), b.artifactDigest());
        assertTrue(a.artifactDigest().matches("^[0-9a-f]{64}$"));
    }

    @Test
    void packagedBytesChangedDenies() {
        byte[] original = PackagedRuntimeTransportArtifactIdentityProvider.loadClasspathBytes();
        String text = new String(original, StandardCharsets.UTF_8).replace("unconfigured-0", "tampered-1");
        RuntimeArtifactIdentity originalId = PackagedRuntimeTransportArtifactIdentityProvider.fromPackagedBytes(original);
        RuntimeArtifactIdentity tampered = PackagedRuntimeTransportArtifactIdentityProvider.fromPackagedBytes(
                text.getBytes(StandardCharsets.UTF_8));
        assertNotEquals(originalId.artifactDigest(), tampered.artifactDigest());
    }

    @Test
    void claimedDigestIgnoredWhenTextAltered() {
        byte[] original = PackagedRuntimeTransportArtifactIdentityProvider.loadClasspathBytes();
        String withClaim = new String(original, StandardCharsets.UTF_8)
                .replaceFirst("\\{", "{\"artifactDigest\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",")
                .replace("unconfigured-0", "other-impl");
        RuntimeArtifactIdentity originalId = PackagedRuntimeTransportArtifactIdentityProvider.fromPackagedBytes(original);
        RuntimeArtifactIdentity altered = PackagedRuntimeTransportArtifactIdentityProvider.fromPackagedBytes(
                withClaim.getBytes(StandardCharsets.UTF_8));
        assertNotEquals(originalId.artifactDigest(), altered.artifactDigest());
    }

    @Test
    void missingAndMalformedManifestDenied() {
        assertThrows(IllegalStateException.class,
                () -> PackagedRuntimeTransportArtifactIdentityProvider.fromPackagedBytes(new byte[0]));
        assertThrows(IllegalStateException.class,
                () -> PackagedRuntimeTransportArtifactIdentityProvider.fromPackagedBytes("{".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void claimedEnvOverrideMismatchDenied() {
        byte[] original = PackagedRuntimeTransportArtifactIdentityProvider.loadClasspathBytes();
        String previous = System.getProperty(PackagedRuntimeTransportArtifactIdentityProvider.CLAIMED_DIGEST_PROPERTY);
        System.setProperty(PackagedRuntimeTransportArtifactIdentityProvider.CLAIMED_DIGEST_PROPERTY,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        try {
            assertThrows(IllegalStateException.class,
                    () -> PackagedRuntimeTransportArtifactIdentityProvider.fromPackagedBytes(original));
        } finally {
            if (previous == null) {
                System.clearProperty(PackagedRuntimeTransportArtifactIdentityProvider.CLAIMED_DIGEST_PROPERTY);
            } else {
                System.setProperty(PackagedRuntimeTransportArtifactIdentityProvider.CLAIMED_DIGEST_PROPERTY, previous);
            }
        }
    }

    @Test
    void testProviderRefusesProductionRuntime() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("snip.integration.security.production-runtime", "true");
        assertThrows(IllegalStateException.class, () -> new MutableTestRuntimeTransportArtifactIdentityProvider(env));
    }
}
