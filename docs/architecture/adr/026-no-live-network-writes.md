# ADR 026 — Prohibition of live network writes in Phase 4

## Status

Accepted for Phase 4.

## Context

Unsafe action must be deniable before invocation.

## Decision

`APPLY_CELL_PARAMETER_CHANGE` is a known action type with risk HIGH and policy DENY. It has no registered capability. Execute is rejected with no MCP call. Simulation is dry-run/synthetic only. Executor identity is `SNIP_ACTION_SERVICE`, never the LLM.

## Consequences

Zero live-network writes. Direct LLM-to-MCP is prohibited.
