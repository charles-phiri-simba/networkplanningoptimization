# SNIP Phase 14 --- Governed Change Planning, Execution Readiness & Safety Control

## Full Implementation Specification

**Specification status:** AUTHORIZED FOR IMPLEMENTATION\
**Architecture status:** ACCEPTED AND FROZEN\
**Architecture baseline:** `6cc29ba8b70b1fbae65fdb70a958cb6c4fb32423`\
**Architecture SHA-256:**
`5665943254581499213A78B67C1A570462F6DCD1DE244C3F793FDE71F54221FA`\
**Phase 13 immutable implementation baseline:**
`5e9400005626fb93d5e61f96be680bea5540df31`\
**Architecture CI:** SUCCESS --- exact SHA verified, workflow `ci`, run
#17 / ID `33333260219`\
**Phase 14 implementation baseline:** NOT YET ESTABLISHED\
**Phase 15:** NOT STARTED

------------------------------------------------------------------------

## 1. Purpose and Defining Principle

This specification authorizes implementation of SNIP Phase 14.

> **Phase 14 transforms an approved network recommendation into a
> deterministic, vendor-neutral, safety-validated and human-authorized
> execution plan. A plan may become `READY_FOR_EXECUTION`, but Phase 14
> provides no mechanism capable of executing that plan against a real
> network.**

Phase 13 answers **what change should SNIP recommend?** Phase 14 answers
**how would that exact approved change be performed safely, and is it
currently ready to be performed?** Phase 15+ may later answer whether
SNIP may actually perform it.

The implementation SHALL stop at `READY_FOR_EXECUTION`.

------------------------------------------------------------------------

## 2. Authorized Scope

Authorized:

-   `com.simba.snip.npo.changeplanning.*`;
-   V15 forward-only Flyway migration;
-   plan entities/repositories/services;
-   approved-proposal eligibility;
-   vendor-neutral operations;
-   expected-state guards;
-   dependency ordering;
-   rollback planning;
-   preconditions;
-   deterministic safety and impact;
-   deterministic SHA-256 plan fingerprint;
-   review and human authorization;
-   deterministic readiness;
-   validity/invalidation/expiration/supersession/cancellation;
-   safe Phase 14 APIs;
-   audit and low-cardinality metrics;
-   configuration;
-   tests;
-   completion report.

Not authorized:

-   Ericsson/Nokia writes;
-   ENM/NetAct mutation;
-   vendor CLI/SSH/AMOS;
-   vendor command generation or persistence;
-   write credential resolution;
-   write-capable connector methods;
-   `EnmTransport` mutation;
-   execution scheduler;
-   automatic execution or rollback;
-   closed-loop optimization;
-   MCP mutation tools;
-   agent authorization;
-   Phase 4 action execution;
-   automatic plan-to-`ProposedAction` conversion;
-   Phase 15.

------------------------------------------------------------------------

## 3. Required Starting State

Cursor SHALL start from exact HEAD:

`6cc29ba8b70b1fbae65fdb70a958cb6c4fb32423`

Run:

``` bash
git rev-parse HEAD
git status --short
git log -3 --oneline --decorate
```

Required: exact HEAD and clean tree. If not, STOP. Do not amend, rebase,
squash, or rewrite the architecture baseline.

------------------------------------------------------------------------

## 4. Frozen Architecture Clarifications

Implementation SHALL preserve:

1.  Phase 13 has no persisted `selectedCandidateId`; resolve the
    authoritative candidate from `proposal.proposedValue` plus exactly
    one `rankOrder = 1` candidate.
2.  `PlanStatus.BLOCKED` is distinct from
    `ExecutionReadinessResult.NOT_READY`.
3.  Authorization produces `AUTHORIZED`; only a later deterministic
    readiness evaluation may produce `READY_FOR_EXECUTION`.
4.  Cancellation is allowed from the accepted active pre-execution
    states, including `AUTHORIZED` and `READY_FOR_EXECUTION`.
5.  API namespace is `/api/v1/change-planning/plans`.
6.  Fingerprints use deterministic canonical serialization of
    execution-significant intent and exclude volatile runtime metadata.

------------------------------------------------------------------------

## 5. Existing Authorities to Reuse

### Phase 13

Reuse actual contracts: `NetworkChangeProposalEntity`,
`NetworkChangeCandidateEntity`, `ProposalStatus`, proposal
validity/governance/query/repository paths, `currentValue`,
`proposedValue`, `sourceSnapshotId`, `sourceSynchronizationExecutionId`,
and candidate `simulationRunId`.

Do not add `selectedCandidateId` to Phase 13. Do not duplicate
recommendation, scoring, ranking, candidate-generation or knowledge-gate
logic.

### Phase 12

Reuse existing authority for knowledge, freshness, synchronization,
drift and canonical state, including the real repository/service types
such as `NetworkKnowledgeConfidence`,
`NetworkKnowledgeConfidenceEvaluator`, `SynchronizationFreshness`,
`SynchronizationFreshnessEvaluator`,
`SynchronizationSourceStateService`, `NetworkDriftService`, and
`RadioConfigurationRepository`.

### Phase 6

Consume existing Phase 13 simulation evidence. Do not silently
re-simulate during plan creation, review, authorization or readiness. No
MCP traversal.

### Phase 4

`NetworkChangePlan != ProposedAction`. No call to
`ActionExecutionService`, `McpCapabilityGateway`, action execute
endpoints or any vendor-execution path.

------------------------------------------------------------------------

## 6. Package

Create:

``` text
com.simba.snip.npo.changeplanning
  api
  authorization
  audit
  config
  metrics
  model
  persist
  policy
  repository
  service
```

Do not place Phase 14 implementation in Phase 13 packages.

------------------------------------------------------------------------

## 7. Core Aggregate and Bounded Scope

Implement `NetworkChangePlanEntity` as the aggregate root.

Initial scope:

-   one APPROVED Phase 13 proposal;
-   zero or one active plan for the same effective proposal/intent;
-   one cell;
-   one parameter;
-   `txPower` only;
-   one forward `SET_PARAMETER` operation;
-   one rollback operation when rollback is required.

The model may be future-shaped for ordered operations, but Phase 14
behavior SHALL enforce `maximum-operation-count = 1`.

------------------------------------------------------------------------

## 8. Status Model

Create:

``` text
DRAFT
VALIDATING
PLANNED
SAFETY_EVALUATING
READY_FOR_REVIEW
AUTHORIZED
READY_FOR_EXECUTION
INVALID
BLOCKED
INVALIDATED
EXPIRED
SUPERSEDED
CANCELLED
```

Forbidden statuses include `EXECUTING`, `EXECUTED`, `APPLYING`,
`APPLIED`, `ROLLING_BACK`, `ROLLED_BACK`.

Semantics:

-   `INVALID`: initial construction/validation failed.
-   `BLOCKED`: valid planning attempt exists but a hard planning/safety
    gate prevents progression.
-   `INVALIDATED`: a previously valid active plan became unsafe/stale.
-   `AUTHORIZED`: human authorization recorded for the exact
    fingerprint.
-   `READY_FOR_EXECUTION`: authorization plus a later deterministic
    readiness pass; inert in Phase 14.

------------------------------------------------------------------------

## 9. Lifecycle

``` text
CREATE
  -> DRAFT
  -> VALIDATING
       -> INVALID | BLOCKED
  -> PLANNED
  -> SAFETY_EVALUATING
       -> BLOCKED
  -> READY_FOR_REVIEW
  -> AUTHORIZED
  -> deterministic readiness
       -> assessment NOT_READY | STALE | UNKNOWN
       -> INVALIDATED | EXPIRED | SUPERSEDED where applicable
       -> READY_FOR_EXECUTION
  -> STOP
```

Cancellation is allowed from:

`DRAFT`, `VALIDATING`, `PLANNED`, `SAFETY_EVALUATING`,
`READY_FOR_REVIEW`, `AUTHORIZED`, `READY_FOR_EXECUTION`

and transitions to terminal `CANCELLED`.

------------------------------------------------------------------------

## 10. Plan Creation Eligibility

`ChangePlanEligibilityService` SHALL require:

1.  proposal exists;
2.  status `APPROVED`;
3.  not expired/invalidated/superseded;
4.  target exists;
5.  parameter is `txPower`;
6.  proposal baseline/current value exists;
7.  authoritative canonical current value exists;
8.  canonical current value equals proposal baseline;
9.  network knowledge acceptable;
10. synchronization trustworthy;
11. relevant unresolved drift absent;
12. exactly one authoritative rank-1 candidate;
13. rank-1 candidate value equals `proposal.proposedValue`;
14. required simulation evidence exists;
15. Twin evidence acceptable under existing semantics;
16. no contradictory active plan.

Unknown mandatory evidence fails closed.

------------------------------------------------------------------------

## 11. Candidate Binding

Resolve the selected candidate from candidate rows belonging to the
proposal where `rankOrder = 1`. Verify its value equals
`NetworkChangeProposalEntity.proposedValue`.

Stable failures:

``` text
PLAN_CANDIDATE_NOT_FOUND
PLAN_CANDIDATE_AMBIGUOUS
PLAN_CANDIDATE_VALUE_MISMATCH
```

Phase 14 MAY persist the resolved candidate reference as provenance. It
is not caller-controlled and need not be fingerprint material when
desired intent/provenance already defines semantics.

------------------------------------------------------------------------

## 12. Create Request

The request SHALL contain only:

``` json
{"proposalId":"<uuid>"}
```

Caller SHALL NOT provide target, parameter, current value, desired
value, candidate, risk, impact, confidence, readiness, fingerprint,
source snapshot, sync ID, vendor, endpoint, protocol, command,
credential, token, lease or rollback value.

------------------------------------------------------------------------

## 13. Vendor-Neutral Intent and Expected-State Guard

Model a `ParameterChangeIntent` with:

-   targetType;
-   targetId;
-   parameter;
-   expectedCurrentValue;
-   desiredValue.

Example:

``` text
IF authoritative txPower == 46
THEN future execution intent is txPower -> 44
ELSE STOP
```

Phase 14 does not perform the `THEN`.

Unconditional "set txPower to 44" intent is forbidden.

------------------------------------------------------------------------

## 14. Forward Operation

Persist `NetworkChangePlanOperationEntity` with safe fields:

-   ID/plan ID;
-   sequence;
-   operation type;
-   target type/ID;
-   parameter;
-   expected current value;
-   desired value;
-   safe risk/metadata if needed.

Initial operation type: `SET_PARAMETER`.

No Ericsson/Nokia syntax, CLI, SSH, HTTP mutation, endpoint, credential,
token or raw vendor payload.

------------------------------------------------------------------------

## 15. Dependency Model

Model explicit operation dependencies. `ChangePlanDependencyService`
SHALL reject self-dependency, duplicates, external-plan references and
cycles, and produce deterministic ordering.

Initial one-operation plans normally have no edges. Cycle behavior still
requires automated proof with future-shaped fixtures.

------------------------------------------------------------------------

## 16. Rollback Planning

With `snip.change-planning.require-rollback=true`, every execution-ready
plan requires rollback intent.

Forward:

`expected 46 -> desired 44`

Rollback:

`expected 44 -> desired 46`

Rollback is expected-state guarded and vendor-neutral.

No rollback executor, rollback endpoint, automatic rollback, or MCP
rollback tool.

------------------------------------------------------------------------

## 17. Preconditions

Persist executable preconditions, including:

``` text
EXPECTED_PARAMETER_VALUE
NETWORK_KNOWLEDGE_CONFIDENCE
SOURCE_SYNCHRONIZATION_FRESHNESS
NO_RELEVANT_DRIFT
TWIN_COMPATIBILITY
PROPOSAL_STILL_VALID
TARGET_EXISTS
ROLLBACK_AVAILABLE
DEPENDENCY_GRAPH_VALID
FINGERPRINT_CURRENT
AUTHORIZATION_CURRENT
```

Persist type, expected condition, safe observed value where appropriate,
result, reason code, checkedAt and safe evidence reference.

Recommended results:

`PASS`, `FAIL`, `UNKNOWN`, `STALE`

`UNKNOWN` and `STALE` never count as PASS.

------------------------------------------------------------------------

## 18. Fingerprint

Implement `ChangePlanFingerprintService`.

Use SHA-256 over a canonical UTF-8 representation.

Include at minimum:

-   proposalId;
-   targetType/targetId;
-   parameter;
-   expectedCurrentValue;
-   desiredValue;
-   operation type and explicit sequence;
-   dependency graph;
-   mandatory precondition definitions;
-   rollback intent;
-   sourceSynchronizationExecutionId;
-   sourceSnapshotId when available;
-   stable safety-policy configuration that materially affects
    eligibility.

Exclude:

-   planId;
-   non-semantic DB IDs;
-   created/reviewed/authorized/checked timestamps;
-   createdBy/reviewedBy/authorizedBy;
-   readiness IDs/results;
-   audit IDs/timestamps;
-   dynamic freshness;
-   continuously changing confidence;
-   candidate UUID when intent/provenance already defines the same
    semantics.

Canonicalization SHALL define stable
field/collection/dependency/precondition/rollback ordering, UTF-8,
canonical null/boolean/enum handling, locale-independent numeric
normalization, and shall not hash arbitrary `toString()` or unordered
JSON.

Semantically identical numeric values must hash identically.

------------------------------------------------------------------------

## 19. Fingerprint-Bound Authorization

Persist:

-   authorizedBy;
-   authorizedAt;
-   authorizedFingerprint.

Authorization is current only when:

`authorizedFingerprint == currentFingerprint`

Material execution-significant change invalidates/stales authorization.
Never transfer authorization silently to a new fingerprint.

Execution-significant intent SHALL not be edited in place after
governance review; material changes require a new/superseding version.

------------------------------------------------------------------------

## 20. Safety Policy and Reason Codes

Implement deterministic `ChangePlanSafetyService` /
`ChangeExecutionSafetyPolicy`.

Check supported parameter, bounds, maximum delta, proposal validity,
knowledge, sync/freshness, drift, target, expected current value, Twin
evidence, rollback, dependency graph and fingerprint.

At minimum support stable reason codes:

``` text
PLAN_CURRENT_VALUE_MISMATCH
PLAN_NETWORK_KNOWLEDGE_LOW
PLAN_NETWORK_KNOWLEDGE_UNKNOWN
PLAN_SYNCHRONIZATION_STALE
PLAN_RELEVANT_DRIFT_PRESENT
PLAN_TARGET_NOT_FOUND
PLAN_PARAMETER_UNSUPPORTED
PLAN_VALUE_OUT_OF_RANGE
PLAN_DELTA_TOO_LARGE
PLAN_ROLLBACK_UNAVAILABLE
PLAN_PROPOSAL_INVALID
PLAN_PROPOSAL_NOT_APPROVED
PLAN_TWIN_STALE
PLAN_EXPIRED
PLAN_DEPENDENCY_INVALID
PLAN_FINGERPRINT_MISMATCH
PLAN_AUTHORIZATION_MISSING
PLAN_AUTHORIZATION_STALE
PLAN_CANDIDATE_NOT_FOUND
PLAN_CANDIDATE_AMBIGUOUS
PLAN_CANDIDATE_VALUE_MISMATCH
```

LLMs cannot override safety.

------------------------------------------------------------------------

## 21. Impact Assessment

Implement deterministic `ChangeImpactAssessmentService` with:

`MINIMAL`, `LOW`, `MEDIUM`, `HIGH`

Initial one-cell txPower plans should normally remain bounded to
minimal/low unless deterministic policy proves otherwise. LLMs cannot
set impact.

------------------------------------------------------------------------

## 22. Authorization and Separation of Duties

Implement `ChangePlanAuthorizer` using the repository's current
synthetic/header pattern.

Permissions:

``` text
VIEW_NETWORK_CHANGE_PLAN
CREATE_NETWORK_CHANGE_PLAN
REVIEW_NETWORK_CHANGE_PLAN
AUTHORIZE_NETWORK_CHANGE_PLAN
CANCEL_NETWORK_CHANGE_PLAN
```

Behavioral rules:

-   VIEW != CREATE;
-   REVIEW != AUTHORIZE;
-   CREATE != AUTHORIZE;
-   CANCEL != AUTHORIZE;
-   vendor-import permission grants none;
-   Phase 13 approval permission does not automatically grant Phase 14
    authorization;
-   agents cannot authorize.

Persist createdBy, reviewedBy and authorizedBy where applicable. Do not
claim real production identity separation beyond what the current
synthetic auth model can prove.

------------------------------------------------------------------------

## 23. Review

Persist append-style `NetworkChangePlanReviewEntity` evidence. REVIEW
grants governance review behavior but not authorization. Review alone
does not make the plan ready.

------------------------------------------------------------------------

## 24. Readiness Model

Create:

``` text
ExecutionReadinessResult:
READY
NOT_READY
STALE
UNKNOWN
```

Do not use `BLOCKED` as a readiness result.

Persist append-oriented `ExecutionReadinessAssessmentEntity` with plan
ID, assessedAt, result, assessed fingerprint, stable reason codes and
safe evidence/provenance.

------------------------------------------------------------------------

## 25. Readiness Algorithm

`ChangePlanReadinessService` SHALL re-read authoritative state and
verify:

1.  eligible lifecycle state;
2.  not expired/cancelled/invalidated/superseded;
3.  Phase 13 proposal still valid;
4.  target exists;
5.  parameter supported;
6.  authoritative current value known;
7.  current value equals expected;
8.  knowledge acceptable;
9.  synchronization/freshness acceptable;
10. relevant drift absent;
11. rollback valid;
12. dependencies valid;
13. mandatory preconditions pass;
14. safety passes;
15. current fingerprint matches persisted intent;
16. authorization exists;
17. authorized fingerprint matches;
18. authorization current;
19. execution window, if modeled, permits readiness.

All pass -\> assessment `READY`; eligible `AUTHORIZED` plan may
transition to `READY_FOR_EXECUTION`.

Otherwise fail closed with `NOT_READY`, `STALE`, `UNKNOWN`, or lifecycle
invalidation/expiration/supersession as appropriate.

------------------------------------------------------------------------

## 26. READY_FOR_EXECUTION Safety Boundary

Entering `READY_FOR_EXECUTION` SHALL NOT:

-   call connector/transport;
-   resolve credentials;
-   create vendor command;
-   perform HTTP mutation/CLI/SSH;
-   invoke MCP;
-   invoke Phase 4 action execution;
-   mutate canonical state;
-   schedule execution;
-   publish an execution command/event;
-   trigger rollback.

No listener, observer or scheduler may treat the state as an execution
trigger.

------------------------------------------------------------------------

## 27. Validity and Durable Invalidation

Implement `ChangePlanValidityService`.

Triggers include current mismatch, LOW/UNKNOWN knowledge, relevant
drift, proposal invalidation/expiration/supersession, target
disappearance, fingerprint mismatch, rollback invalidity and material
evidence invalidation.

Previously valid plans use `INVALIDATED`; historical evidence is
preserved.

To avoid the Phase 13 rollback defect, implement an independent
persistence boundary such as `ChangePlanInvalidationPersistenceService`
with `@Transactional(propagation = REQUIRES_NEW)` where an outer
governance/readiness operation will reject/throw after invalidating.

Integration tests SHALL prove status, invalidatedAt, reason and audit
survive the outer failure for current mismatch, LOW, UNKNOWN and drift.

Do not use self-invocation that bypasses Spring transaction proxies.

------------------------------------------------------------------------

## 28. Expiration and Execution Window

Use UTC `Instant`. Add configurable validity duration and persist
`expiresAt`.

Expired plans cannot be authorized or ready.

Optional earliest/latest execution-window metadata may be modeled only
as readiness constraints. No execution scheduler.

------------------------------------------------------------------------

## 29. Cancellation

Allow cancellation from the seven accepted active states and transition
to terminal `CANCELLED`.

Persist cancelledBy/cancelledAt and audit.

Cancellation has no vendor, rollback, canonical, ProposedAction or MCP
side effects.

Use optimistic locking for cancel/authorize/readiness races.

------------------------------------------------------------------------

## 30. V15 Persistence

Create exactly:

`V15__phase14_change_execution_planning.sql`

Do not modify V1--V14.

Required tables:

``` text
network_change_plan
network_change_plan_operation
network_change_plan_precondition
network_change_plan_rollback_operation
network_change_plan_review
network_change_plan_readiness_assessment
network_change_plan_audit_event
```

A normalized operation-dependency table may be added if needed.

Use existing PostgreSQL conventions: UUID PKs, `TIMESTAMPTZ`, VARCHAR
enums/statuses, FK/index/uniqueness constraints and optimistic
versioning as appropriate.

No secrets, endpoints, commands, credentials or raw vendor payloads.

------------------------------------------------------------------------

## 31. Core Plan Fields

`network_change_plan` should include, as appropriate:

proposal ID, optional resolved candidate provenance, status, plan
version, target type/ID, parameter, expected/desired values,
fingerprint, authorized fingerprint, source system/snapshot/sync
execution, knowledge snapshot at creation, impact/risk,
created/reviewed/authorized/cancelled metadata, expiresAt, invalidation
fields, predecessor/supersededBy, and optimistic JPA version.

Use repository naming conventions.

------------------------------------------------------------------------

## 32. Audit

Append-only application semantics. Required events include:

``` text
PLAN_CREATED
PLAN_VALIDATION_STARTED
PLAN_VALIDATED
PLAN_BLOCKED
PLAN_SAFETY_EVALUATED
PLAN_REVIEWED
PLAN_AUTHORIZED
PLAN_READINESS_EVALUATED
PLAN_READY
PLAN_NOT_READY
PLAN_INVALIDATED
PLAN_EXPIRED
PLAN_CANCELLED
PLAN_SUPERSEDED
```

No execution events.

Audit must exclude secrets, tokens, credential handles, vendor
endpoints, raw responses and commands.

------------------------------------------------------------------------

## 33. API

Implement:

``` text
POST /api/v1/change-planning/plans
GET  /api/v1/change-planning/plans
GET  /api/v1/change-planning/plans/{id}
GET  /api/v1/change-planning/plans/{id}/evidence
POST /api/v1/change-planning/plans/{id}/review
POST /api/v1/change-planning/plans/{id}/authorize
POST /api/v1/change-planning/plans/{id}/cancel
POST /api/v1/change-planning/plans/{id}/readiness
```

Forbidden:

``` text
.../{id}/execute
.../{id}/apply
.../{id}/rollback
.../{id}/vendor-command
```

Do not create aliases or dormant 501 endpoints for forbidden behavior.

Create explicit DTOs. Authorization request must not accept caller
fingerprint as authoritative. Readiness request accepts no hard-gate
overrides.

Use stable 400/403/404/409 semantics consistent with repository
conventions and do not leak stack traces.

------------------------------------------------------------------------

## 34. API Permission Mapping

  -----------------------------------------------------------------------
  Endpoint                            Permission
  ----------------------------------- -----------------------------------
  POST collection                     `CREATE_NETWORK_CHANGE_PLAN`

  GET collection                      `VIEW_NETWORK_CHANGE_PLAN`

  GET detail/evidence                 VIEW or REVIEW

  POST review                         `REVIEW_NETWORK_CHANGE_PLAN`

  POST authorize                      `AUTHORIZE_NETWORK_CHANGE_PLAN`

  POST cancel                         `CANCEL_NETWORK_CHANGE_PLAN`

  POST readiness                      choose and document an explicit
                                      non-escalating governance
                                      permission;
                                      `AUTHORIZE_NETWORK_CHANGE_PLAN` is
                                      acceptable if readiness is treated
                                      as governed mutation
  -----------------------------------------------------------------------

The final readiness-permission choice must be behavioral-tested.

------------------------------------------------------------------------

## 35. Configuration

Add consistent Spring Boot properties:

``` yaml
snip:
  change-planning:
    enabled: true
    validity-duration: PT24H
    maximum-operation-count: 1
    require-rollback: true
    require-current-value-match: true
    require-high-or-medium-knowledge: true
```

No vendor endpoint, vendor credentials, write protocol or mutation path.

Disabled mode fails closed for mutation/governance. Historical reads may
remain if explicitly documented and tested.

------------------------------------------------------------------------

## 36. Metrics

Low-cardinality counters:

``` text
plans_created_total
plans_blocked_total
plans_reviewed_total
plans_authorized_total
plans_invalidated_total
plans_cancelled_total
readiness_checks_total
readiness_ready_total
readiness_not_ready_total
```

Never label with plan/proposal/cell IDs, usernames, endpoints,
fingerprints, secrets or raw exceptions.

------------------------------------------------------------------------

## 37. Agent, MCP and Vendor Isolation

Agents may explain/request safe planning through future authorized
adapters but cannot authorize or override readiness/safety.

No Phase 14 MCP mutation tools such as `execute_change_plan`,
`apply_vendor_change`, `set_tx_power`, `rollback_change`.

Phase 14 production package SHALL have no direct dependency on
`EnmTransport`, Ericsson/Nokia connector implementations,
`CredentialHandle`, Key Vault client classes, vendor credential
providers or vendor write clients.

------------------------------------------------------------------------

## 38. Canonical-State Isolation

Phase 14 may read canonical state but SHALL NOT write
`radio_configuration`.

Tests must prove canonical txPower unchanged after creation, review,
authorization, readiness, cancellation and invalidation.

Canonical state remains observation/reconciliation-owned.

------------------------------------------------------------------------

## 39. Knowledge, Freshness and Drift

Default acceptable knowledge is HIGH/MEDIUM; LOW/UNKNOWN fail closed.

Readiness re-evaluates dynamic confidence/freshness/drift using Phase 12
authority rather than trusting creation snapshots.

Stale/untrustworthy synchronization blocks readiness.

Relevant unresolved drift blocks readiness and may invalidate active
plans.

Do not duplicate Phase 12 algorithms.

------------------------------------------------------------------------

## 40. Current-Value Rule

At creation/readiness:

`authoritative current value == expected current value`

Mismatch after a valid plan causes `INVALIDATED`; never rewrite expected
value to match the network. A new recommendation/plan is required.

------------------------------------------------------------------------

## 41. Idempotency and Concurrency

Repeated identical create requests must not create contradictory active
plans. Choose and document either return-existing or stable-conflict
behavior, backed by persistence/transaction constraints.

Use optimistic JPA locking for authorize/cancel/readiness and stale
updates.

No new distributed governance lock. Phase 8 remains synchronization
concurrency authority.

------------------------------------------------------------------------

## 42. Determinism and No LLM Authority

Given identical proposal, canonical state, evidence, source/sync state,
knowledge, drift and policy, Phase 14 SHALL produce identical operation
intent, dependencies, preconditions, rollback, impact, safety,
fingerprint and readiness.

LLMs may explain only. They cannot set/override expected/desired values,
operations, dependencies, rollback, risk/impact, preconditions, safety,
readiness, authorization, invalidation or expiration.

------------------------------------------------------------------------

## 43. Threat Model

Tests/design SHALL defend against stale readiness, stale authorization
replay, fingerprint mismatch, tampering, current/desired/target
substitution, permission escalation, agent authorization, current-state
race, rollback corruption, dependency manipulation, evidence spoofing,
endpoint/credential/command injection, execution-by-API smuggling,
execution-by-event/listener, MCP execution and canonical mutation.

Fail closed.

------------------------------------------------------------------------

## 44. Recommended Services

``` text
NetworkChangePlanService
ChangePlanEligibilityService
ChangePlanOperationBuilder
ChangePlanDependencyService
ChangePlanPreconditionService
ChangePlanRollbackService
ChangePlanSafetyService
ChangeImpactAssessmentService
ChangePlanFingerprintService
ChangePlanReadinessService
ChangePlanGovernanceService
ChangePlanValidityService
ChangePlanInvalidationPersistenceService
ChangePlanAuditService
```

Exact decomposition may vary without weakening boundaries. Avoid a
monolithic service.

------------------------------------------------------------------------

## 45. Recommended Repositories

``` text
NetworkChangePlanRepository
NetworkChangePlanOperationRepository
NetworkChangePlanPreconditionRepository
NetworkChangePlanRollbackOperationRepository
NetworkChangePlanReviewRepository
ExecutionReadinessAssessmentRepository
NetworkChangePlanAuditEventRepository
```

Add dependency repository if normalized.

------------------------------------------------------------------------

## 46. Test Architecture

Create at minimum:

``` text
ChangePlanningArchitectureIsolationTest
ChangePlanningMandatoryMatrixTest
ChangePlanningApiTest
```

Use focused unit tests for deterministic policies as useful.

Evidence classifications:

-   STRUCTURAL;
-   BEHAVIORAL;
-   INTEGRATION.

Constants, enums, annotations and file existence do not prove
lifecycle/transaction behavior.

------------------------------------------------------------------------

## 47. Shared Testcontainer Isolation

The static PostgreSQL Testcontainer is shared across integration
classes. Every Phase 14 test changing Phase 1--13 shared state must
restore it.

No class-order dependency. Do not use Surefire ordering as a fix.

Restore canonical radio config, Phase 12 drift/source state, Phase 13
proposal/candidate/governance state, assurance/Twin state, and Phase 14
rows as applicable.

Use isolated fixtures, `try/finally`, `@AfterEach`, JDBC
restoration/cleanup as appropriate.

------------------------------------------------------------------------

## 48. Mandatory High-Value Integration Flows

### Happy path

Approved Phase 13 proposal -\> create -\> `READY_FOR_REVIEW` -\> review
-\> authorize -\> `AUTHORIZED` -\> readiness -\> assessment READY -\>
`READY_FOR_EXECUTION`; canonical txPower unchanged; no ProposedAction;
no execution.

### Current mismatch

Create/authorize -\> alter controlled canonical fixture -\> readiness
-\> durable `INVALIDATED` with `PLAN_CURRENT_VALUE_MISMATCH`; restore
fixture.

### Knowledge degradation

HIGH/MEDIUM -\> LOW and UNKNOWN after authorization -\> durable
invalidation; restore Phase 12 state.

### Drift

Introduce relevant unresolved drift -\> readiness/governance -\> durable
invalidation; clean drift.

### Permission separation

REVIEW-only cannot authorize; CREATE-only cannot authorize;
vendor-import and Phase 13 approval permission do not grant Phase 14
authorization.

### Cancellation

Cancel from `AUTHORIZED` and `READY_FOR_EXECUTION`; terminal; no side
effects.

------------------------------------------------------------------------

## 49. Mandatory Matrix --- 180 Requirements

Implement a traceable mandatory matrix with at least these 180 IDs.

### Scope/Baseline 1--10

1 exact architecture baseline; 2 planning/readiness only; 3 no Phase15;
4 plan != proposal; 5 plan != ProposedAction; 6 txPower only; 7 one
target; 8 one parameter; 9 max operation one; 10 no vendor write.

### Proposal Eligibility 11--30

11 proposal exists; 12 APPROVED required; 13 DRAFT blocked; 14
VALIDATING blocked; 15 RECOMMENDED blocked; 16 REJECTED blocked; 17
INVALID blocked; 18 INVALIDATED blocked; 19 EXPIRED blocked; 20
SUPERSEDED blocked; 21 validity rechecked; 22 target rechecked; 23
authoritative current read; 24 caller cannot override current; 25
desired from proposal; 26 caller cannot override desired; 27 snapshot
provenance; 28 sync provenance; 29 Phase12 knowledge authority; 30
Phase12 drift authority.

### Candidate/Twin 31--45

31 no selectedCandidateId dependency; 32 rank1 resolved; 33 exactly one
rank1; 34 proposedValue match; 35 missing fails; 36 ambiguous fails; 37
mismatch fails; 38 simulationRunId retained; 39 simulation evidence
read-only; 40 no silent rerun; 41 no duplicate simulation algorithm; 42
stale Twin safe block; 43 candidate UUID not caller-controlled; 44 safe
provenance; 45 no raw payload.

### Operations 46--60

46 SET_PARAMETER; 47 target type; 48 target ID; 49 parameter; 50
expected value; 51 desired value; 52 sequence; 53 deterministic
sequence; 54 bounded count; 55 no vendor syntax; 56 no endpoint; 57 no
protocol command; 58 no credential; 59 no token; 60 not executable.

### Dependencies 61--70

61 model exists; 62 one-op/no-edge valid; 63 self-cycle rejected; 64
cycle detection behavioral; 65 duplicate edge rejected; 66 external-plan
reference rejected; 67 deterministic order; 68 definitions
fingerprinted; 69 manipulation changes fingerprint/fails; 70 no
execution.

### Rollback 71--85

71 required default; 72 created; 73 target matches; 74 parameter
matches; 75 expected=forward desired; 76 desired=forward expected; 77
deterministic sequence; 78 missing blocks readiness; 79 invalid blocks;
80 fingerprinted; 81 no endpoint; 82 no executor; 83 no automatic
rollback; 84 no vendor command; 85 no canonical mutation.

### Preconditions 86--105

86 expected-value; 87 knowledge; 88 freshness; 89 drift; 90 Twin; 91
proposal validity; 92 target; 93 rollback; 94 dependency; 95
fingerprint; 96 authorization; 97 persisted; 98 evaluated; 99 UNKNOWN
fail; 100 STALE fail; 101 deterministic order; 102 safe evidence; 103 no
raw vendor response; 104 no LLM override; 105 participate in readiness.

### Fingerprint 106--125

106 SHA256; 107 UTF8; 108 field order; 109 collection order; 110 null;
111 enum; 112 boolean; 113 numeric normalization; 114 locale
independent; 115 proposal ID; 116 target; 117 expected/desired; 118
operations; 119 dependencies; 120 preconditions; 121 rollback; 122
source sync/snapshot; 123 volatile timestamps excluded; 124 actors
excluded; 125 repeat deterministic.

### Safety/Impact 126--140

126 parameter gate; 127 bounds; 128 max delta; 129 proposal validity;
130 knowledge; 131 sync/freshness; 132 drift; 133 current value; 134
Twin evidence; 135 rollback; 136 dependency; 137 fingerprint; 138
deterministic safety; 139 deterministic impact; 140 no LLM override.

### Governance 141--160

141 VIEW; 142 CREATE; 143 REVIEW; 144 AUTHORIZE; 145 CANCEL; 146 VIEW !=
CREATE; 147 REVIEW != AUTHORIZE; 148 CREATE != AUTHORIZE; 149 CANCEL !=
AUTHORIZE; 150 vendor-import no authorization; 151 Phase13 approval no
Phase14 authorization; 152 agent cannot authorize; 153 review evidence;
154 authorization actor; 155 authorization time; 156 authorized
fingerprint; 157 status AUTHORIZED; 158 authorization not ready; 159
stale fingerprint blocks; 160 optimistic conflict safe.

### Readiness/Lifecycle 161--180

161 READY; 162 NOT_READY; 163 STALE; 164 UNKNOWN; 165 readiness enum
excludes BLOCKED; 166 PlanStatus includes BLOCKED; 167 authorization
required; 168 all hard gates required; 169 READY assessment persisted;
170 NOT_READY persisted; 171 READY -\> READY_FOR_EXECUTION; 172 ready
state no execution; 173 current mismatch invalidates; 174 LOW
invalidates; 175 UNKNOWN invalidates; 176 drift invalidates; 177
expiration blocks; 178 cancel AUTHORIZED; 179 cancel
READY_FOR_EXECUTION; 180 CANCELLED terminal.

The implementation may add matrix items but may not reduce these.

------------------------------------------------------------------------

## 50. Architecture Isolation Evidence

Structurally prove no `EnmTransport`, vendor connector, credential
resolution, Key Vault, Phase 4 execution or MCP execution dependency in
Phase 14; no automatic ProposedAction; no forbidden API; no
execution-like status; no Phase15; V1--V14 unchanged; V15 only new
migration; no endpoint/command/credential columns.

Structural evidence does not replace behavioral evidence.

------------------------------------------------------------------------

## 51. Durable Invalidation Evidence

Integration tests must deliberately cause invalidation followed by outer
conflict/exception and then reload in a new transaction, proving
`INVALIDATED`, `invalidatedAt`, reason and audit persisted.

Required: current mismatch, LOW, UNKNOWN, relevant drift.

------------------------------------------------------------------------

## 52. Fingerprint Evidence

Test identical intent stable; repeated calls stable; ordering stable;
locale stable; semantic numeric normalization stable;
timestamps/actors/readiness do not change hash;
target/expected/desired/rollback/dependency/source-binding material
changes do.

------------------------------------------------------------------------

## 53. Security/API Evidence

Prove callers cannot inject endpoint, protocol, command, credential,
token, current/desired, risk, impact, readiness, fingerprint, actor,
snapshot/sync identity. Unauthorized governance -\> 403. Errors do not
leak secrets/raw payloads.

------------------------------------------------------------------------

## 54. No-Execution Evidence

Prove Phase 14 cannot reach `ActionExecutionService`,
`McpCapabilityGateway`, `EnmTransport`, vendor connector or credential
resolution; forbidden endpoints absent; no execution listener/scheduler;
no canonical mutation.

------------------------------------------------------------------------

## 55. Audit/Metrics Evidence

Audit append history for creation, validation, block, review,
authorization, readiness, ready/not-ready, invalidation, cancellation;
safe fields only.

Metrics tests inspect tags and prohibit IDs, usernames, fingerprints,
endpoints and raw exceptions.

------------------------------------------------------------------------

## 56. Migration and Regression

Full Testcontainers startup applies V1--V15. Do not edit old migrations.

Before completion run:

``` bash
mvn -B clean test
cd simulator
go test ./...
go build ./cmd/simulator
cd ..
git diff --check
```

All Phase 1--14 Maven tests must pass. Go tests/build must pass locally.
Default CI remains Azure/vendor independent.

------------------------------------------------------------------------

## 57. Configuration/Application Changes

Register Phase 14 properties consistently with the existing Spring Boot
application. Modify `NpoApplication`, `application.yml` and shared
exception handling only where required.

Do not opportunistically refactor prior phases.

------------------------------------------------------------------------

## 58. Documentation Deliverables

Store this specification at:

`docs/implementation/SNIP-PHASE-14-GOVERNED-CHANGE-PLANNING-EXECUTION-READINESS-SAFETY-CONTROL-SPECIFICATION.md`

At completion create:

`docs/implementation/SNIP-PHASE-14-GOVERNED-CHANGE-PLANNING-EXECUTION-READINESS-SAFETY-CONTROL-COMPLETION-REPORT.md`

If a C-level architecture contradiction is discovered, STOP rather than
silently changing accepted architecture.

------------------------------------------------------------------------

## 59. Architecture Acceptance Gates --- 60/60 Required

1 exact architecture baseline; 2 planning/readiness not execution; 3
plan distinct proposal; 4 plan distinct ProposedAction; 5 approved
proposal only; 6 proposal validity rechecked; 7 txPower only; 8 one
target/parameter; 9 desired exclusively proposal; 10 authoritative
current; 11 caller cannot override current; 12 caller cannot override
desired; 13 vendor-neutral operation; 14 no vendor command; 15 no vendor
endpoint; 16 no credential/token; 17 no raw vendor payload; 18
deterministic fingerprint; 19 authorization binds fingerprint; 20
material modification invalidates authorization; 21 preconditions
persisted/evaluated; 22 current-match precondition; 23 knowledge
precondition; 24 drift precondition; 25 proposal-validity precondition;
26 rollback required; 27 rollback guard; 28 rollback no execution; 29
dependency graph; 30 cycles rejected; 31 deterministic safety; 32 no LLM
hard-gate override; 33 deterministic readiness; 34 ready state no
execution; 35 expiration; 36 stale current invalidates; 37 knowledge
degradation invalidates; 38 drift invalidates; 39 reuse Phase12
knowledge; 40 reuse Phase12 drift; 41 reuse Phase13 intelligence; 42 no
duplicate Phase13 algorithms; 43 proposal approval != plan
authorization; 44 REVIEW != AUTHORIZE; 45 agents cannot authorize; 46
vendor-import permissions do not grant governance; 47 no MCP execution;
48 no EnmTransport; 49 no vendor connector; 50 no credential resolution;
51 no automatic ProposedAction; 52 no canonical mutation; 53 V15
forward-only; 54 V1--V14 unchanged; 55 safe append audit; 56 low-card
metrics; 57 CI Azure/vendor independent; 58 test isolation; 59
Phase1--13 regression green; 60 no Phase15.

Each gate must map to STRUCTURAL, BEHAVIORAL and/or INTEGRATION evidence
honestly.

------------------------------------------------------------------------

## 60. Debt Policy and Fail-Closed Rules

Debt cannot defer authorization separation, expected-state guard,
rollback planning, deterministic fingerprint, fingerprint-bound
authorization, durable invalidation, test isolation, vendor-write
prohibition or no-execution boundary.

Unknown current/target/proposal
validity/knowledge/sync/drift/rollback/fingerprint/authorization/simulation
evidence blocks progression. Never turn UNKNOWN into success.

------------------------------------------------------------------------

## 61. Implementation Order

1 verify baseline; 2 add spec; 3 V15; 4 enums/models; 5 entities; 6
repositories; 7 config; 8 authoritative read integration; 9 candidate
resolution; 10 operation builder; 11 dependencies; 12 rollback; 13
preconditions; 14 fingerprint; 15 safety/impact; 16
creation/eligibility; 17 validity/durable invalidation; 18
authorization; 19 review/cancel; 20 readiness; 21 audit; 22 metrics; 23
API/errors; 24 isolation tests; 25 matrix; 26 API/integration tests; 27
targeted tests; 28 full Maven; 29 Go tests; 30 Go build; 31
diff/security searches; 32 completion report; 33 conformance review; 34
STOP before Git commit.

------------------------------------------------------------------------

## 62. Prohibited Shortcuts

No test ordering workaround; no mocking away required transaction
semantics; no hard-coded READY; no authorization-implies-readiness; no
readiness `BLOCKED`; no caller authoritative values; no duplicate
Phase12/13 logic; no silent simulation rerun; no future vendor command
fields; no dormant execute endpoint; no dormant write connector method;
no unused write credential; no execution event; no automatic
ProposedAction; no weakened prior security tests.

A dormant future write hook is still a capability and is forbidden.

------------------------------------------------------------------------

## 63. Completion Verification and Report

Record exact observed results:

``` text
PHASE 14 TARGETED TESTS: <exact>
PHASE 14 MANDATORY MATRIX: 180 / 180 PASS
ARCHITECTURE GATES: 60 / 60 PASS
FULL MAVEN: <exact>
GO TEST: PASS
GO BUILD: PASS
GIT DIFF CHECK: PASS
```

Do not fabricate counts.

Cursor's report SHALL include starting architecture SHA, current HEAD
(still architecture baseline), V15, package/model, candidate binding,
expected-state safety, rollback, dependencies, preconditions,
fingerprint, fingerprint-bound authorization, permission separation,
readiness, durable invalidation, cancellation, API, forbidden endpoints,
audit/metrics, isolation, exact tests, security searches and explicit
no-execution/no-vendor dependencies.

Statuses:

``` text
PHASE 14 ARCHITECTURE STATUS: ACCEPTED
PHASE 14 IMPLEMENTATION STATUS: COMPLETE
CHANGE PLAN MODEL: VERIFIED
EXECUTION READINESS MODEL: VERIFIED
ROLLBACK PLANNING: VERIFIED
REAL VENDOR WRITE CAPABILITY: NOT AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT AUTHORIZED
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
REAL VENDOR CONTINUOUS SYNCHRONIZATION: NOT YET VERIFIED
PHASE 14 GIT BASELINE: NOT YET ESTABLISHED
PHASE 15 STATUS: NOT STARTED
```

Final line exactly:

`PHASE 14 IMPLEMENTATION: COMPLETE — ARCHITECTURAL CONFORMANCE REVIEW PENDING`

If incomplete, do not print that success line.

------------------------------------------------------------------------

## 64. Git Boundary

This implementation task SHALL stop before `git commit`, `git push`, tag
creation, architecture amendment or Phase 15 work.

A separate conformance review must inspect actual code, V15, lifecycle,
permissions, readiness, transaction boundaries, fingerprint, Phase12/13
reuse, no-execution boundary, tests, test isolation and
completion-report truthfulness.

Only after acceptance may the Phase 14 implementation Git baseline be
authorized.

Failed future candidate commits are never amended; corrections use
follow-up commits; exact-SHA CI is required.

------------------------------------------------------------------------

## 65. Production Status

Throughout Phase 14:

``` text
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
REAL VENDOR CONTINUOUS SYNCHRONIZATION: NOT YET VERIFIED
REAL VENDOR WRITE CAPABILITY: NOT AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT AUTHORIZED
PHASE 15 STATUS: NOT STARTED
```

------------------------------------------------------------------------

## 66. Final Normative Invariants

1.  Only APPROVED Phase13 proposals may create plans.
2.  No Phase13 recommendation semantics are changed.
3.  No canonical mutation.
4.  No vendor execution.
5.  No write credential resolution.
6.  No vendor command generation.
7.  Intent remains vendor-neutral.
8.  Expected current state is mandatory.
9.  Desired state comes only from approved proposal.
10. Rank-1 candidate consistency is mandatory.
11. Caller cannot override intent.
12. Rollback is required by default.
13. Rollback is expected-state guarded.
14. Rollback never executes.
15. Dependencies deterministic; cycles rejected.
16. Preconditions persisted and evaluated.
17. Unknown hard gates fail closed.
18. Safety/impact/readiness deterministic.
19. LLM cannot override hard gates.
20. Fingerprint deterministic SHA-256.
21. Volatile metadata excluded.
22. Authorization binds fingerprint.
23. Material intent change invalidates authorization.
24. Proposal approval != plan authorization.
25. REVIEW != AUTHORIZE.
26. Agents cannot authorize.
27. Vendor-import permission cannot authorize.
28. AUTHORIZED != READY_FOR_EXECUTION.
29. Readiness follows authorization.
30. NOT_READY != PlanStatus.BLOCKED.
31. READY_FOR_EXECUTION triggers nothing.
32. Current mismatch invalidates.
33. Knowledge degradation invalidates.
34. Relevant drift invalidates.
35. Expiration prevents readiness.
36. Cancellation is terminal and side-effect free.
37. Durable invalidation survives outer rollback.
38. Phase12 authorities are reused.
39. Phase13 authorities are reused.
40. Phase6 evidence is consumed without silent rerun.
41. Plan != ProposedAction.
42. No automatic ProposedAction.
43. No MCP execution.
44. No EnmTransport dependency.
45. No vendor connector dependency.
46. No credential-resolution dependency.
47. V15 forward-only; V1--V14 unchanged.
48. Audit safe/append-oriented.
49. Metrics low-cardinality.
50. Tests order-independent.
51. Shared prior-phase state restored.
52. CI needs no Azure/vendor infrastructure.
53. Phase1--13 regressions green.
54. No real vendor write capability.
55. No Phase15 implementation.

------------------------------------------------------------------------

## 67. Specification Status

``` text
PHASE 14 ARCHITECTURE STATUS: ACCEPTED
PHASE 14 ARCHITECTURE BASELINE: 6cc29ba8b70b1fbae65fdb70a958cb6c4fb32423
PHASE 14 ARCHITECTURE CI: SUCCESS — EXACT ARCHITECTURE BASELINE SHA VERIFIED

PHASE 14 IMPLEMENTATION SPECIFICATION: COMPLETE
PHASE 14 IMPLEMENTATION: AUTHORIZED
PHASE 14 IMPLEMENTATION STATUS: NOT STARTED
V15: AUTHORIZED FOR PHASE 14 IMPLEMENTATION

REAL VENDOR WRITE CAPABILITY: NOT AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT AUTHORIZED
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
REAL VENDOR CONTINUOUS SYNCHRONIZATION: NOT YET VERIFIED

PHASE 14 IMPLEMENTATION GIT BASELINE: NOT YET ESTABLISHED
PHASE 15 STATUS: NOT STARTED
```

**PHASE 14 IMPLEMENTATION SPECIFICATION: COMPLETE --- AUTHORIZED FOR
IMPLEMENTATION**
