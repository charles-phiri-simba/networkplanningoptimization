# ADR 063 — Optional mTLS and Client Certificate Handling

## Status

Accepted for Phase 9.

## Context

Some vendor endpoints will require client certificates. Not every connector will.

## Decision

mTLS is optional per connector authentication method (`BASIC`, `MTLS`, `BASIC_PLUS_MTLS`). Client private keys come only from the credential provider and are assembled in memory. Phase 9 proves trusted, missing, and untrusted client certificates over a real TLS socket. `BASIC_PLUS_MTLS` is proven on a real TLS socket that requires both factors simultaneously.

## Consequences

OAuth remains deferred. Client keys are never persisted.
