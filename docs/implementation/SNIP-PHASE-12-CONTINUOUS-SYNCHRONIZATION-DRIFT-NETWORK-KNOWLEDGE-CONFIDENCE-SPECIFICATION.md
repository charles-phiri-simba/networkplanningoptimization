# SNIP Phase 12 --- Continuous Synchronization, Drift & Network Knowledge Confidence Implementation Specification

**Repository:** `networkplanningoptimization`\
**Parent immutable baseline:**
`78e699380be37109cfdd2111dd0f29c7052709c3`\
**Parent phase:** Phase 11 --- First Real Vendor Connector & Production
Read-Only Integration\
**Phase 12 architecture:**
`SNIP-PHASE-12-CONTINUOUS-SYNCHRONIZATION-DRIFT-NETWORK-KNOWLEDGE-CONFIDENCE-ARCHITECTURE.md`\
**Specification status:**
`READY FOR IMPLEMENTATION AFTER ARCHITECTURE STATUS IS ACCEPTED`\
**Phase 12 implementation status:** `NOT STARTED`\
**Real vendor continuous synchronization status:** `NOT YET VERIFIED`\
**Production ENM transport:** `NOT CONFIGURED`\
**Phase 13 status:** `NOT STARTED`

------------------------------------------------------------------------

## 1. Purpose

This specification translates the accepted Phase 12 architecture into an
implementation contract for Cursor.

Phase 12 evolves SNIP from manually initiated, governed read-only vendor
import into a continuously synchronized network-intelligence foundation.
It introduces synchronization scheduling and orchestration, durable
source checkpoints, FULL/INCREMENTAL/RECOVERY_FULL semantics, bounded
recovery, freshness, source health, drift intelligence, and
deterministic operational network knowledge confidence.

This specification does **not** authorize vendor-network mutation.

The defining implementation principle is:

> **SNIP must continuously know not only what the network looks like,
> but when that knowledge was established, whether continuity is intact,
> where it diverged, how trustworthy it is, and when authoritative
> recovery is required --- without gaining the ability to change the
> vendor network.**

------------------------------------------------------------------------

## 2. Authority and Precedence

Implementation authority is, in order:

1.  immutable Phase 11 Git baseline
    `78e699380be37109cfdd2111dd0f29c7052709c3`;
2.  accepted Phase 12 architecture;
3.  this Phase 12 implementation specification;
4.  frozen architecture and implementation contracts from Phases 7--11;
5.  existing repository conventions where they do not conflict with the
    above.

If this specification appears to conflict with an accepted Phase 7--12
architectural invariant, the architecture wins.

Cursor MUST NOT reinterpret an implementation convenience as permission
to weaken an architectural boundary.

------------------------------------------------------------------------

## 3. Mandatory Pre-Implementation Gate

Before modifying Phase 12 application code, Cursor MUST verify:

``` text
git branch --show-current
git rev-parse HEAD
git status --short
```

Expected starting branch:

``` text
main
```

Expected parent baseline:

``` text
78e699380be37109cfdd2111dd0f29c7052709c3
```

The working tree should be understood before changes begin.

Cursor MUST also verify that the canonical Phase 12 architecture has
been transitioned to:

``` text
PHASE 12 ARCHITECTURE STATUS: ACCEPTED
```

If the architecture still says `DRAFT — FOR REVIEW`, Cursor MUST perform
only the authorized documentation/status synchronization needed to
record architectural acceptance and MUST NOT begin Phase 12 code
implementation until that status is consistent.

No Phase 11 baseline commit may be amended.

------------------------------------------------------------------------

## 4. Mandatory Repository Inspection Before Design Decisions

Before creating new classes, tables, APIs, configuration objects, or
enums, inspect the existing Phase 7--11 implementation.

At minimum inspect:

-   network import orchestration;
-   `NetworkImportService`;
-   `NetworkImportBatchService`;
-   import batch/entity persistence;
-   Phase 8 lease/fencing services and fencing-token validation;
-   retry/replay/watchdog implementation;
-   Phase 9 connector authorization/security abstractions;
-   Phase 10 credential/trust abstractions;
-   Phase 11 `EricssonEnmConnector`;
-   `EnmTransport`;
-   simulator transport;
-   unconfigured production transport;
-   vendor snapshot/page/source DTO abstractions;
-   snapshot completeness;
-   `ImportExecutionContext`;
-   cancellation token;
-   failure taxonomy/mapping;
-   provenance persistence;
-   audit abstractions;
-   metrics abstractions;
-   connector registry/descriptor/capability model;
-   current controllers and authorization conventions;
-   scheduler conventions already used elsewhere in SNIP;
-   application configuration conventions;
-   Flyway migrations;
-   architecture-boundary tests; and
-   existing test fixtures/build workflow.

Do not create duplicate abstractions where existing Phase 7--11 types
can be safely extended.

------------------------------------------------------------------------

## 5. Implementation Scope

Phase 12 implementation includes:

-   synchronization policy configuration;
-   due-source scheduling;
-   synchronization control plane;
-   manual and scheduled initiation convergence;
-   FULL synchronization;
-   neutral INCREMENTAL synchronization contract;
-   RECOVERY_FULL synchronization;
-   durable checkpoints;
-   checkpoint lifecycle/trust state;
-   checkpoint atomicity/commit safety;
-   crash-window recovery;
-   one-active-sync-per-source behavior;
-   overlap `SKIP`;
-   bounded backpressure;
-   existing Phase 8 lease/fencing integration;
-   stale-holder protection for Phase 12 state;
-   source freshness;
-   external source health;
-   current synchronization source state;
-   drift detection/history;
-   source/domain authority foundation;
-   deterministic network knowledge confidence;
-   safe query APIs;
-   authorized manual recovery;
-   audit/metrics;
-   simulator scenarios;
-   persistence migrations;
-   regression/security/boundary tests;
-   documentation; and
-   Phase 12 completion report.

------------------------------------------------------------------------

## 6. Explicitly Out of Scope

Cursor MUST NOT implement:

``` text
real vendor writes
configuration mutation
parameter changes
command execution
autonomous remediation
closed-loop optimization
Agent -> connector execution
MCP -> connector execution
Phase 4 Action -> connector execution
guessed Ericsson production API
real Ericsson production transport
Nokia production connector
hard delete from incremental omission
unlimited retry
unlimited scheduler backlog
same-source parallel synchronization
dynamic synchronization-policy CRUD
synchronization administration UI
full connector-management UI
field-level authority engine
field-level provenance redesign
automatic Digital Twin rebuild after each sync
broad Agent redesign
new distributed locking replacing Phase 8
mandatory generic circuit-breaker framework
Phase 13 work
```

Real Ericsson E2E is not required for Phase 12 implementation
acceptance.

------------------------------------------------------------------------

## 7. Required End-to-End Runtime Flow

The implementation MUST preserve this logical flow:

``` text
Configured Synchronization Policy
        |
        v
Scheduler Tick / Authorized Manual Request
        |
        v
Synchronization Control Plane
        |
        v
Admission / Enabled / Due / Overlap Evaluation
        |
        v
Phase 8 Lease + Fencing
        |
        v
Load Durable Checkpoint
        |
        v
Select FULL / INCREMENTAL / RECOVERY_FULL
        |
        v
Build Bounded Import/Synchronization Execution Context
        |
        v
Phase 10 Security / Credential / Trust Resolution
        |
        v
Phase 11 Ericsson Read-Only Connector
        |
        v
EnmTransport
        |
        v
Simulator Transport
        |
        v
Vendor-Neutral Source State
        |
        v
Validation
        |
        v
Pre-Commit Cancellation + Fencing Check
        |
        v
Canonical Reconciliation + Provenance
        |
        v
Checkpoint Commit/Advance
        |
        v
Drift Evaluation
        |
        v
Freshness + Source Health + Knowledge Confidence
        |
        v
Audit / Metrics / Durable Outcome
```

Production `EnmTransport` remains unconfigured and fail-closed.

------------------------------------------------------------------------

## 8. Package and Naming Guidance

Follow existing repository package conventions.

Do not create a parallel application architecture solely because this
specification uses conceptual names.

Conceptual names in this specification may be adapted where an existing
repository abstraction clearly owns the responsibility.

Likely new/extended concepts include:

``` text
SynchronizationControlPlane
SynchronizationScheduler
SynchronizationPolicy
SynchronizationMode
SynchronizationInitiator
SynchronizationCheckpoint
SynchronizationCheckpointStatus
SynchronizationSourceState
SynchronizationFreshness
SynchronizationSourceHealth
NetworkDriftObservation
NetworkDriftType
NetworkDriftStatus
NetworkKnowledgeStatus
NetworkKnowledgeConfidence
KnowledgeConfidenceReason
SourceAuthority
SourceAuthorityLevel
SynchronizationFailureCode
```

Create only types that provide a real domain boundary.

------------------------------------------------------------------------

## 9. Synchronization Policy Configuration

Implement configuration-driven source synchronization policy.

Minimum conceptual fields:

``` text
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
```

Requirements:

-   no DB-backed policy CRUD;
-   no UI;
-   no arbitrary runtime user-supplied cron;
-   validate positive/bounded durations;
-   validate `agingAfter < staleAfter`;
-   validate execution/retry limits;
-   default safely;
-   fail closed on invalid policy;
-   policy contains no credential value;
-   policy contains no arbitrary secret-bearing endpoint supplied
    through public APIs.

For Phase 12:

``` text
overlapPolicy = SKIP
```

is the only supported overlap policy.

------------------------------------------------------------------------

## 10. Scheduler Implementation

Implement a lightweight scheduler that periodically asks the control
plane for due configured sources.

The scheduler MUST NOT contain vendor business logic.

It may conceptually:

``` text
tick
 -> enumerate configured enabled policies
 -> determine candidate due sources
 -> apply bounded jitter if configured
 -> call synchronizationControlPlane.triggerScheduled(...)
```

The control plane remains responsible for authoritative admission and
lease acquisition.

Do not dynamically create unbounded framework scheduler jobs.

Do not queue missed executions indefinitely.

------------------------------------------------------------------------

## 11. Due-Time Semantics

A source is due according to its configured cadence and durable/current
source state.

Use UTC instants for correctness.

The scheduler MUST tolerate multiple replicas seeing the same source as
due.

Due-time calculation is not a distributed lock.

The Phase 8 lease remains final execution authority.

------------------------------------------------------------------------

## 12. Overlap Semantics

If a source scope already has an active authoritative synchronization:

``` text
new scheduled trigger -> SKIPPED_OVERLAP
```

Requirements:

-   do not queue it;
-   do not open vendor session;
-   do not resolve credentials;
-   do not mutate canonical state;
-   do not advance checkpoint;
-   record safe operational evidence;
-   emit low-cardinality metric;
-   do not treat overlap skip as vendor failure.

Manual requests must also respect one-active-sync-per-source authority.

------------------------------------------------------------------------

## 13. Multi-Replica Correctness

Do not add a second distributed lock.

Multiple replicas may race to start the same due source.

The winner is determined by existing Phase 8 lease/fencing.

The loser must exit safely without vendor acquisition or
canonical/checkpoint mutation.

Add tests using the existing multi-instance/lease testing approach where
possible.

------------------------------------------------------------------------

## 14. Synchronization Mode Enum

Implement:

``` text
FULL
INCREMENTAL
RECOVERY_FULL
```

Do not collapse `RECOVERY_FULL` into ordinary `FULL` in persisted/audit
semantics even if they share acquisition code.

The reason for recovery must remain observable.

------------------------------------------------------------------------

## 15. Mode Selection

Mode selection belongs to the control plane.

Rules:

-   no valid trusted checkpoint -\> `FULL`;
-   configured/preferred incremental + connector capability + valid
    checkpoint -\> `INCREMENTAL`;
-   `RECOVERY_REQUIRED` or unsafe continuity -\> `RECOVERY_FULL`;
-   incremental requested but unsupported -\> fail closed or use an
    explicitly configured safe policy; do not silently pretend
    incremental occurred;
-   production vendor capability must never be inferred from simulator
    capability.

Record selected mode in execution/audit state.

------------------------------------------------------------------------

## 16. Connector Capability Extension

Extend the existing connector capability model only as needed.

Conceptual capabilities:

``` text
FULL_SYNCHRONIZATION
INCREMENTAL_SYNCHRONIZATION
DURABLE_CHECKPOINT
RESUMABLE_CHECKPOINT
EXPLICIT_REMOVE_EVENT
SOURCE_VERSION
PAGINATION
```

Do not add write/mutation/command capabilities.

The simulator may advertise synthetic incremental capabilities.

The unconfigured production Ericsson transport/connector MUST NOT
falsely advertise an unproven real ENM incremental contract.

------------------------------------------------------------------------

## 17. Checkpoint Persistence

Implement a durable checkpoint entity/table unless an existing table
already provides the complete correctness semantics.

Minimum safe fields:

``` text
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
```

Do not store:

-   credentials;
-   tokens;
-   cookies;
-   private keys;
-   raw authentication responses;
-   unrestricted vendor payloads.

Use Flyway for schema changes.

Never edit historical migrations.

------------------------------------------------------------------------

## 18. Checkpoint Status

Implement semantic equivalents of:

``` text
VALID
UNVERIFIED
INVALID
EXPIRED
RECOVERY_REQUIRED
CHECKPOINT_UNCERTAIN
```

Do not add states without a defined transition/use.

A normal failed incremental execution should leave the previous
committed checkpoint intact unless continuity itself is proven unsafe.

------------------------------------------------------------------------

## 19. Opaque Checkpoint Contract

Create a neutral checkpoint representation that allows the
transport/profile to interpret its own source position.

The synchronization control plane may:

-   load it;
-   persist it;
-   pass it to the connector/transport;
-   inspect neutral status/type/version metadata.

It MUST NOT parse undocumented Ericsson semantics.

The simulator may use a synthetic deterministic sequence/token.

Name/document it explicitly as synthetic.

------------------------------------------------------------------------

## 20. Atomic Reconciliation and Checkpoint Advancement

This is a critical acceptance requirement.

The implementation must ensure:

``` text
source acquisition
 -> validation
 -> normalization
 -> fencing validation
 -> canonical reconciliation
 -> required provenance
 -> checkpoint advancement
```

Checkpoint advancement before successful reconciliation is forbidden.

Prefer one transaction if existing service boundaries allow it safely.

If reconciliation currently uses a transaction boundary that makes a
single transaction impractical, implement an explicit safe commit
protocol and document it in the completion report.

------------------------------------------------------------------------

## 21. Crash-Window Protocol

Explicitly implement/test the case:

``` text
canonical reconciliation committed
process fails before checkpoint advancement is confirmed
```

The implementation MUST NOT silently advance the source position.

Safe options:

1.  proven idempotent reprocessing from the last committed checkpoint;
    or
2.  mark checkpoint/source state `CHECKPOINT_UNCERTAIN` /
    `RECOVERY_REQUIRED` and require `RECOVERY_FULL`.

Choose the simplest approach compatible with existing Phase 7/8
reconciliation.

Document the chosen rule.

------------------------------------------------------------------------

## 22. Incremental Source Representation

Introduce/extend a vendor-neutral incremental representation.

Conceptually:

``` text
VendorIncrementalBatch
{
    sourceSystem
    connectorId
    executionId
    startingCheckpoint
    resultingCheckpoint
    sourceVersion
    observedAt
    changes
    completeness/continuity metadata
}
```

Each change conceptually has:

``` text
UPSERT
REMOVE
```

Do not expose Ericsson-specific object types beyond the adapter
boundary.

------------------------------------------------------------------------

## 23. Incremental Absence Rule

This rule is mandatory:

> Missing from an incremental batch is not removal.

Only an explicit source change with proven `REMOVE` semantics may
represent source removal.

Tests MUST prove that an entity omitted from an incremental batch
remains unaffected.

------------------------------------------------------------------------

## 24. Explicit REMOVE Rule

Even explicit synthetic `REMOVE` MUST use conservative canonical
lifecycle behavior.

Do not hard-delete immediately.

Reuse existing Phase 7/11 lifecycle semantics where available.

If the current model lacks an appropriate safe representation, introduce
the smallest non-destructive state necessary and document it.

------------------------------------------------------------------------

## 25. FULL Semantics

FULL synchronization should reuse the Phase 11 snapshot
acquisition/reconciliation path where possible.

Only:

``` text
FULL + COMPLETE
```

may apply existing safe full-snapshot absence evaluation.

`PARTIAL` and `FAILED` remain non-authoritative for destructive absence
inference.

Preserve Phase 11 policy that PARTIAL/FAILED do not cause unsafe
canonical mutation.

------------------------------------------------------------------------

## 26. RECOVERY_FULL Semantics

`RECOVERY_FULL` performs authoritative full acquisition specifically
because incremental continuity is unsafe.

On successful COMPLETE reconciliation:

-   establish/restore valid checkpoint;
-   clear appropriate recovery-required state;
-   update latest trusted source state;
-   recalculate freshness/health/confidence;
-   preserve audit reason that execution was recovery.

On failure:

-   do not recursively launch another recovery;
-   retain recovery-required/degraded state;
-   preserve previous safe canonical data;
-   do not falsely advance checkpoint.

------------------------------------------------------------------------

## 27. Retry and Recovery

Reuse existing bounded retry primitives where possible.

Do not create duplicate retry frameworks.

Retry remains execution-local.

A new synchronization execution occurs only at the next permitted
cadence or explicit authorized manual request.

Non-retryable authentication/authorization failures terminate promptly.

Respect `Retry-After` and existing Phase 11 vendor throttling semantics
where applicable.

------------------------------------------------------------------------

## 28. Execution Context

Extend/reuse `ImportExecutionContext` rather than creating a conflicting
execution authority.

The synchronization path must carry enough context for:

``` text
executionId
deadline
fencingToken
cancellationToken
synchronizationMode
startingCheckpoint
initiator
source scope
```

Do not put secrets into the context.

------------------------------------------------------------------------

## 29. Cancellation Checks

Cancellation/deadline/fencing checks are required:

-   before credential resolution;
-   before vendor session;
-   before first page/batch;
-   between pages/batches;
-   before retry/backoff;
-   before final validation;
-   before reconciliation;
-   before checkpoint advancement;
-   before drift/current-state update;
-   before successful finalization.

A cancelled/stale execution cannot commit authoritative late state.

------------------------------------------------------------------------

## 30. Synchronization Execution History

Inspect `network_import_batch` before adding a new execution table.

Preferred approach:

-   reuse/extend existing import execution/batch history if it can
    safely represent mode, initiator, scheduling, checkpoint references,
    and synchronization outcome;
-   create `synchronization_execution` only if the existing model cannot
    represent those semantics cleanly.

Do not duplicate entity counts, status, failure, or timestamps without a
clear reason.

Document the decision in the completion report.

------------------------------------------------------------------------

## 31. Current Synchronization Source State

Implement a durable current-state projection, unless existing
persistence already provides an equivalent.

Conceptually:

``` text
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
```

This is current operational state, not full history.

Updates MUST be fencing-aware.

------------------------------------------------------------------------

## 32. Freshness Enum and Evaluation

Implement:

``` text
FRESH
AGING
STALE
DEGRADED
UNKNOWN
```

Use deterministic configuration-driven rules.

Recommended baseline rule structure:

-   `UNKNOWN`: no trusted complete baseline;
-   `FRESH`: trusted state age \< `agingAfter`, checkpoint valid, no
    material degradation;
-   `AGING`: trusted state age \>= `agingAfter` and \< `staleAfter`,
    with usable continuity;
-   `STALE`: trusted state age \>= `staleAfter`;
-   `DEGRADED`: recent trusted data exists but operational
    failures/partial/recovery conditions materially reduce trust.

If precedence between `STALE` and `DEGRADED` is represented through
separate health and freshness fields, keep freshness temporal and health
operational. Do not create contradictory meanings.

Document exact precedence.

------------------------------------------------------------------------

## 33. Source Health

Implement semantic equivalents of:

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

Consolidation is allowed only if existing enums make the same
information unambiguous.

Vendor source health MUST NOT control Spring/Quarkus application
readiness.

Do not live-call ENM or resolve Key Vault secrets from readiness.

------------------------------------------------------------------------

## 34. Consecutive Failure Rules

Track consecutive synchronization failures in source state.

Rules must be deterministic.

Examples:

-   success resets appropriate failure count;
-   authentication failure maps immediately to authentication health;
-   authorization failure maps immediately to authorization health;
-   throttling maps to throttled/degraded according to policy;
-   network timeout/unavailability increments failure count;
-   threshold may degrade/unreachable status;
-   elapsed trusted-state age independently drives staleness.

Do not use LLM reasoning for health classification.

------------------------------------------------------------------------

## 35. Drift Types

Implement at least:

``` text
SOURCE_STATE_DRIFT
SYNCHRONIZATION_DRIFT
```

Source-state drift compares a newly trusted normalized observation to
prior trusted source-derived/canonical state.

Synchronization drift represents lost/unsafe continuity or materially
missed expected synchronization.

Do not conflate ordinary changed configuration with an error.

------------------------------------------------------------------------

## 36. Drift Persistence

Implement durable drift observations unless existing persistence
provides a clear equivalent.

Conceptual safe fields:

``` text
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
```

Do not persist unrestricted raw vendor payloads.

Use normalized safe values/hashes only where needed.

Avoid sensitive/high-cardinality metric labels even though entity IDs
may be persisted in the drift table.

------------------------------------------------------------------------

## 37. Drift Lifecycle

Minimum:

``` text
DETECTED
RESOLVED
```

A later trusted authoritative execution may resolve drift.

Resolution must be fencing/order aware.

An older/stale execution cannot resolve drift created/confirmed by a
newer execution.

Do not implement acknowledgement/assignment/suppression/remediation
workflow in Phase 12.

------------------------------------------------------------------------

## 38. Drift Evaluation Ordering

Drift must be evaluated deterministically against the previous trusted
state and reconciliation result.

Recommended semantic order:

``` text
capture previous trusted normalized state/reference
 -> acquire new trusted observation
 -> compute safe drift evidence
 -> reconcile
 -> persist/resolve drift in the same authoritative execution context
```

Do not mutate canonical state independently from drift logic.

If exact transaction ordering differs because of existing service
boundaries, preserve the invariant that drift describes authoritative
state transitions and stale executions cannot overwrite newer truth.

------------------------------------------------------------------------

## 39. Source Authority Foundation

Implement the smallest useful source/domain authority representation.

Conceptually:

``` text
sourceSystem
entityDomain
authorityLevel
```

Levels:

``` text
AUTHORITATIVE
SUPPLEMENTAL
DERIVED
```

Initial expected relationship:

``` text
ERICSSON_ENM / RADIO_INVENTORY -> AUTHORITATIVE
SNIP / DERIVED_ASSURANCE       -> DERIVED
SNIP / DIGITAL_TWIN            -> DERIVED
```

This may be in-code/configuration if no dynamic persistence is required.

Do not build field-level conflict resolution.

------------------------------------------------------------------------

## 40. Network Knowledge Confidence

Implement:

``` text
HIGH
MEDIUM
LOW
UNKNOWN
```

This is operational confidence.

It MUST be deterministic and non-LLM.

It MUST NOT be represented as a percentage/probability unless a later
architecture explicitly defines one.

------------------------------------------------------------------------

## 41. Knowledge Confidence Reason Codes

Confidence should include safe deterministic reason codes.

Conceptual examples:

``` text
NO_TRUSTED_BASELINE
FRESH_COMPLETE_SYNC
AGING_TRUSTED_BASELINE
SOURCE_STALE
RECENT_TRANSIENT_FAILURE
SOURCE_UNREACHABLE
AUTHENTICATION_FAILED
AUTHORIZATION_FAILED
THROTTLED
CHECKPOINT_INVALID
CHECKPOINT_UNCERTAIN
RECOVERY_REQUIRED
RECENT_PARTIAL_OBSERVATION
CONTINUITY_LOST
```

Use enums/controlled values, not arbitrary free-form reason text as
authoritative logic.

------------------------------------------------------------------------

## 42. Confidence Evaluation Rules

Implement deterministic rules with tests.

Minimum invariants:

### HIGH

Requires all of:

``` text
trusted baseline exists
freshness == FRESH
checkpoint == VALID
latest trusted authoritative state is sufficiently complete
no recovery required
source health permits high trust
continuity intact
```

### MEDIUM

May represent:

``` text
AGING trusted state
recent transient failure
temporary throttling
failed recent incremental attempt while recent trusted baseline remains valid
minor non-continuity-losing warning
```

### LOW

Must include materially unsafe states such as:

``` text
STALE
RECOVERY_REQUIRED
CHECKPOINT_UNCERTAIN
prolonged source unavailability
repeated failures materially reducing trust
materially incomplete current knowledge
```

### UNKNOWN

No trusted complete baseline exists.

`RECOVERY_REQUIRED` MUST NOT evaluate to HIGH.

Document the exact decision table in code/tests and completion report.

------------------------------------------------------------------------

## 43. Network Knowledge Status Persistence

Persist latest source/domain knowledge status, conceptually:

``` text
sourceSystem
domain
freshness
sourceHealth
confidence
reasonCodes
lastTrustedSynchronizationAt
lastTrustedSnapshotId
evaluatedAt
```

Do not persist every recalculation as a new history row unless existing
audit/history mechanisms naturally do so.

Synchronization/import history remains the historical evidence.

------------------------------------------------------------------------

## 44. Knowledge Status Ordering

Update authoritative latest knowledge status only after synchronization
outcome is known.

Successful authoritative sync:

``` text
reconcile/checkpoint success
 -> drift evaluation
 -> source state
 -> knowledge status
```

Failed sync may degrade health/freshness/confidence without pretending
reconciliation succeeded.

All authoritative latest-state writes must be fencing/order safe.

------------------------------------------------------------------------

## 45. Downstream Read Contract

Expose synchronization-derived metadata for downstream use without broad
rewrites.

At minimum make safely queryable:

``` text
sourceSystem
lastTrustedSnapshotId
lastTrustedSynchronizationAt
freshness
sourceHealth
knowledgeConfidence
confidenceReasonCodes
```

Do not automatically change Phase 3 Assurance decisions, Phase 5 Agent
behavior, Phase 6 Twin synchronization, or Phase 4 action rules.

Those layers may consume the metadata later.

------------------------------------------------------------------------

## 46. Manual Synchronization API

Reuse the existing governed integration/import API namespace where
practical.

Manual synchronization requests must:

-   require existing vendor-import authorization;
-   enter the same control plane as scheduled runs;
-   respect `enabled`;
-   respect Phase 8 lease/fencing;
-   respect overlap `SKIP`;
-   never accept credential values;
-   never accept arbitrary endpoints;
-   never accept caller-provided fencing tokens;
-   never accept arbitrary vendor methods.

Exact path should follow repository conventions.

------------------------------------------------------------------------

## 47. Manual Recovery API

Add an explicitly authorized recovery operation through the same control
plane.

Conceptual permission:

``` text
TRIGGER_RECOVERY_SYNCHRONIZATION
```

It triggers `RECOVERY_FULL`.

It does not bypass:

-   enabled state;
-   lease/fencing;
-   Phase 10 security;
-   read-only connector;
-   deadlines;
-   reconciliation/checkpoint invariants;
-   audit.

Do not expose raw checkpoint mutation through API.

------------------------------------------------------------------------

## 48. Read APIs

Provide safe operational visibility consistent with current API
conventions.

Capabilities should cover:

``` text
list configured synchronization sources
get source current state
list/get synchronization/import executions
get safe checkpoint metadata
list/get drift observations
get network knowledge status
```

Checkpoint API responses MUST NOT expose secrets.

If checkpoint values themselves could be vendor-sensitive, expose safe
metadata/hash/type/status rather than unrestricted raw value.

------------------------------------------------------------------------

## 49. Authorization

Use existing authorization mechanisms.

Conceptual permissions:

``` text
VIEW_SYNCHRONIZATION_STATUS
TRIGGER_VENDOR_IMPORT
TRIGGER_RECOVERY_SYNCHRONIZATION
```

Do not invent a parallel auth framework.

Scheduled execution is system authority only for configured read-only
synchronization.

It does not gain general operator or vendor-write privileges.

------------------------------------------------------------------------

## 50. Disabled Source Behavior

For `enabled=false`:

``` text
scheduler -> no execution
manual normal sync -> rejected
manual recovery -> rejected
source health -> DISABLED
credential resolution -> not performed
vendor session -> not opened
```

No hidden override is introduced.

------------------------------------------------------------------------

## 51. Simulator Extensions

Extend the deterministic Phase 11 simulator to support at least:

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

Additional scenarios are allowed if they prove required edge cases.

All data remains synthetic.

------------------------------------------------------------------------

## 52. Synthetic Incremental Contract

Define a simple deterministic simulator-only incremental contract.

Recommended characteristics:

-   monotonically ordered synthetic source sequence/version;
-   opaque checkpoint token at the SNIP neutral boundary;
-   deterministic change batches;
-   explicit UPSERT/REMOVE;
-   deterministic sequence-gap injection;
-   deterministic checkpoint expiration/rejection;
-   deterministic replay of the same batch;
-   no implication that Ericsson ENM uses this mechanism.

Clearly name/document simulator semantics as synthetic.

------------------------------------------------------------------------

## 53. Production Transport

Do not implement a guessed production ENM transport.

The existing fail-closed production behavior remains.

If production mode is selected without a configured proven transport,
return the existing/suitable failure equivalent to:

``` text
PRODUCTION_TRANSPORT_NOT_CONFIGURED
```

Do not add a generic arbitrary HTTP client as a shortcut.

------------------------------------------------------------------------

## 54. Credential and Trust Handling

Preserve Phase 10.

The synchronization layer receives no raw long-lived secret storage
capability.

Production flow remains:

``` text
lease authority
 -> per-session credential/trust resolution
 -> read-only connector session
```

No secret cache is introduced by the scheduler/control plane.

No older secret-version fallback is introduced.

------------------------------------------------------------------------

## 55. Network Security

Preserve Phase 9--11 security:

-   TLS server verification;
-   hostname verification;
-   no trust-all;
-   mTLS only when required by proven profile;
-   read-only vendor identity;
-   controlled egress;
-   no broad production `0.0.0.0/0`;
-   no arbitrary user-supplied endpoint;
-   application policy remains canonical;
-   K8s/Cilium remains defense in depth.

Do not change Phase 10 Cilium limitations into false production claims.

------------------------------------------------------------------------

## 56. Application Health and Readiness

Do not make vendor connectivity a pod-readiness dependency.

Valid state:

``` text
SNIP APPLICATION: READY
ERICSSON SOURCE: UNREACHABLE
KNOWLEDGE: STALE / LOW
```

Readiness endpoints must not open vendor sessions or resolve Key Vault
credentials.

If source health is exposed through health endpoints, it must be
informational/non-readiness-blocking unless existing conventions provide
a clearly separate health group.

------------------------------------------------------------------------

## 57. Audit Events

Extend existing audit mechanisms.

Conceptual events:

``` text
SYNCHRONIZATION_DUE
SYNCHRONIZATION_STARTED
OVERLAP_SKIPPED
LEASE_ACQUIRED
CHECKPOINT_LOADED
MODE_SELECTED
CONNECTOR_SESSION_STARTED
SOURCE_STATE_ACQUIRED
SOURCE_STATE_VALIDATED
RECONCILIATION_STARTED
RECONCILIATION_COMPLETED
CHECKPOINT_ADVANCED
CHECKPOINT_INVALIDATED
CONTINUITY_LOST
RECOVERY_REQUIRED
DRIFT_EVALUATED
KNOWLEDGE_STATUS_UPDATED
SYNCHRONIZATION_COMPLETED
SYNCHRONIZATION_FAILED
LEASE_RELEASED
```

Reuse existing audit vocabulary where semantically equivalent.

No secret/raw vendor payload in audit.

------------------------------------------------------------------------

## 58. Metrics

Extend existing metrics with low-cardinality measurements for:

``` text
synchronization runs
success/failure
overlap skips
duration
mode
checkpoint advance/failure
freshness age
drift detected/resolved
source health transitions
knowledge confidence state
recovery-required transitions
```

Do not use entity IDs, execution IDs, checkpoint values, credential
references, tokens, or raw vendor error text as metric labels.

------------------------------------------------------------------------

## 59. Database Migration Requirements

Use a new forward-only Flyway migration after the current latest
migration.

Before writing it, inspect the current migration sequence.

Add only schema needed for chosen persistence decisions.

Likely concepts:

``` text
synchronization_checkpoint
synchronization_source_state
network_drift_observation
network_knowledge_status
```

Potential execution metadata may be added to existing import tables or a
dedicated table only when justified.

Requirements:

-   no secret-bearing columns;
-   no unrestricted raw payload column;
-   indexes for source/scope/current-state lookup;
-   uniqueness enforcing one current checkpoint/status per intended
    source scope where appropriate;
-   constraints for controlled enums where repository conventions use
    them;
-   historical migrations untouched.

------------------------------------------------------------------------

## 60. Transaction Boundaries

Document transaction boundaries in code and completion report.

At minimum ensure:

-   stale fencing holder cannot commit authoritative Phase 12 state;
-   checkpoint cannot become authoritative before reconciliation;
-   drift/current-state updates cannot overwrite newer execution state;
-   retries do not create duplicate authoritative rows;
-   current status projection remains consistent after failures;
-   crash-window behavior is deterministic.

Avoid excessive `REQUIRES_NEW` use that breaks correctness ordering.

Use it only where existing architecture requires independent durable
evidence and where it cannot create false authoritative state.

------------------------------------------------------------------------

## 61. Source-State Ordering

Use execution ordering/fencing/authoritative timestamps as appropriate.

Do not rely solely on wall-clock completion time to determine authority
when fencing tokens provide stronger ordering.

A late older execution cannot overwrite:

``` text
latestCompletedExecution
checkpoint
freshness
source health
knowledge confidence
drift resolution
recoveryRequired
```

from a newer authoritative execution.

------------------------------------------------------------------------

## 62. UTC Time

Use UTC `Instant`-style correctness semantics for:

``` text
scheduledAt
startedAt
completedAt
observedAt
checkpoint timestamps
lastSuccessAt
lastFailureAt
freshness calculations
drift timestamps
knowledge evaluation
```

No local timezone logic in synchronization correctness.

------------------------------------------------------------------------

## 63. Architecture Boundary Tests

Add explicit tests that fail if forbidden dependencies appear.

At minimum protect:

``` text
Scheduler -> EnmTransport                 FORBIDDEN
Scheduler -> Key Vault/Azure SDK          FORBIDDEN
Agent -> EricssonEnmConnector             FORBIDDEN
Agent -> EnmTransport                     FORBIDDEN
MCP -> EricssonEnmConnector               FORBIDDEN
MCP -> EnmTransport                       FORBIDDEN
Phase 4 -> EricssonEnmConnector           FORBIDDEN
Phase 4 -> EnmTransport                   FORBIDDEN
Synchronization -> vendor mutation API    FORBIDDEN
```

Reuse the Phase 11 isolation-test style where possible.

------------------------------------------------------------------------

## 64. Mandatory Test Matrix

Implement automated tests covering at least the following:

1.  scheduled due source enters control plane;
2.  scheduler never directly invokes transport;
3.  manual and scheduled initiators converge on the same runtime;
4.  disabled source does not execute;
5.  disabled source does not resolve credentials/open session;
6.  overlapping scheduled trigger is skipped;
7.  manual trigger also respects active-source lease;
8.  overlap skip does not mutate canonical/checkpoint state;
9.  multi-replica lease race permits one authoritative execution;
10. stale holder cannot reconcile;
11. stale holder cannot advance checkpoint;
12. stale holder cannot overwrite source state;
13. stale holder cannot overwrite knowledge status;
14. stale holder cannot resolve newer drift;
15. first source with no checkpoint selects FULL;
16. successful FULL COMPLETE reconciles and advances checkpoint;
17. failed FULL does not advance checkpoint;
18. PARTIAL does not infer destructive absence;
19. valid incremental capability/checkpoint selects INCREMENTAL;
20. unsupported incremental fails closed according to policy;
21. successful INCREMENTAL advances only after reconciliation;
22. incremental omission does not remove entity;
23. explicit synthetic REMOVE follows conservative lifecycle;
24. same incremental batch replay is idempotent;
25. same full snapshot replay is idempotent;
26. checkpoint rejected -\> recovery required;
27. checkpoint expired -\> recovery required;
28. sequence gap -\> recovery required;
29. recovery-required cannot continue ordinary incremental;
30. next permitted recovery selects RECOVERY_FULL;
31. successful recovery full restores valid checkpoint;
32. failed recovery does not recursively launch new jobs;
33. normal failure retries are bounded;
34. failed execution waits for next permitted cadence;
35. `Retry-After` remains bounded/deadline-aware where applicable;
36. cancellation before reconcile causes no authoritative mutation;
37. cancellation before checkpoint causes no unsafe advancement;
38. deadline expiry prevents late authoritative commit;
39. reconciliation-success/checkpoint-uncertain crash window is safe;
40. current source state survives restart/persistence reload;
41. checkpoint survives restart/persistence reload;
42. freshness UNKNOWN before trusted baseline;
43. successful trusted full -\> FRESH;
44. time progression -\> AGING;
45. time progression -\> STALE;
46. operational failure can produce DEGRADED;
47. application readiness remains ready during vendor outage;
48. authentication failure maps safely;
49. authorization failure maps safely;
50. throttling maps safely;
51. source-state drift detected;
52. synchronization drift detected;
53. drift record contains no raw vendor payload;
54. later trusted state resolves applicable drift;
55. stale execution cannot resolve newer drift;
56. HIGH confidence after trusted fresh complete state;
57. AGING/transient warning produces expected MEDIUM policy;
58. STALE produces LOW;
59. RECOVERY_REQUIRED produces LOW;
60. no trusted baseline produces UNKNOWN;
61. Agent/LLM cannot override confidence;
62. confidence reason codes deterministic;
63. source/domain confidence scoped correctly;
64. manual recovery requires authorization;
65. view synchronization status requires appropriate authorization;
66. API cannot supply credential values;
67. API cannot supply arbitrary endpoint;
68. API cannot supply fencing token/lease ownership;
69. API cannot mutate checkpoint directly;
70. production transport remains fail-closed;
71. no vendor write capability advertised;
72. no arbitrary HTTP dispatch added;
73. readiness performs no live ENM/Key Vault access;
74. audit contains no secrets/raw payload;
75. metrics avoid high-cardinality sensitive labels;
76. Phase 7--11 regression tests remain green;
77. default CI remains Azure-independent;
78. default CI remains real-vendor-independent.

The final number of tests may exceed this matrix.

------------------------------------------------------------------------

## 65. Security Hygiene Searches

Before completion, search the changed repository for accidental
introduction of:

``` text
real Ericsson hostnames
real Ericsson IP addresses
real credentials
client_secret
bearer tokens
private keys
trust-all TLS
disabled hostname verification
0.0.0.0/0 vendor egress
POST/PUT/PATCH/DELETE vendor mutation methods
setParameter
executeCommand
applyConfiguration
arbitrary HTTP method dispatch
Agent imports of connector/transport
MCP imports of connector/transport
Phase 4 imports of connector/transport
raw vendor payload persistence
```

Interpret matches carefully; test strings/docs may legitimately describe
forbidden behavior.

No real secret/endpoints may be committed.

------------------------------------------------------------------------

## 66. Build and Dependency Rules

Do not add dependencies unless required.

In particular:

-   do not add a new HTTP client for guessed ENM behavior;
-   do not add Azure dependencies to synchronization code;
-   do not add a scheduler framework if the existing application stack
    already supports scheduling;
-   do not add a retry/circuit-breaker framework if existing retry
    primitives suffice.

Any new dependency must be justified in the completion report.

------------------------------------------------------------------------

## 67. Default CI Contract

The existing default CI workflow must remain independent of:

``` text
Azure subscription
az login
Azure Key Vault
Workload Identity live tenant
real Ericsson ENM
real vendor credentials
private vendor DNS/network
```

Phase 12 tests use deterministic local/simulator/contract proof.

Do not modify CI to require real infrastructure.

------------------------------------------------------------------------

## 68. Real Vendor E2E Status

Phase 12 implementation completion may legitimately state:

``` text
SIMULATOR/CONTRACT STATUS: VERIFIED
REAL VENDOR CONTINUOUS SYNCHRONIZATION STATUS: NOT YET VERIFIED
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
```

Do not mark real vendor E2E verified based on simulator tests.

Do not block implementation architectural acceptance solely because real
ENM remains unavailable/unconfigured.

------------------------------------------------------------------------

## 69. Documentation Updates

Update documentation consistently after implementation.

Expected documents include, according to repository conventions:

``` text
docs/architecture/SNIP-PHASE-12-CONTINUOUS-SYNCHRONIZATION-DRIFT-NETWORK-KNOWLEDGE-CONFIDENCE-ARCHITECTURE.md
docs/implementation/SNIP-PHASE-12-CONTINUOUS-SYNCHRONIZATION-DRIFT-NETWORK-KNOWLEDGE-CONFIDENCE-SPECIFICATION.md
docs/implementation/SNIP-PHASE-12-CONTINUOUS-SYNCHRONIZATION-DRIFT-NETWORK-KNOWLEDGE-CONFIDENCE-COMPLETION-REPORT.md
docs/implementation/SNIP-IMPLEMENTATION-CONTEXT.md
docs/implementation/SNIP-IMPLEMENTATION-STATUS.md
.cursor/rules/snip-architecture.mdc
README.md
```

If the repository intentionally keeps a root architecture copy, keep it
status/content synchronized with the canonical architecture according to
existing convention.

Do not rewrite historical phase documents unnecessarily.

------------------------------------------------------------------------

## 70. Completion Report

Create:

``` text
docs/implementation/SNIP-PHASE-12-CONTINUOUS-SYNCHRONIZATION-DRIFT-NETWORK-KNOWLEDGE-CONFIDENCE-COMPLETION-REPORT.md
```

The report must contain:

-   parent baseline SHA;
-   implementation date;
-   architecture/spec references;
-   files added/modified;
-   persistence decision and migration;
-   execution-history reuse/new-table decision;
-   synchronization policy design;
-   scheduler/control-plane flow;
-   FULL/INCREMENTAL/RECOVERY_FULL behavior;
-   checkpoint representation and lifecycle;
-   reconciliation/checkpoint atomicity strategy;
-   crash-window strategy;
-   fencing extension;
-   overlap/backpressure behavior;
-   retry/recovery behavior;
-   simulator incremental contract;
-   freshness rules;
-   source-health rules;
-   drift model/lifecycle;
-   source-authority representation;
-   confidence decision table/reason codes;
-   API/authorization changes;
-   audit/metrics changes;
-   security-boundary verification;
-   dependency changes or explicit statement of none;
-   Maven test count/results;
-   Go test/build results;
-   Git status;
-   known limitations;
-   inherited technical debt;
-   new technical debt;
-   real-vendor status;
-   production transport status;
-   explicit no-write/no-Phase-13 statement.

Before architectural review, the report MUST end with:

``` text
PHASE 12 STATUS: IMPLEMENTED — PENDING ARCHITECTURAL ACCEPTANCE
```

Cursor MUST NOT self-declare architectural acceptance.

------------------------------------------------------------------------

## 71. Known Inherited Constraints to Preserve

Do not accidentally claim Phase 12 resolves unrelated debt.

Examples include:

-   Phase 5 non-interruptible per-Agent timeout debt;
-   Phase 6 synthetic/non-vendor-calibrated digital-twin model;
-   Phase 7 fixture/simulator limitations where still relevant;
-   Phase 8 PostgreSQL-level multi-instance proof vs full Kubernetes
    multi-replica E2E;
-   Phase 10 Cilium FQDN-cache limitation;
-   Phase 10 private-endpoint/private-DNS platform target;
-   Phase 11 real Ericsson production transport unresolved;
-   Phase 11 real-vendor E2E not yet verified.

Carry forward only debt that remains true after implementation.

------------------------------------------------------------------------

## 72. Forbidden Git Actions During Implementation

Until architectural review is complete, Cursor MUST NOT:

``` text
git add .
git commit
git push
git tag
amend Phase 11
establish Phase 12 baseline
start Phase 13
```

Selective staging is not required during implementation review.

The user/architect will authorize the Phase 12 immutable Git baseline
only after completion-report review and architectural acceptance.

------------------------------------------------------------------------

## 73. Verification Commands

Before reporting Phase 12 implementation complete, run from repository
root:

``` text
mvn -B test
go test ./...
go build ./cmd/simulator
git status --short
git diff --check
```

If repository structure requires the Go commands from a subdirectory,
use the established Phase 11 command/location convention and report it
exactly.

Also perform architecture/security searches required by this
specification.

Do not hide failing tests.

------------------------------------------------------------------------

## 74. Required Verification Evidence

The completion report must record:

``` text
Maven tests:
total / failures / errors / skipped

Go tests:
PASS/FAIL

Go simulator build:
exit result

git diff --check:
PASS/FAIL

working tree:
expected changed files only

default CI:
not required before implementation review unless explicitly requested
```

The exact Maven test count is expected to be greater than the frozen
Phase 11 count of 199 because Phase 12 adds tests, but no artificial
target count is required.

All existing tests must remain green.

------------------------------------------------------------------------

## 75. Architecture Conformance Review Checklist

Before stopping, Cursor must self-check implementation conformance
without self-accepting it:

``` text
[ ] vendor path remains read-only
[ ] scheduler does not call transport
[ ] Phase 8 lease/fencing remains authority
[ ] one active sync per source scope
[ ] overlap = SKIP
[ ] FULL/INCREMENTAL/RECOVERY_FULL distinct
[ ] incremental capability not assumed
[ ] checkpoint opaque/durable
[ ] checkpoint advances only after reconcile
[ ] crash window handled
[ ] incremental omission != removal
[ ] recovery full safe/bounded
[ ] retry bounded
[ ] freshness deterministic
[ ] source health independent of readiness
[ ] drift observational
[ ] confidence deterministic/non-LLM
[ ] confidence source/domain scoped
[ ] stale execution cannot overwrite newer truth
[ ] Phase 10 secret handling preserved
[ ] production ENM transport still unconfigured
[ ] no real endpoints/credentials
[ ] Agent/MCP/Phase 4 isolation preserved
[ ] default CI Azure/vendor independent
[ ] real-vendor E2E not falsely claimed
[ ] Phase 13 not started
```

Any failed item must be reported.

------------------------------------------------------------------------

## 76. Required Phase 12 Implementation End State

At the end of implementation, before architectural acceptance:

``` text
PHASE 12 ARCHITECTURE STATUS: ACCEPTED
PHASE 12 IMPLEMENTATION STATUS: COMPLETE — PENDING ARCHITECTURAL ACCEPTANCE
SIMULATOR/CONTRACT STATUS: VERIFIED
REAL VENDOR CONTINUOUS SYNCHRONIZATION STATUS: NOT YET VERIFIED
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
PHASE 12 GIT BASELINE: NOT ESTABLISHED
PHASE 13 STATUS: NOT STARTED
```

The completion report final line must be exactly:

``` text
PHASE 12 STATUS: IMPLEMENTED — PENDING ARCHITECTURAL ACCEPTANCE
```

------------------------------------------------------------------------

## 77. Acceptance Scenario

The implementation must be able to prove, through deterministic
simulator/contract tests:

``` text
configured source
 -> becomes due
 -> scheduler triggers control plane
 -> Phase 8 lease acquired
 -> no checkpoint => FULL
 -> complete canonical state established
 -> checkpoint committed
 -> freshness FRESH
 -> confidence HIGH

synthetic source changes
 -> due again
 -> valid incremental capability/checkpoint
 -> INCREMENTAL
 -> source-state drift detected
 -> canonical reconciliation
 -> checkpoint advances
 -> knowledge remains trusted

synthetic continuity failure
 -> checkpoint rejected/gap
 -> RECOVERY_REQUIRED
 -> confidence degrades
 -> no unsafe incremental continuation
 -> no immediate job storm

next permitted execution
 -> RECOVERY_FULL
 -> complete trusted state restored
 -> new valid checkpoint
 -> freshness FRESH
 -> confidence HIGH
```

This proves Phase 12 neutral synchronization architecture.

It does not prove real Ericsson continuous synchronization.

------------------------------------------------------------------------

## 78. Stop Condition

After implementation, tests, documentation, completion report, and
conformance checks are complete:

**STOP.**

Do not commit.

Do not push.

Do not establish a Phase 12 Git baseline.

Do not start Phase 13.

Return the completion report and verification evidence for architectural
review.

------------------------------------------------------------------------

## 79. Cursor Implementation Instruction

Cursor should implement Phase 12 according to the accepted architecture
and this specification, preserving all frozen Phase 7--11 behavior.

The implementation should prefer the smallest coherent extension of
existing abstractions over parallel infrastructure.

When a repository detail differs from a conceptual name in this
specification, preserve the architectural responsibility rather than
mechanically forcing the proposed class name.

When an architectural ambiguity is encountered that could change a
frozen invariant, stop and report it rather than guessing.

No real Ericsson endpoint, credential, proprietary production interface,
vendor mutation operation, or Phase 13 work may be introduced.

------------------------------------------------------------------------

## 80. Final Specification Invariants

The following are non-negotiable:

``` text
Vendor interaction remains READ_ONLY.

Scheduler is an initiator, not a vendor client.

Phase 8 lease/fencing remains synchronization concurrency authority.

One authoritative synchronization per source scope.

Overlap policy = SKIP.

FULL, INCREMENTAL, RECOVERY_FULL remain distinct.

Incremental semantics are capability/profile-dependent.

Checkpoint values are durable and opaque.

Checkpoint never advances before successful reconciliation.

Crash uncertainty is fail-safe.

Incremental omission never means deletion.

Explicit REMOVE remains non-destructive by default.

Retry and recovery are bounded.

Freshness is deterministic.

Source health is separate from application readiness.

Drift is observational.

Network knowledge confidence is deterministic operational confidence.

Agents/LLMs cannot assign authoritative confidence.

Stale executions cannot overwrite newer synchronization truth.

Phase 9/10/11 security remains authoritative.

Production secrets remain per-session and outside synchronization persistence.

Production ENM transport remains NOT CONFIGURED.

Real vendor continuous synchronization remains NOT YET VERIFIED.

Default CI remains Azure-independent and vendor-independent.

Agents/MCP/Phase 4 do not directly operate vendor synchronization.

No vendor write capability is introduced.

Phase 13 remains NOT STARTED.
```

------------------------------------------------------------------------

## 81. Specification Status

``` text
PHASE 12 ARCHITECTURE STATUS: MUST BE ACCEPTED BEFORE CODE IMPLEMENTATION
PHASE 12 SPECIFICATION STATUS: READY
PHASE 12 IMPLEMENTATION STATUS: NOT STARTED
SIMULATOR/CONTRACT STATUS: NOT YET VERIFIED
REAL VENDOR CONTINUOUS SYNCHRONIZATION STATUS: NOT YET VERIFIED
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
PHASE 12 GIT BASELINE: NOT ESTABLISHED
PHASE 13 STATUS: NOT STARTED
```

**No Phase 12 Git baseline is authorized by this specification.**
