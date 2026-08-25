# ADR 042 — Simulation evidence, confidence, and limitations

## Status

Accepted for Phase 6.

## Context

A bare predicted KPI would look like operational truth. Phase 6 simulations are synthetic evidence.

## Decision

Every successful result includes categorical confidence (`LOW` / `MEDIUM` / `HIGH`, initially `LOW`), structured limitations (`NO_RF_PROPAGATION_MODEL`, `NO_VENDOR_CALIBRATION`, `NO_MOBILITY_MODEL`, `NO_TRAFFIC_FORECAST`, `SYNTHETIC_KPI_MODEL`), assumptions, Twin/model versions, and `synthetic=true`. Scenario comparison exposes metric trade-offs and does not select an optimum. LLM narrative cannot overwrite these fields.

## Consequences

Decision Intelligence may consume the structured DTO. Automatic optimization remains prohibited.
