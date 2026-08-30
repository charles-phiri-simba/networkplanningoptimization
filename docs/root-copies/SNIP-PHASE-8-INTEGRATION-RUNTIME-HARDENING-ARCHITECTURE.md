# SNIP Phase 8 --- Integration Runtime Hardening & Reliable Synchronization Architecture

## 1. Purpose

Phase 8 hardens the multi-vendor import runtime established in Phase 7
before SNIP is allowed to connect to real Ericsson or Nokia systems.

Starting baseline:

``` text
Branch: main
Commit: 10bcd3369d68a3304687a007324da4566e048098
Message: feat: establish SNIP Phase 7 multi-vendor network integration foundation
Phase 7: ARCHITECTURALLY ACCEPTED / FROZEN
CI: PASS
Maven: 116 tests, 0 failures
Go: tests/build PASS
Working tree: clean
Phase 8: not started
```

The architectural question is:

> **How can SNIP make multi-vendor imports concurrency-safe,
> replay-aware, recoverable, observable, and operationally trustworthy
> before connecting to real Ericsson/Nokia systems?**

The governing distinction is:

> **Phase 7 defines what canonical changes an import means. Phase 8
> defines how that import is executed reliably.**

Phase 8 must wrap runtime guarantees around Phase 7. It must not
redesign Phase 7 reconciliation.

------------------------------------------------------------------------

## 2. Architectural Position

Phase 7:

``` text
Fixture Adapter
      ↓
SourceSnapshot
      ↓
Normalize / Validate
      ↓
Deterministic Reconciliation
      ↓
Canonical Operational State
```

Phase 8:

``` text
Import Request
      ↓
Source / Scope Resolution
      ↓
Snapshot Identity / Fingerprint
      ↓
Replay / Retry / New Classification
      ↓
PostgreSQL Lease + Fencing Token
      ↓
Durable Import Execution
      ↓
Snapshot Read
      ↓
Phase-Level Checkpoints
      ↓
Phase 7 Normalization / Validation / Reconciliation
      ↓
Atomic Canonical Commit
      ↓
Terminal Execution Outcome
      ↓
Immutable Attempt History / Audit / Metrics
```

Phase 8 remains fixture-first and read-only.

------------------------------------------------------------------------

## 3. Core Runtime Principle

An import is a **durable execution attempt**, not merely a synchronous
method invocation.

Every execution must have durable identity and lifecycle information
equivalent to:

``` text
ImportExecution
  executionId
  sourceSystem
  sourceScope
  sourceSnapshotId
  canonicalSnapshotHash
  executionType
  attemptNumber
  previousExecutionId
  originalSuccessfulExecutionId
  status
  requestedAt
  startedAt
  completedAt
  ownerInstanceId
  leaseFencingToken
  failureCode
  retryable
```

Prefer evolving Phase 7 `NetworkImportBatch` into this richer runtime
record rather than introducing a competing import concept.

------------------------------------------------------------------------

## 4. Source Scope

Concurrency isolation is based on:

``` text
sourceSystem + sourceScope
```

For Phase 8 fixtures:

``` text
ERICSSON_FIXTURE / DEFAULT
NOKIA_FIXTURE    / DEFAULT
```

The model must be extensible to future scopes such as NETWORK, REGION,
MARKET or CLUSTER without implementing a complex hierarchy now.

Different source/scope keys may execute independently.

The same source/scope must have at most one active mutating import.

------------------------------------------------------------------------

## 5. PostgreSQL Lease

Use PostgreSQL as the coordination authority.

Conceptual lease:

``` text
ImportLease
  leaseKey
  sourceSystem
  sourceScope
  ownerExecutionId
  ownerInstanceId
  fencingToken
  acquiredAt
  heartbeatAt
  expiresAt
```

Do not add Redis, ZooKeeper, etcd, Kafka-based locks, or another
coordination system.

------------------------------------------------------------------------

## 6. Lease Acquisition

Before a NEW or RETRY execution can mutate canonical state:

1.  resolve source/scope;
2.  acquire the source/scope lease atomically;
3.  receive a fencing token;
4.  persist ownership on the execution;
5.  proceed only while ownership remains valid.

If another active execution owns the same source/scope:

``` text
LEASE_UNAVAILABLE / BUSY
```

No second mutating execution starts.

There is no global import lock.

------------------------------------------------------------------------

## 7. Lease Heartbeat and Expiration

Leases must expire if their owner stops renewing them.

Use configurable values conceptually equivalent to:

``` text
leaseDuration
heartbeatInterval
```

A bounded heartbeat renews active execution ownership.

Exact defaults are implementation configuration, not hard-coded
architecture.

A crashed/stalled process must not retain a lease forever.

------------------------------------------------------------------------

## 8. Fencing Tokens

Every successful lease acquisition receives a monotonically increasing
fencing token for that source/scope.

Example:

``` text
Execution A -> token 17
lease expires
Execution B -> token 18
```

Before canonical commit, the execution must prove that:

``` text
ownerExecutionId matches
AND
fencingToken is still current
AND
lease has not been lost
```

A zombie execution with token 17 must not commit after token 18 has been
issued.

Failure outcome:

``` text
FAILED
failureCode = LEASE_LOST
```

This is a locked Phase 8 safety invariant.

------------------------------------------------------------------------

## 9. Execution Lifecycle

Keep the persisted status model intentionally small:

``` text
REQUESTED
RUNNING
COMPLETED
FAILED
TIMED_OUT
REJECTED
```

Detailed phase progress belongs in append-only checkpoints rather than
proliferating execution statuses.

Terminal execution records are immutable except for narrowly required
terminalization metadata written as part of the terminal transition.

------------------------------------------------------------------------

## 10. Execution Type

Persist:

``` text
NEW
RETRY
REPLAY
```

### NEW

No previous completed/failed execution exists for the immutable snapshot
identity.

### RETRY

A prior attempt for the same logical snapshot ended unsuccessfully and
the failure is retryable.

### REPLAY

The same snapshot has already completed successfully and is submitted
again.

These concepts must not be conflated.

------------------------------------------------------------------------

## 11. Snapshot Execution Identity

The logical lookup key is:

``` text
sourceSystem
+ sourceScope
+ sourceSnapshotId
```

Additionally compute a stable canonical snapshot fingerprint:

``` text
canonicalSnapshotHash
```

The hash must be deterministic for semantically identical normalized
snapshot content.

------------------------------------------------------------------------

## 12. Snapshot ID Immutability

Lock the invariant:

> **A sourceSnapshotId is immutable with respect to snapshot content
> within a source/scope.**

If the same source/scope/snapshot ID is presented with a different
canonical fingerprint:

``` text
REJECTED
failureCode = SNAPSHOT_ID_CONTENT_MISMATCH
retryable = false
```

No canonical mutation is allowed.

This prevents ambiguous or rewritten source history.

------------------------------------------------------------------------

## 13. Replay Semantics

A successful snapshot must not be re-applied by default.

A replay creates a **new lightweight immutable REPLAY execution record**
that:

``` text
references original successful execution
records the replay request
canonicalMutation = false
does not acquire a mutating import path unnecessarily
does not CREATE/UPDATE canonical entities
does not re-run missing transitions
does not create a Twin version
```

The replay is part of operational audit history.

This replaces the Phase 7 limitation where repeated successful imports
could create ordinary new batches.

------------------------------------------------------------------------

## 14. Retry Semantics

A failed or timed-out retryable execution may be retried explicitly.

A retry creates a new immutable attempt:

``` text
attemptNumber = prior + 1
previousExecutionId = prior execution
executionType = RETRY
```

Example:

``` text
Attempt 1 -> FAILED
Attempt 2 -> TIMED_OUT
Attempt 3 -> COMPLETED
```

All attempts remain queryable.

No failed attempt is overwritten or deleted.

------------------------------------------------------------------------

## 15. Retryability

Persist a deterministic retryability classification.

Bounded failure codes should include equivalents of:

``` text
ADAPTER_ERROR
SNAPSHOT_READ_FAILED
SCHEMA_UNSUPPORTED
VALIDATION_FATAL
LEASE_UNAVAILABLE
LEASE_LOST
EXECUTION_TIMEOUT
RECONCILIATION_FAILED
DATABASE_COMMIT_FAILED
SNAPSHOT_ID_CONTENT_MISMATCH
```

Examples:

``` text
SNAPSHOT_READ_FAILED           -> potentially retryable
DATABASE_COMMIT_FAILED         -> retryable
SCHEMA_UNSUPPORTED             -> not retryable
SNAPSHOT_ID_CONTENT_MISMATCH   -> not retryable
```

Phase 8 does not implement automatic retries.

------------------------------------------------------------------------

## 16. Duplicate Active Request

If the same snapshot is already RUNNING for the same source/scope, a
second request must not create another mutating execution.

It should return or reference the active execution, or return a
deterministic busy/conflict response according to the API contract.

If a different snapshot arrives for the same source/scope while an
import is active, it is also BUSY.

No queue is introduced.

------------------------------------------------------------------------

## 17. Checkpoints

Persist append-only phase-level checkpoints:

``` text
SNAPSHOT_READ
NORMALIZATION_COMPLETED
VALIDATION_COMPLETED
RECONCILIATION_COMPLETED
CANONICAL_COMMIT_COMPLETED
```

A checkpoint contains enough metadata for operational diagnosis,
including execution ID and timestamp.

Checkpoints are not a record-level resume mechanism.

------------------------------------------------------------------------

## 18. No Partial Resume

Phase 8 does not resume from an arbitrary record or checkpoint.

After failure, a RETRY may restart from snapshot acquisition/read.

The checkpoints exist for:

``` text
diagnosis
timing
audit
failure localization
future recovery evolution
```

not complex continuation semantics.

------------------------------------------------------------------------

## 19. Reconciliation Plan

Before canonical commit, Phase 7 reconciliation results should be
represented as a bounded deterministic plan equivalent to:

``` text
ImportPlan
  creates[]
  updates[]
  unchanged[]
  conflicts[]
  rejections[]
  missingTransitions[]
```

This makes the commit deterministic and testable.

It must not alter the Phase 7 reconciliation rules.

No LLM or Agent participates.

------------------------------------------------------------------------

## 20. Atomic Canonical Commit

Canonical mutation for a source/scope snapshot should be atomic wherever
practical.

Flow:

``` text
read
normalize
validate
build reconciliation plan
      ↓
BEGIN canonical transaction
      ↓
verify lease + fencing token
apply canonical creates/updates
apply SourceReferences
apply conflicts/rejections
apply valid missing transitions
append required canonical-side state
      ↓
verify ownership if needed
COMMIT
```

If the commit fails:

``` text
canonical mutation rolls back
execution -> FAILED
failureCode = DATABASE_COMMIT_FAILED or bounded equivalent
```

No half-applied canonical snapshot is accepted.

------------------------------------------------------------------------

## 21. Durable Execution Metadata

Execution lifecycle metadata must survive canonical rollback.

Use separate durable transaction boundaries for execution
status/audit/checkpoints where required.

This allows SNIP to retain:

``` text
FAILED
TIMED_OUT
LEASE_LOST
```

even when canonical changes are rolled back.

------------------------------------------------------------------------

## 22. Timeout / Watchdog

Phase 8 introduces a bounded import execution watchdog.

If the execution exceeds its configured overall limit:

``` text
TIMED_OUT
failureCode = EXECUTION_TIMEOUT
```

The execution must not later transition to COMPLETED.

A late/zombie worker must be prevented from committing by
execution-state and fencing checks.

This watchdog applies to integration imports only.

Do not redesign the Phase 5 Agent timeout technical debt in Phase 8.

------------------------------------------------------------------------

## 23. Lease Recovery

An execution that is RUNNING but whose lease has expired is abandoned.

Before/while accepting future work, the runtime may recover such
executions deterministically:

``` text
status = FAILED
failureCode = LEASE_EXPIRED or LEASE_LOST
```

Do not create a complex distributed recovery coordinator.

------------------------------------------------------------------------

## 24. Startup Recovery

On application startup, detect obviously abandoned RUNNING executions
whose leases are no longer valid.

Mark them terminal according to the bounded failure model.

Do not steal an apparently valid lease.

Phase 8 is designed for multi-instance readiness but does not claim full
Kubernetes multi-replica operational proof.

------------------------------------------------------------------------

## 25. Real Concurrency Proof

Concurrency safety must be proven with **real multi-threaded PostgreSQL
integration tests**, not only mocked lease transitions.

At minimum prove:

``` text
same source/scope
two concurrent contenders
exactly one active mutating owner
```

and fencing protection against stale ownership.

Use the repository's PostgreSQL Testcontainers conventions.

------------------------------------------------------------------------

## 26. Independent Scope Concurrency

The lease design must allow independent source/scope keys to execute
concurrently.

For example:

``` text
ERICSSON_FIXTURE / DEFAULT
NOKIA_FIXTURE    / DEFAULT
```

must not contend on a global lock.

The implementation should include a deterministic proof where practical.

------------------------------------------------------------------------

## 27. No Import Queue

Phase 8 does not introduce:

``` text
Kafka import commands
RabbitMQ
job queues
worker pools
distributed job scheduler
```

A busy same-scope import is rejected/reported rather than queued.

------------------------------------------------------------------------

## 28. No Automatic Retry

Retry is explicit.

Do not implement:

``` text
retry forever
automatic exponential retry
background retry scheduler
```

The runtime may classify a failure as retryable, but it does not
automatically re-execute it.

------------------------------------------------------------------------

## 29. Cancellation

Do not expose fake cancellation.

`CANCELLED` is not required for Phase 8 unless safe cooperative
cancellation already exists naturally.

Cancellation remains deferred.

------------------------------------------------------------------------

## 30. Phase 7 Reconciliation Ownership

Phase 7 remains authoritative for:

``` text
canonical normalization
canonical validation
CREATE
UPDATE
UNCHANGED
CONFLICT
REJECT
SourceReference semantics
source authority
MISSING / ACTIVE behavior
conflict safety
```

Phase 8 must not redesign these rules.

------------------------------------------------------------------------

## 31. Phase 7 Vendor Boundary

Phase 8 still uses:

``` text
EricssonFixtureAdapter
NokiaFixtureAdapter
```

No real ENM or NetAct connector is introduced.

Vendor-specific DTOs still terminate at the adapter boundary.

------------------------------------------------------------------------

## 32. Digital Twin Relationship

Phase 6 semantics remain unchanged.

A successful NEW/RETRY import that changes Twin-relevant canonical state
may make a Twin STALE.

Phase 8 must not invoke `TwinSynchronizationService`.

A REPLAY performs zero canonical mutation and therefore must not create
additional Twin changes or versions.

------------------------------------------------------------------------

## 33. Agent Relationship

The five Phase 5 Agents remain unchanged.

No Integration Operations Agent is introduced.

Agents do not own:

``` text
lease acquisition
retry override
replay override
vendor adapters
import recovery
```

No Agent-to-MCP or Agent-to-vendor path is added.

------------------------------------------------------------------------

## 34. MCP / Governed Action Relationship

Phase 4 remains authoritative for governed actions.

Import execution is an integration control path, not a new MCP
capability.

Do not register vendor import/write MCP tools.

`APPLY_CELL_PARAMETER_CHANGE` remains HIGH / DENY.

------------------------------------------------------------------------

## 35. Telemetry and RAG

Phase 2 remains authoritative for telemetry/KPI ingestion.

Phase 8 does not add vendor streaming telemetry.

Operational imports are not vectorized into RAG.

------------------------------------------------------------------------

## 36. Persistence Direction

Prefer evolving Phase 7 `network_import_batch` with runtime fields
equivalent to:

``` text
execution_type
attempt_number
previous_execution_id
original_successful_execution_id
source_scope
canonical_snapshot_hash
failure_code
retryable
owner_instance_id
lease_fencing_token
requested_at
```

Add persistence equivalent to:

``` text
network_import_lease
network_import_checkpoint
```

Reuse Phase 7 audit where appropriate.

Avoid parallel duplicate execution tables unless repository constraints
clearly require them.

------------------------------------------------------------------------

## 37. Immutable History

Terminal import executions are historical records.

Do not delete/replace failed attempts when retrying.

REPLAY is also a new immutable operational record referencing the
original successful execution.

This is intentionally stronger than some older carried technical debt
elsewhere in SNIP.

Do not use Phase 8 to redesign those older subsystems.

------------------------------------------------------------------------

## 38. Operational APIs

Preserve Phase 7 APIs where possible.

Expose enough runtime state to query:

``` text
imports/executions
single execution
execution checkpoints
execution type
attempt lineage
failure code
retryability
source/scope
snapshot identity
```

Do not break existing Phase 7 clients unnecessarily.

------------------------------------------------------------------------

## 39. Health / Observability

Expose/log runtime health equivalent to:

``` text
activeImports
expiredLeases
stuckExecutions
lastSuccessfulImportBySource
```

Metrics should include:

``` text
importLeaseAcquired
importLeaseRejected
importLeaseExpired
importRetries
importReplays
importTimeouts
importFailuresByCode
importConcurrentRequestRejected
importExecutionDurationMs
importCheckpointDurationMs
```

Retain Phase 7 metrics.

Correlate by:

``` text
executionId
sourceSystem
sourceScope
sourceSnapshotId
fencingToken
```

------------------------------------------------------------------------

## 40. Canonical Proof A --- Same-Scope Concurrency

Two real concurrent PostgreSQL-backed import attempts contend for the
same source/scope.

Expected:

``` text
one mutating owner
one BUSY / lease rejection
one canonical mutation path
```

------------------------------------------------------------------------

## 41. Canonical Proof B --- Independent Scope Concurrency

Different source/scope keys do not share a global lock.

Expected:

``` text
Ericsson DEFAULT lease != Nokia DEFAULT lease
```

------------------------------------------------------------------------

## 42. Canonical Proof C --- Successful Replay

Import snapshot successfully.

Submit identical successful snapshot again.

Expected:

``` text
new REPLAY execution
references original success
canonicalMutation = false
no CREATE
no UPDATE
no MISSING reapplication
no duplicate SourceReference
```

------------------------------------------------------------------------

## 43. Canonical Proof D --- Failed Retry

Force a deterministic retryable failure.

Attempt 1:

``` text
FAILED
attemptNumber = 1
```

Retry:

``` text
RETRY
attemptNumber = 2
previousExecutionId = attempt1
```

Both remain persisted.

------------------------------------------------------------------------

## 44. Canonical Proof E --- Lease Expiry / Zombie Fencing

Execution A owns token N.

Its lease expires.

Execution B acquires token N+1.

Execution A attempts a late commit.

Expected:

``` text
A -> FAILED / LEASE_LOST
A cannot commit
B remains authoritative owner
```

------------------------------------------------------------------------

## 45. Canonical Proof F --- Snapshot ID Content Mismatch

Submit the same source/scope/sourceSnapshotId with a different canonical
fingerprint.

Expected:

``` text
REJECTED
SNAPSHOT_ID_CONTENT_MISMATCH
retryable = false
no canonical mutation
```

------------------------------------------------------------------------

## 46. Canonical Proof G --- Twin Stability on Replay

A successful import changes Twin-relevant state and may make the Twin
STALE.

Replay the same successful snapshot.

Expected:

``` text
no canonical mutation
Twin state not additionally changed
no automatic Twin version
```

------------------------------------------------------------------------

## 47. Canonical Proof H --- Atomic Rollback

Force canonical commit failure.

Expected:

``` text
canonical transaction rolled back
execution FAILED
failure persisted
lease released/allowed to expire safely
no half-applied operational state
```

------------------------------------------------------------------------

## 48. Security

No production vendor credentials or endpoints.

Lease ownership uses a service-generated `ownerInstanceId`, not
user-provided authority.

API callers cannot supply fencing tokens or claim lease ownership.

No live network write path is introduced.

------------------------------------------------------------------------

## 49. Multi-Instance Readiness

Database lease/fencing must be designed so multiple future application
instances can coordinate.

Phase 8 does not require:

``` text
Kubernetes deployment
multi-pod E2E
leader election
distributed scheduler
```

Do not overclaim distributed production readiness.

------------------------------------------------------------------------

## 50. Real Connector Relationship

Phase 8 creates the runtime contract future connectors must use.

Future conceptual path:

``` text
EricssonEnmAdapter
      ↓
Phase 8 Reliable Import Runtime
      ↓
Phase 7 Canonical Reconciliation
```

Real connectors remain deferred.

------------------------------------------------------------------------

## 51. Dry-Run Import

A reconciliation plan may make future dry-run imports possible, but
Phase 8 does not expose or implement a dry-run import product/API.

------------------------------------------------------------------------

## 52. Explicitly Out of Scope

Do not implement:

``` text
real Ericsson ENM
real Nokia NetAct
production vendor credentials
vendor REST/database/SFTP connectivity
SNMP
NETCONF
gNMI
3GPP Bulk CM
scheduled imports
continuous synchronization
incremental change feed
import command queue
worker pool
automatic retry loop
automatic polling
cancellation API
record-level resume
dry-run import API
vendor telemetry
vendor writes
AI reconciliation
field-level mastership
automatic conflict resolution
automatic Twin synchronization
Integration Operations Agent
vendor MCP tools
Kubernetes multi-replica proof
Phase 9
```

------------------------------------------------------------------------

## 53. Locked Phase 8 Decisions

-   Phase: **Integration Runtime Hardening & Reliable Synchronization**
-   Baseline: `10bcd3369d68a3304687a007324da4566e048098`
-   Vendor connectivity: fixture adapters remain
-   Phase 7 reconciliation: unchanged/frozen
-   Runtime unit: durable import execution
-   Source-scope key: `sourceSystem + sourceScope`
-   Fixture scope: `DEFAULT`
-   Concurrency: one active mutating import per source/scope
-   Cross-scope parallelism: allowed
-   Coordination: PostgreSQL lease
-   Heartbeat: required
-   Lease expiration: required
-   Fencing token: required
-   Real concurrency proof: multi-threaded PostgreSQL integration tests
-   Execution types: NEW / RETRY / REPLAY
-   Successful replay: new immutable REPLAY record
-   Replay canonical mutation: none
-   Replay references original successful execution: yes
-   Retry: new immutable attempt
-   Attempt lineage: persisted
-   Attempt history: immutable
-   Automatic retry: prohibited
-   Queueing: prohibited
-   Snapshot fingerprint: required
-   Snapshot ID content immutability: required
-   Same ID/different content: REJECT
-   Checkpoints: append-only phase-level
-   Partial resume: prohibited/deferred
-   Canonical commit: atomic
-   Execution metadata: durable independently of canonical rollback
-   Import watchdog: required
-   Late completion after timeout: prohibited
-   Zombie commit after lease loss: prohibited by fencing
-   Startup abandoned-execution recovery: bounded
-   Cancellation: deferred
-   Scheduler/polling: deferred
-   Real ENM/NetAct: deferred
-   Vendor telemetry: excluded
-   Agents: unchanged
-   MCP: unchanged; no vendor tools
-   Digital Twin: may become STALE; no auto-sync
-   Live network writes: prohibited
-   Phase 9: not started

------------------------------------------------------------------------

## 54. ADR Direction

Expected sequential ADRs after Phase 7 ADR 050:

``` text
051 Import Execution Runtime
052 Source-Scope Lease and Fencing
053 NEW / RETRY / REPLAY Semantics
054 Snapshot Identity and Canonical Fingerprint
055 Immutable Import Attempt History
056 Import Checkpoints and Recovery
057 Atomic Canonical Commit
058 Integration Runtime Timeout and Watchdog
```

------------------------------------------------------------------------

## 55. Architectural Outcome

At completion:

``` text
Vendor Fixture
     ↓
Phase 7 Adapter
     ↓
Snapshot Identity
     ↓
Phase 8 Runtime Guard
     ├─ Replay classification
     ├─ Retry lineage
     ├─ Lease
     ├─ Heartbeat
     ├─ Fencing
     ├─ Watchdog
     └─ Checkpoints
     ↓
Phase 7 Deterministic Reconciliation
     ↓
Atomic Canonical Commit
     ↓
Immutable Execution History
     ↓
Operational State
```

SNIP progression becomes:

``` text
KNOW
  ↓
UNDERSTAND
  ↓
OBSERVE CHANGE
  ↓
ASSESS
  ↓
ACT SAFELY
  ↓
COORDINATE INTELLIGENTLY
  ↓
SIMULATE BEFORE CHANGE
  ↓
INTEGRATE ACROSS VENDORS
  ↓
SYNCHRONIZE RELIABLY
```

Only after this runtime is proven should the architecture consider
connecting real Ericsson/Nokia systems.
