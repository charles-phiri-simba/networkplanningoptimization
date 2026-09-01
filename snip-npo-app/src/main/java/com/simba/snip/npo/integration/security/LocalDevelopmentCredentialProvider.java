package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.config.ConnectorSecurityProperties;
import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalDevelopmentCredentialProvider implements ConnectorCredentialProvider {

    public static final String CANARY_SECRET = "PHASE9_CANARY_SECRET_VALUE";

    private final ConnectorSecurityProperties properties;
    private final Clock clock;
    private final Map<String, Stored> store = new ConcurrentHashMap<>();

    private final java.util.concurrent.atomic.AtomicInteger resolveCalls = new java.util.concurrent.atomic.AtomicInteger();

    public LocalDevelopmentCredentialProvider(ConnectorSecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public CredentialProviderType providerType() {
        return CredentialProviderType.LOCAL_DEVELOPMENT;
    }

    @Override
    public synchronized CredentialHandle resolve(ConnectorIdentity identity) {
        resolveCalls.incrementAndGet();
        assertEnabled();
        Stored stored = store.get(requiredRef(identity));
        if (stored == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential reference is not configured");
        }
        if (!stored.credentialRef.equals(identity.credentialRef())
                || stored.ownerConnectorId == null
                || !stored.ownerConnectorId.equals(identity.connectorId())) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential reference is not bound to this identity");
        }
        Instant now = clock.instant();
        if (stored.expiresAt != null && !stored.expiresAt.isAfter(now)) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential has expired");
        }
        CredentialMetadata metadata = new CredentialMetadata(
                stored.credentialRef,
                CredentialProviderType.LOCAL_DEVELOPMENT,
                stored.credentialType,
                stored.version,
                now,
                stored.expiresAt
        );
        if (stored.credentialType == CredentialType.CLIENT_CERTIFICATE && stored.username != null) {
            return CredentialHandle.basicPlusMtls(
                    metadata, stored.username, stored.secret, stored.certificateDer, stored.privateKeyPkcs8);
        }
        if (stored.credentialType == CredentialType.CLIENT_CERTIFICATE) {
            return CredentialHandle.clientCertificate(metadata, stored.certificateDer, stored.privateKeyPkcs8);
        }
        return CredentialHandle.usernamePassword(metadata, stored.username, stored.secret);
    }

    @Override
    public CredentialMetadata metadata(ConnectorIdentity identity) {
        assertEnabled();
        Stored stored = store.get(requiredRef(identity));
        if (stored == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential reference is not configured");
        }
        return new CredentialMetadata(
                stored.credentialRef,
                CredentialProviderType.LOCAL_DEVELOPMENT,
                stored.credentialType,
                stored.version,
                clock.instant(),
                stored.expiresAt
        );
    }

    public void putUsernamePassword(
            String credentialRef,
            String ownerConnectorId,
            String username,
            char[] password,
            String version,
            Instant expiresAt
    ) {
        store.put(credentialRef, new Stored(
                credentialRef,
                ownerConnectorId,
                CredentialType.USERNAME_PASSWORD,
                username,
                password == null ? null : Arrays.copyOf(password, password.length),
                null,
                null,
                version,
                expiresAt
        ));
    }

    public void putClientCertificate(
            String credentialRef,
            String ownerConnectorId,
            byte[] certificateDer,
            byte[] privateKeyPkcs8,
            String version,
            Instant expiresAt
    ) {
        store.put(credentialRef, new Stored(
                credentialRef,
                ownerConnectorId,
                CredentialType.CLIENT_CERTIFICATE,
                null,
                null,
                certificateDer == null ? null : Arrays.copyOf(certificateDer, certificateDer.length),
                privateKeyPkcs8 == null ? null : Arrays.copyOf(privateKeyPkcs8, privateKeyPkcs8.length),
                version,
                expiresAt
        ));
    }

    public void putBasicPlusMtls(
            String credentialRef,
            String ownerConnectorId,
            String username,
            char[] password,
            byte[] certificateDer,
            byte[] privateKeyPkcs8,
            String version,
            Instant expiresAt
    ) {
        store.put(credentialRef, new Stored(
                credentialRef,
                ownerConnectorId,
                CredentialType.CLIENT_CERTIFICATE,
                username,
                password == null ? null : Arrays.copyOf(password, password.length),
                certificateDer == null ? null : Arrays.copyOf(certificateDer, certificateDer.length),
                privateKeyPkcs8 == null ? null : Arrays.copyOf(privateKeyPkcs8, privateKeyPkcs8.length),
                version,
                expiresAt
        ));
    }

    public void rotateUsernamePassword(String credentialRef, char[] password, String version) {
        Stored current = store.get(credentialRef);
        if (current == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential reference is not configured");
        }
        putUsernamePassword(credentialRef, current.ownerConnectorId, current.username, password, version, current.expiresAt);
    }

    public void expire(String credentialRef, Instant expiresAt) {
        Stored current = store.get(credentialRef);
        if (current == null) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential reference is not configured");
        }
        store.put(credentialRef, new Stored(
                current.credentialRef,
                current.ownerConnectorId,
                current.credentialType,
                current.username,
                current.secret,
                current.certificateDer,
                current.privateKeyPkcs8,
                current.version,
                expiresAt
        ));
    }

    public int resolveCalls() {
        return resolveCalls.get();
    }

    private void assertEnabled() {
        if (properties.isProductionRuntime()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_UNAVAILABLE, "local credential provider is disabled in production");
        }
        if (!properties.isLocalCredentialsEnabled()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "local credential provider is disabled");
        }
    }

    private static String requiredRef(ConnectorIdentity identity) {
        if (identity == null || identity.credentialRef() == null || identity.credentialRef().isBlank()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, "credential reference is missing");
        }
        return identity.credentialRef();
    }

    private static final class Stored {
        private final String credentialRef;
        private final String ownerConnectorId;
        private final CredentialType credentialType;
        private final String username;
        private final char[] secret;
        private final byte[] certificateDer;
        private final byte[] privateKeyPkcs8;
        private final String version;
        private final Instant expiresAt;

        private Stored(
                String credentialRef,
                String ownerConnectorId,
                CredentialType credentialType,
                String username,
                char[] secret,
                byte[] certificateDer,
                byte[] privateKeyPkcs8,
                String version,
                Instant expiresAt
        ) {
            this.credentialRef = credentialRef;
            this.ownerConnectorId = ownerConnectorId;
            this.credentialType = credentialType;
            this.username = username;
            this.secret = secret;
            this.certificateDer = certificateDer;
            this.privateKeyPkcs8 = privateKeyPkcs8;
            this.version = version;
            this.expiresAt = expiresAt;
        }
    }
}
