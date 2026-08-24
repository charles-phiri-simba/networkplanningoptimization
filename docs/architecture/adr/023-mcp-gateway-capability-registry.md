# ADR 023 — MCP Gateway and capability registry

## Status

Accepted for Phase 4.

## Context

Only registered capabilities may execute.

## Decision

`CapabilityRegistry` registers `remediation.generate.v1` and `simulation.cell-parameter.v1`. `McpCapabilityGateway` verifies identity, registration, policy, approval, risk, dry-run, and idempotency, then fails closed. No live-apply capability is registered.

## Consequences

Unknown, disabled, or incompatible capabilities cannot invoke MCP.
