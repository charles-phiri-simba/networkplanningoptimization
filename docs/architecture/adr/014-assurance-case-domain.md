# ADR 014 — Assurance Case domain model

## Status

Accepted for Phase 3.

## Context

Phase 3 must persist a first-class condition object distinct from raw telemetry and from a future incident ticket.

## Decision

PostgreSQL tables `assurance_case` and `assurance_evidence` hold `AssuranceCase` / `AssuranceEvidence`. Canonical `caseType` is `DEGRADING_RADIO_QUALITY`. Status values are `OPEN`, `ACKNOWLEDGED`, `RESOLVED`. Automatic resolution is deferred; cases remain `OPEN` until a later phase defines a recovery window.

## Consequences

No incident platform or ITSM integration. Evidence is operational fact only, not LLM prose.
