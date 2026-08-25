# ADR 028 — Chief Orchestrator boundary

## Status

Accepted for Phase 5.

## Context

Specialists must not form a freeform mesh or invoke MCP.

## Decision

The Chief Orchestration Agent plans, delegates, enforces limits, and stops. `AgentOrchestrationService` is the only control plane. Specialists return to the orchestrator. The orchestrator does not call MCP or ActionExecutionService.

## Consequences

Communication is orchestrator-mediated. Direct Agent-to-MCP paths are prohibited.
