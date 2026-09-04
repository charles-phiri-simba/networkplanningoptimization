package com.simba.snip.npo.vendorcertification.policy;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import org.springframework.stereotype.Component;

@Component
public class Phase17SeparationOfDutiesPolicy {

    public void requirePrincipal(String principalId, String role) {
        if (Phase17PrincipalCanon.isBlank(principalId) || !isValidPrincipal(principalId)) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, role + " principal denied");
        }
    }

    public void requireDistinct(String left, String right, String rule) {
        requirePrincipal(left, "left");
        requirePrincipal(right, "right");
        if (Phase17PrincipalCanon.samePrincipal(left, right)) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, rule);
        }
    }

    public void denyAgentOrMcp(String principalId) {
        if (Phase17PrincipalCanon.isBlank(principalId)) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "principal denied");
        }
        String id = Phase17PrincipalCanon.canonical(principalId);
        if (id.startsWith("agent:") || id.contains("agent-")) {
            throw new Phase17Exception(Phase17DenialCode.P17_AGENT_DENIED, "agents cannot certify");
        }
        if (id.startsWith("mcp:") || id.contains("mcp-")) {
            throw new Phase17Exception(Phase17DenialCode.P17_MCP_DENIED, "MCP cannot certify");
        }
    }

    public void requirePermission(String held, String required) {
        if (held == null || held.isBlank() || !held.equals(required)) {
            throw new Phase17Exception(Phase17DenialCode.P17_SOD_VIOLATION, "missing permission " + required);
        }
    }

    public boolean isValidPrincipal(String principalId) {
        if (Phase17PrincipalCanon.isBlank(principalId)) {
            return false;
        }
        String canonical = Phase17PrincipalCanon.canonical(principalId);
        return canonical.length() <= 128;
    }
}
