# ADR 068 — No Real Vendor Connectivity Until Security Gate Is Accepted

## Status

Accepted for Phase 9.

## Context

Phase 8 architectural questions resolved that real ENM/NetAct must not precede integration security.

## Decision

Phase 9 proves MOCK_SECURE connectivity only. No Ericsson ENM, Nokia NetAct, live Azure Key Vault, production credentials, vendor writes, vendor telemetry, or scheduled sync. Phase 10 is not started.

## Consequences

The first real connector may be designed only after Phase 9 is architecturally accepted.
