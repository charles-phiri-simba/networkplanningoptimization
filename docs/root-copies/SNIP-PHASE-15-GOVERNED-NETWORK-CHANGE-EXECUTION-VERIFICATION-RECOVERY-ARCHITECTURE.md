# SNIP Phase 15 — Governed Network Change Execution, Verification & Recovery Architecture

## Document Status

**Status:** ACCEPTED AND FROZEN  
**Implementation:** NOT STARTED  
**Implementation specification:** NOT YET ISSUED  
**Phase 15 Git baseline:** NOT ESTABLISHED  
**Phase 16:** NOT STARTED

## Authoritative Parent Baseline

**Phase 14 immutable implementation baseline:**  
`043c5ad98b8a12fb8073ba40364a2e287d2cc65a`

Phase 15 architecture is **accepted and frozen**. Implementation MUST NOT begin until a separate implementation specification is issued and authorized.

---

# 1. Phase Title

**SNIP Phase 15 — Governed Network Change Execution, Verification & Recovery**

# 2. Defining Principle

> **Phase 15 introduces SNIP’s first governed execution capability. It may execute an authorized `READY_FOR_EXECUTION` plan only against an explicitly permitted non-production execution target, verify the resulting state independently, and recover deterministically when required. Real vendor-network mutation remains prohibited.**

Phase 14 answers: **What exact change is safe, authorized and ready to perform?**

Phase 15 answers: **Can SNIP perform that exact authorized plan through a controlled execution protocol, prove what happened, verify the resulting state, and recover safely if the result is uncertain or incorrect?**

Phase 15 does **not** authorize mutation of a production Ericsson ENM, Nokia NetAct, or other live vendor network.

# 3. Architectural Progression

```text
Phase 13 approved recommendation
        ↓
Phase 14 NetworkChangePlan
        ↓
READY_FOR_EXECUTION
        ↓
──────────────── Phase 15 boundary ────────────────
        ↓
Execution Request
        ↓
Preliminary Execution Admission
        ↓
Mandatory Execution Review
        ↓
Mandatory Execution Authorization
        ↓
Execution Lease + Final Preflight
        ↓
Non-production Execution Adapter
        ↓
APPLIED / OUTCOME_UNKNOWN / FAILED
        ↓
Independent Verification
        ↓
VERIFIED or RECOVERY_REQUIRED
        ↓
Governed Rollback or MANUAL_INTERVENTION_REQUIRED
```

Phase 15 SHALL establish a distinct **Network Change Execution Plane**. It MUST NOT add execution behavior directly to Phase 14 planning services, Phase 11 read-only transport, MCP mutation tools, or Agent orchestration.

# 4. Primary Aggregate — `NetworkChangeExecution`

A Phase 14 `NetworkChangePlan` describes an authorized intent. A Phase 15 `NetworkChangeExecution` records one governed attempt to perform that intent.

```text
NetworkChangePlan 1 ───────── 0..N NetworkChangeExecution (historical/terminal)
NetworkChangePlan 1 ───────── at most 1 ACTIVE NetworkChangeExecution
```

A plan MAY have multiple **terminal** execution records only under controlled retry/recovery semantics (for example, a failed or cancelled attempt followed by a separately requested and authorized new execution). At most **one ACTIVE** execution MAY exist per plan at any time. Arbitrary repeated execution of the same authorized plan is prohibited.

Minimum execution aggregate fields:

```text
executionId
planId
planVersion
planFingerprint
executionTargetId
executionTargetType
executionTargetEnvironment
executionFingerprint
authorizedExecutionFingerprint
status
requestedBy
requestedAt
reviewedBy
reviewedAt
authorizedBy
authorizedAt
admittedAt
startedAt
completedAt
executionLeaseId
fencingToken
failureCode
failureDetailSafe
verificationStatus
verificationCompletedAt
recoveryStatus
rollbackStatus
createdAt
updatedAt
version
```

The aggregate MUST NOT contain secrets, vendor credentials, private keys, bearer tokens, raw authentication material, or raw vendor payloads by default.

# 5. Execution Target Model

Phase 15 introduces:

```text
ExecutionTarget
ExecutionTargetDescriptor
ExecutionTargetRegistry
```

Initial permitted target types:

```text
SIMULATOR
CONTROLLED_SANDBOX
```

Explicitly prohibited target types:

```text
ERICSSON_ENM_PRODUCTION
NOKIA_NETACT_PRODUCTION
LIVE_VENDOR_NETWORK
```

Target capabilities may include:

```text
PARAMETER_WRITE
PARAMETER_READBACK
ROLLBACK
EXPECTED_STATE_GUARD
IDEMPOTENT_OPERATION
```

Admission MUST fail closed when required capability support is absent or unknown.

Each `ExecutionTargetDescriptor` MUST bind at minimum:

```text
targetId
targetType
environment
adapterProfileId
capabilityProfileVersion
```

Target identity is execution-significant. Substitution of any bound field after authorization invalidates authorization.

### CONTROLLED_SANDBOX definition

`CONTROLLED_SANDBOX` is an **explicitly configured non-production execution target** that MUST satisfy all of the following:

- environment classification is known and non-production;
- target is registered on an explicit allow-list;
- no production network routing or production vendor account;
- no production inventory authority;
- credentials, if any, are synthetic/test-only and resolved outside Phase 15 production packages;
- target cannot be auto-discovered or inferred from canonical/import state.

If environment classification is unknown, admission MUST fail closed. `CONTROLLED_SANDBOX` MUST NOT activate without explicit configuration and registry entry. It MUST NOT be interpreted as arbitrary external infrastructure or as a loophole for production vendor writes.

# 6. Execution Adapter SPI

Phase 15 SHALL introduce an execution-specific SPI independent of the Phase 11 read-only transport:

```java
public interface NetworkChangeExecutionAdapter {
    ExecutionTargetDescriptor target();
    OperationExecutionResult execute(
        AuthorizedExecutionOperation operation,
        ExecutionContext context);
    VerificationObservation verify(
        AuthorizedExecutionOperation operation,
        ExecutionContext context);
    RollbackExecutionResult rollback(
        AuthorizedRollbackOperation operation,
        ExecutionContext context);
}
```

Initial implementation:

```text
SimulatorExecutionAdapter
```

Optional explicitly configured non-production implementation:

```text
SandboxExecutionAdapter
```

There SHALL be no production ENM/NetAct write implementation in Phase 15.

`execute()` and `verify()` MAY be implemented by the same adapter class, but verification MUST remain a **separate logical observation operation**. Verification MUST NOT treat the mutation response as authoritative evidence. Where practical, verification SHOULD use an independent read path (for example, a dedicated readback method) even when colocated in one adapter. The implementation specification MAY introduce a separate `ExecutionStateObservationAdapter` only if needed; architecturally, observation semantics MUST remain independent from mutation reporting.

# 7. Phase 11 Read-Only Boundary Preservation

Phase 11 remains read-only. Phase 15 MUST NOT:

- add write methods to `EnmTransport`;
- add write capability to existing Ericsson read-only connectors;
- inject `EnmTransport` into Phase 15 execution services;
- inject vendor connectors into Phase 15 execution services;
- inject Phase 10 `CredentialHandle` into Phase 15 execution services;
- resolve vendor credentials for execution;
- reuse import APIs as mutation paths.

Required invariant:

```text
changeexecution production package
→ no EnmTransport
→ no Ericsson connector
→ no Nokia connector
→ no CredentialHandle
→ no Azure Key Vault dependency
```

# 8. Execution Admission

Introduce `ExecutionAdmissionService`.

A `READY_FOR_EXECUTION` plan is necessary but not sufficient. Admission is **preliminary validation** that establishes an execution record eligible for human review and authorization. It is distinct from final preflight immediately before mutation.

### Admission protocol (safe ordering)

```text
1. resolve immutable execution-significant inputs from Phase 14 + configured target
2. preliminary admission validation (no lease)
3. persist ADMITTED / READY_FOR_EXECUTION_AUTHORIZATION
4. human review (mandatory)
5. human execution authorization (mandatory)
6. acquire execution lease + fencing token
7. final authoritative revalidation under lease/fencing
8. mutate target
9. verify
10. release lease
```

Preliminary admission MUST verify:

1. plan exists;
2. plan status is `READY_FOR_EXECUTION`;
3. plan is not expired;
4. plan is not invalidated;
5. plan is not cancelled;
6. plan is not superseded;
7. originating Phase 13 proposal remains valid;
8. plan fingerprint remains current;
9. Phase 14 authorization fingerprint remains current;
10. requested execution target is configured and permitted;
11. target environment is non-production;
12. target supports all required capabilities;
13. target still exists;
14. rollback plan remains valid;
15. dependency graph remains valid;
16. Phase 14 safety policy remains satisfied;
17. execution window permits execution if configured;
18. no conflicting **active** execution owns the protected scope;
19. no conflicting **active** execution exists for the same plan;
20. execution lease is available.

Final preflight immediately before mutation, **under acquired lease/fencing**, MUST revalidate:

1. plan still `READY_FOR_EXECUTION` and not invalidated/superseded/cancelled/expired;
2. Phase 14 and Phase 15 authorization fingerprints still current;
3. execution target binding unchanged;
4. expected current state still matches;
5. knowledge confidence remains acceptable;
6. synchronization/freshness remains trustworthy;
7. no relevant unresolved drift exists;
8. execution window still permits execution if configured;
9. fencing token still valid;
10. no conflicting active execution owns the protected scope.

Human execution authorization MUST be present before step 6. Admission MUST NOT require completed authorization; authorization is a separate mandatory gate between admission and lease acquisition.

Unknown mandatory evidence means **deny**.

# 9. Final Expected-State Guard

Before mutation Phase 15 MUST enforce the Phase 14 expected-state guard:

```text
target    = CELL-001
parameter = txPower
expected  = 46
desired   = 44
```

Required behavior:

```text
READ ACTUAL CURRENT STATE

IF actual != expected
    ABORT
    DO NOT MUTATE TARGET
ELSE
    continue
```

If actual state is unknown, stale, or unavailable, mutation is prohibited.

Application-level read-then-write is **not atomic** against an external target. Phase 15 MUST NOT claim stronger atomicity than the target provides. Where the target supports atomic compare-and-set, conditional mutation, or revision-token guards, the adapter SHOULD enforce the guard at the target boundary. Where the target cannot provide atomic guard semantics, verification and execution evidence MUST explicitly account for residual race between final read and write. The final expected-state read MUST occur immediately before mutation under valid lease/fencing.

# 10. Initial Scope Restriction

Phase 15 supports only:

```text
operation: SET_PARAMETER
target type: CELL
parameter: txPower
maximum operations: 1
```

Out of scope includes antenna tilt, neighbors, carrier activation, cell lock/unlock, frequency changes, multi-cell execution, multi-parameter execution, bulk execution, and vendor-specific command execution.

# 11. Execution Lifecycle

Recommended statuses:

```text
REQUESTED
ADMISSION_CHECKING
ADMISSION_REJECTED
ADMITTED
READY_FOR_EXECUTION_AUTHORIZATION
AUTHORIZED
EXECUTING
APPLIED
EXECUTION_OUTCOME_UNKNOWN
VERIFYING
VERIFIED
EXECUTION_FAILED
VERIFICATION_FAILED
RECOVERY_REQUIRED
ROLLBACK_REQUESTED
ROLLBACK_AUTHORIZED
ROLLING_BACK
ROLLBACK_APPLIED
ROLLED_BACK
ROLLBACK_FAILED
MANUAL_INTERVENTION_REQUIRED
CANCELLED_BEFORE_MUTATION
CANCELLED
```

Normative lifecycle for forward execution:

```text
REQUESTED
→ ADMISSION_CHECKING
→ ADMITTED
→ READY_FOR_EXECUTION_AUTHORIZATION
→ (mandatory human review)
→ (mandatory human authorization)
→ AUTHORIZED
→ acquire lease + final preflight
→ EXECUTING
→ APPLIED / EXECUTION_OUTCOME_UNKNOWN / EXECUTION_FAILED
→ VERIFYING
→ VERIFIED / VERIFICATION_FAILED / RECOVERY_REQUIRED
```

`REVIEW_NETWORK_CHANGE_EXECUTION` is **mandatory** before `AUTHORIZE_NETWORK_CHANGE_EXECUTION`. Review records evidence only; it does not authorize mutation.

Do not collapse materially different operational conditions into generic `SUCCESS` or `FAILED`.

# 12. `APPLIED` Is Not `VERIFIED`

Mandatory invariant:

```text
APPLIED != VERIFIED
```

Adapter success proves only that the execution operation was reported as applied. Successful completion requires independent readback.

Only a positive verification observation permits `VERIFIED`.

# 13. Independent Verification

Introduce `ExecutionVerificationService`.

Verification compares the desired Phase 14 state with independently observed target state.

Possible results:

```text
VERIFIED
MISMATCH
UNKNOWN
TIMEOUT
SOURCE_UNAVAILABLE
STALE_OBSERVATION
```

Only `VERIFIED` is successful completion.

Execution and verification SHALL be separate logical operations and separately persisted evidence.

Verification MUST observe the **execution target**, not SNIP canonical projection tables. A verification observation MUST include an observation timestamp (or target-native revision token when available). Observations captured before mutation started, or with timestamps/revisions that cannot prove post-mutation state, MUST be classified as `STALE_OBSERVATION` and MUST NOT verify success. Unknown, unavailable, or timed-out observation MUST NOT verify success. The mutation response itself MUST NOT count as verification.

# 14. Canonical-State Isolation

Execution target state is distinct from SNIP canonical state.

Phase 15 MUST NOT execute by directly updating `radio_configuration` or another canonical projection table.

For simulator execution:

```text
simulated target state != canonical knowledge state
```

Canonical state may change only through the existing synchronization/reconciliation architecture.

# 15. Phase 12 Reconciliation After Execution

Conceptual post-verification flow:

```text
Phase 15 execution VERIFIED
        ↓
new observation / synchronization required
        ↓
Phase 12 synchronization/reconciliation
        ↓
canonical knowledge refreshed
```

Phase 15 SHALL NOT fake canonical reconciliation by directly writing successful target state into canonical tables.

Phase 15 MUST NOT invoke Phase 12 synchronization directly, schedule synchronization, or mark canonical reconciliation successful. After `VERIFIED`, Phase 15 MAY only record that **synchronization is required** and/or emit a durable **synchronization-needed** operational signal for human or later authorized workflow. Phase 12 synchronization/reconciliation authority remains unchanged.

# 16. Execution Authorization

Phase 14 authorization and Phase 15 execution authorization are distinct.

Phase 14 authorization means: **this plan may become execution-ready.**

Phase 15 authorization means: **execute this exact plan against this exact target under the current execution conditions.**

Introduce:

```text
AUTHORIZE_NETWORK_CHANGE_EXECUTION
```

Execution authorization MUST be human-originated. Agents cannot authorize execution.

Execution authorization binds a **preliminarily admitted and reviewed** execution whose fingerprint was validated at authorization time. Final mutable-state validation MUST occur immediately before mutation under lease/fencing. Expired execution window after authorization MUST reject execution even if authorization remains recorded.

Neither admission nor review implies authorization. Neither Phase 14 plan authorization nor Phase 15 review implies execution authorization.

# 17. Execution Request DTO

Recommended create request:

```json
{
  "planId": "...",
  "executionTargetId": "snip-simulator"
}
```

The caller MUST NOT provide target cell, parameter, expected value, desired value, rollback value, operation type, fingerprint, vendor command, endpoint, protocol, or credentials. These derive from authoritative Phase 14 state and configured execution target.

# 18. Execution Target Binding

Execution authorization MUST bind the exact target identity/type/environment. Authorization for a simulator must not be reusable for another simulator, sandbox, or production target.

Target substitution invalidates execution authorization.

# 19. Execution Fingerprint

Phase 15 SHALL create a deterministic SHA-256 execution fingerprint over execution-significant canonical fields such as:

```text
planFingerprint
planVersion
executionTargetId
executionTargetType
executionTargetEnvironment
adapterProfileId
capabilityProfileVersion
ordered operation bindings
rollback binding
stable execution-policy fields
executionWindow binding when configured
```

Exclude execution ID, timestamps, actors, audit, attempts, dynamic verification observations, metrics, lease IDs, fencing tokens, and arbitrary non-significant UUIDs.

Canonicalization requirements:

- UTF-8;
- stable field order;
- stable collection order;
- explicit null representation;
- canonical numeric formatting;
- SHA-256.

Persist:

```text
executionFingerprint
authorizedExecutionFingerprint
```

Execution requires equality. Otherwise reject with `EXECUTION_AUTHORIZATION_STALE`.

# 20. Separation of Duties

Required permissions:

```text
VIEW_NETWORK_CHANGE_EXECUTION
REQUEST_NETWORK_CHANGE_EXECUTION
REVIEW_NETWORK_CHANGE_EXECUTION
AUTHORIZE_NETWORK_CHANGE_EXECUTION
CANCEL_NETWORK_CHANGE_EXECUTION
VIEW_NETWORK_CHANGE_EXECUTION_EVIDENCE
REQUEST_NETWORK_CHANGE_ROLLBACK
AUTHORIZE_NETWORK_CHANGE_ROLLBACK
```

Execution authorization MUST NOT be implied by Phase 13 proposal approval, Phase 14 review, Phase 14 authorization, Phase 15 review, view permission, or rollback authorization.

Phase 14 plan authorization and Phase 15 execution authorization MUST be **distinct permissions and distinct human actions**. Architecture MUST support configurable separation of duties; deployments SHOULD enforce different actors for plan authorization and execution authorization unless an explicit policy exception is configured.

`REVIEW_NETWORK_CHANGE_EXECUTION` is mandatory before execution authorization. Review does not authorize mutation.

# 21. Idempotency

Phase 15 distinguishes three idempotency layers:

1. **HTTP/API request idempotency** — repeated client requests return/reference the same durable execution evidence without duplicate orchestration side effects;
2. **Execution-attempt idempotency** — at most one forward mutation attempt per authorized execution unless a separately authorized retry workflow exists;
3. **Target mutation idempotency** — depends on target/adapter capability; SNIP MUST NOT assume universal target-side deduplication.

Execution APIs MUST be idempotent at the orchestration layer. Repeated submission must not perform duplicate mutation.

SNIP SHALL maintain durable execution identity and attempt state. Terminal duplicate requests return/reference existing evidence instead of re-performing mutation.

Returning an existing execution record does **not** imply target-side exactly-once mutation.

Phase 15 MUST NOT claim universal exactly-once external mutation semantics.

Correct statement:

> SNIP provides durable idempotent orchestration and execution evidence. It does not claim universal exactly-once network mutation across arbitrary target systems.

# 22. Ambiguous Outcomes

First-class state:

```text
EXECUTION_OUTCOME_UNKNOWN
```

Example:

```text
request sent
→ target applies value
→ connection drops before response
```

SNIP MUST NOT blindly retry.

Required flow:

```text
EXECUTION_OUTCOME_UNKNOWN
        ↓
VERIFYING
        ↓
observe target
```

If desired value is observed: `VERIFIED`.

If pre-change value is observed: Phase 15 MUST NOT perform an automatic forward retry. Transition to `MANUAL_INTERVENTION_REQUIRED` or require a **new execution request** with fresh review, authorization, lease acquisition, and final preflight. No blind retry.

If a third value is observed: `MANUAL_INTERVENTION_REQUIRED`.

If observation is unknown/unavailable/stale: remain in verification/recovery-safe state; do not infer success.

# 23. Write-Side Retry Classification

Write-side failures SHALL distinguish:

```text
PRE_EXECUTION_TRANSIENT_FAILURE
EXECUTION_REJECTED
EXECUTION_OUTCOME_UNKNOWN
POST_EXECUTION_VERIFICATION_FAILURE
```

Automatic retry after an outcome-unknown write is prohibited until verification determines target state.

Phase 8 read-side retry semantics MUST NOT be blindly reused for writes.

### Attempt model

Each execution owns numbered durable attempts (`network_change_execution_attempt`). Initial Phase 15 rule:

```text
maximum forward mutation attempts per execution = 1
```

unless a separately authorized retry/recovery workflow explicitly creates a new authorized execution or authorized retry attempt. Multiple attempts MUST NOT devolve into an uncontrolled retry loop.

# 24. Execution Lease and Fencing

Phase 15 SHALL use distributed execution ownership.

Initial protected scope:

```text
executionTargetId + cellId + parameter
```

Example:

```text
snip-simulator:CELL-001:txPower
```

Before mutation:

```text
acquire execution lease
obtain fencing token
final authoritative revalidation under lease/fencing
execute
verify
release
```

A stale holder MUST NOT start/continue an operation, mark application/verification success, authorize recovery, execute rollback, or overwrite newer evidence.

Phase 8 lease/fencing infrastructure MAY be reused only with an **execution-specific resource namespace** distinct from import/synchronization lease resources. Reuse MUST NOT allow an import lease holder or execution lease holder to interfere with the other's authority.

Fencing primarily protects SNIP authority, durable state, and adapter pre-send behavior. Fencing cannot reliably recall a write already accepted by an external target; architecture MUST NOT overclaim external-system fencing.

# 25. Concurrency

Initial invariants:

> At most one **active** execution may exist per plan.

> At most one **active** execution may own the same protected scope (`executionTargetId + cellId + parameter`).

The same logical cell on different execution targets/environments MAY be concurrent because target identity is part of the protected scope.

Conflicting execution result:

```text
EXECUTION_CONFLICT
```

Concurrency correctness must be proven behaviorally, not merely by database annotations.

# 26. Recovery

Verification failure does not automatically trigger rollback.

Required transition:

```text
VERIFICATION_FAILED
        ↓
RECOVERY_REQUIRED
```

A deterministic recovery evaluation may recommend `ROLLBACK_ELIGIBLE` or `MANUAL_INTERVENTION_REQUIRED` without executing either. No LLM authority. No automatic rollback.

`MANUAL_INTERVENTION_REQUIRED` is a **terminal safe-stop operational state**. It indicates SNIP cannot safely proceed automatically and human operators must investigate using persisted evidence. It is NOT permission for SNIP to perform an out-of-band vendor write, hidden command path, or production mutation. SNIP MAY explain evidence; it MUST NOT expose alternate mutation APIs.

# 27. Rollback Source of Truth

Phase 15 MUST consume the rollback plan persisted by Phase 14. It MUST NOT invent a replacement rollback value.

Example:

```text
forward:  expected 46 → desired 44
rollback: expected 44 → desired 46
```

# 28. Rollback Expected-State Guard

Before rollback:

```text
READ ACTUAL

IF actual == rollback.expected
    rollback
ELSE
    STOP
    MANUAL_INTERVENTION_REQUIRED
```

No blind rollback is permitted.

# 29. Rollback Authorization

Rollback requires distinct human authorization:

```text
AUTHORIZE_NETWORK_CHANGE_ROLLBACK
```

Phase 14 authorization and Phase 15 forward authorization do not authorize rollback.

Automatic rollback remains **NOT AUTHORIZED**.

Rollback authorization MUST bind deterministically via a rollback authorization fingerprint over execution-significant fields including:

```text
executionId
original planFingerprint / planVersion
rollback operation binding
executionTargetId / type / environment
adapterProfileId
rollback expected state
rollback desired state
stable rollback policy fields
```

Target or rollback binding substitution after rollback authorization invalidates authorization.

# 30. Rollback Verification

Mandatory invariant:

```text
ROLLBACK_APPLIED != ROLLED_BACK
```

Only independent readback of restored state permits `ROLLED_BACK`.

Mismatch leads to `ROLLBACK_FAILED` or `MANUAL_INTERVENTION_REQUIRED` according to deterministic policy.

Rollback ambiguous outcomes (`ROLLBACK_OUTCOME_UNKNOWN`) MUST follow the same safety model as forward ambiguous outcomes: verify before concluding, no blind retry, unknown/stale/third-state evidence leads toward manual intervention.

# 31. Cancellation

Cancellation is safe only before a possibly-mutating operation has started.

Use `CANCELLED_BEFORE_MUTATION` when cancellation occurs before mutation may have reached the target.

Once mutation may have reached the target, cancellation must not blindly set `CANCELLED`; ambiguous outcome must first be resolved through verification.

# 32. Execution Windows

If Phase 14 supplies an execution window, Phase 15 SHALL enforce it at:

- execution request/admission;
- execution authorization;
- **final preflight immediately before mutation** (mandatory).

```text
now < windowStart → deny
now > windowEnd   → deny
```

If the window expires after authorization but before mutation, execution MUST be rejected. Phase 15 SHALL NOT automatically execute when a window opens. No execution scheduler is introduced.

# 33. Stable Reason Codes

Minimum stable reason codes:

```text
EXECUTION_PLAN_NOT_READY
EXECUTION_PLAN_EXPIRED
EXECUTION_PLAN_INVALIDATED
EXECUTION_PLAN_CANCELLED
EXECUTION_PLAN_SUPERSEDED
EXECUTION_AUTHORIZATION_MISSING
EXECUTION_AUTHORIZATION_STALE
EXECUTION_TARGET_NOT_ALLOWED
EXECUTION_TARGET_NOT_FOUND
EXECUTION_TARGET_CAPABILITY_MISSING
EXECUTION_TARGET_ENVIRONMENT_PROHIBITED
EXECUTION_CURRENT_VALUE_MISMATCH
EXECUTION_KNOWLEDGE_LOW
EXECUTION_KNOWLEDGE_UNKNOWN
EXECUTION_SYNCHRONIZATION_STALE
EXECUTION_RELEVANT_DRIFT_PRESENT
EXECUTION_LEASE_UNAVAILABLE
EXECUTION_FENCING_TOKEN_STALE
EXECUTION_CONFLICT
EXECUTION_OPERATION_REJECTED
EXECUTION_OPERATION_TIMEOUT
EXECUTION_OUTCOME_UNKNOWN
EXECUTION_VERIFICATION_MISMATCH
EXECUTION_VERIFICATION_TIMEOUT
EXECUTION_VERIFICATION_UNKNOWN
ROLLBACK_AUTHORIZATION_MISSING
ROLLBACK_AUTHORIZATION_STALE
ROLLBACK_CURRENT_VALUE_MISMATCH
ROLLBACK_OPERATION_FAILED
ROLLBACK_OUTCOME_UNKNOWN
ROLLBACK_VERIFICATION_FAILED
MANUAL_INTERVENTION_REQUIRED
```

# 34. Audit

Execution audit is append-only. Required events include:

```text
EXECUTION_REQUESTED
EXECUTION_ADMISSION_STARTED
EXECUTION_ADMISSION_REJECTED
EXECUTION_ADMITTED
EXECUTION_REVIEWED
EXECUTION_AUTHORIZED
EXECUTION_AUTHORIZATION_REJECTED
EXECUTION_STARTED
OPERATION_STARTED
OPERATION_APPLIED
OPERATION_REJECTED
OPERATION_OUTCOME_UNKNOWN
OPERATION_FAILED
VERIFICATION_STARTED
VERIFICATION_SUCCEEDED
VERIFICATION_FAILED
RECOVERY_REQUIRED
ROLLBACK_REQUESTED
ROLLBACK_REVIEWED
ROLLBACK_AUTHORIZED
ROLLBACK_STARTED
ROLLBACK_APPLIED
ROLLBACK_OUTCOME_UNKNOWN
ROLLBACK_VERIFIED
ROLLBACK_FAILED
MANUAL_INTERVENTION_REQUIRED
EXECUTION_CANCELLED
EXECUTION_COMPLETED
```

Audit MUST NOT persist credentials, tokens, private keys, secrets, or raw vendor responses by default.

# 35. Durable Failure Evidence

Critical failure evidence MUST survive outer request rollback, including:

```text
EXECUTION_OUTCOME_UNKNOWN
VERIFICATION_FAILED
RECOVERY_REQUIRED
MANUAL_INTERVENTION_REQUIRED
ROLLBACK_FAILED
ROLLBACK_OUTCOME_UNKNOWN
```

The implementation specification SHALL define transaction boundaries sufficient to prevent failed HTTP requests from erasing execution evidence. Dedicated independently transactional persistence services MUST be used for critical failure evidence and terminal safety states.

# 36. Persistence

Migration:

```text
V16__phase15_governed_change_execution.sql
```

Required tables:

```text
network_change_execution
network_change_execution_operation
network_change_execution_attempt
network_change_execution_authorization
network_change_execution_verification
network_change_execution_recovery
network_change_execution_rollback
network_change_execution_audit_event
```

Optional normalized tables may be added if justified.

`network_change_execution_recovery` records deterministic recovery evaluation/evidence. `network_change_execution_rollback` records rollback operation/evidence. Both are required; neither replaces the other.

V1–V15 remain immutable.

# 37. API Namespace

Canonical namespace:

```text
/api/v1/change-execution/executions
```

Recommended endpoints:

```text
POST /executions
GET  /executions
GET  /executions/{executionId}
GET  /executions/{executionId}/evidence
POST /executions/{executionId}/review
POST /executions/{executionId}/authorize
POST /executions/{executionId}/execute
POST /executions/{executionId}/verify
POST /executions/{executionId}/cancel
POST /executions/{executionId}/rollback/request
POST /executions/{executionId}/rollback/review
POST /executions/{executionId}/rollback/authorize
POST /executions/{executionId}/rollback/execute
```

No generic vendor-command endpoint is allowed.

`POST /executions/{executionId}/execute` orchestrates lease acquisition, final preflight, mutation, and SHOULD transition into verification automatically. `POST /executions/{executionId}/verify` remains available for outcome-unknown/manual recovery paths and MUST NOT duplicate mutation. Repeated execute on terminal verified execution MUST return existing evidence without duplicate mutation. Rollback execute MUST require completed rollback authorization and rollback fingerprint validation.

# 38. API Safety

Create request contains only authoritative references. Server derives target, parameter, expected value, desired value, rollback value, operations, plan fingerprint, and execution fingerprint.

Unknown JSON fields SHOULD be rejected. Mass-assignment of execution-significant fields is prohibited.

# 39. Agent Boundary

Agents may read status/evidence and explain deterministic decisions. Agents may NOT request execution, authorize execution, execute, authorize rollback, execute rollback, alter target, alter expected/desired/rollback value, or bypass policy.

No Agent may directly invoke an execution adapter.

# 40. MCP Boundary

Phase 15 MCP remains read-only.

Potential safe capabilities:

```text
get_execution
get_execution_status
get_execution_evidence
```

Prohibited:

```text
execute_change
apply_parameter
set_tx_power
rollback_change
vendor_command
```

MCP cannot become an alternate execution path.

Read-only Phase 15 MCP capabilities MAY be architecturally permitted but implementation specification MAY defer them. Mutation MCP tools remain prohibited.

# 41. Simulator as First Executable Target

Phase 15 `SimulatorExecutionAdapter` is a **distinct execution-plane target** and MUST NOT reuse Phase 11 read-only `SimulatorEnmTransport`, `EricssonEnmConnector`, or import/sync pathways. It MUST NOT treat the Go Kafka telemetry simulator (`simulator/cmd/simulator`) as the mutation target unless a future explicitly bounded extension provides parameter read/write semantics; Phase 15 architecture assumes an in-process or explicitly registered simulator execution target store whose state is separate from SNIP canonical tables.

Required successful flow:

```text
source/canonical context: CELL-001 txPower = 46
Phase 13: recommend 44
Phase 14: plan 46 → 44 → READY_FOR_EXECUTION
Phase 15: create simulator execution
→ preliminary admission
→ mandatory review
→ mandatory authorize
→ acquire lease + final preflight
→ execute simulator mutation
→ APPLIED
→ independent readback = 44
→ VERIFIED
```

Execution mutates simulator **execution target** state, not canonical state directly. Canonical DB remains unchanged until Phase 12 synchronization/reconciliation observes the target.

# 42. Simulator Failure Injection

The simulator SHOULD support deterministic test-only modes:

```text
SUCCESS
REJECT_BEFORE_APPLY
TIMEOUT_BEFORE_APPLY
TIMEOUT_AFTER_APPLY
APPLY_WRONG_VALUE
READBACK_TIMEOUT
READBACK_STALE
ROLLBACK_FAILURE
ROLLBACK_TIMEOUT_AFTER_APPLY
```

Failure injection must be disabled outside controlled simulator/test profiles.

# 43. Determinism

For equal authoritative plan, authorization, target, policy, current state, knowledge state, drift state and lease ownership, the admission decision MUST be deterministic.

LLMs SHALL NOT decide whether to execute, what value to write, whether expected-state matches, whether verification succeeded, whether rollback is permitted, or whether rollback succeeded. LLMs may explain deterministic results only.

# 44. Configuration

Recommended configuration contract:

```yaml
snip:
  change-execution:
    enabled: false
    maximum-operation-count: 1
    permitted-target-types:
      - SIMULATOR
    require-execution-authorization: true
    require-current-value-match: true
    require-verification: true
    require-rollback-authorization: true
    automatic-rollback-enabled: false
```

`CONTROLLED_SANDBOX` MUST be added to `permitted-target-types` only by explicit operator configuration together with explicit sandbox registry entries.

Real-vendor prohibition MUST be **structural**, not dependent solely on a boolean. No production vendor write adapter may exist in the Phase 15 binary. A `real-vendor-execution-enabled` flag MUST NOT appear as an operative runtime switch in Phase 15 because it would misleadingly imply production mutation could be enabled by configuration alone. Phase 16 or later is required for real-network writes.

# 45. Package Boundary

Canonical package:

```text
com.simba.snip.npo.changeexecution
```

Suggested layout:

```text
changeexecution/
├── api/
├── config/
├── domain/
├── entity/
├── repository/
├── service/
├── adapter/
│   ├── spi/
│   └── simulator/
├── security/
├── audit/
├── metrics/
└── exception/
```

# 46. Core Services

Recommended services:

```text
NetworkChangeExecutionService
ExecutionAdmissionService
ExecutionAuthorizationService
ExecutionFingerprintService
ExecutionTargetRegistry
ExecutionLeaseService
ChangeOperationExecutionService
ExecutionVerificationService
ExecutionRecoveryService
RollbackAuthorizationService
RollbackExecutionService
ExecutionValidityService
ExecutionAuditService
ExecutionMetrics
```

SPI:

```text
NetworkChangeExecutionAdapter
```

# 47. Observability

Low-cardinality metrics SHOULD include:

```text
snip_change_execution_requested_total
snip_change_execution_admitted_total
snip_change_execution_rejected_total
snip_change_execution_started_total
snip_change_execution_verified_total
snip_change_execution_failed_total
snip_change_execution_outcome_unknown_total
snip_change_execution_rollback_requested_total
snip_change_execution_rollback_verified_total
snip_change_execution_manual_intervention_total
```

Do not use `cellId`, `planId`, `executionId`, actor, fingerprint, or vendor endpoint as metric labels.

Bounded labels such as `targetType` and `environment` MAY be used when drawn from a fixed enumeration.

# 48. Threat Model

Phase 15 MUST defend against:

1. plan substitution;
2. target substitution;
3. environment substitution;
4. stale plan authorization replay;
5. stale execution authorization replay;
6. fingerprint tampering;
7. expected-state race;
8. duplicate execute requests;
9. duplicate rollback requests;
10. concurrent executors;
11. stale fencing holder;
12. operation replay;
13. ambiguous write outcome;
14. desired-value injection;
15. rollback-value injection;
16. execution outside window;
17. permission escalation;
18. agent-triggered execution;
19. MCP-triggered execution;
20. simulator-to-production substitution;
21. vendor-command injection;
22. endpoint injection;
23. credential injection;
24. fake verification;
25. audit tampering;
26. execution-status forgery;
27. rollback authorization replay;
28. recovery bypass;
29. automatic rollback escalation;
30. direct canonical mutation;
31. malicious sandbox misclassification;
32. target capability downgrade or profile change after authorization;
33. stale target configuration substitution;
34. verification replay using stale cached readback;
35. forged simulator mutation result treated as verification;
36. duplicate authorization replay across environments;
37. rollback target substitution;
38. cross-environment execution routing;
39. denial-of-service via execution lease starvation.

No event-driven automatic execution path (proposal approved, plan ready, authorization granted, window opened, synchronization completed, agent output) may trigger mutation.

# 49. Test Evidence Architecture

Every mandatory requirement SHALL be classified before implementation as:

```text
STRUCTURAL
BEHAVIORAL
INTEGRATION
```

Every matrix item SHALL have an explicit evidence target. Passing source-string checks are not sufficient proof of runtime behavior.

The detailed Phase 15 implementation specification SHOULD target approximately **240 mandatory matrix items**, with exact count fixed by the specification.

# 50. Mandatory Test Domains

The matrix SHALL cover at least:

1. architecture isolation;
2. Phase 14 plan admission;
3. target binding;
4. target environment prohibition;
5. target capabilities;
6. execution fingerprint;
7. execution authorization;
8. separation of duties;
9. expected-state guard;
10. operation derivation;
11. single-operation txPower scope;
12. simulator execution;
13. independent readback;
14. verification;
15. ambiguous outcomes;
16. retry safety;
17. idempotency;
18. execution leases;
19. fencing;
20. concurrency;
21. execution windows;
22. cancellation;
23. durable failure evidence;
24. rollback source-of-truth;
25. rollback authorization;
26. rollback expected-state guard;
27. rollback execution;
28. rollback verification;
29. manual intervention;
30. audit;
31. metrics;
32. API authorization;
33. DTO attack surface;
34. agent isolation;
35. MCP isolation;
36. vendor-write isolation;
37. credential isolation;
38. canonical-state isolation;
39. Phase 12 reconciliation boundary;
40. simulator failure injection;
41. migration;
42. optimistic locking;
43. shared Testcontainer isolation;
44. Phase 1–14 regression;
45. Go simulator regression/build.

# 51. Critical Integration Scenarios

## A — Successful execution

```text
READY plan → request → admission → review → authorize
→ lease + expected-state recheck → simulator mutation → APPLIED
→ independent readback → VERIFIED
```

## B — Current state changed before execution

```text
READY plan → current state changes → execute
→ expected-state mismatch → zero mutation
```

## C — Timeout after apply

```text
execute → target applies → response lost
→ EXECUTION_OUTCOME_UNKNOWN → no blind retry
→ readback desired → VERIFIED
```

## D — Wrong resulting state

```text
execute → target ends at wrong value
→ VERIFICATION_FAILED → RECOVERY_REQUIRED
```

## E — Rollback without authorization

```text
RECOVERY_REQUIRED → rollback execute request
→ rejected → zero rollback mutation
```

## F — Authorized rollback success

```text
RECOVERY_REQUIRED → rollback request → review → authorize
→ rollback expected-state matches → rollback execute
→ readback original state → ROLLED_BACK
```

## G — Rollback expected-state mismatch

```text
actual differs from rollback expected
→ no rollback mutation → MANUAL_INTERVENTION_REQUIRED
```

## H — Concurrent execution

```text
two executions for CELL-001:txPower
→ one owns execution authority
→ second rejected
```

## I — Stale authorization

```text
execution authorized → execution-significant binding stale
→ execute → EXECUTION_AUTHORIZATION_STALE → zero mutation
```

## J — Duplicate execute

```text
execution verified → duplicate execute request
→ existing result returned → no duplicate mutation
```

## K — Authorization target substitution

```text
execution authorized for target A
→ target registry/config changes to target B before execute
→ final preflight/authorization fingerprint mismatch
→ zero mutation
```

## L — Execution window expiration after authorization

```text
execution authorized within window
→ window ends before execute
→ final preflight rejects → zero mutation
```

## M — Stale fencing holder

```text
execution lease lost/expired
→ stale holder attempts execute/verify/rollback
→ rejected → zero mutation / no state overwrite
```

## N — Rollback outcome unknown

```text
rollback execute → target may have changed → response lost
→ ROLLBACK_OUTCOME_UNKNOWN → verify readback
→ no blind retry
```

## O — Stale readback cannot verify

```text
post-mutation verify returns pre-mutation cached value
→ STALE_OBSERVATION → not VERIFIED
```

## P — Canonical DB unchanged after simulator execution

```text
simulator execution VERIFIED
→ radio_configuration unchanged
→ canonical changes only after Phase 12 synchronization
```

## Q — Agent/MCP cannot execute

```text
agent or MCP attempts execute/authorize/rollback
→ rejected → zero mutation
```

# 52. Shared PostgreSQL Testcontainer Isolation

Every Phase 15 test that mutates shared Phase 1–15 state MUST restore its fixtures, including as applicable:

```text
radio_configuration
Phase 6 twin state
Phase 12 synchronization/knowledge/drift
Phase 13 proposal/candidate state
Phase 14 plan/review/readiness state
Phase 15 execution state
simulator target state
```

No test-class ordering dependence. No Surefire ordering workaround.

# 53. Security Boundary Invariants at Closure

```text
REAL VENDOR WRITE CAPABILITY: NOT AUTHORIZED
PRODUCTION ERICSSON WRITE ADAPTER: NOT PRESENT
PRODUCTION NOKIA WRITE ADAPTER: NOT PRESENT
PHASE 11 ENM TRANSPORT: READ-ONLY / UNCHANGED
VENDOR WRITE CREDENTIAL RESOLUTION: NONE
AGENT EXECUTION: NOT AUTHORIZED
MCP EXECUTION: NOT AUTHORIZED
AUTOMATIC EXECUTION: NOT AUTHORIZED
AUTOMATIC ROLLBACK: NOT AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT AUTHORIZED
```

# 54. Explicitly Out of Scope

Phase 15 does NOT include production Ericsson ENM writes, production Nokia NetAct writes, production mutation credentials, vendor CLI/REST/SSH mutation, automatic scheduled execution, agent-triggered execution, MCP-triggered execution, automatic rollback, closed-loop optimization, multi-cell execution, multi-parameter execution, bulk execution, production canary rollout, production maintenance orchestration, or production break-glass mutation.

# 55. Phase 16 Boundary

Potential future title:

**SNIP Phase 16 — Production Vendor Write Integration, Change Control & Controlled Real-Network Execution**

Only a later accepted architecture may authorize real vendor write interfaces, write-capable identities, production maintenance windows, blast-radius control, canaries, production rollback semantics, break-glass controls, production kill switches, and external change-management integration.

# 56. Architecture Acceptance Gates

The following gates are mandatory before Phase 15 architecture is accepted:

1. Parent Phase 14 baseline pinned exactly.
2. Execution plane distinct from planning plane.
3. `NetworkChangeExecution` distinct aggregate.
4. Phase 14 plan remains source of truth.
5. Only `READY_FOR_EXECUTION` plans enter admission.
6. Admission revalidates current state.
7. Unknown mandatory evidence fails closed.
8. Expected-state guard mandatory.
9. txPower only.
10. One operation maximum.
11. Execution target explicitly bound.
12. Simulator permitted.
13. Controlled sandbox explicitly bounded.
14. Production target prohibited.
15. No real vendor write adapter.
16. Phase 11 `EnmTransport` read-only and unchanged.
17. No execution credential resolution.
18. Phase 14 authorization distinct from Phase 15 authorization.
19. Human execution authorization mandatory.
20. Agent execution authorization prohibited.
21. MCP execution prohibited.
22. Deterministic SHA-256 execution fingerprint.
23. Authorization fingerprint-bound.
24. Target substitution invalidates authorization.
25. Execution idempotent.
26. No exactly-once external mutation claim.
27. Ambiguous outcome first-class.
28. No blind retry after ambiguous write.
29. Execution lease/fencing exists.
30. Stale holder cannot mutate execution state.
31. One active execution per protected scope.
32. One active execution per plan.
33. `APPLIED != VERIFIED`.
34. Independent verification mandatory.
35. Verification observes target, not canonical projection.
36. Canonical state not mutation target.
37. Phase 12 reconciliation boundary preserved.
38. Verification mismatch drives recovery.
39. Automatic rollback prohibited.
40. Rollback source of truth inherited from Phase 14.
41. Rollback authorization separate and fingerprint-bound.
42. Rollback expected-state guard mandatory.
43. `ROLLBACK_APPLIED != ROLLED_BACK`.
44. Rollback readback verification mandatory.
45. Unsafe rollback mismatch requires manual intervention.
46. Execution windows enforced at final preflight.
47. Cancellation cannot falsely imply target cancellation.
48. Durable failure evidence survives outer failure.
49. Stable reason codes exist.
50. Append-only audit exists.
51. No secrets/raw credentials in persistence.
52. API cannot accept caller-controlled mutation values.
53. No generic vendor command endpoint.
54. Simulator failure injection test-only/controlled.
55. LLMs have no execution decision authority.
56. Low-cardinality metrics only.
57. V16 is only new migration.
58. V1–V15 unchanged.
59. Shared Testcontainer isolation formalized.
60. Mandatory matrix evidence types explicit.
61. Default CI Azure/vendor independent.
62. Real vendor write capability remains NOT AUTHORIZED.
63. Closed-loop optimization remains NOT AUTHORIZED.
64. Phase 16 remains NOT STARTED.
65. Mandatory execution review before authorization.
66. Final preflight under lease before mutation.
67. No automatic forward retry after outcome-unknown/pre-change observation.
68. Maximum one forward mutation attempt unless separately authorized.
69. CONTROLLED_SANDBOX explicitly bounded and opt-in.
70. Simulator execution adapter distinct from Phase 11 read-only transport.
71. No event-driven automatic execution.
72. Execution/post-rollback observation independence preserved.
73. Phase 15 disabled by default until explicitly enabled.

# 57. Implementation Lifecycle

After architecture acceptance:

```text
architecture review
→ architecture freeze
→ architecture Git baseline
→ exact-SHA architecture CI
→ implementation specification
→ Cursor implementation
→ completion report
→ architectural conformance review
→ corrections if necessary
→ evidence closure
→ implementation candidate commit
→ exact-SHA CI
→ immutable Phase 15 implementation baseline
```

No implementation baseline is established merely because tests pass locally.

# 58. Final Phase 15 Architecture Status at Issuance

```text
PHASE 15 ARCHITECTURE STATUS:
ACCEPTED AND FROZEN

PHASE 15 IMPLEMENTATION STATUS:
NOT STARTED

PHASE 15 IMPLEMENTATION SPECIFICATION:
NOT YET ISSUED

PHASE 15 GIT BASELINE:
NOT ESTABLISHED

GOVERNED EXECUTION PLANE:
DESIGNED

SIMULATOR EXECUTION:
ARCHITECTURALLY AUTHORIZED AFTER IMPLEMENTATION ACCEPTANCE

CONTROLLED SANDBOX EXECUTION:
ARCHITECTURALLY SUPPORTED ONLY WHEN EXPLICITLY CONFIGURED

EXPECTED-STATE GUARD:
MANDATORY

EXECUTION AUTHORIZATION:
MANDATORY

EXECUTION TARGET BINDING:
MANDATORY

POST-CHANGE VERIFICATION:
MANDATORY

AMBIGUOUS OUTCOME HANDLING:
MANDATORY

ROLLBACK AUTHORIZATION:
MANDATORY

AUTOMATIC ROLLBACK:
NOT AUTHORIZED

AGENT EXECUTION:
NOT AUTHORIZED

MCP EXECUTION:
NOT AUTHORIZED

REAL VENDOR WRITE CAPABILITY:
NOT AUTHORIZED

CLOSED-LOOP OPTIMIZATION:
NOT AUTHORIZED

PRODUCTION ENM TRANSPORT:
NOT CONFIGURED

REAL VENDOR CONTINUOUS SYNCHRONIZATION:
NOT YET VERIFIED

PHASE 16:
NOT STARTED
```

---

**PHASE 15 ARCHITECTURE: ACCEPTED AND FROZEN**
