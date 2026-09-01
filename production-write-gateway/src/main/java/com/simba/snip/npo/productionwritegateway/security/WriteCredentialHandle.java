package com.simba.snip.npo.productionwritegateway.security;

public final class WriteCredentialHandle {

    private final String credentialProfileId;
    private final String versionIdentifier;
    private final char[] secretMaterial;

    public WriteCredentialHandle(String credentialProfileId, String versionIdentifier, char[] secretMaterial) {
        this.credentialProfileId = credentialProfileId;
        this.versionIdentifier = versionIdentifier;
        this.secretMaterial = secretMaterial == null ? new char[0] : secretMaterial;
    }

    public String credentialProfileId() {
        return credentialProfileId;
    }

    public String versionIdentifier() {
        return versionIdentifier;
    }

    public void destroy() {
        java.util.Arrays.fill(secretMaterial, '\0');
    }

    @Override
    public String toString() {
        return "WriteCredentialHandle{profile=" + credentialProfileId + ", version=" + versionIdentifier + "}";
    }
}
