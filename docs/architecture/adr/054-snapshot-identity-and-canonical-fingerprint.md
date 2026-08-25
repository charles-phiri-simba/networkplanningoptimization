# ADR 054 — Snapshot Identity and Canonical Fingerprint

## Status

Accepted for Phase 8.

## Context

If a source snapshot id can be reused with different content, history and replay become ambiguous.

## Decision

Logical identity is `sourceSystem + sourceScope + sourceSnapshotId`. SNIP computes a deterministic SHA-256 `canonicalSnapshotHash` over normalized canonical content with stable ordering. Capture time and vendor transport formatting are excluded. The same id with a different hash is `REJECTED` / `SNAPSHOT_ID_CONTENT_MISMATCH` and is not retryable. No canonical mutation occurs.

## Consequences

Vendors cannot silently rewrite a snapshot id. Equivalent normalized content hashes equally across Ericsson and Nokia transport shapes.
