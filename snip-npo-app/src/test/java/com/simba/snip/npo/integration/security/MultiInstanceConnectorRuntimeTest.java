package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.domain.ImportBusyException;
import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportLease;
import com.simba.snip.npo.integration.ImportLeaseService;
import com.simba.snip.npo.integration.ImportRuntimeException;
import com.simba.snip.npo.integration.NetworkImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(InMemoryAzureKeyVaultTestConfiguration.class)
class MultiInstanceConnectorRuntimeTest extends AbstractPostgresIT {

    @DynamicPropertySource
    static void replicaA(DynamicPropertyRegistry registry) {
        registry.add("snip.integration.security.local-credentials-enabled", () -> "false");
        registry.add("snip.integration.security.production-runtime", () -> "true");
        registry.add("snip.integration.security.azure-key-vault.enabled", () -> "true");
        registry.add("snip.integration.security.azure-key-vault.vault-uri",
                () -> "https://snip-phase10-int.vault.azure.net");
        registry.add("snip.integration.security.azure-key-vault.authentication", () -> "WORKLOAD_IDENTITY");
        registry.add("snip.integration.instance-id", () -> "replica-a");
    }

    @Autowired
    private ImportLeaseService leaseA;

    @Autowired
    private ConnectorRegistry registryA;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void independentlyInstantiatedReplicaDoesNotResolveSecretWithoutLease() {
        try (ConfigurableApplicationContext replicaB = startReplica("replica-b")) {
            NetworkImportService importB = replicaB.getBean(NetworkImportService.class);
            ConnectorRegistry registryB = replicaB.getBean(ConnectorRegistry.class);
            InMemoryAzureKeyVaultSecretAccessor vaultB =
                    (InMemoryAzureKeyVaultSecretAccessor) replicaB.getBean(AzureKeyVaultSecretAccessor.class);
            registryA.enable(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER, true);
            registryB.enable(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER, true);
            put(vaultB);
            vaultB.resetGets();
            UUID owner = persistRequested("ERICSSON_SECURE_MOCK", "ERICSSON");
            ImportLease held = leaseA.acquire("ERICSSON_SECURE_MOCK", "DEFAULT", owner, "replica-a").orElseThrow();
            int before = vaultB.gets();
            assertThrows(ImportBusyException.class,
                    () -> importB.importSecure(ConnectorDefinition.ERICSSON_ENM_INT_INVENTORY_READER));
            assertEquals(before, vaultB.gets());
            leaseA.release(held);
        }
    }

    @Test
    void differentScopesProceedIndependently() {
        try (ConfigurableApplicationContext replicaB = startReplica("replica-b-scope")) {
            ImportLeaseService leaseB = replicaB.getBean(ImportLeaseService.class);
            InMemoryAzureKeyVaultSecretAccessor vaultB =
                    (InMemoryAzureKeyVaultSecretAccessor) replicaB.getBean(AzureKeyVaultSecretAccessor.class);
            ConnectorRegistry registryB = replicaB.getBean(ConnectorRegistry.class);
            registryB.enable(ConnectorDefinition.NOKIA_NETACT_INT_INVENTORY_READER, true);
            vaultB.put(
                    "snip-int-nokia-inventory-reader",
                    "nokia-v1",
                    "{\"username\":\"nokia-reader\",\"password\":\"unused\"}",
                    true
            );
            UUID ericssonOwner = persistRequested("ERICSSON_SECURE_MOCK", "ERICSSON");
            ImportLease ericsson = leaseA.acquire(
                    "ERICSSON_SECURE_MOCK", "DEFAULT", ericssonOwner, "replica-a").orElseThrow();
            UUID nokiaExec = persistRequested("NOKIA_SECURE_MOCK", "NOKIA");
            ImportLease nokia = leaseB.acquire(
                    "NOKIA_SECURE_MOCK", "DEFAULT", nokiaExec, "replica-b-scope").orElseThrow();
            assertEquals("replica-a", ericsson.ownerInstanceId());
            assertEquals("replica-b-scope", nokia.ownerInstanceId());
            assertNotEquals(ericsson.leaseKey(), nokia.leaseKey());
            leaseA.release(ericsson);
            leaseB.release(nokia);
        }
    }

    @Test
    void expiredOwnerCannotCommitAfterSuccessorTakesLease() {
        try (ConfigurableApplicationContext replicaB = startReplica("replica-b-recovery")) {
            ImportLeaseService leaseB = replicaB.getBean(ImportLeaseService.class);
            UUID staleExec = persistRequested("ERICSSON_SECURE_MOCK", "ERICSSON");
            ImportLease stale = leaseA.acquire(
                    "ERICSSON_SECURE_MOCK", "DEFAULT", staleExec, "replica-a").orElseThrow();
            jdbc.update(
                    "UPDATE network_import_lease SET expires_at = ? WHERE lease_key = ?",
                    Timestamp.from(Instant.now().minusSeconds(60)),
                    stale.leaseKey()
            );
            leaseB.recoverExpired("ERICSSON_SECURE_MOCK", "DEFAULT");
            UUID successorExec = persistRequested("ERICSSON_SECURE_MOCK", "ERICSSON");
            ImportLease successor = leaseB.acquire(
                    "ERICSSON_SECURE_MOCK", "DEFAULT", successorExec, "replica-b-recovery").orElseThrow();
            assertTrue(successor.fencingToken() > stale.fencingToken());
            ImportRuntimeException lost = assertThrows(
                    ImportRuntimeException.class, () -> leaseA.assertOwnership(stale));
            assertEquals(ImportFailureCode.LEASE_LOST, lost.failureCode());
            leaseB.release(successor);
        }
    }

    private static void put(InMemoryAzureKeyVaultSecretAccessor vault) {
        vault.put(
                "snip-int-ericsson-inventory-reader",
                "v-multi",
                "{\"username\":\"ericsson-reader\",\"password\":\""
                        + LocalDevelopmentCredentialProvider.CANARY_SECRET + "\"}",
                true
        );
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
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()),
                "replica-a"
        );
        return id;
    }

    private ConfigurableApplicationContext startReplica(String instanceId) {
        return new SpringApplicationBuilder(InMemoryAzureKeyVaultTestConfiguration.class, NpoApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.main.web-application-type=none",
                        "--snip.integration.instance-id=" + instanceId,
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--snip.integration.security.production-runtime=true",
                        "--snip.integration.security.local-credentials-enabled=false",
                        "--snip.integration.security.azure-key-vault.enabled=true",
                        "--snip.integration.security.azure-key-vault.vault-uri=https://snip-phase10-int.vault.azure.net",
                        "--snip.integration.security.azure-key-vault.authentication=WORKLOAD_IDENTITY",
                        "--snip.generator=stub"
                );
    }
}
