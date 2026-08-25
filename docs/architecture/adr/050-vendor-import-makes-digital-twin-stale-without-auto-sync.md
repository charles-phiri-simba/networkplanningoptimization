# ADR 050 — Vendor Import Makes Digital Twin Stale Without Auto-Sync

## Status

Accepted for Phase 7.

## Context

Changing operational `txPower` changes the Phase 6 Twin fingerprint. Automatically creating a new Twin version would hide freshness from operators and bypass manual synchronization.

## Decision

A successful import may update operational state and therefore make an existing Twin `STALE`. Import must not call `TwinSynchronizationService` or insert a Twin version. STALE/EXPIRED simulation remains blocked until explicit resynchronization.

## Consequences

Phase 6 freshness policy is unchanged. Controlled `CELL-001` proof is isolated and restored after the test.
