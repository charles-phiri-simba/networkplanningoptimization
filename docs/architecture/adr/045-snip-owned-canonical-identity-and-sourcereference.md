# ADR 045 — SNIP-Owned Canonical Identity and SourceReference

## Status

Accepted for Phase 7.

## Context

Vendor DNs and moIds are not stable SNIP identifiers. Losing the mapping would prevent provenance and conflict detection.

## Decision

SNIP owns canonical IDs (`SITE-E001`, `CELL-N001`, …). Vendor IDs remain `SourceReference` rows with source system, vendor, source entity id/DN, first/last seen, snapshot provenance, and `ACTIVE`/`MISSING` status. Duplicates on replay are prevented by a unique constraint.

## Consequences

Field-level provenance is deferred. One authoritative `SourceReference` per canonical entity is enforced with a partial unique index.
