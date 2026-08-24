# ADR 012 — Temporal context and trend strategy

## Status

Accepted for Phase 2.

## Context

The LLM must not calculate deterministic facts. Engineers need current value, last-N history, and a trend per KPI.

## Decision

`NetworkContextService` exposes last N observations per metric (default N=5) ordered by event time, plus `INCREASING` / `DECREASING` / `STABLE` / `INSUFFICIENT_DATA` from first-versus-last in that window. When `SNIP_SIMULATOR` observations exist for a metric, that source is used for the series so static `DEMO_SEED` rows do not invert the canonical `0.04→0.12` scenario. Trends are passed into the prompt as `TEMPORAL KPI HISTORY / TRENDS`. Operational telemetry is not written to the vector store.

## Consequences

Stored KPI values remain ratios; presentation still uses `0.12 ratio (12%)`. Mixed non-monotonic windows are classified by endpoints only in Phase 2.
