# ADR 034 — Bounded synchronous orchestration

## Status

Accepted for Phase 5.

## Context

Unbounded Agent loops would escape human control.

## Decision

Runs are request-bounded and synchronous. Configurable limits: max steps, Agent calls, model calls, retries, proposed actions, per-Agent duration, overall timeout. Violations emit `LIMIT_REACHED` and terminate. No background, recurring, or self-resuming Agents.

## Consequences

Autonomy remains Level 1 (guided). Infinite loops are rejected by control limits.
