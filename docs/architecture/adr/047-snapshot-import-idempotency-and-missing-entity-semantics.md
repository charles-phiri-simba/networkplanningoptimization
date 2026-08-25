# ADR 047 — Snapshot Import, Idempotency and Missing-Entity Semantics

## Status

Accepted for Phase 7.

## Context

Partial vendor dumps must not retire unrelated cells. Identical reimport must not duplicate graph edges.

## Decision

Each attempt is a `NetworkImportBatch`. Replaying the same snapshot creates a new batch but canonical state is idempotent (`created=0`, `updated=0`, `unchanged>0`). Missing-entity detection runs only for `completeSnapshot=true`: previously seen source entities absent now become `MISSING` and are not physically deleted. Reappearance returns `ACTIVE`. Partial snapshots must not mark omitted entities `MISSING`.

## Consequences

`RETIRED` is not added. Physical deletion from missing source data is prohibited.
