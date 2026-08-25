# ADR 057 — Atomic Canonical Commit

## Status

Accepted for Phase 8.

## Context

A half-applied snapshot would corrupt operational state. Execution metadata must survive canonical rollback.

## Decision

Phase 7 reconciliation produces a deterministic plan, then apply runs in one canonical transaction. The transaction asserts lease ownership and fencing token before and after mutation. If commit fails, canonical changes roll back and the execution is `FAILED` with a bounded code such as `DATABASE_COMMIT_FAILED` or `LEASE_LOST`. Execution status, audit, and checkpoints use separate durable transactions.

## Consequences

No half-applied snapshot is accepted. A zombie with a stale fencing token cannot commit.
