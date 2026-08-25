package com.simba.snip.npo.api;

import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.integration.FixtureKind;
import com.simba.snip.npo.integration.NetworkImportQueryService;
import com.simba.snip.npo.integration.NetworkImportService;
import com.simba.snip.npo.integration.security.ConnectorSecurityQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
public class IntegrationController {

    private final NetworkImportService importService;
    private final NetworkImportQueryService queryService;
    private final ConnectorSecurityQueryService securityQueryService;

    public IntegrationController(
            NetworkImportService importService,
            NetworkImportQueryService queryService,
            ConnectorSecurityQueryService securityQueryService
    ) {
        this.importService = importService;
        this.queryService = queryService;
        this.securityQueryService = securityQueryService;
    }

    @PostMapping("/api/v1/integration/imports/ericsson")
    public ImportBatchDto importEricsson(@RequestBody(required = false) CreateImportRequest request) {
        return queryService.importDetail(importService.importEricsson(parseKind(request)).getId());
    }

    @PostMapping("/api/v1/integration/imports/nokia")
    public ImportBatchDto importNokia(@RequestBody(required = false) CreateImportRequest request) {
        return queryService.importDetail(importService.importNokia(parseKind(request)).getId());
    }

    @PostMapping("/api/v1/integration/imports/connectors/{connectorId}")
    public ImportBatchDto importConnector(
            @PathVariable String connectorId,
            @RequestBody(required = false) java.util.Map<String, Object> ignored
    ) {
        return queryService.importDetail(importService.importSecure(connectorId).getId());
    }

    @GetMapping("/api/v1/integration/connectors/security")
    public java.util.List<java.util.Map<String, Object>> connectorSecurity() {
        return securityQueryService.readiness();
    }

    @GetMapping("/api/v1/integration/imports/{importId}/security-audit")
    public java.util.List<java.util.Map<String, Object>> securityAudit(@PathVariable UUID importId) {
        return securityQueryService.audit(importId);
    }

    @GetMapping("/api/v1/integration/imports")
    public List<ImportBatchDto> imports() {
        return queryService.listImports();
    }

    @GetMapping("/api/v1/integration/imports/{importId}")
    public ImportBatchDto importDetail(@PathVariable UUID importId) {
        return queryService.importDetail(importId);
    }

    @GetMapping("/api/v1/integration/imports/{importId}/checkpoints")
    public List<ImportCheckpointDto> checkpoints(@PathVariable UUID importId) {
        return queryService.checkpoints(importId);
    }

    @GetMapping("/api/v1/integration/health")
    public java.util.Map<String, Object> integrationHealth() {
        return queryService.runtimeHealth();
    }

    @GetMapping("/api/v1/integration/conflicts")
    public List<ImportConflictDto> conflicts() {
        return queryService.listConflicts();
    }

    @GetMapping("/api/v1/integration/conflicts/{conflictId}")
    public ImportConflictDto conflict(@PathVariable UUID conflictId) {
        return queryService.conflict(conflictId);
    }

    @GetMapping("/api/v1/integration/rejections")
    public List<ImportRejectionDto> rejections() {
        return queryService.listRejections();
    }

    private static FixtureKind parseKind(CreateImportRequest request) {
        if (request == null || request.fixtureKind() == null || request.fixtureKind().isBlank()) {
            return FixtureKind.NORMAL;
        }
        try {
            return FixtureKind.valueOf(request.fixtureKind().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DomainValidationException("unsupported fixture kind: " + request.fixtureKind());
        }
    }
}
