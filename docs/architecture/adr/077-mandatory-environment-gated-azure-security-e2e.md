# ADR 077 — Mandatory Environment-Gated Azure Security E2E

## Status

Accepted for Phase 10.

## Context

In-memory Key Vault stand-ins cannot prove AKS Workload Identity, Azure RBAC, or real secret versioning.

## Decision

Default CI remains Azure-independent. Architectural acceptance of Phase 10 additionally requires a protected, environment-gated Azure E2E on non-production AKS: pod authenticates with Workload Identity, retrieves a synthetic Key Vault secret, exercises least privilege, rotation, and the secure mock connector. GitHub Actions must not retrieve the connector secret for the pod. If Azure E2E cannot run, Phase 10 status is not recommended.

## Consequences

`ACCEPTANCE RECOMMENDED` requires both default CI and Azure E2E. Missing Azure evidence is not waived by local tests.
