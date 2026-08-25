# ADR 043 — Vendor Adapter Boundary

## Status

Accepted for Phase 7.

## Context

Ericsson and Nokia inventory representations differ. If vendor DTOs leak into Assurance, Agents, Twin, or MCP, SNIP cannot stay vendor-neutral.

## Decision

Vendor specificity ends at `NetworkSourceAdapter`. In-code registry maps `ERICSSON` → `EricssonFixtureAdapter` and `NOKIA` → `NokiaFixtureAdapter`. Adapters are read-only, return `SourceSnapshot`, and must not persist JPA entities. Higher layers consume canonical/operational types only.

## Consequences

Unsupported vendors fail deterministically. Dynamic plugins and remote vendor MCP are deferred.
