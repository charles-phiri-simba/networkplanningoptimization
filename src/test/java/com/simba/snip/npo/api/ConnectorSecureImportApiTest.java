package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.agent.AgentRegistry;
import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.NetworkImportQueryService;
import com.simba.snip.npo.integration.NetworkImportService;
import com.simba.snip.npo.integration.security.AuthenticationMethod;
import com.simba.snip.npo.integration.security.ConnectorAuthorizationProfile;
import com.simba.snip.npo.integration.security.ConnectorCapability;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.security.ConnectorEndpoint;
import com.simba.snip.npo.integration.security.ConnectorEndpointRegistry;
import com.simba.snip.npo.integration.security.ConnectorIdentity;
import com.simba.snip.npo.integration.security.ConnectorNetworkPolicy;
import com.simba.snip.npo.integration.security.ConnectorRegistry;
import com.simba.snip.npo.integration.security.ConnectorSecurityException;
import com.simba.snip.npo.integration.security.ConnectorTrustProfile;
import com.simba.snip.npo.integration.security.LocalDevelopmentCredentialProvider;
import com.simba.snip.npo.integration.security.TrustMode;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.ConnectorSecurityAuditEventRepository;
import com.simba.snip.npo.persist.NetworkSourceReferenceRepository;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConnectorSecureImportApiTest extends AbstractPostgresIT {

    @DynamicPropertySource
    static void localCredentials(DynamicPropertyRegistry registry) {
        registry.add("snip.integration.security.local-credentials-enabled", () -> "true");
    }

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private NetworkImportService importService;

    @Autowired
    private NetworkImportQueryService queryService;

    @Autowired
    private ConnectorRegistry connectorRegistry;

    @Autowired
    private ConnectorEndpointRegistry endpointRegistry;

    @Autowired
    private LocalDevelopmentCredentialProvider credentials;

    @Autowired
    private CellRepository cellRepository;

    @Autowired
    private NetworkSourceReferenceRepository sourceReferenceRepository;

    @Autowired
    private ConnectorSecurityAuditEventRepository securityAuditRepository;

    @Autowired
    private AgentRegistry agentRegistry;

    private MockWebServer server;
    private final AtomicInteger writeCalls = new AtomicInteger();
    private final AtomicInteger redirectTargetHits = new AtomicInteger();
    private final AtomicInteger inventoryHits = new AtomicInteger();

    @AfterEach
    void stopServer() throws IOException {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

    @Test
    void trustedTlsBasicImportCreatesCanonicalStateAndNeverCallsWrite() throws Exception {
        HeldCertificate root = ca("trusted-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert, false, validPassword());
        bindEricsson(root, AuthenticationMethod.BASIC, true, List.of(serverCert.certificate()), "localhost", server.getPort());
        int writesBefore = writeCalls.get();
        ImportBatchDto batch = postConnector(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER, Map.of(
                "credentialRef", ConnectorRegistry.NOKIA_CREDENTIAL_REF,
                "endpointUrl", "https://evil.example"
        ));
        assertEquals("COMPLETED", batch.status());
        assertTrue("NEW".equals(batch.executionType()) || "REPLAY".equals(batch.executionType()));
        assertEquals("ERICSSON_SECURE_MOCK", batch.sourceSystem());
        assertNotNull(cellRepository.findByCellId("CELL-E-SEC001").orElse(null));
        assertEquals(1, sourceReferenceRepository.findByCanonicalEntityIdOrderByCanonicalEntityTypeAsc("CELL-E-SEC001").size());
        assertEquals(writesBefore, writeCalls.get());
        assertCanaryAbsent(batch);
        ResponseEntity<String> audit = http.getForEntity(
                "/api/v1/integration/imports/" + batch.importId() + "/security-audit", String.class);
        assertFalse(audit.getBody().contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
        assertEquals(5, agentRegistry.list().size());
    }

    @Test
    void untrustedCaFailsWithoutCanonicalMutation() throws Exception {
        HeldCertificate root = ca("untrusted-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert, false, validPassword());
        bindEricsson(root, AuthenticationMethod.BASIC, true, List.of(), "localhost", server.getPort());
        connectorRegistry.replaceTrust(new ConnectorTrustProfile(
                ConnectorRegistry.ERICSSON_TRUST, TrustMode.SYSTEM_CA, List.of(), true, List.of("localhost"), null));
        ImportBatchDto batch = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.TLS_TRUST_FAILED.name(), batch.failureCode());
        assertCanaryAbsent(batch);
    }

    @Test
    void hostnameMismatchFails() throws Exception {
        HeldCertificate root = ca("host-ca");
        HeldCertificate serverCert = serverCert(root, "wrong.host.test");
        startServer(root, serverCert, false, validPassword());
        bindEricsson(root, AuthenticationMethod.BASIC, true, List.of(root.certificate()), "localhost", server.getPort());
        ImportBatchDto batch = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.TLS_TRUST_FAILED.name(), batch.failureCode());
        assertCanaryAbsent(batch);
    }

    @Test
    void wrongPasswordFailsClosed() throws Exception {
        HeldCertificate root = ca("auth-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert, false, validPassword());
        bindEricsson(root, AuthenticationMethod.BASIC, true, List.of(root.certificate()), "localhost", server.getPort());
        credentials.putUsernamePassword(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                "wrong-password".toCharArray(),
                "v-wrong",
                Instant.now().plusSeconds(3600)
        );
        ImportBatchDto batch = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED.name(), batch.failureCode());
        assertCanaryAbsent(batch);
    }

    @Test
    void credentialRotationIsPickedUpWithoutRestart() throws Exception {
        HeldCertificate root = ca("rot-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert, false, "rotated-secret".toCharArray());
        bindEricsson(root, AuthenticationMethod.BASIC, true, List.of(root.certificate()), "localhost", server.getPort());
        credentials.putUsernamePassword(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                LocalDevelopmentCredentialProvider.CANARY_SECRET.toCharArray(),
                "vA",
                Instant.now().plusSeconds(3600)
        );
        ImportBatchDto first = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", first.status());
        credentials.rotateUsernamePassword(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF, "rotated-secret".toCharArray(), "vB");
        ImportBatchDto second = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("COMPLETED", second.status());
        assertTrue(securityAuditRepository.findByExecutionIdOrderByOccurredAtAsc(second.importId()).stream()
                .anyMatch(event -> "vB".equals(event.getCredentialVersion())));
    }

    @Test
    void expiredCredentialFailsBeforeRead() throws Exception {
        HeldCertificate root = ca("exp-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert, false, validPassword());
        bindEricsson(root, AuthenticationMethod.BASIC, true, List.of(root.certificate()), "localhost", server.getPort());
        credentials.expire(ConnectorRegistry.ERICSSON_CREDENTIAL_REF, Instant.now().minusSeconds(60));
        ImportBatchDto batch = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED.name(), batch.failureCode());
    }

    @Test
    void disabledConnectorFailsBeforeCredentialResolve() {
        connectorRegistry.enable(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER, false);
        int before = credentials.resolveCalls();
        ImportBatchDto batch = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.CONNECTOR_DISABLED.name(), batch.failureCode());
        assertEquals(before, credentials.resolveCalls());
    }

    @Test
    void missingReadCapabilityIsDeniedBeforeNetwork() throws Exception {
        HeldCertificate root = ca("authz-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert, false, validPassword());
        bindEricsson(root, AuthenticationMethod.BASIC, true, List.of(root.certificate()), "localhost", server.getPort());
        connectorRegistry.replaceAuthorization(new ConnectorAuthorizationProfile(
                ConnectorAuthorizationProfile.READ_ONLY_NETWORK_INVENTORY,
                EnumSet.of(
                        ConnectorCapability.READ_SITE,
                        ConnectorCapability.READ_GNB,
                        ConnectorCapability.READ_CELL,
                        ConnectorCapability.READ_NEIGHBOURS
                )
        ));
        int before = credentials.resolveCalls();
        ImportBatchDto batch = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED.name(), batch.failureCode());
        assertEquals(before, credentials.resolveCalls());
        connectorRegistry.replaceAuthorization(ConnectorAuthorizationProfile.readOnlyNetworkInventory());
    }

    @Test
    void unapprovedHostIsDeniedBeforeConnect() {
        connectorRegistry.replaceNetworkPolicy(new ConnectorNetworkPolicy(
                ConnectorRegistry.ERICSSON_NETWORK, List.of("mock-ericsson.int"), Set.of(443), true, false));
        endpointRegistry.register(new ConnectorEndpoint(
                ConnectorRegistry.ERICSSON_ENDPOINT_REF, URI.create("https://169.254.169.254")));
        connectorRegistry.enable(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER, true);
        credentials.putUsernamePassword(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                validPassword(),
                "v1",
                Instant.now().plusSeconds(3600)
        );
        ImportBatchDto batch = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.NETWORK_POLICY_DENIED.name(), batch.failureCode());
    }

    @Test
    void crossVendorCredentialResolutionIsDenied() {
        credentials.putUsernamePassword(
                ConnectorRegistry.NOKIA_CREDENTIAL_REF,
                ConnectorDefinition.NOKIA_NETACT_INT_INVENTORY_READER,
                "nokia-reader",
                "nokia-secret".toCharArray(),
                "n1",
                Instant.now().plusSeconds(3600)
        );
        ConnectorIdentity spoofed = new ConnectorIdentity(
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ERICSSON_SECURE_MOCK",
                com.simba.snip.npo.integration.Vendor.ERICSSON,
                "INT",
                "INVENTORY_READER",
                ConnectorRegistry.NOKIA_CREDENTIAL_REF,
                ConnectorRegistry.ERICSSON_TRUST,
                ConnectorAuthorizationProfile.READ_ONLY_NETWORK_INVENTORY,
                ConnectorRegistry.ERICSSON_NETWORK,
                true
        );
        ConnectorSecurityException ex = assertThrows(
                ConnectorSecurityException.class, () -> credentials.resolve(spoofed));
        assertEquals(ImportFailureCode.CREDENTIAL_RESOLUTION_FAILED, ex.failureCode());
        assertFalse(ex.getMessage().contains("nokia-secret"));
    }

    @Test
    void mtlsSucceedsWithTrustedClientCertAndFailsWhenMissingOrUntrusted() throws Exception {
        HeldCertificate root = ca("mtls-ca");
        HeldCertificate otherRoot = ca("other-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        HeldCertificate client = new HeldCertificate.Builder()
                .commonName("ericsson-client")
                .signedBy(root)
                .build();
        HeldCertificate untrustedClient = new HeldCertificate.Builder()
                .commonName("untrusted-client")
                .signedBy(otherRoot)
                .build();
        startServer(root, serverCert, true, validPassword());
        bindEricsson(root, AuthenticationMethod.MTLS, true, List.of(root.certificate()), "localhost", server.getPort());

        credentials.putUsernamePassword(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                validPassword(),
                "v-basic",
                Instant.now().plusSeconds(3600)
        );
        ImportBatchDto missing = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals(ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED.name(), missing.failureCode());

        credentials.putClientCertificate(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                untrustedClient.certificate().getEncoded(),
                untrustedClient.keyPair().getPrivate().getEncoded(),
                "mtls-bad",
                Instant.now().plusSeconds(3600)
        );
        ImportBatchDto untrusted = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", untrusted.status());
        assertTrue(Set.of(
                ImportFailureCode.TLS_TRUST_FAILED.name(),
                ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED.name(),
                ImportFailureCode.SNAPSHOT_READ_FAILED.name()
        ).contains(untrusted.failureCode()));

        credentials.putClientCertificate(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                client.certificate().getEncoded(),
                client.keyPair().getPrivate().getEncoded(),
                "mtls-good",
                Instant.now().plusSeconds(3600)
        );
        ImportBatchDto mtls = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("COMPLETED", mtls.status());
    }

    @Test
    void basicPlusMtlsRequiresBothFactorsSimultaneously() throws Exception {
        HeldCertificate root = ca("dual-ca");
        HeldCertificate otherRoot = ca("dual-other-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        HeldCertificate trustedClient = new HeldCertificate.Builder()
                .commonName("ericsson-dual-client")
                .signedBy(root)
                .build();
        HeldCertificate untrustedClient = new HeldCertificate.Builder()
                .commonName("ericsson-dual-untrusted")
                .signedBy(otherRoot)
                .build();
        startServer(root, serverCert, true, true, validPassword());
        bindEricsson(root, AuthenticationMethod.BASIC_PLUS_MTLS, true, List.of(root.certificate()), "localhost", server.getPort());
        credentials.putBasicPlusMtls(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                validPassword(),
                trustedClient.certificate().getEncoded(),
                trustedClient.keyPair().getPrivate().getEncoded(),
                "dual-good",
                Instant.now().plusSeconds(3600)
        );

        int writesBefore = writeCalls.get();
        int inventoryBefore = inventoryHits.get();

        ImportBatchDto bothValid = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("COMPLETED", bothValid.status());
        assertTrue("NEW".equals(bothValid.executionType()) || "REPLAY".equals(bothValid.executionType()));
        assertTrue(inventoryHits.get() > inventoryBefore);
        assertEquals(writesBefore, writeCalls.get());
        assertCanaryAbsent(bothValid);
        assertNotNull(cellRepository.findByCellId("CELL-E-SEC001").orElse(null));

        CanonicalSnapshot afterSuccess = canonicalSnapshot();
        int inventoryAfterSuccess = inventoryHits.get();

        credentials.putBasicPlusMtls(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                "wrong-password".toCharArray(),
                trustedClient.certificate().getEncoded(),
                trustedClient.keyPair().getPrivate().getEncoded(),
                "dual-bad-basic",
                Instant.now().plusSeconds(3600)
        );
        ImportBatchDto invalidBasic = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertFailClosed(invalidBasic, ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED);
        assertTrue(inventoryHits.get() > inventoryAfterSuccess);
        assertEquals(writesBefore, writeCalls.get());
        assertCanonicalUnchanged(afterSuccess);
        assertFalse(String.valueOf(invalidBasic.error()).contains("wrong-password"));

        int inventoryAfterInvalidBasic = inventoryHits.get();
        credentials.putUsernamePassword(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                validPassword(),
                "dual-missing-cert",
                Instant.now().plusSeconds(3600)
        );
        ImportBatchDto missingCert = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertFailClosed(missingCert, ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED);
        assertEquals(inventoryAfterInvalidBasic, inventoryHits.get());
        assertEquals(writesBefore, writeCalls.get());
        assertCanonicalUnchanged(afterSuccess);

        credentials.putBasicPlusMtls(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                validPassword(),
                untrustedClient.certificate().getEncoded(),
                untrustedClient.keyPair().getPrivate().getEncoded(),
                "dual-untrusted-cert",
                Instant.now().plusSeconds(3600)
        );
        ImportBatchDto untrusted = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", untrusted.status());
        assertEquals("UNREAD", untrusted.sourceSnapshotId());
        assertTrue(Set.of(
                ImportFailureCode.TLS_TRUST_FAILED.name(),
                ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED.name(),
                ImportFailureCode.SNAPSHOT_READ_FAILED.name()
        ).contains(untrusted.failureCode()));
        assertCanaryAbsent(untrusted);
        assertEquals(0, untrusted.entitiesCreated());
        assertEquals(0, untrusted.entitiesUpdated());
        assertEquals(inventoryAfterInvalidBasic, inventoryHits.get());
        assertEquals(writesBefore, writeCalls.get());
        assertCanonicalUnchanged(afterSuccess);
    }

    @Test
    void redirectIsNotFollowed() throws Exception {
        HeldCertificate root = ca("redir-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert, false, validPassword());
        bindEricsson(root, AuthenticationMethod.BASIC, true, List.of(root.certificate()), "localhost", server.getPort());
        ConnectorDefinition current = connectorRegistry.require(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER);
        connectorRegistry.replace(new ConnectorDefinition(
                current.connectorId(), current.vendor(), current.sourceSystem(), current.sourceScope(),
                current.endpointRef(), "/redirect", current.credentialRef(), current.trustProfileId(),
                current.authorizationProfileId(), current.networkPolicyId(), current.authenticationMethod(),
                current.credentialProvider(), current.requiredCapabilities(), true, current.mode()
        ));
        int hits = inventoryHits.get();
        ImportBatchDto batch = detail(importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals("FAILED", batch.status());
        assertEquals(hits, inventoryHits.get());
        assertTrue(redirectTargetHits.get() >= 1);
    }

    @Test
    void connectorSecurityReadinessContainsNoSecrets() {
        ResponseEntity<String> response = http.getForEntity("/api/v1/integration/connectors/security", String.class);
        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody().contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
        assertTrue(response.getBody().contains("ERICSSON_ENM_INT_INVENTORY_READER"));
    }

    private void bindEricsson(
            HeldCertificate root,
            AuthenticationMethod method,
            boolean enabled,
            List<X509Certificate> trusted,
            String hostname,
            int port
    ) {
        connectorRegistry.replaceTrust(new ConnectorTrustProfile(
                ConnectorRegistry.ERICSSON_TRUST,
                TrustMode.CUSTOM_CA,
                trusted.isEmpty() ? List.of(root.certificate()) : trusted,
                true,
                List.of(hostname),
                null
        ));
        connectorRegistry.replaceNetworkPolicy(new ConnectorNetworkPolicy(
                ConnectorRegistry.ERICSSON_NETWORK, List.of(hostname), Set.of(port, 443), true, false));
        endpointRegistry.register(new ConnectorEndpoint(
                ConnectorRegistry.ERICSSON_ENDPOINT_REF, URI.create("https://" + hostname + ":" + port)));
        ConnectorDefinition current = connectorRegistry.require(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER);
        connectorRegistry.replace(new ConnectorDefinition(
                current.connectorId(), current.vendor(), current.sourceSystem(), current.sourceScope(),
                current.endpointRef(), "/inventory", current.credentialRef(), current.trustProfileId(),
                current.authorizationProfileId(), current.networkPolicyId(), method,
                current.credentialProvider(), current.requiredCapabilities(), enabled, current.mode()
        ));
        connectorRegistry.replaceAuthorization(ConnectorAuthorizationProfile.readOnlyNetworkInventory());
        credentials.putUsernamePassword(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                validPassword(),
                "v1",
                Instant.now().plusSeconds(3600)
        );
    }

    private void startServer(HeldCertificate root, HeldCertificate serverCert, boolean requireClientAuth, char[] expectedPassword)
            throws Exception {
        startServer(root, serverCert, requireClientAuth, !requireClientAuth, expectedPassword);
    }

    private void startServer(
            HeldCertificate root,
            HeldCertificate serverCert,
            boolean requireClientAuth,
            boolean requireBasicAuth,
            char[] expectedPassword
    ) throws Exception {
        HandshakeCertificates.Builder builder = new HandshakeCertificates.Builder()
                .heldCertificate(serverCert, root.certificate())
                .addTrustedCertificate(root.certificate());
        HandshakeCertificates handshake = builder.build();
        server = new MockWebServer();
        javax.net.ssl.SSLSocketFactory socketFactory = handshake.sslSocketFactory();
        if (requireClientAuth) {
            socketFactory = new ClientAuthSslSocketFactory(socketFactory);
        }
        server.useHttps(socketFactory, false);
        String inventory = load("/integration/secure/ericsson-inventory.json");
        String expectedToken = "Basic " + Base64.getEncoder().encodeToString(
                ("ericsson-reader:" + new String(expectedPassword)).getBytes(StandardCharsets.UTF_8));
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("POST".equals(request.getMethod()) && "/lock".equals(request.getPath())) {
                    writeCalls.incrementAndGet();
                    return new MockResponse().setResponseCode(200);
                }
                if ("/redirect".equals(request.getPath())) {
                    redirectTargetHits.incrementAndGet();
                    return new MockResponse().setResponseCode(302).addHeader("Location", "https://evil.example/inventory");
                }
                if (!"/inventory".equals(request.getPath())) {
                    return new MockResponse().setResponseCode(404);
                }
                inventoryHits.incrementAndGet();
                if (requireBasicAuth) {
                    String auth = request.getHeader("Authorization");
                    if (auth == null || !auth.equals(expectedToken)) {
                        return new MockResponse().setResponseCode(401);
                    }
                }
                return new MockResponse().setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(inventory);
            }
        });
        server.start(InetAddress.getByName("localhost"), 0);
    }

    private void assertFailClosed(ImportBatchDto batch, ImportFailureCode expected) {
        assertEquals("FAILED", batch.status());
        assertEquals(expected.name(), batch.failureCode());
        assertEquals("UNREAD", batch.sourceSnapshotId());
        assertEquals(0, batch.entitiesCreated());
        assertEquals(0, batch.entitiesUpdated());
        assertCanaryAbsent(batch);
    }

    private CanonicalSnapshot canonicalSnapshot() {
        return new CanonicalSnapshot(
                cellRepository.findByCellId("CELL-E-SEC001").isPresent(),
                sourceReferenceRepository.findByCanonicalEntityIdOrderByCanonicalEntityTypeAsc("CELL-E-SEC001").size()
        );
    }

    private void assertCanonicalUnchanged(CanonicalSnapshot snapshot) {
        assertEquals(snapshot.cellPresent(), cellRepository.findByCellId("CELL-E-SEC001").isPresent());
        assertEquals(snapshot.sourceRefs(),
                sourceReferenceRepository.findByCanonicalEntityIdOrderByCanonicalEntityTypeAsc("CELL-E-SEC001").size());
    }

    private record CanonicalSnapshot(boolean cellPresent, int sourceRefs) {
    }

    private ImportBatchDto postConnector(String connectorId, Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<ImportBatchDto> response = http.postForEntity(
                "/api/v1/integration/imports/connectors/" + connectorId,
                new HttpEntity<>(body, headers),
                ImportBatchDto.class
        );
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private void assertCanaryAbsent(ImportBatchDto batch) {
        assertFalse(String.valueOf(batch.error()).contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
        ResponseEntity<String> detail = http.getForEntity("/api/v1/integration/imports/" + batch.importId(), String.class);
        assertFalse(detail.getBody().contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
    }

    private ImportBatchDto detail(com.simba.snip.npo.persist.NetworkImportBatchEntity entity) {
        return queryService.importDetail(entity.getId());
    }

    private static char[] validPassword() {
        return LocalDevelopmentCredentialProvider.CANARY_SECRET.toCharArray();
    }

    private static HeldCertificate ca(String name) {
        return new HeldCertificate.Builder().certificateAuthority(1).commonName(name).build();
    }

    private static HeldCertificate serverCert(HeldCertificate ca, String hostname) {
        return new HeldCertificate.Builder()
                .commonName(hostname)
                .addSubjectAlternativeName(hostname)
                .signedBy(ca)
                .build();
    }

    private static String load(String resource) throws IOException {
        try (InputStream in = ConnectorSecureImportApiTest.class.getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class ClientAuthSslSocketFactory extends javax.net.ssl.SSLSocketFactory {
        private final javax.net.ssl.SSLSocketFactory delegate;

        private ClientAuthSslSocketFactory(javax.net.ssl.SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        private java.net.Socket configure(java.net.Socket socket) {
            javax.net.ssl.SSLSocket ssl = (javax.net.ssl.SSLSocket) socket;
            ssl.setNeedClientAuth(true);
            return ssl;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public java.net.Socket createSocket(String host, int port) throws IOException {
            return configure(delegate.createSocket(host, port));
        }

        @Override
        public java.net.Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return configure(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public java.net.Socket createSocket(InetAddress host, int port) throws IOException {
            return configure(delegate.createSocket(host, port));
        }

        @Override
        public java.net.Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
                throws IOException {
            return configure(delegate.createSocket(address, port, localAddress, localPort));
        }

        @Override
        public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose) throws IOException {
            return configure(delegate.createSocket(s, host, port, autoClose));
        }
    }
}
