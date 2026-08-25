# ADR 061 — Managed Identity / Workload Identity for Production Vault Access

## Status

Accepted for Phase 9.

## Context

Bootstrap client secrets for Key Vault recreate the secret-distribution problem.

## Decision

The production vault authentication target is Azure Managed Identity / Workload Identity with least-privilege secret-read on configured connector credentials. Phase 9 records the contract only; no Azure SDK network calls and no stored vault client secret.

## Consequences

Phase 10+ live Key Vault integration should use platform identity, not a checked-in secret.
