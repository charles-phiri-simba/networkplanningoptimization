# ADR 015 — Deterministic condition detection

## Status

Accepted for Phase 3.

## Context

The LLM must not decide whether a threshold was crossed.

## Decision

`DegradingRadioQualityDetector` evaluates last-N temporal context from `NetworkContextService`. Canonical rule: `BLER_DL >= snip.assurance-bler-dl-threshold` AND `BLER_DL` trend `INCREASING`. Evaluation runs after a successful telemetry projection, not inside Kafka listener code. Defaults: warning `0.08`, major `0.10`, critical `0.12` (ratios).

## Consequences

Healthy or insufficient-trend series produce no case. Statistical / ML anomaly detection remains out of scope.
