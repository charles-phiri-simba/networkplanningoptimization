package com.simba.snip.npo.integration.security;

import java.util.Arrays;

/**
 * Session-scoped secret material. Values never appear in {@code toString}, JSON, or equality diagnostics.
 */
public final class CredentialHandle {

    private final CredentialMetadata metadata;
    private final char[] secret;
    private final byte[] certificateDer;
    private final byte[] privateKeyPkcs8;
    private final String username;

    private CredentialHandle(
            CredentialMetadata metadata,
            String username,
            char[] secret,
            byte[] certificateDer,
            byte[] privateKeyPkcs8
    ) {
        this.metadata = metadata;
        this.username = username;
        this.secret = secret == null ? null : Arrays.copyOf(secret, secret.length);
        this.certificateDer = certificateDer == null ? null : Arrays.copyOf(certificateDer, certificateDer.length);
        this.privateKeyPkcs8 = privateKeyPkcs8 == null ? null : Arrays.copyOf(privateKeyPkcs8, privateKeyPkcs8.length);
    }

    public static CredentialHandle usernamePassword(CredentialMetadata metadata, String username, char[] password) {
        return new CredentialHandle(metadata, username, password, null, null);
    }

    public static CredentialHandle clientCertificate(
            CredentialMetadata metadata,
            byte[] certificateDer,
            byte[] privateKeyPkcs8
    ) {
        return new CredentialHandle(metadata, null, null, certificateDer, privateKeyPkcs8);
    }

    public static CredentialHandle basicPlusMtls(
            CredentialMetadata metadata,
            String username,
            char[] password,
            byte[] certificateDer,
            byte[] privateKeyPkcs8
    ) {
        return new CredentialHandle(metadata, username, password, certificateDer, privateKeyPkcs8);
    }

    public CredentialMetadata metadata() {
        return metadata;
    }

    public String username() {
        return username;
    }

    public char[] secretCopy() {
        return secret == null ? null : Arrays.copyOf(secret, secret.length);
    }

    public byte[] certificateDerCopy() {
        return certificateDer == null ? null : Arrays.copyOf(certificateDer, certificateDer.length);
    }

    public byte[] privateKeyPkcs8Copy() {
        return privateKeyPkcs8 == null ? null : Arrays.copyOf(privateKeyPkcs8, privateKeyPkcs8.length);
    }

    public void clear() {
        if (secret != null) {
            Arrays.fill(secret, '\0');
        }
        if (privateKeyPkcs8 != null) {
            Arrays.fill(privateKeyPkcs8, (byte) 0);
        }
    }

    @Override
    public String toString() {
        return "CredentialHandle[redacted, credentialRef=" + (metadata == null ? null : metadata.credentialRef()) + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
