package com.simba.snip.npo.productionchange;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkTargetEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeMigrationSpecTest {

    private static final List<String> REQUIRED_TABLES = List.of(
            "production_network_target",
            "production_network_change",
            "production_change_review",
            "production_change_authorization",
            "production_change_control",
            "production_execution_grant",
            "production_gateway_attempt",
            "production_gateway_evidence",
            "production_execution_verification",
            "production_execution_recovery",
            "production_execution_rollback",
            "production_execution_lease",
            "production_target_health",
            "production_change_audit_event",
            "production_rate_limit_state"
    );

    @Test
    void targetEntityFields() {
        Set<String> names = Stream.of(ProductionNetworkTargetEntity.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertTrue(names.contains("targetId"));
        assertTrue(names.contains("vendor"));
        assertTrue(names.contains("platform"));
        assertTrue(names.contains("credentialProfileId"));
        assertTrue(names.contains("certificationLevel"));
        assertTrue(names.contains("targetState"));
        assertTrue(names.contains("targetFingerprint"));
        assertTrue(names.contains("expectedStateGuardStrength"));
        assertFalse(names.contains("secret"));
        assertFalse(names.contains("password"));
        assertFalse(names.contains("token"));
    }

    @Test
    void targetNoSecretColumns() throws IOException {
        String migration = Files.readString(v17());
        String targetBlock = migration.substring(
                migration.indexOf("CREATE TABLE production_network_target"),
                migration.indexOf("CREATE TABLE production_network_change"));
        String lower = targetBlock.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("secret"));
        assertFalse(lower.contains("password"));
        assertFalse(lower.contains("private_key"));
        assertFalse(lower.contains("access_token"));
        assertTrue(targetBlock.contains("credential_profile_id"));
    }

    @Test
    void distinctFromPhase15Execution() {
        assertNotSame(ProductionNetworkChangeEntity.class, NetworkChangeExecutionEntity.class);
        assertNotEquals(ProductionNetworkChangeEntity.class.getSimpleName(),
                NetworkChangeExecutionEntity.class.getSimpleName());
    }

    @Test
    void grantDistinctFromCredential() {
        Set<String> names = Stream.of(ProductionExecutionGrantEntity.class.getDeclaredFields())
                .map(Field::getName)
                .map(n -> n.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        assertFalse(names.contains("secret"));
        assertFalse(names.contains("password"));
        assertFalse(names.contains("credential"));
        assertTrue(names.contains("grantid"));
        assertTrue(names.contains("status"));
    }

    @Test
    void allTablesDefinedAndV17Absent() throws IOException {
        // Historical spec-time assertion was V17 absent. Implementation now requires V17 present.
        Path migration = v17();
        assertTrue(Files.exists(migration), "V17 migration must exist after implementation authorization");
        String sql = Files.readString(migration);
        for (String table : REQUIRED_TABLES) {
            assertTrue(sql.contains("CREATE TABLE " + table), "missing table " + table);
        }
        assertTrue(sql.contains("VARCHAR(64)"));
        assertFalse(sql.replace("VARCHAR(64)", "").contains("CHAR(64)"));
        int hashChecks = 0;
        int from = 0;
        while (true) {
            int idx = sql.indexOf("chk_sha256_", from);
            if (idx < 0) {
                break;
            }
            hashChecks++;
            from = idx + 1;
        }
        assertTrue(hashChecks == 13, "expected 13 SHA-256 CHECK constraints, found " + hashChecks);
        assertTrue(sql.contains("^[0-9a-f]{64}$"));
        assertTrue(sql.contains("^[0-9a-fA-F]{64}$"),
                "Phase 14/15 fingerprints are frozen uppercase SHA-256 and must remain 64-hex");
    }

    private static Path v17() {
        return ProductionChangeSourcePaths.appMainResources()
                .resolve("db/migration/V17__phase16_production_change_execution.sql");
    }
}
