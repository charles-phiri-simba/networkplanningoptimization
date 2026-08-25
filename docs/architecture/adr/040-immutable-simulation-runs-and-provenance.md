# ADR 040 — Immutable simulation runs and provenance

## Status

Accepted for Phase 6.

## Context

Overwriting a completed run would destroy reproducible evidence history.

## Decision

Each governed execution inserts a new `SimulationRun` with its Twin version, model identity, metrics, confidence, limitations, assumptions, and Twin provenance. Re-execution allocates a new `simulationId`. Completed rows are not updated.

## Consequences

Scenario A vs B on the same baseline remains comparable. FAILED MCP attempts do not rewrite succeeded runs.
