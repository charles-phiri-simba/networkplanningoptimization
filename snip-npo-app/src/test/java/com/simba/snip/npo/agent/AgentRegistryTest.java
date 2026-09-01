package com.simba.snip.npo.agent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRegistryTest {

    @Test
    void fiveEnabledAgentsShareOneModelProfileAndLeastPrivilege() {
        AgentRegistry registry = new AgentRegistry();
        assertEquals(5, registry.list().size());
        assertEquals(1, registry.list().stream().map(AgentDefinition::modelProfile).distinct().count());
        assertEquals(Set.of(AgentServiceKind.KNOWLEDGE_RAG), registry.require(AgentRegistry.KNOWLEDGE).allowedServices());
        assertEquals(Set.of(AgentServiceKind.NETWORK_CONTEXT), registry.require(AgentRegistry.CONTEXT).allowedServices());
        assertEquals(Set.of(AgentServiceKind.ASSURANCE_READ), registry.require(AgentRegistry.ASSURANCE).allowedServices());
        assertEquals(Set.of(AgentServiceKind.DECISION_SYNTHESIS), registry.require(AgentRegistry.DECISION).allowedServices());
        assertEquals(Set.of(AgentServiceKind.RUN_CONTROL), registry.require(AgentRegistry.CHIEF).allowedServices());
        registry.list().forEach(item -> assertTrue(item.enabled()));
    }

    @Test
    void permissionGuardDeniesCrossServiceAccess() {
        AgentPermissionGuard guard = new AgentPermissionGuard(new AgentRegistry());
        assertThrows(RuntimeException.class,
                () -> guard.assertAllowed(AgentRegistry.KNOWLEDGE, AgentServiceKind.RUN_CONTROL));
        assertThrows(RuntimeException.class,
                () -> guard.assertAllowed(AgentRegistry.CHIEF, AgentServiceKind.KNOWLEDGE_RAG));
        guard.assertAllowed(AgentRegistry.CONTEXT, AgentServiceKind.NETWORK_CONTEXT);
    }

    @Test
    void agentConstructorsDoNotDependOnMcpOrExecution() {
        assertNoForbiddenDependency(KnowledgeAgent.class);
        assertNoForbiddenDependency(ContextAgent.class);
        assertNoForbiddenDependency(AssuranceAgent.class);
        assertNoForbiddenDependency(DecisionAgent.class);
        assertNoForbiddenDependency(ChiefOrchestrationAgent.class);
        assertNoForbiddenDependency(AgentOrchestrationService.class);
    }

    @Test
    void candidateActionMappingIsDeterministic() {
        assertEquals("GENERATE_REMEDIATION_PLAN", DecisionAgent.candidateType("recommend the next safe action").name());
        assertEquals("SIMULATE_CELL_PARAMETER_CHANGE",
                DecisionAgent.candidateType("propose SIMULATE_CELL_PARAMETER_CHANGE").name());
        assertEquals("APPLY_CELL_PARAMETER_CHANGE",
                DecisionAgent.candidateType("propose APPLY_CELL_PARAMETER_CHANGE").name());
    }

    private static void assertNoForbiddenDependency(Class<?> type) {
        Set<String> forbidden = Set.of(
                "McpCapabilityGateway",
                "ActionExecutionService",
                "ActionApprovalService",
                "McpServerController"
        );
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            Set<String> names = Arrays.stream(constructor.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(Collectors.toSet());
            for (String name : forbidden) {
                assertFalse(names.contains(name), type.getSimpleName() + " must not depend on " + name);
            }
        }
    }
}
