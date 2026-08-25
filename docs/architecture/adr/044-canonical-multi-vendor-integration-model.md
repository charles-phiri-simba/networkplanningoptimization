# ADR 044 — Canonical Multi-Vendor Integration Model

## Status

Accepted for Phase 7.

## Context

Vendor-specific tables would duplicate Site/gNB/Cell and prevent a single operational graph.

## Decision

Adapters normalize into SNIP canonical records (`CanonicalSite`, `CanonicalGnb`, `CanonicalCell`, `CanonicalCellConfiguration`, `CanonicalNeighbourRelation`). Persistence reuses existing operational tables. No Ericsson/Nokia operational tables. No raw vendor payload archive.

## Consequences

Downstream Context, Assurance, Agents, and Twin remain vendor-blind. Vendor schema evolution is an adapter concern (`ERICSSON_FIXTURE_V1` / `NOKIA_FIXTURE_V1`).
