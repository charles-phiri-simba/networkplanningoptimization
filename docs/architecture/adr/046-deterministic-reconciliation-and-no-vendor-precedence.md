# ADR 046 — Deterministic Reconciliation and No Vendor Precedence

## Status

Accepted for Phase 7.

## Context

Silent last-writer-wins across Ericsson and Nokia would corrupt canonical state.

## Decision

Reconciliation outcomes are `CREATE` / `UPDATE` / `UNCHANGED` / `CONFLICT` / `REJECT`. One current authoritative source per canonical entity. No global Ericsson>Nokia (or reverse) precedence. Same-source changed snapshots may `UPDATE`. A second source with different values persists `IntegrationConflict` (`OPEN`) and does not overwrite.

## Consequences

Conflicts are reported, not auto-resolved. LLM/Agent reconciliation is prohibited.
