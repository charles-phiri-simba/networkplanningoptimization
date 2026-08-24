package com.simba.snip.npo.mcp;

import com.simba.snip.npo.action.ActionMetrics;
import com.simba.snip.npo.action.ActionRules;
import com.simba.snip.npo.action.ActionStatus;
import com.simba.snip.npo.action.ApprovalDecision;
import com.simba.snip.npo.action.CapabilityRegistry;
import com.simba.snip.npo.action.PolicyOutcome;
import com.simba.snip.npo.action.RiskLevel;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.domain.DomainConflictException;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.ActionApprovalEntity;
import com.simba.snip.npo.persist.ProposedActionEntity;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fail-closed MCP client/gateway. LLM components must not call this.
 */
@Component
public class McpCapabilityGateway {

    private final RestClient restClient;
    private final SnipProperties properties;
    private final ActionMetrics metrics;
    private final ApplicationContext applicationContext;

    public McpCapabilityGateway(
            SnipProperties properties,
            ActionMetrics metrics,
            ApplicationContext applicationContext
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofMillis(properties.getMcpTimeoutMs());
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.properties = properties;
        this.metrics = metrics;
        this.applicationContext = applicationContext;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> invoke(
            ProposedActionEntity action,
            ActionApprovalEntity approval,
            Map<String, Object> arguments
    ) {
        verify(action, approval, arguments);
        String capabilityId = action.getCapabilityId();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", capabilityId);
        params.put("arguments", arguments);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", action.getId().toString());
        request.put("method", "tools/call");
        request.put("params", params);
        long started = System.nanoTime();
        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl() + "/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);
            metrics.recordMcpLatencyMs((System.nanoTime() - started) / 1_000_000);
            if (response == null) {
                throw new DomainConflictException("empty MCP response");
            }
            if (response.get("error") != null) {
                throw new DomainConflictException("MCP error: " + response.get("error"));
            }
            Object result = response.get("result");
            if (result instanceof Map<?, ?> resultMap && resultMap.get("structuredContent") instanceof Map<?, ?> structured) {
                metrics.incrementMcpInvocations();
                return (Map<String, Object>) structured;
            }
            throw new DomainConflictException("MCP result missing structuredContent");
        } catch (RestClientException ex) {
            metrics.incrementMcpFailures();
            throw new DomainConflictException("MCP invocation failed: " + ex.getMessage());
        }
    }

    private void verify(ProposedActionEntity action, ActionApprovalEntity approval, Map<String, Object> arguments) {
        if (action == null) {
            throw new DomainValidationException("action is required");
        }
        if (ActionStatus.SUCCEEDED.name().equals(action.getStatus())) {
            throw new DomainConflictException("successful action cannot be reinvoked");
        }
        CapabilityRegistry.CapabilityDefinition capability = CapabilityRegistry.find(action.getCapabilityId())
                .orElseThrow(() -> new DomainValidationException("unknown capability"));
        if (!capability.enabled()) {
            throw new DomainValidationException("capability is disabled");
        }
        if (capability.capabilityId().equals(ActionRules.CAPABILITY_REMEDIATION)
                && !"GENERATE_REMEDIATION_PLAN".equals(action.getActionType())) {
            throw new DomainValidationException("action/capability are incompatible");
        }
        if (capability.capabilityId().equals(ActionRules.CAPABILITY_SIMULATION)
                && !"SIMULATE_CELL_PARAMETER_CHANGE".equals(action.getActionType())) {
            throw new DomainValidationException("action/capability are incompatible");
        }
        PolicyOutcome policy = PolicyOutcome.valueOf(action.getPolicyDecision());
        if (policy == PolicyOutcome.DENY) {
            throw new DomainConflictException("policy DENY cannot reach MCP");
        }
        if (policy == PolicyOutcome.REQUIRE_APPROVAL
                && (approval == null || !ApprovalDecision.APPROVED.name().equals(approval.getDecision()))) {
            throw new DomainConflictException("approval is required before MCP invocation");
        }
        RiskLevel risk = RiskLevel.valueOf(action.getRiskLevel());
        if (risk == RiskLevel.HIGH || risk == RiskLevel.CRITICAL) {
            throw new DomainConflictException("risk " + risk + " cannot reach MCP");
        }
        if (capability.dryRunOnly() && !truthy(arguments.get("dryRun"))) {
            throw new DomainValidationException("dryRun=true is required before MCP invocation");
        }
    }

    private String baseUrl() {
        String configured = properties.getMcpBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        if (applicationContext instanceof WebServerApplicationContext web) {
            return "http://127.0.0.1:" + web.getWebServer().getPort();
        }
        return "http://127.0.0.1:8080";
    }

    private static boolean truthy(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }
}
