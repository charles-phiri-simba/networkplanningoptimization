# ADR 067 — Security Failure Mapping into Reliable Import Runtime

## Status

Accepted for Phase 9.

## Context

Phase 8 already terminalizes executions with bounded failure codes. Connector security must not invent a parallel lifecycle.

## Decision

Map connector failures onto Phase 8 codes: `CREDENTIAL_RESOLUTION_FAILED`, `CONNECTOR_AUTHENTICATION_FAILED`, `TLS_TRUST_FAILED`, `CONNECTOR_AUTHORIZATION_DENIED`, `NETWORK_POLICY_DENIED`, `CONNECTOR_DISABLED`. Same-scope lease is acquired before expensive connector I/O. Security failures produce a durable terminal execution with zero canonical mutation. Retry remains explicit; there is no automatic retry.

## Consequences

Phase 8 lease/fencing/watchdog/replay semantics stay authoritative.
