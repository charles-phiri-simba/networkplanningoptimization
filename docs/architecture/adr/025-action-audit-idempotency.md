# ADR 025 — Append-only action audit and idempotency

## Status

Accepted for Phase 4.

## Context

Governance must be reconstructable and retries must not double-invoke.

## Decision

`action_audit_event` is append-only. `actionId` is the execution idempotency key: a `SUCCEEDED` action returns the prior result and does not reinvoke MCP.

## Consequences

HTTP retries are safe after success. Audit rows are never replaced.
