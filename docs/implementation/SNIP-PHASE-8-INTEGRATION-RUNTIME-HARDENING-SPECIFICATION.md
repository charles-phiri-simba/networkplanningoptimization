# SNIP Phase 8 --- Integration Runtime Hardening & Reliable Synchronization Implementation Specification

## 1. Authority and Baseline

This is the bounded Cursor implementation contract for Phase 8.

Start exactly from:

``` text
Branch: main
Commit: 10bcd3369d68a3304687a007324da4566e048098
Commit message: feat: establish SNIP Phase 7 multi-vendor network integration foundation
Phase 7: ARCHITECTURALLY ACCEPTED / FROZEN
CI: success
Maven: 116 tests, 0 failures
Go: tests/build PASS
Working tree: clean
Phase 8: not started
```

Read `SNIP-PHASE-8-INTEGRATION-RUNTIME-HARDENING-ARCHITECTURE.md`
completely before modifying code.

The architecture document is authoritative.

Implement Phase 8 only.

Do not start Phase 9.

Do not commit or push a Phase 8 baseline until architectural review,
freeze, and explicit authorization.

------------------------------------------------------------------------

## 2. Objective

Implement a reliable execution envelope around the accepted Phase 7
import pipeline:

``` text
Import Request
      ↓
Source/Scope Resolution
      ↓
Snapshot Fingerprint
      ↓
NEW / RETRY / REPLAY
      ↓
PostgreSQL Lease + Fencing
      ↓
Durable Execution
      ↓
Checkpointed Phase 7 Pipeline
      ↓
Atomic Canonical Commit
      ↓
Immutable Attempt History
```

The implementation must prove runtime reliability without introducing
real vendor connectivity.

------------------------------------------------------------------------

## 3. Preserve Phases 1--7

Do not redesign accepted architecture.

Especially preserve:

``` text
Phase 7 vendor adapter boundary
Phase 7 canonical model
Phase 7 deterministic reconciliation
Phase 7 source authority/conflict semantics
Phase 7 ACTIVE/MISSING semantics
Phase 6 manual/on-demand Twin synchronization
Phase 6 STALE simulation blocking
Phase 5 five-Agent model
Phase 5 Agent-to-MCP prohibition
Phase 4 governed action authority
APPLY_CELL_PARAMETER_CHANGE = HIGH / DENY
Phase 2 telemetry boundary
RAG separation from operational state
```

No live network write path.

------------------------------------------------------------------------

## 4. Repository Discovery Before Editing

Before implementation:

1.  verify HEAD equals the Phase 7 SHA;
2.  inspect the Phase 7 import entities/services/APIs/migration;
3.  identify the current `NetworkImportBatch` lifecycle;
4.  identify current transaction boundaries;
5.  identify the current source snapshot identifiers;
6.  identify existing audit/metrics conventions;
7.  identify current Twin freshness calculation;
8.  identify the next Flyway version;
9.  update the Cursor architecture rule from Phase 7-only to Phase
    8-only while preserving frozen decisions.

Do not invent a parallel architecture if existing types can be safely
evolved.

------------------------------------------------------------------------

## 5. Evolve NetworkImportBatch

Prefer evolving the Phase 7 import batch into the durable execution
record.

Add/equivalent fields:

``` text
executionType
attemptNumber
previousExecutionId
originalSuccessfulExecutionId
sourceScope
canonicalSnapshotHash
failureCode
retryable
ownerInstanceId
leaseFencingToken
requestedAt
```

Retain existing Phase 7 fields/counts/provenance.

Do not destroy Phase 7 historical semantics.

------------------------------------------------------------------------

## 6. Required Enums / Value Types

Implement bounded values equivalent to:

``` text
ImportExecutionType:
  NEW
  RETRY
  REPLAY

ImportExecutionStatus:
  REQUESTED
  RUNNING
  COMPLETED
  FAILED
  TIMED_OUT
  REJECTED

ImportFailureCode:
  ADAPTER_ERROR
  SNAPSHOT_READ_FAILED
  SCHEMA_UNSUPPORTED
  VALIDATION_FATAL
  LEASE_UNAVAILABLE
  LEASE_LOST
  LEASE_EXPIRED
  EXECUTION_TIMEOUT
  RECONCILIATION_FAILED
  DATABASE_COMMIT_FAILED
  SNAPSHOT_ID_CONTENT_MISMATCH
```

Reuse existing status types where compatible rather than duplicating
them.

------------------------------------------------------------------------

## 7. Source Scope

Introduce a bounded source scope representation.

For existing fixture adapters:

``` text
sourceScope = DEFAULT
```

Lease key:

``` text
sourceSystem + sourceScope
```

Do not use vendor alone as the lock key.

Do not introduce a global import mutex.

------------------------------------------------------------------------

## 8. PostgreSQL Lease Persistence

Create persistence equivalent to:

``` text
network_import_lease
```

Required data:

``` text
lease_key
source_system
source_scope
owner_execution_id
owner_instance_id
fencing_token
acquired_at
heartbeat_at
expires_at
```

Enforce one current lease row per source/scope.

Use database-safe atomic acquisition/update semantics.

------------------------------------------------------------------------

## 9. Fencing Token

The fencing token must monotonically increase whenever ownership of a
source/scope is newly acquired after prior ownership.

The canonical commit path must validate current ownership/token.

Do not trust an in-memory boolean such as `hasLease`.

A stale execution must be unable to commit.

------------------------------------------------------------------------

## 10. Lease Service

Implement responsibilities equivalent to:

``` text
ImportLeaseService
  acquire(sourceSystem, sourceScope, executionId, ownerInstanceId)
  heartbeat(...)
  assertOwnership(...)
  release(...)
  recoverExpired(...)
```

Names may follow repository conventions.

Lease operations must be deterministic and transactional.

------------------------------------------------------------------------

## 11. Instance Identity

Generate/configure a service-owned runtime instance ID.

Do not accept it from the import API request.

It exists only for runtime ownership/diagnostics.

------------------------------------------------------------------------

## 12. Lease Configuration

Add configuration equivalent to:

``` text
snip.integration.lease-duration
snip.integration.heartbeat-interval
snip.integration.execution-timeout
```

Use repository-consistent Spring configuration.

Validate that heartbeat interval is meaningfully shorter than lease
duration.

Tests may use shorter deterministic values.

------------------------------------------------------------------------

## 13. Snapshot Fingerprint

Compute a deterministic `canonicalSnapshotHash` from normalized
canonical snapshot content.

Requirements:

-   stable ordering;
-   stable representation;
-   vendor transport formatting must not influence the canonical hash;
-   equivalent content produces the same hash;
-   changed canonical content produces a different hash.

Use a standard cryptographic digest such as SHA-256.

Do not include volatile fields such as import time in the fingerprint.

------------------------------------------------------------------------

## 14. Snapshot Identity Lookup

Lookup identity by:

``` text
sourceSystem
sourceScope
sourceSnapshotId
```

Before mutation, compare the incoming canonical fingerprint with prior
execution history.

------------------------------------------------------------------------

## 15. Same Snapshot ID / Different Content

If a prior execution exists for the same identity and stored canonical
hash differs:

Create/terminalize an execution as:

``` text
executionType = NEW or classified request context
status = REJECTED
failureCode = SNAPSHOT_ID_CONTENT_MISMATCH
retryable = false
```

No lease-backed canonical mutation may occur.

No reconciliation commit.

------------------------------------------------------------------------

## 16. NEW Classification

Classify as NEW when no successful or failed attempt history exists for
that immutable snapshot identity.

Initial:

``` text
attemptNumber = 1
previousExecutionId = null
```

------------------------------------------------------------------------

## 17. RETRY Classification

If prior attempt for the same immutable snapshot failed/timed out and is
retryable:

``` text
executionType = RETRY
attemptNumber = prior attempt + 1
previousExecutionId = prior execution
```

Create a new row.

Do not update/delete the old failed attempt.

------------------------------------------------------------------------

## 18. Non-Retryable Failure

If the latest relevant failed/rejected execution is not retryable, a
resubmission must not silently run as RETRY.

Return a deterministic conflict/rejection response and preserve history.

------------------------------------------------------------------------

## 19. REPLAY Classification

If the same immutable snapshot previously COMPLETED successfully:

Create a new execution:

``` text
executionType = REPLAY
status = COMPLETED
originalSuccessfulExecutionId = successful execution
canonicalMutation = false (DTO/derived semantic)
```

Do not reapply canonical state.

Do not rerun missing transitions.

Do not create/update SourceReferences.

Do not acquire a mutating lease unless implementation requires a short
non-mutating consistency check; prefer avoiding it.

Append replay audit.

------------------------------------------------------------------------

## 20. Replay Counts

A REPLAY execution should clearly report zero mutation counts:

``` text
entitiesCreated = 0
entitiesUpdated = 0
```

Other counts may be zero or derived according to the DTO contract, but
must not misleadingly imply reconciliation mutation.

------------------------------------------------------------------------

## 21. Active Duplicate Request

When an execution for the same source/scope is actively RUNNING:

-   same snapshot request: return/reference active execution or
    deterministic HTTP 409;
-   different snapshot same scope: HTTP 409/BUSY;
-   do not queue;
-   do not start a second mutating execution.

Document exact API behavior.

------------------------------------------------------------------------

## 22. Durable Checkpoints

Add persistence equivalent to:

``` text
network_import_checkpoint
```

Fields equivalent to:

``` text
checkpoint_id
execution_id
checkpoint_type
recorded_at
details/summary if needed
```

Append-only types:

``` text
SNAPSHOT_READ
NORMALIZATION_COMPLETED
VALIDATION_COMPLETED
RECONCILIATION_COMPLETED
CANONICAL_COMMIT_COMPLETED
```

Do not update/delete checkpoints.

------------------------------------------------------------------------

## 23. No Record-Level Resume

A RETRY restarts the import pipeline.

Do not persist every normalized record merely to resume mid-stream.

Do not implement checkpoint resume logic.

------------------------------------------------------------------------

## 24. Reconciliation Plan

Refactor only as necessary so Phase 7 reconciliation can produce a
deterministic plan before mutation.

Equivalent shape:

``` text
creates
updates
unchanged
conflicts
rejections
missingTransitions
```

Do not change the decision rules that populate those categories.

Add regression tests proving Phase 7 outcomes are unchanged.

------------------------------------------------------------------------

## 25. Atomic Canonical Commit

Apply the mutating reconciliation plan inside a single canonical
transaction where repository design permits.

Before commit, assert:

``` text
execution status is RUNNING
lease owner == execution
fencing token == execution token
lease valid
```

If ownership is lost:

``` text
FAILED
LEASE_LOST
```

and canonical transaction rolls back.

------------------------------------------------------------------------

## 26. Execution Metadata Transaction Boundary

Execution status/failure/checkpoint history must remain durable if
canonical mutation rolls back.

Use explicit Spring transaction propagation/services where necessary.

Do not swallow exceptions that should terminalize an execution.

------------------------------------------------------------------------

## 27. Heartbeat

While a mutating import is RUNNING, renew the lease at the configured
interval or at deterministic safe phase boundaries plus scheduled
heartbeat where necessary for a long operation.

Tests must prove lease expiry behavior.

Avoid thread leaks.

Shut heartbeat activity down when execution becomes terminal.

------------------------------------------------------------------------

## 28. Watchdog

Implement a bounded import execution watchdog.

When timeout is reached:

``` text
status = TIMED_OUT
failureCode = EXECUTION_TIMEOUT
retryable = true (unless architecture-consistent reason says otherwise)
```

The timed-out execution must never later become COMPLETED.

Late canonical commit must fail because status/lease/fencing no longer
permits it.

Do not change Phase 5 Agent timeout implementation.

------------------------------------------------------------------------

## 29. Expired Lease Recovery

Implement bounded recovery for:

``` text
RUNNING execution
+
expired/missing lease ownership
```

Terminalize as FAILED with a bounded lease failure code.

Recovery may run:

-   on startup;
-   before acquisition for the affected source/scope;
-   through a small runtime recovery service.

Do not add a general scheduler.

------------------------------------------------------------------------

## 30. Lease Release

On normal terminal outcome, release ownership safely.

On process death, expiry is the fallback.

A stale execution must not be able to delete/release a newer owner's
lease.

Use fencing/owner predicates.

------------------------------------------------------------------------

## 31. Flyway

Phase 7 introduced its migration before this phase. Inspect the
repository and use the next sequential migration.

Expected if Phase 7 used V8:

``` text
V9__integration_runtime_hardening.sql
```

Do not assume without checking.

Migration should evolve import execution persistence and add
lease/checkpoint structures.

No destructive reset of Phase 7 data.

------------------------------------------------------------------------

## 32. Import APIs

Preserve Phase 7 import trigger APIs.

Extend returned DTOs with runtime fields where appropriate:

``` text
executionType
attemptNumber
previousExecutionId
originalSuccessfulExecutionId
sourceScope
canonicalSnapshotHash
status
failureCode
retryable
leaseFencingToken (only if safe/appropriate for read DTO; never accepted from client)
```

Do not expose a client ability to set ownership/fencing.

------------------------------------------------------------------------

## 33. Execution Read APIs

Expose or extend read APIs so operators can inspect:

``` text
GET /api/v1/integration/imports
GET /api/v1/integration/imports/{executionId}
GET /api/v1/integration/imports/{executionId}/checkpoints
```

If repository naming strongly favors `/executions`, aliases/new
endpoints are acceptable without breaking existing Phase 7 APIs.

Pagination is not required unless already present.

------------------------------------------------------------------------

## 34. Retry API

Prefer **resubmitting the same configured import request** and letting
runtime classification produce RETRY.

Do not add a separate retry endpoint unless it materially simplifies the
existing API and remains deterministic.

No caller may override `retryable`.

------------------------------------------------------------------------

## 35. Replay API

The same normal import endpoint may classify a repeated successful
snapshot as REPLAY.

Do not require a special replay endpoint.

------------------------------------------------------------------------

## 36. Fixture Control for Tests

Add only the fixture/test hooks necessary to deterministically prove:

``` text
long-running import
retryable failure
same ID changed content
canonical commit failure
lease expiry
```

Keep such behavior test/profile-specific or bounded fixture variants.

Do not introduce production backdoors.

------------------------------------------------------------------------

## 37. Required Real Concurrency Test --- Same Scope

Use real multi-threaded execution against Testcontainers PostgreSQL.

Arrange two contenders for the same source/scope while the first holds
the lease.

Assert:

``` text
exactly one lease owner
second request rejected/busy
only one mutating canonical execution
no duplicate canonical changes
```

Do not satisfy this criterion with mocks.

------------------------------------------------------------------------

## 38. Required Real Concurrency Test --- Different Scope

Use real PostgreSQL-backed concurrent execution for independent
source/scope keys where practical.

Assert that Ericsson DEFAULT and Nokia DEFAULT can independently own
leases.

No global lock.

------------------------------------------------------------------------

## 39. Required Fencing Test

Use real PostgreSQL persistence to prove:

1.  execution A acquires token N;
2.  A loses/expires lease;
3.  execution B acquires token N+1;
4.  A attempts commit/ownership assertion;
5.  A is rejected as LEASE_LOST;
6.  A cannot mutate canonical state.

This is a release-blocking Phase 8 proof.

------------------------------------------------------------------------

## 40. Required Replay Test

1.  import a normal snapshot successfully;
2.  submit identical snapshot again;
3.  assert new immutable REPLAY execution;
4.  assert reference to original success;
5.  assert zero canonical mutation;
6.  assert no duplicate SourceReference/neighbour;
7.  assert original execution unchanged.

------------------------------------------------------------------------

## 41. Required Retry Test

1.  cause deterministic retryable failure;
2.  assert FAILED attempt 1;
3.  resubmit same immutable snapshot after removing failure condition;
4.  assert RETRY attempt 2;
5.  assert previousExecutionId;
6.  assert attempt 1 still exists unchanged;
7.  assert attempt 2 can complete.

------------------------------------------------------------------------

## 42. Required Snapshot Mismatch Test

1.  import snapshot ID X/content A;
2.  submit X/content B with changed canonical hash;
3.  assert REJECTED;
4.  assert `SNAPSHOT_ID_CONTENT_MISMATCH`;
5.  assert non-retryable;
6.  assert no canonical mutation.

------------------------------------------------------------------------

## 43. Required Timeout Test

Use deterministic test configuration/fixture delay.

Assert:

``` text
TIMED_OUT
EXECUTION_TIMEOUT
no later COMPLETED transition
no late canonical commit
```

Avoid flaky wall-clock assumptions; use bounded synchronization
primitives where possible.

------------------------------------------------------------------------

## 44. Required Atomic Rollback Test

Force a failure during canonical commit.

Assert:

``` text
all canonical mutation rolled back
execution FAILED
failure history persisted
checkpoint history preserved as appropriate
lease no longer grants stale ownership
```

------------------------------------------------------------------------

## 45. Required Startup/Expired Recovery Test

Persist/simulate an abandoned RUNNING execution with expired lease.

Run recovery.

Assert deterministic terminal failure and no canonical mutation.

------------------------------------------------------------------------

## 46. Required Twin Replay Stability Test

Use an isolated Phase 6/7 integration flow:

1.  establish CURRENT Twin;
2.  successful import changes Twin-relevant state;
3.  Twin becomes STALE;
4.  replay same successful snapshot;
5.  no canonical mutation;
6.  Twin remains STALE;
7.  no automatic new Twin version.

Do not call `TwinSynchronizationService`.

------------------------------------------------------------------------

## 47. Phase 7 Regression Tests

Retain/prove:

``` text
Ericsson normal import
Nokia normal import
equivalent normalization
same-source UPDATE
UNCHANGED
CONFLICT
REJECT
MISSING
partial-snapshot safety
reappearance ACTIVE
CELL-001 Twin staleness
```

Runtime hardening must not change reconciliation semantics.

------------------------------------------------------------------------

## 48. Metrics / Logging

Add metrics/logging equivalent to:

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

Correlate logs with:

``` text
executionId
sourceSystem
sourceScope
sourceSnapshotId
fencingToken
```

Do not log secrets or full raw vendor payloads.

------------------------------------------------------------------------

## 49. Runtime Health

Expose through existing health/diagnostic conventions enough information
to determine:

``` text
active imports
expired leases
stuck/abandoned executions
last successful import per source
```

Do not add a new observability stack.

------------------------------------------------------------------------

## 50. Security / Control Review

Completion report must verify:

-   client cannot provide fencing token;
-   client cannot provide ownerInstanceId;
-   no vendor credentials;
-   no ENM/NetAct endpoint;
-   no vendor write method;
-   no vendor MCP tool;
-   no Agent import override;
-   no LLM reconciliation;
-   no automatic retry;
-   no automatic Twin synchronization;
-   no live network mutation.

------------------------------------------------------------------------

## 51. No Scheduler / Queue

Do not add:

``` text
@Scheduled vendor polling
Kafka import command topic
RabbitMQ
worker queue
background retry queue
```

Imports remain explicit/on-demand.

------------------------------------------------------------------------

## 52. No Real Connectors

Do not implement real:

``` text
Ericsson ENM
Nokia NetAct
REST vendor client
vendor database client
SFTP
SNMP
NETCONF
gNMI
3GPP Bulk CM
```

Fixture adapters remain the source boundary.

------------------------------------------------------------------------

## 53. No Phase 9

No Phase 9 architecture, code, migrations, ADRs or placeholder
implementation.

A recommendation in the completion report is allowed; implementation is
not.

------------------------------------------------------------------------

## 54. ADRs

Create sequential ADRs after Phase 7 ADR 050:

``` text
051 Import Execution Runtime
052 Source-Scope Lease and Fencing
053 NEW RETRY REPLAY Semantics
054 Snapshot Identity and Canonical Fingerprint
055 Immutable Import Attempt History
056 Import Checkpoints and Recovery
057 Atomic Canonical Commit
058 Integration Runtime Timeout and Watchdog
```

Use established ADR format.

------------------------------------------------------------------------

## 55. Documentation

Add root/docs copies following prior phase conventions:

``` text
SNIP-PHASE-8-INTEGRATION-RUNTIME-HARDENING-ARCHITECTURE.md
SNIP-PHASE-8-INTEGRATION-RUNTIME-HARDENING-SPECIFICATION.md
```

Update:

``` text
README.md
docs/implementation/SNIP-IMPLEMENTATION-CONTEXT.md
docs/implementation/SNIP-IMPLEMENTATION-STATUS.md
.cursor/rules/snip-architecture.mdc
```

Phase 7 must remain recorded as frozen.

------------------------------------------------------------------------

## 56. Required Test Commands

Run:

``` text
mvn -B test
go test ./...
go build ./cmd/simulator
```

All Phase 1--7 regressions plus new Phase 8 tests must pass.

Do not require Ollama, ENM, NetAct, external vendor systems, production
credentials or an external PostgreSQL server in default CI.

Use Testcontainers PostgreSQL for runtime concurrency proof.

------------------------------------------------------------------------

## 57. Acceptance --- Baseline

-   [ ] Starts exactly from `10bcd3369d68a3304687a007324da4566e048098`.
-   [ ] Phase 7 remains architecturally frozen.
-   [ ] Phase 1--7 regressions pass.
-   [ ] Phase 9 not started.

------------------------------------------------------------------------

## 58. Acceptance --- Runtime Execution

-   [ ] Import is represented as durable execution.
-   [ ] Source scope persisted.
-   [ ] NEW / RETRY / REPLAY persisted.
-   [ ] Attempt number persisted.
-   [ ] Previous attempt lineage persisted.
-   [ ] Original successful execution reference persisted for REPLAY.
-   [ ] Terminal attempt history immutable.
-   [ ] Failed attempts are not replaced/deleted.

------------------------------------------------------------------------

## 59. Acceptance --- Lease / Concurrency

-   [ ] PostgreSQL lease implemented.
-   [ ] Lease key is sourceSystem + sourceScope.
-   [ ] No global import lock.
-   [ ] Heartbeat implemented.
-   [ ] Expiration implemented.
-   [ ] Fencing token implemented.
-   [ ] Stale owner cannot release newer lease.
-   [ ] Stale owner cannot commit.
-   [ ] Same-scope contention proven with real multi-threaded PostgreSQL
    test.
-   [ ] Different scopes do not contend globally.

------------------------------------------------------------------------

## 60. Acceptance --- Snapshot Identity

-   [ ] Deterministic canonical fingerprint implemented.
-   [ ] Volatile metadata excluded from hash.
-   [ ] Same content produces same hash.
-   [ ] Changed canonical content changes hash.
-   [ ] Same snapshot ID/different hash rejected.
-   [ ] Mismatch performs no canonical mutation.

------------------------------------------------------------------------

## 61. Acceptance --- Replay

-   [ ] Successful duplicate classified REPLAY.
-   [ ] New immutable REPLAY execution created.
-   [ ] REPLAY references original success.
-   [ ] REPLAY does not CREATE.
-   [ ] REPLAY does not UPDATE.
-   [ ] REPLAY does not rerun MISSING transitions.
-   [ ] REPLAY does not duplicate SourceReferences/neighbours.
-   [ ] REPLAY does not create Twin version.

------------------------------------------------------------------------

## 62. Acceptance --- Retry

-   [ ] Retryable failure classified deterministically.
-   [ ] Explicit resubmission becomes RETRY.
-   [ ] RETRY creates new execution.
-   [ ] attemptNumber increments.
-   [ ] previousExecutionId set.
-   [ ] prior failed execution remains immutable.
-   [ ] no automatic retry loop.

------------------------------------------------------------------------

## 63. Acceptance --- Checkpoint / Recovery

-   [ ] Phase-level checkpoints append-only.
-   [ ] No record-level resume.
-   [ ] Expired RUNNING execution can be recovered to terminal failure.
-   [ ] Startup recovery bounded.
-   [ ] No scheduler introduced.

------------------------------------------------------------------------

## 64. Acceptance --- Atomicity / Timeout

-   [ ] Canonical commit is atomic.
-   [ ] Commit verifies lease/fencing.
-   [ ] Forced commit failure rolls back canonical changes.
-   [ ] Execution failure survives rollback.
-   [ ] Watchdog/timeout implemented.
-   [ ] TIMED_OUT cannot later become COMPLETED.
-   [ ] Late zombie commit blocked.

------------------------------------------------------------------------

## 65. Acceptance --- Existing Architecture

-   [ ] Phase 7 reconciliation semantics unchanged.
-   [ ] Vendor-specific DTOs remain at adapter boundary.
-   [ ] No real ENM/NetAct.
-   [ ] No vendor telemetry.
-   [ ] No vendor writes.
-   [ ] No vendor MCP tools.
-   [ ] Five-Agent model unchanged.
-   [ ] No Integration Operations Agent.
-   [ ] `APPLY_CELL_PARAMETER_CHANGE` remains HIGH / DENY.
-   [ ] No automatic Twin synchronization.
-   [ ] Phase 6 stale simulation blocking remains intact.

------------------------------------------------------------------------

## 66. Acceptance --- CI / Hygiene

-   [ ] `mvn -B test` passes.
-   [ ] Real concurrency tests run in Maven suite.
-   [ ] `go test ./...` passes.
-   [ ] `go build ./cmd/simulator` passes.
-   [ ] Default CI needs no Ollama/vendor systems.
-   [ ] No secrets/credentials committed.
-   [ ] No generated binaries/IDE/log/model/DB data committed.
-   [ ] ADRs 051--058 created.
-   [ ] Documentation/status/rule updated.
-   [ ] Phase 9 not started.

------------------------------------------------------------------------

## 67. Required Completion Report

Create:

``` text
docs/implementation/SNIP-PHASE-8-COMPLETION-REPORT.md
```

Include:

1.  Executive Summary
2.  Phase 7 Baseline Verification
3.  Scope Delivered
4.  Runtime Architecture
5.  Durable Import Execution
6.  Source Scope
7.  PostgreSQL Lease
8.  Heartbeat / Expiration
9.  Fencing Token
10. Execution Lifecycle
11. NEW / RETRY / REPLAY
12. Snapshot Identity
13. Canonical Fingerprint
14. Snapshot Immutability
15. Replay Semantics
16. Retry Semantics
17. Retryability / Failure Codes
18. Immutable Attempt History
19. Checkpoints
20. Recovery
21. Watchdog / Timeout
22. Reconciliation Plan
23. Atomic Canonical Commit
24. Transaction Boundaries
25. Persistence / Flyway
26. APIs
27. Same-Scope Concurrency Proof
28. Independent-Scope Concurrency Proof
29. Fencing / Zombie Proof
30. Replay Proof
31. Retry Proof
32. Snapshot Mismatch Proof
33. Timeout Proof
34. Atomic Rollback Proof
35. Startup Recovery Proof
36. Twin Replay Stability Proof
37. Phase 7 Regression
38. Phase 6 Twin Boundary
39. Phase 5 Agent Boundary
40. Phase 4 MCP Boundary
41. Telemetry / RAG Boundary
42. Observability
43. Runtime Health
44. Security / Zero-Live-Write Review
45. Tests
46. Local E2E Evidence
47. ADRs
48. Performance
49. Acceptance PASS/FAIL
50. Known Limitations
51. Technical Debt
52. Lessons Learned
53. Recommended Next Phase
54. Architectural Questions

End with exactly one:

``` text
PHASE 8 STATUS: ACCEPTANCE RECOMMENDED
```

or:

``` text
PHASE 8 STATUS: ACCEPTANCE NOT RECOMMENDED
```

Do not mark Phase 8 architecturally accepted yourself.

------------------------------------------------------------------------

## 68. Architectural Questions for Review

Without broadening implementation, give recommendations on:

1.  whether PostgreSQL lease/fencing is sufficient for the next stage or
    whether an external coordination system is ever justified;
2.  whether real vendor connectors should follow immediately after Phase
    8 or whether additional integration security/credential architecture
    should be designed first;
3.  whether manual/on-demand imports should remain for the first real
    connector phase or scheduled synchronization should be introduced;
4.  whether raw snapshot archival/replay should now be reconsidered;
5.  whether multi-instance Kubernetes proof should precede production
    vendor connectivity;
6.  whether import cancellation is worth introducing before long-running
    real connectors.

------------------------------------------------------------------------

## 69. Git Safety

During implementation:

-   do not commit;
-   do not push;
-   do not amend Phase 7;
-   do not create a Phase 9 branch;
-   do not use the completion report to self-authorize acceptance.

When implementation is complete, leave the working tree as Phase 8
uncommitted work and stop for architectural review.

------------------------------------------------------------------------

## 70. Final Instruction to Cursor

Treat this as authorization for **Phase 8 only**.

The objective is:

> **Prove that SNIP can execute Phase 7 multi-vendor imports through a
> durable, concurrency-safe, replay-aware, retry-aware, fenced,
> checkpointed, timeout-bounded and atomically committed runtime before
> any real vendor system is connected.**

Preserve all Phase 1--7 architecture.

Do not connect to ENM or NetAct. Do not add vendor credentials. Do not
add vendor telemetry. Do not add vendor writes. Do not add automatic
retry. Do not add import queues or schedulers. Do not automatically
synchronize the Digital Twin. Do not start Phase 9.

When implementation and validation are complete:

1.  produce the Phase 8 completion report;
2.  leave all Phase 8 work uncommitted;
3.  do not push;
4.  STOP for architectural review.
