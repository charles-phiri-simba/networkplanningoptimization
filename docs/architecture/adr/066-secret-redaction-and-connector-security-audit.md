# ADR 066 — Secret Redaction and Connector Security Audit

## Status

Accepted for Phase 9.

## Context

Security operations need provenance without leaking secrets.

## Decision

Append-only `connector_security_audit_event` and `connector_session` store metadata only: connector id, credentialRef, version, endpointRef, trust profile, server certificate fingerprint, sanitized failure codes. Canary secret `PHASE9_CANARY_SECRET_VALUE` must never appear in logs, audit, API bodies, or exception messages.

## Consequences

Release is blocked if the canary appears in any of those surfaces.
