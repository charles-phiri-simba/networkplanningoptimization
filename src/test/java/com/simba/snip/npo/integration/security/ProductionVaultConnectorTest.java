package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.api.ImportBatchDto;
import com.simba.snip.npo.domain.ImportBusyException;
import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportLeaseService;
import com.simba.snip.npo.integration.NetworkImportQueryService;
import com.simba.snip.npo.integration.NetworkImportService;
import com.simba.snip.npo.persist.CellRepository;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(InMemoryAzureKeyVaultTestConfiguration.class)
class ProductionVaultConnectorTest extends AbstractPostgresIT {

    @DynamicPropertySource
    static void productionVault(DynamicPropertyRegistry registry) {
        registry.add("snip.integration.security.local-credentials-enabled", () -> "false");
        registry.add("snip.integration.security.production-runtime", () -> "true");
        registry.add("snip.integration.security.network-policy-configured", () -> "true");
        registry.add("snip.integration.security.azure-key-vault.enabled", () -> "true");
        registry.add("snip.integration.security.azure-key-vault.vault-uri",
                () -> "https://snip-phase10-int.vault.azure.net");
        registry.add("snip.integration.security.azure-key-vault.authentication", () -> "WORKLOAD_IDENTITY");
        registry.add("snip.integration.instance-id", () -> "replica-a");
    }

    @Autowired
    private NetworkImportService importService;

    @Autowired
    private NetworkImportQueryService queryService;

    @Autowired
    private ConnectorRegistry connectorRegistry;

    @Autowired
    private ConnectorEndpointRegistry endpointRegistry;

    @Autowired
    private AzureKeyVaultSecretAccessor vaultAccessor;

    @Autowired
    private LocalDevelopmentCredentialProvider localCredentials;

    @Autowired
    private ImportLeaseService leaseService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CellRepository cellRepository;

    @Autowired
    private NetworkSourceReferenceRepository sourceReferenceRepository;

    @Autowired
    private TestRestTemplate http;

    private MockWebServer server;
    private final AtomicInteger writeCalls = new AtomicInteger();

    @AfterEach
    void stopServer() throws IOException {
        if (server != null) {
            server.shutdown();
            server = null;
        }
        inMemory().resetGets();
        inMemory().forceFailure(null);
        leaseService.find("ERICSSON_SECURE_MOCK", "DEFAULT").ifPresent(leaseService::release);
        leaseService.find("NOKIA_SECURE_MOCK", "DEFAULT").ifPresent(leaseService::release);
    }

    @Test
    void vaultBackedImportCreatesCanonicalStateAndRedactsSecret() throws Exception {
        HeldCertificate root = ca("vault-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert);
        bindEricsson(root, server.getPort());
        putLatest("v-import", LocalDevelopmentCredentialProvider.CANARY_SECRET);
        ImportBatchDto batch = queryService.importDetail(
                importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER).getId());
        assertEquals("COMPLETED", batch.status());
        assertTrue("NEW".equals(batch.executionType()) || "REPLAY".equals(batch.executionType()));
        assertTrue(cellRepository.findByCellId("CELL-E-SEC001").isPresent());
        assertEquals(0, writeCalls.get());
        assertFalse(String.valueOf(batch.error()).contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
        ResponseEntity<String> audit = http.getForEntity(
                "/api/v1/integration/imports/" + batch.importId() + "/security-audit", String.class);
        assertFalse(audit.getBody().contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
        assertTrue(audit.getBody().contains("AZURE_KEY_VAULT"));
        assertTrue(inMemory().gets() >= 1);
    }

    @Test
    void localStoreIsIgnoredWhenVaultFails() throws Exception {
        HeldCertificate root = ca("nofallback-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert);
        bindEricsson(root, server.getPort());
        localCredentials.putUsernamePassword(
                ConnectorRegistry.ERICSSON_CREDENTIAL_REF,
                ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER,
                "ericsson-reader",
                LocalDevelopmentCredentialProvider.CANARY_SECRET.toCharArray(),
                "local-ignored",
                java.time.Instant.now().plusSeconds(3600)
        );
        inMemory().forceFailure(ImportFailureCode.VAULT_SECRET_NOT_FOUND);
        ImportBatchDto batch = queryService.importDetail(
                importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER).getId());
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VAULT_SECRET_NOT_FOUND.name(), batch.failureCode());
        assertEquals("UNREAD", batch.sourceSnapshotId());
        assertEquals(0, batch.entitiesCreated());
        assertFalse(String.valueOf(batch.error()).contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
    }

    @Test
    void leaseMissDoesNotResolveVaultSecret() {
        connectorRegistry.enable(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER, true);
        putLatest("v-lease", LocalDevelopmentCredentialProvider.CANARY_SECRET);
        inMemory().resetGets();
        UUID owner = persistRequested("ERICSSON_SECURE_MOCK", "ERICSSON");
        assertTrue(leaseService.acquire("ERICSSON_SECURE_MOCK", "DEFAULT", owner, "foreign-replica").isPresent());
        int before = inMemory().gets();
        assertThrows(ImportBusyException.class,
                () -> importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
        assertEquals(before, inMemory().gets());
        leaseService.find("ERICSSON_SECURE_MOCK", "DEFAULT")
                .ifPresent(leaseService::release);
    }

    @Test
    void disabledSecretFailsClosedWithZeroCanonicalMutation() throws Exception {
        HeldCertificate root = ca("disabled-ca");
        HeldCertificate serverCert = serverCert(root, "localhost");
        startServer(root, serverCert);
        bindEricsson(root, server.getPort());
        putLatest("v-old", LocalDevelopmentCredentialProvider.CANARY_SECRET);
        putLatest("v-new", "unused-newer");
        inMemory().disableLatest("snip-int-ericsson-inventory-reader");
        boolean cellBefore = cellRepository.findByCellId("CELL-E-SEC001").isPresent();
        ImportBatchDto batch = queryService.importDetail(
                importService.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER).getId());
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VAULT_SECRET_DISABLED.name(), batch.failureCode());
        assertEquals("UNREAD", batch.sourceSnapshotId());
        assertEquals(cellBefore, cellRepository.findByCellId("CELL-E-SEC001").isPresent());
        assertEquals(0, writeCalls.get());
    }

    @Test
    void healthAndReadinessDoNotCallVault() {
        putLatest("v-health", LocalDevelopmentCredentialProvider.CANARY_SECRET);
        inMemory().resetGets();
        ResponseEntity<String> health = http.getForEntity("/api/v1/integration/health", String.class);
        ResponseEntity<String> readiness = http.getForEntity("/api/v1/integration/connectors/security", String.class);
        assertEquals(200, health.getStatusCode().value());
        assertEquals(200, readiness.getStatusCode().value());
        assertTrue(health.getBody().contains("AZURE_KEY_VAULT"));
        assertTrue(readiness.getBody().contains("workloadIdentityConfigured"));
        assertTrue(readiness.getBody().contains("vaultConfigured"));
        assertTrue(readiness.getBody().contains("networkPolicyConfigured"));
        assertFalse(health.getBody().contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
        assertFalse(readiness.getBody().contains(LocalDevelopmentCredentialProvider.CANARY_SECRET));
        assertEquals(0, inMemory().gets());
    }

    private UUID persistRequested(String sourceSystem, String vendor) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO network_import_batch (
                    id, source_system, vendor, source_snapshot_id, vendor_schema_version, fixture_kind,
                    started_at, status, entities_read, entities_created, entities_updated, entities_unchanged,
                    entities_rejected, conflicts_detected, missing_entities_detected, execution_type,
                    attempt_number, source_scope, requested_at, owner_instance_id
                ) VALUES (?, ?, ?, 'UNREAD', 'TEST', 'NORMAL', ?, 'REQUESTED', 0, 0, 0, 0, 0, 0, 0, 'NEW', 1, 'DEFAULT', ?, ?)
                """,
                id,
                sourceSystem,
                vendor,
                java.sql.Timestamp.from(java.time.Instant.now()),
                java.sql.Timestamp.from(java.time.Instant.now()),
                "foreign-replica"
        );
        return id;
    }

    private void bindEricsson(HeldCertificate root, int port) {
        connectorRegistry.replaceTrust(new ConnectorTrustProfile(
                ConnectorRegistry.ERICSSON_TRUST,
                TrustMode.CUSTOM_CA,
                List.of(root.certificate()),
                true,
                List.of("localhost"),
                null
        ));
        connectorRegistry.replaceNetworkPolicy(new ConnectorNetworkPolicy(
                ConnectorRegistry.ERICSSON_NETWORK, List.of("localhost"), Set.of(port, 443), true, false));
        endpointRegistry.register(new ConnectorEndpoint(
                ConnectorRegistry.ERICSSON_ENDPOINT_REF, URI.create("https://localhost:" + port)));
        connectorRegistry.enable(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER, true);
        ConnectorDefinition current = connectorRegistry.require(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER);
        assertEquals(CredentialProviderType.AZURE_KEY_VAULT, current.credentialProvider());
    }

    private void putLatest(String version, String password) {
        inMemory().put(
                "snip-int-ericsson-inventory-reader",
                version,
                "{\"username\":\"ericsson-reader\",\"password\":\"" + password + "\"}",
                true
        );
    }

    private InMemoryAzureKeyVaultSecretAccessor inMemory() {
        return (InMemoryAzureKeyVaultSecretAccessor) vaultAccessor;
    }

    private void startServer(HeldCertificate root, HeldCertificate serverCert) throws Exception {
        HandshakeCertificates handshake = new HandshakeCertificates.Builder()
                .heldCertificate(serverCert, root.certificate())
                .addTrustedCertificate(root.certificate())
                .build();
        server = new MockWebServer();
        server.useHttps(handshake.sslSocketFactory(), false);
        String inventory;
        try (InputStream in = ProductionVaultConnectorTest.class.getResourceAsStream(
                "/integration/secure/ericsson-inventory.json")) {
            inventory = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String expectedToken = "Basic " + Base64.getEncoder().encodeToString(
                ("ericsson-reader:" + LocalDevelopmentCredentialProvider.CANARY_SECRET)
                        .getBytes(StandardCharsets.UTF_8));
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if ("POST".equals(request.getMethod()) && "/lock".equals(request.getPath())) {
                    writeCalls.incrementAndGet();
                    return new MockResponse().setResponseCode(200);
                }
                if (!"/inventory".equals(request.getPath())) {
                    return new MockResponse().setResponseCode(404);
                }
                String auth = request.getHeader("Authorization");
                if (auth == null || !auth.equals(expectedToken)) {
                    return new MockResponse().setResponseCode(401);
                }
                return new MockResponse().setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody(inventory);
            }
        });
        server.start(InetAddress.getByName("localhost"), 0);
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
}
