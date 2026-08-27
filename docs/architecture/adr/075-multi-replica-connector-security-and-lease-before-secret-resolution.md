# ADR 075 — Multi-Replica Connector Security and Lease-Before-Secret Resolution

## Status

Accepted for Phase 10.

## Context

Two replicas of the connector runtime share PostgreSQL. Duplicate vault resolution on a lost same-scope race would expand secret exposure.

## Decision

Phase 8 source-scope lease and fencing remain canonical. Lease acquisition occurs before Key Vault resolution and before connector I/O. A same-scope losing replica must not retrieve the connector secret. Independent source/scope pairs may proceed concurrently. A successor replica after lease expiry receives a new fencing token; the stale owner cannot commit.

## Consequences

Proof requires independently instantiated runtimes, not two threads on one service object. Default CI proves this against Testcontainers PostgreSQL; Azure E2E should prove it with AKS replicas.
