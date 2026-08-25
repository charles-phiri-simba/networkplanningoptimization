# ADR 041 — Phase 4 MCP remains the simulation execution boundary

## Status

Accepted for Phase 6.

## Context

A public `POST /simulate` would bypass human approval and the capability registry.

## Decision

Management APIs may synchronize Twins, create scenario definitions, and read/compare completed results. Authoritative execution remains `SIMULATE_CELL_PARAMETER_CHANGE` → MEDIUM / REQUIRE_APPROVAL → MCP `simulation.cell-parameter.v1` → `DigitalTwinSimulationService`. Capability ID is unchanged. Risk/policy semantics are unchanged.

## Consequences

Agents still cannot call MCP. APPLY remains HIGH / DENY. There is no ungoverned execution route.
