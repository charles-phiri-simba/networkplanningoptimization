package com.simba.snip.npo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoResetScriptContractTest {

    @Test
    void resetScriptsEncodeOwnershipAndDualConfirmation() throws IOException {
        String ps1 = Files.readString(repoRoot().resolve("scripts/snip-demo-reset.ps1"));
        String sh = Files.readString(repoRoot().resolve("scripts/snip-demo-reset.sh"));
        for (String body : List.of(ps1, sh)) {
            assertTrue(body.contains("SNIP_DEMO_RESET"));
            assertTrue(body.contains("SNIP_DEMO_RESET_CONFIRM"));
            assertTrue(body.contains("snip-demo"));
            assertTrue(body.contains("snip-postgres"));
            assertTrue(body.contains("com.docker.compose.project"));
            assertTrue(body.contains("com.docker.compose.volume"));
            assertTrue(body.contains("docker-compose.yml"));
            assertTrue(body.contains("snip-npo-app"));
            assertTrue(body.contains("no such volume") || body.contains("already absent"));
            assertTrue(body.contains("Docker daemon is unavailable") || body.contains("docker info"));
            assertTrue(body.contains("docker volume inspect failed") || body.contains("inspect failed"));
            assertTrue(body.contains("volume labels do not prove"));
            assertFalse(body.contains("docker volume prune"));
            assertFalse(body.contains("docker system prune"));
        }
        assertTrue(ps1.contains("StringComparison]::Ordinal"));
        assertTrue(ps1.contains("Docker daemon is unavailable"));
        assertTrue(ps1.contains("docker volume inspect failed"));
        assertTrue(sh.contains("no such volume"));
        assertTrue(sh.contains("Docker daemon is unavailable"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void missingResetEnvExitsWithoutDestruction() throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-File",
                repoRoot().resolve("scripts/snip-demo-reset.ps1").toString()
        );
        builder.directory(repoRoot().toFile());
        builder.environment().remove("SNIP_DEMO_RESET");
        builder.environment().remove("SNIP_DEMO_RESET_CONFIRM");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        assertTrue(finished);
        assertEquals(2, process.exitValue());
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("docker-compose.yml")) && Files.exists(cwd.resolve("snip-npo-app"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("docker-compose.yml"))) {
            return parent;
        }
        throw new IllegalStateException("cannot locate repository root from " + cwd);
    }
}
