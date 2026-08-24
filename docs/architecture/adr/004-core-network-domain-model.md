# ADR 004 — Core network domain model

## Status

Accepted for Phase 1B.

## Context

Phase 1A/1A.1 could attach a synthetic KPI JSON record. Phase 1B must represent cellular-network state as structured domain context without modelling every 3GPP object.

## Decision

Persist the smallest useful graph:

```text
Site 1--* gNB 1--* Cell
                     +-- radio configuration parameters
                     +-- KPI observations
                     +-- neighbour relationships
```

Stable domain IDs (`SITE-001`, `GNB-001`, `CELL-001`) are separate from UUID persistence keys. KPI observations are rows (`metric`, `value`, `observedAt`) rather than one column per KPI. Neighbour rows are read-only facts; this phase does not optimise neighbour lists.

Intentionally excluded: full 3GPP information models, vendor parameter trees, AMF/UDM objects, live OSS inventory, neighbour optimisation.

## Consequences

Context resolution can assemble Cell + gNB + Site + recent KPIs + radio + neighbours for `CELL-001`. Completeness of the real RAN is not claimed.
