# ADR 072 — Per-Session Key Vault Resolution and No Secret Fallback

## Status

Accepted for Phase 10.

## Context

Cached vendor secret values would freeze rotation and hide disable/revoke. Falling back to an older version or the local provider would hide production failures.

## Decision

Each connector session resolves the latest enabled secret version (pinned version is exceptional). SDK clients may be reused per vault; secret **values** are not cached. Disabled, missing, unauthorized, or unavailable secrets fail closed. Production has no local-provider fallback and no older-version fallback.

## Consequences

Rotation is picked up on the next session without restart. Vault outage maps to retryable `VAULT_UNAVAILABLE` without automatic import retry.
