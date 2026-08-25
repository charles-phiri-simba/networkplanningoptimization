# ADR 037 — Twin freshness and stale-simulation policy

## Status

Accepted for Phase 6.

## Context

Simulating against a Twin that no longer matches operational state would present stale numbers as evidence.

## Decision

Freshness is deterministic:

- `EXPIRED` if `now − synchronizedAt ≥ snip.twin-expired-hours` (default 24).
- `STALE` if the current operational fingerprint (txPower value/effectiveFrom plus each current KPI value/observedAt) differs from the snapshot fingerprint.
- `CURRENT` otherwise.

Simulation is allowed only for `CURRENT`. `STALE` and `EXPIRED` fail closed and require explicit resynchronization. The evaluator never resynchronizes.

## Consequences

A newer KPI observation or radio-configuration change blocks simulation until a new Twin version exists.
