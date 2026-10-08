package com.simba.snip.npo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoWindowsToolingTest {

    @Test
    void windowsScriptsEncodePortMappingLoggingAndResetGuards() throws IOException {
        String up = Files.readString(repoRoot().resolve("scripts/snip-demo-up.ps1"));
        String reset = Files.readString(repoRoot().resolve("scripts/snip-demo-reset.ps1"));
        String ready = Files.readString(repoRoot().resolve("scripts/snip-demo-ready.ps1"));

        assertTrue(up.contains("SERVER_PORT"));
        assertTrue(up.contains("SPRING_DATASOURCE_URL"));
        assertTrue(up.contains("SNIP_API_TARGET"));
        assertTrue(up.contains("SNIP_DB_PORT"));
        assertTrue(up.contains("SNIP_HOST_PORT"));
        assertTrue(up.contains("jdbc:postgresql://127.0.0.1:$DbPort/snip"));
        assertTrue(up.contains("-pl snip-npo-app"));
        assertTrue(up.contains("spring-boot:run"));
        assertTrue(up.contains("spring-boot.run.workingDirectory"));
        assertTrue(up.contains("spring-boot.run.profiles=demo"));
        assertTrue(up.contains("HasExited"));
        assertTrue(up.contains("api-$runStamp.log") || up.contains("api-$runStamp"));
        assertTrue(up.contains("testdata\\kpis.json") || up.contains("testdata/kpis.json"));
        assertTrue(up.contains("KPI file is missing"));
        assertTrue(up.contains("Write-DemoLogExcerpt"));
        assertTrue(up.contains("Resolve-SnipJava17Home"));
        assertTrue(up.contains("UseShellExecute = $false"));
        assertTrue(up.contains("startup category"));
        assertTrue(up.contains("process status"));
        assertTrue(up.contains("backend health failure"));
        assertTrue(up.contains("log file location"));
        assertTrue(up.contains("checksum mismatch"));
        assertTrue(up.contains("SERVER_PORT = \"$ApiPort\""));
        assertFalse(up.contains("-f snip-npo-app"));
        assertFalse(up.contains("foreach ($home in"));
        assertFalse(up.contains("docker volume prune"));
        assertFalse(up.contains("docker system prune"));
        assertFalse(up.contains("Get-Content Env:"));

        assertTrue(reset.contains("StringComparison]::Ordinal"));
        assertTrue(reset.contains("no such volume"));
        assertTrue(reset.contains("Docker daemon is unavailable"));
        assertTrue(reset.contains("docker volume inspect failed"));
        assertTrue(reset.contains("com.docker.compose.project"));
        assertTrue(reset.contains("com.docker.compose.volume"));
        assertTrue(reset.contains("volume labels do not prove snip-demo/snip-postgres ownership"));
        assertFalse(reset.contains("docker volume prune"));
        assertFalse(reset.contains("docker system prune"));

        assertTrue(ready.contains("SNIP_HOST_PORT"));
        assertTrue(ready.contains("SNIP_API_TARGET"));
        assertTrue(ready.contains("/api/v1/twins/"));
        assertTrue(ready.contains("scopeId"));
        assertTrue(ready.contains("CURRENT"));
        assertTrue(ready.contains("stale"));
        assertTrue(ready.contains("production-change"));
        assertTrue(ready.contains("Local file is not authoritative"));
        assertTrue(ready.contains("CURRENT is not proven by the file") || ready.contains("not proven by the file"));
        assertFalse(ready.contains("api.pid"));
    }

    @Test
    void posixScriptsSharePortMappingAndStaleTwinGuard() throws IOException {
        String up = Files.readString(repoRoot().resolve("scripts/snip-demo-up.sh"));
        String ready = Files.readString(repoRoot().resolve("scripts/snip-demo-ready.sh"));
        String reset = Files.readString(repoRoot().resolve("scripts/snip-demo-reset.sh"));
        assertTrue(up.contains("SERVER_PORT"));
        assertTrue(up.contains("SPRING_DATASOURCE_URL"));
        assertTrue(up.contains("startup category"));
        assertTrue(up.contains("-pl snip-npo-app"));
        assertTrue(up.contains("spring-boot.run.workingDirectory"));
        assertTrue(up.contains("testdata/kpis.json"));
        assertTrue(up.contains("KPI file is missing"));
        assertTrue(up.contains("api-${RUN_STAMP}.log"));
        assertTrue(up.contains("checksum mismatch"));
        assertFalse(up.contains("-f snip-npo-app/pom.xml"));
        assertTrue(ready.contains("SNIP_HOST_PORT"));
        assertTrue(ready.contains("scopeId"));
        assertTrue(ready.contains("not proven by the file"));
        assertTrue(reset.contains("Docker daemon is unavailable"));
        assertTrue(reset.contains("no such volume"));
        assertTrue(reset.contains("docker volume inspect failed"));
        assertTrue(reset.contains("volume labels do not prove"));
    }

    @Test
    void repoRootKpiFileExistsForJvmWorkingDirectoryContract() throws IOException {
        Path kpi = repoRoot().resolve("testdata/kpis.json");
        assertTrue(Files.isRegularFile(kpi), "snip.kpi-file=testdata/kpis.json must exist at repository root");
        assertFalse(Files.exists(repoRoot().resolve("snip-npo-app/testdata/kpis.json")));
        String yml = Files.readString(repoRoot().resolve("snip-npo-app/src/main/resources/application.yml"));
        assertTrue(yml.contains("kpi-file: testdata/kpis.json"));
        String repository = Files.readString(
                repoRoot().resolve("snip-npo-app/src/main/java/com/simba/snip/npo/context/KpiRepository.java"));
        assertTrue(repository.contains("Path.of(properties.getKpiFile())"));
    }

    @Test
    void currentRunLogsDoNotReuseSharedApiLogFile() throws IOException {
        String upPs1 = Files.readString(repoRoot().resolve("scripts/snip-demo-up.ps1"));
        String upSh = Files.readString(repoRoot().resolve("scripts/snip-demo-up.sh"));
        assertTrue(upPs1.contains("api-$runStamp.log"));
        assertTrue(upSh.contains("api-${RUN_STAMP}.log"));
        assertFalse(upPs1.contains("$apiLog = Join-Path $logDir 'api.log'"));
        assertFalse(upSh.contains("API_LOG=\"$LOG_DIR/api.log\""));
        int flywayPs1 = upPs1.indexOf("checksum mismatch");
        int excerptFn = upPs1.indexOf("function Write-DemoLogExcerpt");
        assertTrue(excerptFn >= 0 && flywayPs1 > excerptFn);
        assertTrue(upPs1.contains("Get-Content $Path -Raw"));
        assertFalse(upPs1.contains("Get-Content $apiLog -Raw"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void lowercaseResetConfirmationIsRejected() throws Exception {
        int code = runReset(Map.of(
                "SNIP_DEMO_RESET", "yes",
                "SNIP_DEMO_RESET_CONFIRM", "snip-demo"
        ));
        assertEquals(2, code);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void missingResetConfirmationIsRejected() throws Exception {
        int code = runReset(Map.of("SNIP_DEMO_RESET", "YES"));
        assertEquals(2, code);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void wrongCaseConfirmTokenIsRejected() throws Exception {
        int code = runReset(Map.of(
                "SNIP_DEMO_RESET", "YES",
                "SNIP_DEMO_RESET_CONFIRM", "SNIP-DEMO"
        ));
        assertEquals(2, code);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void absentVolumeWithExactConfirmExitsZeroWhenVolumeMissing() throws Exception {
        Process inspect = new ProcessBuilder("docker", "volume", "inspect", "snip-demo_snip-postgres")
                .redirectErrorStream(true)
                .start();
        boolean finished = inspect.waitFor(20, TimeUnit.SECONDS);
        assertTrue(finished);
        if (inspect.exitValue() == 0) {
            return;
        }
        int code = runReset(Map.of(
                "SNIP_DEMO_RESET", "YES",
                "SNIP_DEMO_RESET_CONFIRM", "snip-demo"
        ));
        assertEquals(0, code);
    }

    private static int runReset(Map<String, String> extraEnv) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-File",
                repoRoot().resolve("scripts/snip-demo-reset.ps1").toString()
        );
        builder.directory(repoRoot().toFile());
        builder.environment().remove("SNIP_DEMO_RESET");
        builder.environment().remove("SNIP_DEMO_RESET_CONFIRM");
        builder.environment().putAll(extraEnv);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS));
        return process.exitValue();
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
