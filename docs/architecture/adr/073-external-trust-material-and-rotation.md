# ADR 073 — External Trust Material and Rotation

## Status

Accepted for Phase 10.

## Context

Phase 9 used in-memory custom CA bytes in tests. Production trust material must be able to rotate without an application restart.

## Decision

When a CUSTOM_CA profile has no in-memory certificates and a trust secret is configured, trust PEM/DER is loaded from Key Vault for that session. Strict hostname verification is unchanged. A new session observes a new trust version without restart.

## Consequences

Trust-material failure maps to `TRUST_MATERIAL_RESOLUTION_FAILED` and must not mutate canonical state.
