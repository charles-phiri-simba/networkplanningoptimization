# ADR 029 — Structured plan and no chain-of-thought persistence

## Status

Accepted for Phase 5.

## Context

Agent reasoning must be inspectable without storing hidden deliberation.

## Decision

Persist a structured `AgentPlan` and `PlanStep` records plus short output summaries. Do not persist raw prompts, private chain-of-thought, or unrestricted scratchpads. Audit summaries stay bounded.

## Consequences

Replay and review use plan/steps/audit/case memory only.
