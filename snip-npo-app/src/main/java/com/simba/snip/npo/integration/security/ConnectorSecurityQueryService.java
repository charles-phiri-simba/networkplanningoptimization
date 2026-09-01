package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.config.ConnectorSecurityProperties;
import com.simba.snip.npo.persist.ConnectorSecurityAuditEventEntity;
import com.simba.snip.npo.persist.ConnectorSecurityAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConnectorSecurityQueryService {

    private final ConnectorRegistry registry;
    private final ConnectorSecurityProperties properties;
    private final AzureKeyVaultCredentialProvider azureKeyVaultCredentialProvider;
    private final ConnectorSecurityAuditEventRepository auditRepository;

    public ConnectorSecurityQueryService(
            ConnectorRegistry registry,
            ConnectorSecurityProperties properties,
            AzureKeyVaultCredentialProvider azureKeyVaultCredentialProvider,
            ConnectorSecurityAuditEventRepository auditRepository
    ) {
        this.registry = registry;
        this.properties = properties;
        this.azureKeyVaultCredentialProvider = azureKeyVaultCredentialProvider;
        this.auditRepository = auditRepository;
    }

    public List<Map<String, Object>> readiness() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ConnectorDefinition definition : registry.all()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("connectorId", definition.connectorId());
            row.put("enabled", definition.enabled());
            row.put("credentialConfigStatus", credentialStatus(definition));
            row.put("trustConfigStatus", registry.trust(definition.trustProfileId()) == null ? "UNAVAILABLE" : "READY");
            row.put("authorizationStatus", registry.authorization(definition.authorizationProfileId()) == null
                    ? "UNAVAILABLE" : "READY");
            row.put("networkPolicyStatus", registry.networkPolicy(definition.networkPolicyId()) == null
                    ? "UNAVAILABLE" : "READY");
            ConnectorReadinessStatus overall = overall(definition);
            row.put("overallSecurityStatus", overall.name());
            row.put("credentialProviderMode", definition.credentialProvider().name());
            row.put("workloadIdentityConfigured", azureKeyVaultCredentialProvider.workloadIdentityConfigured());
            row.put("vaultConfigured", azureKeyVaultCredentialProvider.configured());
            row.put("networkPolicyConfigured", properties.isNetworkPolicyConfigured());
            row.put("connectorSecurityStatus", overall.name());
            row.put("accessMode", "READ_ONLY");
            row.put("implementationType", definition.mode().name());
            row.put("transportConfigured", definition.mode() != ConnectorMode.REAL);
            row.put("liveInventoryProbed", false);
            rows.add(row);
        }
        return rows;
    }

    public List<Map<String, Object>> audit(UUID executionId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ConnectorSecurityAuditEventEntity event : auditRepository.findByExecutionIdOrderByOccurredAtAsc(executionId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("eventId", event.getEventId().toString());
            row.put("eventType", event.getEventType());
            row.put("occurredAt", event.getOccurredAt().toString());
            row.put("credentialRef", event.getCredentialRef());
            row.put("credentialVersion", event.getCredentialVersion());
            row.put("failureCode", event.getFailureCode());
            row.put("details", event.getDetails());
            rows.add(row);
        }
        return rows;
    }

    private String credentialStatus(ConnectorDefinition definition) {
        if (definition.mode() == ConnectorMode.SIMULATOR) {
            return "NOT_REQUIRED";
        }
        if (definition.credentialProvider() == CredentialProviderType.AZURE_KEY_VAULT) {
            return azureKeyVaultCredentialProvider.configured() ? "READY" : "UNAVAILABLE";
        }
        return properties.isLocalCredentialsEnabled() ? "READY" : "UNAVAILABLE";
    }

    private ConnectorReadinessStatus overall(ConnectorDefinition definition) {
        if (!definition.enabled()) {
            return ConnectorReadinessStatus.UNAVAILABLE;
        }
        if ("UNAVAILABLE".equals(credentialStatus(definition))) {
            return ConnectorReadinessStatus.DEGRADED;
        }
        return ConnectorReadinessStatus.READY;
    }
}
