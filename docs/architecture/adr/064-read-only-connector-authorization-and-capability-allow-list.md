# ADR 064 — Read-Only Connector Authorization and Capability Allow-List

## Status

Accepted for Phase 9.

## Context

A vendor account may be accidentally overprivileged. SNIP must still not expose writes.

## Decision

Authorization is a positive allow-list. The initial profile is `READ_ONLY_NETWORK_INVENTORY` (READ_SITE / READ_GNB / READ_CELL / READ_CONFIGURATION / READ_NEIGHBOURS). Adapters declare required capabilities; anything not allowed is `CONNECTOR_AUTHORIZATION_DENIED`. `ReadOnlyVendorClient` exposes only bounded GET inventory semantics. A mock sentinel write endpoint exists solely to prove SNIP never calls it.

## Consequences

Unknown capabilities fail closed before network read.
