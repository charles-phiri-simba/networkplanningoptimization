# ADR 069 — Direct Azure Key Vault Provider for Production Connector Secrets

## Status

Accepted for Phase 10.

## Context

Phase 9 defined `AzureKeyVaultCredentialProvider` as a contract only. Production connector secrets must come from an enterprise secret store, not application configuration or the local development provider.

## Decision

Canonical production retrieval is the Azure SDK `SecretClient` against a configured vault URI and fixed connector credential references. Key Vault CSI is deferred and non-canonical. Public APIs cannot supply vault URI, secret name, version, or credentialRef.

## Consequences

Default CI stays Azure-independent via an in-memory accessor. Live Key Vault access is an environment-gated production/E2E concern.
