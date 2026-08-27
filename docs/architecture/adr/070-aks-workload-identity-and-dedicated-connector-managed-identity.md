# ADR 070 — AKS Workload Identity and Dedicated Connector Managed Identity

## Status

Accepted for Phase 10.

## Context

Phase 9 named Managed Identity as the production target. Connector runtime must not use developer credentials, node identity, or a shared cluster identity with broad vault rights.

## Decision

Production uses explicit `WorkloadIdentityCredential` (Phase 9 `MANAGED_IDENTITY` is an alias). A dedicated user-assigned identity `snip-connector-secrets-mi` federates to Kubernetes service account `snip-connector-runtime`. `DefaultAzureCredential` is local development only and is refused when `productionRuntime=true`.

## Consequences

IMDS/node-identity fallback is not an accepted production path. Agents, LLM, and MCP cannot use this identity.
