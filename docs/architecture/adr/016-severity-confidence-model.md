# ADR 016 — Severity and confidence model

## Status

Accepted for Phase 3.

## Context

Severity and confidence must be deterministic and explainable.

## Decision

Severity: `INFO` / `WARNING` / `MAJOR` / `CRITICAL`. Confidence: `LOW` / `MEDIUM` / `HIGH` (evidence completeness, not a model probability).

Initial mapping for `DEGRADING_RADIO_QUALITY`:

- `BLER_DL >= 0.08` and INCREASING → at least `WARNING`
- `BLER_DL >= 0.10` and INCREASING → at least `MAJOR`
- `BLER_DL >= 0.12` and INCREASING and `PRB_UTILIZATION_DL` INCREASING → `CRITICAL`
- Confidence `HIGH` when PRB is INCREASING, `MEDIUM` when PRB series exists but is not INCREASING, `LOW` when only BLER evidence exists

The LLM must not overwrite these values.

## Consequences

Corroborating PRB is co-occurrence, not causality.
