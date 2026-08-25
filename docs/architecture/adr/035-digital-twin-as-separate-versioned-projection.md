# ADR 035 — Digital Twin as a separate versioned projection

## Status

Accepted for Phase 6.

## Context

Operational cell state, SNIP persistence, and hypothetical simulation state must not be collapsed. A simulation that mutates live rows would be indistinguishable from a network change.

## Decision

`NetworkTwin` is a cell-scoped projection, not the operational database. Each successful synchronization creates an immutable `NetworkTwinVersion`. Simulations execute only against a recorded baseline version.

## Consequences

Reality, SNIP operational state, Twin state, and Scenario state remain distinct. Whole-RAN / core / transport Twins remain out of scope.
