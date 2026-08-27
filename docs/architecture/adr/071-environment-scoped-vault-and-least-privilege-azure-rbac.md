# ADR 071 — Environment-Scoped Vault and Least-Privilege Azure RBAC

## Status

Accepted for Phase 10.

## Context

A single vault or over-broad identity would allow INT workloads to read PROD connector secrets.

## Decision

Connector identities and secret names are environment-scoped (`snip-{env}-{vendor}-inventory-reader`). The dedicated UAMI is assigned Key Vault Secrets User (GET/LIST of secret metadata as required by GET) on the intended vault only. SET, DELETE, and unrelated secret scopes are denied. Distinct INT/PROD vaults are the target isolation; if a second vault is not available, E2E uses an unauthorized secret scope and documents the limitation.

## Consequences

Secret enumeration APIs are not added to SNIP. RBAC proof belongs in Azure E2E, not default CI.
