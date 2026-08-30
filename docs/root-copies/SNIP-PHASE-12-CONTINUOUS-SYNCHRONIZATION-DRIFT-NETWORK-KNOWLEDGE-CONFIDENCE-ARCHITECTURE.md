# SNIP Phase 12 --- Continuous Synchronization, Drift & Network Knowledge Confidence Architecture

**Repository:** `networkplanningoptimization`\
**Parent immutable baseline:**
`78e699380be37109cfdd2111dd0f29c7052709c3`\
**Architecture status:** `ACCEPTED`\
**Implementation status:** `COMPLETE`\
**Simulator/contract status:** `VERIFIED`\
**Real vendor continuous synchronization status:** `NOT YET VERIFIED`\
**Production ENM transport:** `NOT CONFIGURED`\
**Phase 12 Git baseline:** `NOT YET ESTABLISHED`\
**Phase 13 status:** `NOT STARTED`

------------------------------------------------------------------------

## 1. Purpose

Phase 12 defines the architecture by which SNIP evolves from governed,
manually initiated read-only vendor import into a continuously
synchronized network-intelligence platform.

Phase 11 established Ericsson ENM as the first real-vendor target behind
a strictly read-only connector and `EnmTransport`, while deliberately
leaving the actual production Ericsson interface unresolved. Phase 12
builds above that boundary. It introduces scheduled synchronization,
durable checkpoints, full/incremental/recovery semantics, source
freshness, drift intelligence, bounded recovery, and deterministic
operational network knowledge confidence.

> **Phase 12 teaches SNIP how to know whether its understanding of the
> network is current, complete, trustworthy, divergent, or recovering.
> It still does not teach SNIP how to change the network.**

## 2. Objective

Establish a production-grade synchronization control plane that safely
maintains SNIP canonical network state from authorized external sources,
detects stale or divergent knowledge, preserves durable source
continuity, performs bounded recovery, and exposes trustworthy
synchronization metadata to downstream intelligence without introducing
vendor mutation.

## 3. Frozen Parent Architecture

Phase 12 starts from immutable Phase 11 baseline:

``` text
78e699380be37109cfdd2111dd0f29c7052709c3
```

The following remain authoritative:

-   Phase 7 canonical reconciliation and provenance.
-   Phase 8 PostgreSQL lease/fencing and import authority.
-   Phase 9 TLS, hostname verification, read-only authorization, and
    application egress.
-   Phase 10 Azure Key Vault, Microsoft Entra Workload Identity,
    secret-level RBAC, per-session credential resolution, and no
    older-version fallback.
-   Phase 11 Ericsson ENM read-only target, `EnmTransport`, simulator
    contract, COMPLETE/PARTIAL/FAILED semantics, bounded acquisition,
    cooperative cancellation, and Agent/MCP/Phase 4 isolation.

Phase 12 MUST extend rather than replace those contracts.

## 4. Architectural Boundary

``` text
Synchronization Policy
        |
        v
Synchronization Scheduler
        |
        v
Synchronization Control Plane
        |
        v
Phase 8 Lease / Fencing
        |
        v
Phase 9 / 10 Security
        |
        v
Phase 11 Read-Only Connector
        |
        v
External Source
        |
        v
Snapshot / Incremental Observation
        |
        v
Validation + Reconciliation
        |
        v
Canonical Network State
        |
        +--> Checkpoint
        +--> Freshness
        +--> Drift
        +--> Source Health
        +--> Knowledge Confidence
```

The scheduler is an initiator only. It MUST NOT directly call vendor
transports, resolve credentials, reconcile entities, or update
checkpoints.

## 5. Read-Only Direction

The only vendor-state direction introduced by this phase is:

``` text
Vendor -> SNIP
```

Phase 12 MUST NOT introduce configuration writes, parameter changes,
commands, remediation, or any other `SNIP -> Vendor` network mutation.

## 6. Synchronization Control Plane

A first-class synchronization orchestration responsibility, conceptually
`SynchronizationControlPlane`, owns:

1.  due-source evaluation;
2.  synchronization-policy evaluation;
3.  execution admission;
4.  one-active-execution-per-source semantics;
5.  entry into Phase 8 lease/fencing;
6.  loading the last committed checkpoint;
7.  mode selection;
8.  bounded execution context creation;
9.  invocation of the Phase 11 connector/import path;
10. source-state validation;
11. pre-commit fencing revalidation;
12. canonical reconciliation;
13. safe checkpoint advancement;
14. drift evaluation;
15. freshness/source-health calculation;
16. knowledge-confidence calculation;
17. audit/metrics; and
18. durable execution completion.

It does not own vendor protocol logic, Azure SDK logic, secret storage,
TLS implementation, canonical mapper internals, or a new distributed
lock.

## 7. Legal Initiators

Legal initiation types are:

``` text
MANUAL
SCHEDULED
```

An authorized operator and the scheduler MUST converge on the same
synchronization control plane. Recovery is a governed synchronization
mode/request, not a bypass.

## 8. Synchronization Modes

Phase 12 defines:

``` text
FULL
INCREMENTAL
RECOVERY_FULL
```

### FULL

A complete authoritative observation of the configured source scope. A
successfully reconciled COMPLETE full observation may establish trusted
source truth and apply existing safe Phase 7 absence semantics. Hard
delete remains forbidden.

### INCREMENTAL

Consumes explicitly reported source changes after a durable committed
checkpoint. Incremental support is capability/profile-dependent. Phase
12 MUST NOT assume Ericsson ENM exposes a particular delta feed, event
stream, timestamp query, cursor, or notification mechanism.

### RECOVERY_FULL

Re-establishes trusted source truth after continuity becomes unsafe.

``` text
uncertain incremental continuity
        |
        v
do not guess missing state
        |
        v
RECOVERY_REQUIRED
        |
        v
RECOVERY_FULL
```

## 9. Synchronization Capabilities

Conceptual capabilities include:

``` text
FULL_SYNCHRONIZATION
INCREMENTAL_SYNCHRONIZATION
DURABLE_CHECKPOINT
RESUMABLE_CHECKPOINT
EXPLICIT_REMOVE_EVENT
SOURCE_VERSION
PAGINATION
```

A connector MUST advertise only proven capabilities. Neutral SNIP
support for a concept does not imply a vendor transport supports it.

## 10. Durable Checkpoints

A synchronization checkpoint is persistent correctness state.

Conceptually:

``` text
SynchronizationCheckpoint
{
    sourceSystem
    connectorId
    synchronizationScope
    checkpointType
    checkpointValue
    sourceVersion
    lastSuccessfulExecutionId
    lastSuccessfulSnapshotId
    lastSuccessfulStartedAt
    lastSuccessfulCompletedAt
    lastObservedAt
    synchronizationMode
    completeness
    fencingToken
    status
    createdAt
    updatedAt
}
```

It MUST survive pod/process restart, deployment, replica replacement,
and cluster rescheduling.

## 11. Opaque Vendor Position

`checkpointValue` is opaque to the control plane. Depending on a proven
transport it may be a timestamp, sequence, revision, cursor, token,
generation, or another vendor-supported position.

Only the vendor transport/profile interprets vendor-specific checkpoint
semantics. SNIP MUST NOT infer undocumented semantics.

## 12. Checkpoint State

Conceptual states:

``` text
VALID
UNVERIFIED
INVALID
EXPIRED
RECOVERY_REQUIRED
CHECKPOINT_UNCERTAIN
```

A failed incremental execution does not automatically invalidate the
last committed checkpoint. A proven continuity failure does.

## 13. Checkpoint Advancement Invariant

Correct ordering:

``` text
Acquire source state
 -> Validate
 -> Normalize
 -> Revalidate lease/fencing
 -> Commit canonical reconciliation
 -> Commit required provenance
 -> Advance checkpoint
```

Forbidden:

``` text
Acquire source position
 -> Advance checkpoint
 -> Attempt reconciliation
```

A source position MUST NOT become authoritative before its associated
reconciliation succeeds.

Where practical, reconciliation/provenance/checkpoint advancement SHOULD
share a transaction. Otherwise an explicit commit protocol must prevent
premature authoritative checkpoint visibility.

## 14. Reconciliation/Checkpoint Crash Window

Phase 12 MUST handle:

``` text
reconciliation committed
        |
        v
process fails
        |
        X
checkpoint advancement uncertain
```

The safe state is `CHECKPOINT_UNCERTAIN`, followed by either proven
idempotent reprocessing from the last committed checkpoint or
`RECOVERY_REQUIRED -> RECOVERY_FULL`.

The implementation must test this failure window.

## 15. Idempotency

Repeated COMPLETE snapshots and repeated incremental change sets MUST
NOT duplicate canonical entities, provenance, drift state, or checkpoint
advancement. Repeated trusted observations should primarily result in
unchanged semantics where appropriate.

## 16. Full vs Incremental Absence

For trusted full state:

``` text
FULL + COMPLETE -> existing safe absence evaluation may occur
```

For incremental state:

> An entity not present in an incremental synchronization MUST NOT be
> interpreted as absent.

Incremental changes conceptually distinguish:

``` text
UPSERT
REMOVE
```

`REMOVE` must be explicitly reported by a proven source contract. It is
never synthesized from omission. Even explicit removal follows
conservative canonical lifecycle rules rather than hard deletion.

## 17. Conservative Lifecycle

Phase 12 preserves concepts such as:

``` text
ACTIVE
NOT_OBSERVED
STALE_CANDIDATE
```

No hard delete may be inferred from incremental absence, source outage,
partial snapshot, failed synchronization, stale knowledge, or a single
unsafe observation.

## 18. Scheduled Synchronization

Phase 12 is the first phase where scheduled vendor synchronization is
legal.

The only legal path is:

``` text
Scheduler
 -> Synchronization Control Plane
 -> Phase 8 Runtime
 -> Phase 11 Connector
```

The scheduler MUST NOT resolve credentials, call `EnmTransport`,
reconcile entities, update checkpoints, or calculate drift/confidence.

## 19. Synchronization Policy

Conceptually:

``` text
SynchronizationPolicy
{
    sourceSystem
    connectorId
    enabled
    preferredMode
    cadence
    requestTimeout
    maxExecutionDuration
    maxConsecutiveFailures
    agingAfter
    staleAfter
    overlapPolicy
    retryPolicy
    recoveryPolicy
}
```

Policy remains configuration-driven in Phase 12. Dynamic DB-backed
policy administration and policy UI are out of scope.

## 20. Overlap and Backpressure

The only supported Phase 12 overlap policy is:

``` text
SKIP
```

One authoritative synchronization may be active per source scope.

If a five-minute cadence encounters an eight-minute active execution,
the later due trigger is recorded/skipped rather than queued.

`QUEUE`, `PARALLEL`, and `CANCEL_PREVIOUS` are out of scope.

The architecture may later support a bounded system-wide concurrency
limit. Unlimited task creation/backlog is forbidden.

## 21. Multi-Replica Scheduling

Multiple replicas may observe a due source. Phase 8 PostgreSQL
lease/fencing remains the authority that determines the winner. Phase 12
does not introduce a second lock or require new scheduler leader
election solely for correctness.

## 22. Schedule Jitter

Bounded jitter may be used to reduce synchronized polling bursts. Jitter
is an optimization, not correctness authority, and must remain
bounded/testable.

## 23. Retry, Replay, Resume, Recovery

These terms are distinct:

``` text
RETRY
Repeat a failed operation according to bounded execution-local retry.

REPLAY
Reprocess a known import according to Phase 8 semantics.

RESUME
Continue from a vendor-supported durable checkpoint.

RECOVERY_FULL
Discard unsafe incremental-continuity assumptions and rebuild trusted truth.
```

Resume is legal only if the transport/profile explicitly proves
resumability. Otherwise SNIP starts from the last committed safe
checkpoint or performs `RECOVERY_FULL`.

## 24. Failure and Recovery Policy

Phase 11 failure taxonomy remains authoritative where applicable. Phase
12 may add synchronization-level concepts such as:

``` text
CHECKPOINT_INVALID
CHECKPOINT_EXPIRED
CHECKPOINT_CONTINUITY_LOST
CHECKPOINT_UNCERTAIN
RECOVERY_REQUIRED
RECOVERY_FAILED
SYNCHRONIZATION_STALE
OVERLAP_SKIPPED
SOURCE_DISABLED
INCREMENTAL_NOT_SUPPORTED
```

Retry is bounded inside an execution.

``` text
retryable failure
 -> bounded retry
 -> execution succeeds or fails
 -> next new execution waits for permitted cadence
```

Continuity failure:

``` text
continuity failure
 -> RECOVERY_REQUIRED
 -> next permitted synchronization = RECOVERY_FULL
```

No unbounded immediate failure/recovery chain is allowed. An authorized
operator may explicitly request recovery.

## 25. Cancellation and Deadlines

Phase 11 cooperative cancellation remains applicable. A synchronization
execution conceptually carries:

``` text
executionId
deadline
fencingToken
cancellationToken
synchronizationMode
checkpoint
```

Cancellation/deadline checks occur before session acquisition, source
acquisition, between pages/batches, before retries/backoff, before
reconciliation, before checkpoint advancement, and before authoritative
source-state updates.

This does not claim to resolve Phase 5 Agent timeout debt.

## 26. Fencing Extension

A stale/lost lease holder MUST NOT:

-   reconcile canonical state;
-   advance a checkpoint;
-   replace latest source state;
-   resolve newer drift;
-   overwrite freshness;
-   overwrite source health;
-   overwrite knowledge confidence; or
-   claim successful synchronization.

Phase 8 fencing therefore protects Phase 12 synchronization correctness
state as well as canonical reconciliation.

## 27. Synchronization Execution

Durable execution history is required. Conceptually:

``` text
SynchronizationExecution
{
    executionId
    sourceSystem
    connectorId
    synchronizationScope
    mode
    initiator
    scheduledAt
    startedAt
    completedAt
    startingCheckpoint
    resultingCheckpoint
    status
    snapshotId
    entitiesObserved
    entitiesCreated
    entitiesUpdated
    entitiesUnchanged
    entitiesMissing
    driftDetected
    failureCode
    retryCount
    fencingToken
}
```

Existing `network_import_batch` SHOULD be reused where practical. The
implementation must inspect Phase 7--11 persistence before creating a
redundant execution table.

## 28. Execution State Machine

Successful flow:

``` text
SCHEDULED
 -> ACQUIRING_LEASE
 -> ACQUIRING
 -> VALIDATING
 -> RECONCILING
 -> ADVANCING_CHECKPOINT
 -> COMPLETED
```

Retryable branch:

``` text
ACQUIRING
 -> FAILED_RETRYABLE
 -> BACKING_OFF
 -> ACQUIRING
```

Continuity branch:

``` text
ACQUIRING / VALIDATING
 -> CONTINUITY_LOST
 -> RECOVERY_REQUIRED
```

Recovery:

``` text
RECOVERY_REQUIRED
 -> RECOVERY_FULL
 -> COMPLETED | FAILED
```

Exact persistence enums may follow repository conventions while
preserving these semantics.

## 29. Source Freshness

First-class states:

``` text
FRESH
AGING
STALE
DEGRADED
UNKNOWN
```

Freshness is derived from last trusted synchronization, expected
cadence, thresholds, completeness, failure history, checkpoint/recovery
state, and source health. It is not manually assigned by callers,
Agents, or vendor payloads.

## 30. External Source Health

Conceptual states:

``` text
HEALTHY
SYNCHRONIZING
DEGRADED
STALE
UNREACHABLE
AUTHENTICATION_FAILED
AUTHORIZATION_FAILED
THROTTLED
RECOVERING
DISABLED
UNKNOWN
```

The invariant is:

> **External source health MUST NOT directly determine SNIP
> application/Kubernetes readiness.**

SNIP may be READY while an Ericsson source is UNREACHABLE and network
knowledge is STALE/LOW confidence.

Readiness must not unnecessarily live-call ENM or resolve production
credentials.

## 31. Consecutive Failures and Circuit Breaking

Consecutive synchronization failures contribute to source health.
Authentication/authorization failures may immediately produce
corresponding health states.

Phase 12 does not require a new generic circuit-breaker framework.
Bounded retry, cadence, source health, and recovery policy are the
primary controls.

## 32. Drift Intelligence

Phase 12 introduces observational drift intelligence.

### Source State Drift

A new trusted source observation differs from previously accepted
source-derived/canonical state.

Example:

``` text
previous: cell.txPower = 38
observed: cell.txPower = 40
=> SOURCE_STATE_DRIFT
```

This may represent a legitimate external network change.

### Synchronization Drift

Expected source continuity/freshness can no longer be proven, such as
sequence discontinuity, expired/rejected checkpoint, prolonged gap, or
inability to establish complete trusted state.

Drift detection never authorizes remediation.

## 33. Drift Boundary

Legal:

``` text
detect -> record safe evidence -> expose -> inform intelligence
```

Forbidden:

``` text
detect -> repair vendor
detect -> execute command
detect -> bypass reconciliation
detect -> Agent invokes connector
```

## 34. Drift Observation

Conceptually:

``` text
NetworkDriftObservation
{
    driftId
    sourceSystem
    connectorId
    synchronizationScope
    entityType
    entityId
    driftType
    previousValueHash
    observedValueHash
    observedAt
    snapshotId
    executionId
    severity
    status
}
```

Complete raw vendor payloads are not persisted for drift.

Initial lifecycle:

``` text
DETECTED
RESOLVED
```

A stale execution MUST NOT resolve drift belonging to newer
authoritative state.

## 35. Source Authority

Authority is scoped rather than global.

Conceptually:

``` text
SourceAuthority
{
    sourceSystem
    entityDomain
    authorityLevel
}
```

Initial levels:

``` text
AUTHORITATIVE
SUPPLEMENTAL
DERIVED
```

Example:

``` text
ERICSSON_ENM / RADIO_INVENTORY -> AUTHORITATIVE
SNIP / DERIVED_ASSURANCE       -> DERIVED
SNIP / DIGITAL_TWIN            -> DERIVED
```

A dynamic field-level authority engine is out of scope.

## 36. Network Knowledge Confidence

Phase 12 introduces deterministic **operational** confidence:

``` text
HIGH
MEDIUM
LOW
UNKNOWN
```

This is not ML probability and is not LLM-generated.

`HIGH` requires trusted fresh source state, valid continuity, sufficient
completeness, healthy source state, and no unresolved recovery
requirement.

`MEDIUM` represents usable knowledge with warnings such as aging data,
transient failure, throttling, or a failed recent incremental run while
a recent trusted baseline remains valid.

`LOW` may represent stale data, repeated failure, recovery-required
state, checkpoint uncertainty, prolonged unavailability, or materially
incomplete knowledge.

`UNKNOWN` means no trusted complete synchronization has established
usable source knowledge.

## 37. Confidence Scope and Calculation

Confidence is source/domain scoped.

Conceptually:

``` text
NetworkKnowledgeStatus
{
    sourceSystem
    domain
    freshness
    sourceHealth
    confidence
    reasonCodes
    lastTrustedSynchronizationAt
    lastTrustedSnapshotId
    evaluatedAt
}
```

Confidence is deterministically derived from freshness, checkpoint
state, trusted completeness, source health, recovery state, continuity,
and drift.

Agents/LLMs cannot assign or override authoritative operational
confidence.

## 38. Current Source State

A source-scoped current-state record should support efficient
operational queries:

``` text
SynchronizationSourceState
{
    sourceSystem
    connectorId
    synchronizationScope
    currentExecutionId
    latestCompletedExecutionId
    lastSuccessAt
    lastFailureAt
    consecutiveFailures
    freshness
    health
    confidence
    checkpointStatus
    recoveryRequired
    updatedAt
}
```

Updates are fencing-aware. Historical evidence remains in
execution/import/audit records.

## 39. Downstream Intelligence

Phase 12 exposes, without broadly redesigning earlier phases:

``` text
sourceSystem
sourceSnapshotId
lastTrustedSynchronizationAt
freshness
sourceHealth
knowledgeConfidence
confidenceReasonCodes
```

Assurance, Agents, Decision Intelligence, and Digital Twin may consume
this metadata later. Phase 12 establishes the contract; it does not add
broad confidence-aware decision policy.

## 40. Agent, MCP, and Phase 4 Isolation

Agents may read synchronization status, freshness, confidence, drift,
and last outcome.

Agents MUST NOT call the vendor connector/transport, acquire leases,
advance checkpoints, or schedule synchronization directly.

MCP MUST NOT directly call vendor synchronization/transport.

Phase 4 Actions MUST NOT become an indirect `Action -> Sync -> ENM`
path.

Any future governed operational capability requires separate
architectural approval.

## 41. Digital Twin

Phase 12 may expose latest trusted snapshot/version, last canonical
change time, freshness, and confidence. It does not automatically
rebuild the Digital Twin after every synchronization.

## 42. Persistence

Required durable concepts include:

``` text
synchronization_checkpoint
synchronization_source_state
network_drift_observation
network_knowledge_status
```

A separate `synchronization_execution` table is optional and should
exist only if existing import history cannot safely represent required
control-plane metadata.

Historical migrations must not be altered.

## 43. Provenance and Raw Data

Phase 11 provenance remains authoritative and should remain recoverable
through source vendor/system/object/snapshot/observed-at/execution
context.

Phase 12 does not require field-level provenance redesign.

Complete raw vendor payloads are not persisted by default. Safe
normalized metadata, hashes, provenance, checkpoints, drift metadata,
and failure/audit evidence may be persisted.

## 44. Security and Credentials

Phase 10 remains canonical:

``` text
AKS Workload Identity
 -> Azure Key Vault
 -> per-session credential resolution
 -> read-only vendor session
```

No secret value may enter checkpoints, synchronization executions,
source health, drift, metrics, audit, or public APIs.

No Azure SDK/Key Vault implementation belongs in the synchronization
scheduler/control plane or Ericsson connector.

## 45. TLS, Authorization, and Egress

Phase 9--11 security remains:

-   mandatory TLS server/hostname verification;
-   no trust-all/insecure TLS;
-   mTLS only when the actual selected profile requires it;
-   least-privilege read-only vendor identity;
-   application egress policy as canonical;
-   Kubernetes/Cilium as defense in depth;
-   no broad `0.0.0.0/0` vendor egress;
-   controlled vendor routing required for real deployment.

The Phase 10 Cilium FQDN-cache limitation remains a deployment-readiness
consideration.

## 46. Production Ericsson Interface

Phase 12 intentionally preserves:

``` text
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
REAL VENDOR CONTINUOUS SYNCHRONIZATION: NOT YET VERIFIED
```

No REST/Bulk-CM/CLI/NETCONF/event-stream interface may be guessed.
Production transport continues to fail closed until an actual authorized
interface/profile is selected.

## 47. Simulator Contract

The simulator should prove neutral synchronization semantics with
synthetic scenarios such as:

``` text
FULL_SUCCESS
INCREMENTAL_SUCCESS
NO_CHANGES
SOURCE_CHANGES
CHECKPOINT_EXPIRED
CHECKPOINT_REJECTED
SEQUENCE_GAP
RECOVERY_FULL_SUCCESS
RECOVERY_FULL_FAILURE
SOURCE_THROTTLED
SOURCE_UNAVAILABLE
LONG_RUNNING_SYNC
OVERLAPPING_TRIGGER
STALE_SOURCE
DRIFT_DETECTED
DRIFT_RESOLVED
```

Synthetic checkpoint/incremental semantics prove SNIP's neutral contract
only and MUST NOT be represented as Ericsson behavior.

## 48. Default CI and Real-Vendor Separation

Default CI remains Azure-, Key-Vault-, and vendor-independent. It must
not require `az login`, Azure credentials, live Key Vault, real Ericsson
credentials, endpoints, or private network access.

Implementation acceptance and real-vendor verification remain separate:

``` text
PHASE 12 IMPLEMENTATION:
SIMULATOR/CONTRACT VERIFIED

REAL VENDOR CONTINUOUS SYNCHRONIZATION:
NOT YET VERIFIED
```

Simulator evidence MUST NOT be presented as real-vendor evidence.

## 49. Observability

Low-cardinality metrics should cover synchronization
runs/success/failure, overlap skips, duration, modes, checkpoint
advances/failures, freshness age, drift detected/resolved, health
transitions, confidence state, and recovery-required transitions.

No secrets, raw payloads, sensitive error bodies, or high-cardinality
entity identifiers belong in metrics.

## 50. Audit Narrative

Conceptual successful narrative:

``` text
SYNCHRONIZATION_DUE
SYNCHRONIZATION_STARTED
LEASE_ACQUIRED
CHECKPOINT_LOADED
MODE_SELECTED
CONNECTOR_SESSION_STARTED
SOURCE_STATE_ACQUIRED
SOURCE_STATE_VALIDATED
RECONCILIATION_STARTED
RECONCILIATION_COMPLETED
CHECKPOINT_ADVANCED
DRIFT_EVALUATED
KNOWLEDGE_STATUS_UPDATED
SYNCHRONIZATION_COMPLETED
LEASE_RELEASED
```

Safe failure events include connector failure, checkpoint invalidation,
continuity loss, recovery required, synchronization failure, and overlap
skipped.

No secret or unrestricted vendor payload may appear in audit data.

## 51. API and Authorization

Phase 12 should expose safe operational capabilities consistent with
existing API conventions:

-   list synchronization sources;
-   inspect source state;
-   list execution history;
-   inspect safe checkpoint metadata;
-   inspect drift;
-   inspect network knowledge status;
-   manually trigger permitted synchronization; and
-   manually trigger authorized recovery.

Conceptual permissions include:

``` text
VIEW_SYNCHRONIZATION_STATUS
TRIGGER_VENDOR_IMPORT
TRIGGER_RECOVERY_SYNCHRONIZATION
```

Scheduled execution acts as configured system authority limited to
read-only synchronization.

API callers cannot provide secret values, Key Vault URIs, arbitrary
endpoints, fencing tokens, lease ownership, arbitrary vendor cursors, or
arbitrary protocol operations.

## 52. Disabled Sources

`enabled=false` means:

-   scheduler does not run the source;
-   normal manual synchronization is rejected;
-   source health is `DISABLED`;
-   no vendor session is opened.

No Phase 12 override bypasses this setting.

## 53. Time Semantics

Correctness timestamps use UTC instants for scheduling, observation,
execution, checkpoints, freshness, and drift. Display timezone is
presentation-only.

## 54. Architectural Test Obligations

The implementation specification must require tests proving at least:

1.  due scheduled source enters the control plane;
2.  scheduler never directly invokes transport;
3.  manual/scheduled initiators share the runtime;
4.  disabled source does not execute;
5.  overlap is skipped;
6.  multi-replica competition yields one authoritative holder;
7.  stale holder cannot reconcile;
8.  stale holder cannot advance checkpoint or current source state;
9.  successful FULL advances checkpoint;
10. failed FULL does not;
11. successful INCREMENTAL advances only after reconciliation;
12. incremental omission does not infer deletion;
13. explicit synthetic REMOVE follows safe lifecycle;
14. invalid/expired/rejected checkpoint forces recovery;
15. sequence discontinuity forces recovery;
16. unsupported incremental capability fails closed;
17. recovery full restores trusted state;
18. failed recovery does not recursively storm;
19. reconciliation/checkpoint crash window is safe;
20. repeated full and incremental inputs are idempotent;
21. retry is bounded and cadence-governed;
22. cancellation/deadline prevent unsafe late commits;
23. freshness transitions deterministically;
24. source health is independent of application readiness;
25. auth/authz failures map safely;
26. source-state and synchronization drift are detected;
27. later trusted state can resolve drift;
28. stale execution cannot resolve newer drift;
29. confidence is deterministic;
30. HIGH requires trusted fresh state;
31. recovery-required cannot remain HIGH;
32. Agent/MCP/Phase 4 isolation remains;
33. no vendor write capability exists;
34. no raw payload persistence;
35. secrets/tokens/private keys are absent from API/audit/metrics;
36. production transport fails closed;
37. default CI needs no Azure/real vendor; and
38. Phase 7--11 regression suites remain green.

## 55. Non-Goals

Phase 12 explicitly excludes:

``` text
real vendor writes
configuration mutation
parameter changes
command execution
autonomous remediation
closed-loop optimization
Agent -> connector
MCP -> connector
Phase 4 Action -> connector
guessed Ericsson production API
requirement for real ENM E2E
Nokia production connector
hard delete from incremental absence
unlimited retry/backlog
same-source parallel synchronization
dynamic policy administration UI
connector-management UI
field-level authority engine
field-level provenance redesign
automatic Twin rebuild after every sync
broad Agent redesign
new distributed lock replacing Phase 8
mandatory generic circuit-breaker framework
Phase 13 implementation
```

## 56. Deferred Work

Deferred beyond Phase 12:

-   actual Ericsson production transport selection;
-   real Ericsson continuous-sync E2E;
-   Nokia NetAct production integration;
-   dynamic synchronization-policy administration;
-   field-level authority/conflict resolution;
-   field-level provenance;
-   richer drift workflow/acknowledgement/suppression;
-   automatic Digital Twin refresh policy;
-   confidence-aware decision gating;
-   governed Agent/MCP synchronization requests;
-   high-scale multi-source scheduling optimization;
-   advanced source partitioning; and
-   any controlled network mutation.

## 57. Architecture Acceptance Gates

Phase 12 architecture may be accepted only if these invariants remain
explicit:

1.  Vendor interaction remains read-only.
2.  Scheduler never calls vendor transport directly.
3.  Phase 8 lease/fencing remains concurrency authority.
4.  One active synchronization exists per source scope.
5.  Overlap policy is `SKIP`.
6.  FULL, INCREMENTAL, and RECOVERY_FULL are distinct.
7.  Incremental support is capability/profile-dependent.
8.  Durable checkpoints are opaque vendor/source positions.
9.  Checkpoints advance only after successful authoritative
    reconciliation.
10. Reconciliation/checkpoint crash uncertainty is handled safely.
11. Lost continuity forces recovery.
12. Incremental absence never means deletion.
13. Explicit removal remains conservatively reconciled.
14. Recovery falls back to authoritative FULL.
15. Retry is bounded and execution-local.
16. Failures do not create unbounded immediate job chains.
17. Freshness is derived.
18. Vendor source health is separate from app readiness.
19. Drift is observational, never remediation.
20. Knowledge confidence is deterministic operational confidence.
21. Confidence is source/domain scoped.
22. Agents/LLMs cannot assign authoritative confidence.
23. Stale executions cannot reconcile or update authoritative sync
    state.
24. Scheduling/backpressure/recovery are bounded.
25. Phase 9--11 security boundaries remain authoritative.
26. Production credentials remain per-session and outside sync
    persistence.
27. Default CI remains Azure/vendor independent.
28. Actual Ericsson production interface remains unresolved until
    selected.
29. No real credentials/endpoints are required for Phase 12
    implementation.
30. Agents/MCP/Phase 4 cannot directly operate connectors.
31. No vendor network-write capability is introduced.
32. Real-vendor verification remains distinct from simulator
    verification.
33. Phase 13 does not start during Phase 12.

## 58. Architecture Completion Scenario

A simulator-backed Phase 12 proof should demonstrate:

``` text
configured source
 -> becomes due
 -> scheduler triggers control plane
 -> lease acquired
 -> checkpoint loaded
 -> FULL sync
 -> canonical state established
 -> checkpoint committed
 -> freshness FRESH / confidence HIGH
 -> synthetic source changes
 -> INCREMENTAL sync
 -> drift identified
 -> canonical reconcile
 -> checkpoint advanced
 -> continuity failure
 -> RECOVERY_REQUIRED / confidence degraded
 -> next permitted RECOVERY_FULL
 -> trusted state restored
 -> new checkpoint committed
 -> freshness FRESH / confidence HIGH
```

This proves the neutral synchronization architecture, not a real
Ericsson production transport.

## 59. Open Questions for the Later Specification

The following remain implementation-dependent:

1.  Which existing Phase 7--11 tables can represent synchronization
    execution metadata without duplication?
2.  Is a dedicated `synchronization_execution` table necessary?
3.  What existing configuration conventions should represent
    synchronization policies?
4.  Which current controller/API namespace should own synchronization
    status and recovery?
5.  What exact deterministic thresholds map freshness/source health into
    HIGH/MEDIUM/LOW?
6.  What commit pattern best handles
    reconciliation-success/checkpoint-advance uncertainty with current
    service boundaries?
7.  What synthetic incremental/checkpoint contract should the simulator
    use without implying Ericsson semantics?
8.  What initial source/domain authority representation is needed for
    Ericsson radio inventory?
9.  Which existing audit/metrics abstractions should be extended?

The actual Ericsson production interface remains separately unresolved
and MUST NOT be guessed.

## 60. Review and Implementation Gate

This document is the accepted Phase 12 architecture.

Implementation is complete and architecturally accepted. Cursor or any implementation agent MUST NOT:

-   implement production ENM transport;
-   commit/push a Phase 12 Git baseline without explicit authorization;
-   begin Phase 13.

## 61. Architecture Status

``` text
PHASE 12 ARCHITECTURE STATUS: ACCEPTED
PHASE 12 IMPLEMENTATION STATUS: COMPLETE
SIMULATOR/CONTRACT STATUS: VERIFIED
REAL VENDOR CONTINUOUS SYNCHRONIZATION STATUS: NOT YET VERIFIED
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
PHASE 12 GIT BASELINE: NOT YET ESTABLISHED
PHASE 13 STATUS: NOT STARTED
```

## 62. Final Architectural Statement

Phase 12 evolves SNIP from a governed manual read-only vendor-import
foundation into a continuously synchronized network-intelligence
foundation without weakening the security, reconciliation, governance,
or read-only boundaries established in Phases 7--11.

It introduces durable synchronization correctness, checkpoint
continuity, source freshness, drift intelligence, bounded recovery, and
deterministic network knowledge confidence.

> **SNIP must know not only what the network looks like, but also when
> that knowledge was established, whether continuity is intact, where it
> diverged, how trustworthy it currently is, and when authoritative
> recovery is required.**

Implementation is complete and architecturally accepted. A Phase 12 Git baseline is **NOT YET ESTABLISHED**. Phase 13 is **NOT STARTED**.
