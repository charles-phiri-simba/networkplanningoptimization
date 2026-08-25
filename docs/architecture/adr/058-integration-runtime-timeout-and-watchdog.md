# ADR 058 — Integration Runtime Timeout and Watchdog

## Status

Accepted for Phase 8.

## Context

Phase 5 non-interruptible per-Agent timeout is accepted technical debt and must not be redesigned. Imports still need a bounded overall limit.

## Decision

A dedicated import watchdog marks a still-`RUNNING` execution `TIMED_OUT` / `EXECUTION_TIMEOUT` when the configured limit is exceeded, releases the lease, and prevents a later `COMPLETED` transition. Late workers fail fencing or status checks. This watchdog applies only to integration imports.

## Consequences

Timed-out executions remain retryable by explicit resubmission. Cooperative cancellation remains deferred. Phase 5 Agent timeout is unchanged.
