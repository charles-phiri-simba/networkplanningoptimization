# ADR 060 — Credential Provider and External Secret Store Strategy

## Status

Accepted for Phase 9.

## Context

Vendor credentials must never live in Git, application.yml, the database, fixtures, Agent memory, MCP, or RAG.

## Decision

Introduce `ConnectorCredentialProvider` with per-session `resolve` / `metadata`. Phase 9 implements a local/test provider and an Azure Key Vault contract/configuration model. Live Key Vault calls are deferred. Secrets are session-scoped handles that never appear in `toString`, JSON, logs, or audit.

## Consequences

CI remains offline. Production must later connect the Azure provider without changing the import runtime.
