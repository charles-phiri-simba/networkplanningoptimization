# ADR 078 — No Real Vendor Connector Until Production Secret Gate Is Accepted

## Status

Accepted for Phase 10.

## Context

Phase 9 deferred real ENM/NetAct until a production secret and identity gate existed.

## Decision

Phase 10 still targets MOCK_SECURE / synthetic credentials only. Real Ericsson ENM and Nokia NetAct connectors, real vendor credentials, vendor writes, vendor telemetry, and scheduled synchronization remain Phase 11+. Phase 11 must not start until this production-secret gate is architecturally accepted.

## Consequences

No `EricssonEnmAdapter` / `NokiaNetActAdapter` live HTTP clients. Connector traffic remains fixture or secure mock endpoints.
