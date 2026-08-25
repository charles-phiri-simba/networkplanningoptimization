# ADR 052 — Source-Scope Lease and Fencing

## Status

Accepted for Phase 8.

## Context

Absence of distributed import locking was an accepted Phase 7 limitation. Two mutating imports for the same source/scope can race. Redis, ZooKeeper, etcd, and Kafka locks are out of scope.

## Decision

PostgreSQL is the coordination authority. The lease key is `sourceSystem + sourceScope` (fixtures use `DEFAULT`). Acquisition is atomic and issues a monotonically increasing fencing token. Heartbeat renews expiry. A stale owner cannot release or commit after a newer token is issued. There is no global import lock.

## Consequences

Independent scopes may run concurrently. Same-scope contention returns busy/`LEASE_UNAVAILABLE`. Multi-instance Kubernetes proof remains deferred.
