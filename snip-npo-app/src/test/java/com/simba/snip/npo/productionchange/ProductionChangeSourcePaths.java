package com.simba.snip.npo.productionchange;

import java.nio.file.Files;
import java.nio.file.Path;

final class ProductionChangeSourcePaths {

    private ProductionChangeSourcePaths() {
    }

    static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("snip-npo-app")) && Files.exists(cwd.resolve("production-write-gateway"))) {
            return cwd;
        }
        if (cwd.getFileName() != null && "snip-npo-app".equals(cwd.getFileName().toString())) {
            return cwd.getParent();
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("snip-npo-app"))) {
            return parent;
        }
        return cwd;
    }

    static Path appMainJava() {
        return repoRoot().resolve("snip-npo-app/src/main/java");
    }

    static Path appMainResources() {
        return repoRoot().resolve("snip-npo-app/src/main/resources");
    }

    static Path gatewayMainJava() {
        return repoRoot().resolve("production-write-gateway/src/main/java");
    }

    static Path protocolPom() {
        return repoRoot().resolve("production-change-protocol/pom.xml");
    }
}
