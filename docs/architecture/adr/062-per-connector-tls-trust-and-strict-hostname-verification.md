# ADR 062 — Per-Connector TLS Trust and Strict Hostname Verification

## Status

Accepted for Phase 9.

## Context

Vendors may present different CA chains. Trust-all TLS would hide MITM. Mutating the JVM default trust store would couple connectors.

## Decision

TLS is mandatory. Each connector builds a private `SSLContext` from a typed trust profile (`SYSTEM_CA` or `CUSTOM_CA`). Hostname verification is HTTPS-strict. Trust-all strategies are prohibited. Real TLS handshakes are proven in Maven tests with ephemeral certificates.

## Consequences

Untrusted CA and hostname mismatch fail closed as `TLS_TRUST_FAILED` with no canonical mutation.
