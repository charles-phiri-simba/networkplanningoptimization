# ADR 039 — Deterministic versioned synthetic simulation model

## Status

Accepted for Phase 6.

## Context

An LLM must not invent authoritative KPI predictions. Vendor RF engines are out of scope.

## Decision

`CellParameterSimulationModel` (`modelId=snip.synthetic.cell-parameter.v1`, `modelVersion=1.0`, `modelType=RULE_BASED`) is a documented linear synthetic formula over txPower delta, BLER_DL, PRB_UTILIZATION_DL, and optional THROUGHPUT_DL. Identical input yields identical output. It is labelled as not vendor-calibrated RF physics.

## Consequences

Numeric results are testable without Ollama. Full RF, ray tracing, and calibration remain closed.
