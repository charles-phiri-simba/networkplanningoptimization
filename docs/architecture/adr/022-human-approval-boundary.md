# ADR 022 — Human approval boundary

## Status

Accepted for Phase 4.

## Context

Parameter simulation is medium risk and must fail closed without a human decision.

## Decision

`ActionApproval` records `APPROVED`/`REJECTED` with actor and time. Simulation cannot reach MCP until `APPROVED`. Rejection is terminal for execution.

## Consequences

No manufactured approval. LLM is never `decidedBy`.
