package com.simba.snip.npo.productioncampaign.evidence;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase18EvidenceMapAcceptanceTest {

    @Test
    void frozenEvidenceMapMatchesArchitectureAndRepositoryEvidence() {
        Path repo = repoRoot();
        Path map = repo.resolve("docs/implementation/phase18-gate-evidence-map.json");
        Path architecture = repo.resolve(
                "docs/architecture/SNIP-PHASE-18-PRODUCTION-NETWORK-CHANGE-CAMPAIGNS-PROGRESSIVE-DELIVERY-OPERATIONAL-SAFETY-GOVERNANCE-ARCHITECTURE.md");
        assertTrue(Files.exists(map));
        List<String> errors = new Phase18EvidenceMapValidator().validate(repo, map, architecture);
        assertTrue(errors.isEmpty(), () -> String.join("\n", errors));
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("docs/implementation/phase18-gate-evidence-map.json"))) {
            return cwd;
        }
        return cwd.getParent();
    }
}
