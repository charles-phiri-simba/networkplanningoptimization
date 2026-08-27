package com.simba.snip.npo.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.config.ConnectorSecurityProperties;
import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportRuntimeException;
import com.simba.snip.npo.integration.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import okhttp3.tls.HeldCertificate;

import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AzureKeyVaultCredentialProviderTest {

    private static final String VAULT = "https://snip-phase10-int.vault.azure.net";
    private static final String SECRET_NAME = "snip-int-ericsson-inventory-reader";
    private static final String CANARY = LocalDevelopmentCredentialProvider.CANARY_SECRET;

    private ConnectorSecurityProperties properties;
    private InMemoryAzureKeyVaultSecretAccessor accessor;
    private AzureVaultReferenceRegistry references;
    private ConnectorSecurityMetrics metrics;
    private AzureKeyVaultCredentialProvider provider;

    @BeforeEach
    void setUp() {
        properties = productionProperties();
        accessor = new InMemoryAzureKeyVaultSecretAccessor();
        references = new AzureVaultReferenceRegistry(properties);
        metrics = new ConnectorSecurityMetrics();
        provider = new AzureKeyVaultCredentialProvider(
                properties, accessor, references, metrics, new ObjectMapper());
    }

    @Test
    void resolvesLatestUsernamePasswordWithoutCachingValue() {
        accessor.put(SECRET_NAME, "v1", payload("ericsson-reader", CANARY), true);
        CredentialHandle handle = provider.resolve(ericssonIdentity());
        assertEquals("v1", handle.metadata().versionIdentifier());
        assertEquals(CredentialProviderType.AZURE_KEY_VAULT, handle.metadata().provider());
        assertEquals("ericsson-reader", handle.username());
        assertEquals(CANARY, new String(handle.secretCopy()));
        assertFalse(handle.toString().contains(CANARY));
        assertEquals(1, accessor.gets());
        accessor.put(SECRET_NAME, "v2", payload("ericsson-reader", "rotated"), true);
        CredentialHandle rotated = provider.resolve(ericssonIdentity());
        assertEquals("v2", rotated.metadata().versionIdentifier());
        assertEquals("rotated", new String(rotated.secretCopy()));
        assertEquals(1, metrics.credentialVersionChangesObserved());
        assertEquals(2, metrics.vaultCredentialResolutions());
    }

    @Test
    void disabledLatestDoesNotFallBackToOlderVersion() {
        accessor.put(SECRET_NAME, "v1", payload("ericsson-reader", CANARY), true);
        accessor.put(SECRET_NAME, "v2", payload("ericsson-reader", "newer"), true);
        accessor.disableLatest(SECRET_NAME);
        ConnectorSecurityException ex = assertThrows(
                ConnectorSecurityException.class, () -> provider.resolve(ericssonIdentity()));
        assertEquals(ImportFailureCode.VAULT_SECRET_DISABLED, ex.failureCode());
        assertFalse(ImportRuntimeException.retryableDefault(ex.failureCode()));
        assertFalse(ex.getMessage().contains(CANARY));
        assertEquals(1, metrics.vaultSecretDisabled());
    }

    @Test
    void mapsForcedVaultFailuresWithoutSecretText() {
        assertMapped(ImportFailureCode.VAULT_AUTHENTICATION_FAILED, false);
        assertMapped(ImportFailureCode.VAULT_ACCESS_DENIED, false);
        assertMapped(ImportFailureCode.VAULT_SECRET_NOT_FOUND, false);
        assertMapped(ImportFailureCode.VAULT_UNAVAILABLE, true);
    }

    @Test
    void missingSecretIsNotFound() {
        ConnectorSecurityException ex = assertThrows(
                ConnectorSecurityException.class, () -> provider.resolve(ericssonIdentity()));
        assertEquals(ImportFailureCode.VAULT_SECRET_NOT_FOUND, ex.failureCode());
        assertFalse(ImportRuntimeException.retryableDefault(ex.failureCode()));
    }

    @Test
    void productionDoesNotUseLocalProviderWhenVaultFails() {
        LocalDevelopmentCredentialProvider local = new LocalDevelopmentCredentialProvider(
                properties, java.time.Clock.systemUTC());
        local.putUsernamePassword(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                CANARY.toCharArray(),
                "local-v1",
                java.time.Instant.now().plusSeconds(3600)
        );
        accessor.forceFailure(ImportFailureCode.VAULT_ACCESS_DENIED);
        ConnectorSecurityException vault = assertThrows(
                ConnectorSecurityException.class, () -> provider.resolve(ericssonIdentity()));
        assertEquals(ImportFailureCode.VAULT_ACCESS_DENIED, vault.failureCode());
        ConnectorSecurityException localDenied = assertThrows(
                ConnectorSecurityException.class, () -> local.resolve(ericssonIdentity()));
        assertEquals(ImportFailureCode.VAULT_UNAVAILABLE, localDenied.failureCode());
    }

    @Test
    void parsesClientCertificatePayloadInMemoryOnly() throws Exception {
        HeldCertificate cert = new HeldCertificate.Builder().commonName("client").build();
        String json = "{"
                + "\"certificateDerBase64\":\"" + Base64.getEncoder().encodeToString(cert.certificate().getEncoded()) + "\","
                + "\"privateKeyPkcs8Base64\":\"" + Base64.getEncoder().encodeToString(cert.keyPair().getPrivate().getEncoded()) + "\""
                + "}";
        references.bindType(ConnectorRegistry.ERICSSON_CREDENTIAL_REF, CredentialType.CLIENT_CERTIFICATE);
        accessor.put(SECRET_NAME, "cert-v1", json, true);
        CredentialHandle handle = provider.resolve(ericssonIdentity());
        assertEquals(CredentialType.CLIENT_CERTIFICATE, handle.metadata().credentialType());
        assertEquals("cert-v1", handle.metadata().versionIdentifier());
        assertFalse(handle.toString().contains("BEGIN"));
        handle.clear();
    }

    @Test
    void trustMaterialUsesLatestVaultVersionWithoutRestart() throws Exception {
        HeldCertificate first = new HeldCertificate.Builder().certificateAuthority(1).commonName("ca-v1").build();
        HeldCertificate second = new HeldCertificate.Builder().certificateAuthority(1).commonName("ca-v2").build();
        properties.getAzureKeyVault().getTrustSecretNames()
                .put(ConnectorRegistry.ERICSSON_TRUST, "snip-int-ericsson-trust");
        InMemoryAzureKeyVaultSecretAccessor trustAccessor = accessor;
        ConnectorTrustMaterialProvider trust = new ConnectorTrustMaterialProvider(trustAccessor, references, metrics);
        trustAccessor.put("snip-int-ericsson-trust", "t1", pem(first.certificate()), true);
        ConnectorTrustProfile emptyCustom = new ConnectorTrustProfile(
                ConnectorRegistry.ERICSSON_TRUST, TrustMode.CUSTOM_CA, List.of(), true, List.of("localhost"), null);
        ConnectorTrustProfile v1 = trust.resolve(emptyCustom);
        assertEquals("CN=ca-v1", v1.customTrustedCertificates().get(0).getSubjectX500Principal().getName());
        trustAccessor.put("snip-int-ericsson-trust", "t2", pem(second.certificate()), true);
        ConnectorTrustProfile v2 = trust.resolve(emptyCustom);
        assertEquals("CN=ca-v2", v2.customTrustedCertificates().get(0).getSubjectX500Principal().getName());
        assertNotEquals(
                v1.customTrustedCertificates().get(0).getSubjectX500Principal().getName(),
                v2.customTrustedCertificates().get(0).getSubjectX500Principal().getName());
        assertTrue(v2.strictHostnameVerification());
    }

    @Test
    void vaultUnavailableIsRetryableOthersAreNot() {
        assertTrue(ImportRuntimeException.retryableDefault(ImportFailureCode.VAULT_UNAVAILABLE));
        assertFalse(ImportRuntimeException.retryableDefault(ImportFailureCode.VAULT_AUTHENTICATION_FAILED));
        assertFalse(ImportRuntimeException.retryableDefault(ImportFailureCode.VAULT_ACCESS_DENIED));
        assertFalse(ImportRuntimeException.retryableDefault(ImportFailureCode.VAULT_SECRET_NOT_FOUND));
        assertFalse(ImportRuntimeException.retryableDefault(ImportFailureCode.VAULT_SECRET_DISABLED));
        assertFalse(ImportRuntimeException.retryableDefault(ImportFailureCode.TRUST_MATERIAL_RESOLUTION_FAILED));
    }

    private void assertMapped(ImportFailureCode code, boolean retryable) {
        accessor.forceFailure(code);
        ConnectorSecurityException ex = assertThrows(
                ConnectorSecurityException.class, () -> provider.resolve(ericssonIdentity()));
        assertEquals(code, ex.failureCode());
        assertEquals(retryable, ImportRuntimeException.retryableDefault(code));
        assertFalse(ex.getMessage().contains(CANARY));
        accessor.forceFailure(null);
        accessor.resetGets();
    }

    private static ConnectorIdentity ericssonIdentity() {
        return new ConnectorIdentity(
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ERICSSON_SECURE_MOCK",
                Vendor.ERICSSON,
                "INT",
                "INVENTORY_READER",
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorRegistry.ERICSSON_TRUST,
                ConnectorAuthorizationProfile.READ_ONLY_NETWORK_INVENTORY,
                ConnectorRegistry.ERICSSON_NETWORK,
                true
        );
    }

    private static ConnectorSecurityProperties productionProperties() {
        ConnectorSecurityProperties properties = new ConnectorSecurityProperties();
        properties.setProductionRuntime(true);
        properties.setLocalCredentialsEnabled(false);
        properties.getAzureKeyVault().setEnabled(true);
        properties.getAzureKeyVault().setVaultUri(VAULT);
        properties.getAzureKeyVault().setAuthentication(ConnectorSecurityProperties.AUTH_WORKLOAD_IDENTITY);
        properties.getAzureKeyVault().getSecretNames()
                .put(ConnectorRegistry.ERICSSON_CREDENTIAL_REF, SECRET_NAME);
        return properties;
    }

    private static String payload(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static String pem(X509Certificate certificate) throws Exception {
        return "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certificate.getEncoded())
                + "\n-----END CERTIFICATE-----\n";
    }
}
