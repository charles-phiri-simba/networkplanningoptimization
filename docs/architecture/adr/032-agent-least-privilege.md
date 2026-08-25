# ADR 032 — Agent least-privilege permissions

## Status

Accepted for Phase 5.

## Context

Every Agent is a constrained principal.

## Decision

An in-code registry lists `allowedServices`. `AgentPermissionGuard` enforces them at invocation. Knowledge may use RAG only; Context may read NetworkContextService only; Assurance may read cases only; Decision may synthesise only; the Chief may control the run only. Constructors do not take MCP, execution, or approval services.

## Consequences

Permission violations fail closed. Proposal creation is an orchestrator adapter over Phase 4 `ActionProposalService`, not an Agent MCP call.
