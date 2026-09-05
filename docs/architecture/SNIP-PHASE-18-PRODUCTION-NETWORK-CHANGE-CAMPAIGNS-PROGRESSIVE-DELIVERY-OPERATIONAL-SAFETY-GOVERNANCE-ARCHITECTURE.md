# SNIP Phase 18 — Production Network Change Campaigns, Progressive Delivery & Operational Safety Governance

## Architecture Document

**Status:** ACCEPTED FOR DOCUMENT FREEZE  
**Date:** 2026-09-05  
**Phase:** 18  
**Document type:** Architecture (normative; accepted for document freeze; not yet a Git architecture baseline)  
**Parent Phase 17 immutable implementation baseline:** `d1751cca70391babf712bce3c6bcc29238ce0c86`  
**Phase 17 architecture Git baseline:** `77fd24c0fd32c920c97ff5169f4bc8a93a77b208`  
**Phase 17 architecture SHA-256:** `ea92c6e9183234485da83798ab4fc91c224cfbd1dad80bc464d41009fce576a0`  
**Phase 17 implementation specification SHA-256:** `7f203e6d7ad21fd14c101fc3c255a27fa83e312273fde49608a3c0e2f590ddbe`  
**Implementation:** NOT AUTHORIZED  
**Implementation specification:** NOT STARTED  
**Architecture Git baseline:** NOT YET AUTHORIZED  
**V19:** NOT CREATED  
**Phase 19:** NOT STARTED  
**Real production execution:** NOT AUTHORIZED  
**Real production campaign execution:** NOT AUTHORIZED  
**Ericsson production write protocol:** UNRESOLVED  
**Ericsson production write transport:** UNCONFIGURED / NOT IMPLEMENTED  
**Ericsson production endpoint:** NONE / NOT CONFIGURED  
**Ericsson production auth method:** UNRESOLVED / EXTERNALLY CONFIGURED  
**Ericsson production credential:** NONE  
**Nokia production write support:** DEFERRED  
**Closed-loop optimization:** NOT AUTHORIZED  

This document is architecture only. It does **not** authorize implementation, V19 creation, a real Ericsson write transport, production endpoint configuration, credential creation, Git freeze, architecture baselining, or real production campaign execution.

---

# 0. Purpose

This architecture defines the **production network change campaign, progressive-delivery, and operational safety governance** plane that may — only after inherited Phase 16/17 controls remain satisfied and later, independently authorized campaign qualification — coordinate multiple individually governed network changes as a controlled production campaign.

Phase 18 starts where Phase 17 stops. Phase 17 remains the immutable owner of certified vendor write transport, target onboarding, and production operational readiness. Phase 16 remains the immutable owner of production change governance, grants, consume-before-send, lease/fencing, kill switch, rate/blast controls, expected-state policy, mutation attempt state, verification, rollback governance, audit, credential isolation, and the separate production-write-gateway runtime.

Phase 18 **coordinates** existing individual production controls. It does **not** replace, weaken, or bypass them.

This document does **not** make campaign execution part of normal automated workflow. Acceptance of this architecture for document freeze does **not** authorize real production execution or real production campaign execution.

---

# 1. Defining Principle

> SNIP may coordinate multiple individually governed network changes as a controlled production campaign only when every mutation remains independently authorized, bounded, observable, progressively released, and automatically stoppable; campaign orchestration never creates authority to mutate the network.

> When SNIP loses certainty, SNIP loses permission to progress, not responsibility to resolve an already possible external mutation.

Corollaries:

1. Campaign orchestration is a coordination plane, not a mutation plane.
2. Every production mutation remains an individually governed Phase 16 execution through the Production Write Gateway.
3. Progressive delivery is a sequence of human-released, individually verified, reconciled, and observed mutations.
4. Loss of certainty denies new progression or send; it does not erase duty to resolve a possible external mutation.
5. Agent, MCP, scheduler, event-driven, and closed-loop production campaign execution remain **NOT AUTHORIZED**.
6. Architecture acceptance for document freeze does **not** imply production authorization.

---

# 2. Parent Baseline and Logical Lineage

## 2.1 Immutable parents

| Artifact | SHA / status |
|---|---|
| Phase 17 immutable implementation baseline | `d1751cca70391babf712bce3c6bcc29238ce0c86` |
| Phase 17 architecture Git baseline | `77fd24c0fd32c920c97ff5169f4bc8a93a77b208` |
| Phase 17 architecture SHA-256 | `ea92c6e9183234485da83798ab4fc91c224cfbd1dad80bc464d41009fce576a0` |
| Phase 17 implementation specification SHA-256 | `7f203e6d7ad21fd14c101fc3c255a27fa83e312273fde49608a3c0e2f590ddbe` |
| Phase 16 immutable implementation baseline | `f4e09b42f7b8f56c3794fae3c91a50a7af490c82` |
| Phase 16 architecture Git baseline | `8c0791b67ddd9121b1dd5d0abf452c056a8c9a52` |
| Phase 15 immutable implementation baseline | `ae9c13d55b444fa50090813495b32b82f97c2ec3` |
| Phase 13 Git baseline | `5e9400005626fb93d5e61f96be680bea5540df31` |
| Phase 11 Git baseline | `78e699380be37109cfdd2111dd0f29c7052709c3` |

Phase 17 remains **CLOSED / FROZEN / EXACT-SHA VERIFIED**. Phase 18 **MUST NOT** amend Phase 17 architecture, specification, implementation, V18 schema, or Git baseline.

## 2.2 Logical lineage (non-substitutable)

```text
Phase 11  Read-only production vendor integration
  → Phase 12  Continuous synchronization / drift / confidence
  → Phase 13  Optimization proposals and recommendations
  → Phase 14  Governed change planning and execution readiness
  → Phase 15  Controlled sandbox execution and verification
  → Phase 16  Production-write security boundary and individual governed execution
  → Phase 17  Certified vendor write transport / target onboarding / operational readiness
  → Phase 18  Production campaigns / progressive delivery / campaign safety governance
```

## 2.3 Authorization chain (non-substitutable)

```text
Phase 13 proposal approval
  ≠ Phase 14 plan authorization
  ≠ Phase 15 execution authorization
  ≠ Phase 16 production authorization
  ≠ Phase 17 transport / target certification
  ≠ Phase 18 campaign authorization
  ≠ Phase 18 cohort release
  ≠ Phase 16 ProductionExecutionGrant
  ≠ Level-4 / C4 production campaign execution
```

Each layer requires an independent human decision against its own bound artifact. Campaign authorization is **never** sufficient for production mutation.

## 2.4 What Phase 17 already proved (and what it did not)

Phase 17 defined certified vendor write transport, target onboarding, and operational readiness. It did **not**:

- configure a real Ericsson write protocol
- implement a production Ericsson write transport
- add a real vendor endpoint
- add a real production credential
- execute Level 1 / Level 2 certification
- satisfy Level 3 or Level 4
- authorize real production execution
- define production campaign coordination

## 2.5 Current security state inherited at Phase 17 closure

```text
ERICSSON PRODUCTION WRITE PROTOCOL: UNRESOLVED
ERICSSON PRODUCTION TRANSPORT: UNCONFIGURED / NOT IMPLEMENTED
ERICSSON PRODUCTION ENDPOINT: NONE / NOT CONFIGURED
ERICSSON PRODUCTION AUTH METHOD: UNRESOLVED / EXTERNALLY CONFIGURED
ERICSSON PRODUCTION CREDENTIAL: NONE
PRODUCTION EXECUTION DEFAULT: DISABLED
P17 L1: NOT EXECUTED
P17 L2: NOT EXECUTED
P17 L3: NOT SATISFIED
P17 L4: NOT SATISFIED
REAL PRODUCTION EXECUTION: NOT AUTHORIZED
AGENT / MCP / SCHEDULED / EVENT EXECUTION: NOT AUTHORIZED
AUTOMATIC ROLLBACK: NOT AUTHORIZED
CLOSED-LOOP EXECUTION: NOT AUTHORIZED
NOKIA: DEFERRED
```

Phase 18 architecture **MUST** preserve this default security state. This document itself does not change it.

---

# 3. Scope

## 3.1 Initial Phase 18 scope (hard)

| Constraint | Value |
|---|---|
| Vendor | ERICSSON first |
| Nokia | DEFERRED |
| Targets per campaign | exactly one `ProductionNetworkTarget` |
| Object | CELL |
| Parameter | txPower |
| Parameters per campaign item | 1 |
| Forward mutations per child production execution | 1 |
| Canary | mandatory |
| Initial canary | exactly one cell |
| Production campaign mutation concurrency | exactly 1 |
| Item execution | sequential |
| Progression mode | MANUAL only |
| Cross-target campaign | forbidden |
| Multi-parameter campaign item | forbidden |
| Generic vendor commands | forbidden |
| Arbitrary endpoints | forbidden |
| Generic production configuration payload | forbidden |
| SSH / CLI escape hatch | forbidden |
| Closed-loop production autonomy | forbidden |

## 3.2 What Phase 18 adds

Phase 18 adds architecture for:

- revisioned production change campaigns
- immutable authorized campaign scope
- progressive delivery by cohort
- campaign execution bindings
- campaign lease/fencing and safety generations
- dual health and post-change observation
- campaign safety exposure (forward and recovery)
- monotonic forward-progression closure
- campaign evidence and tamper-evident audit

Phase 18 does **not** add a second Phase 16 grant writer, a vendor-write protocol, or application possession of vendor write credentials.

---

# 4. Architecture Non-Goals

Phase 18 explicitly does **not**:

- introduce closed-loop production autonomy
- introduce automatic cohort progression
- introduce automatic cohort release
- introduce automatic rollback
- introduce automatic resumption
- introduce automatic retry of ambiguous mutation
- grant Agent/MCP mutation authority
- grant scheduler/event mutation authority
- introduce parallel initial production campaign mutations
- introduce arbitrary/multi-parameter production mutation
- introduce generic vendor commands
- introduce arbitrary endpoints
- introduce SSH/CLI
- introduce cross-target campaigns
- introduce Nokia production write
- place vendor-write credentials in the application
- infer an Ericsson write protocol
- bypass Phase 16 or Phase 17
- treat software existence as production authorization
- start Phase 19
- create V19
- create an implementation specification or evidence map

---

# 5. Fundamental Authority Model

The following inequalities are architectural invariants, not slogans:

```text
CAMPAIGN AUTHORITY != MUTATION AUTHORITY
CAMPAIGN RELEASE != PRODUCTION EXECUTION GRANT
CAMPAIGN LEASE != PRODUCTION EXECUTION LEASE
FORWARD SAFETY BUDGET != MUTATION AUTHORITY
RECOVERY SAFETY BUDGET != ROLLBACK AUTHORITY
VENDOR ACCEPTED != PRODUCTION VERIFIED
PRODUCTION VERIFIED != CANONICAL RECONCILED
CANONICAL RECONCILED != OBSERVATION HEALTHY
OBSERVATION HEALTHY != HUMAN COHORT RELEASE
ABORT != ROLLBACK
SAFETY SUSPENSION != FORWARD PROGRESSION CLOSURE
RECOVERY SUCCESS != FORWARD PROGRESSION REOPENED
SOFTWARE CAPABILITY != EXTERNAL CERTIFICATION
EXTERNAL CERTIFICATION != PRODUCTION AUTHORIZATION
UNKNOWN / STALE / ERROR = DENY
```

Campaign orchestration never creates authority to mutate the network.

---

# 6. Bounded Context and Conceptual Services

Conceptual bounded context:

```text
com.simba.snip.npo.productioncampaign
```

Conceptual services (architecture only; Java classes are **not** created by this document):

- `ProductionCampaignService`
- `CampaignRevisionService`
- `CampaignReviewService`
- `CampaignAuthorizationService`
- `CampaignReleaseService`
- `CampaignProgressionService`
- `CampaignHealthService`
- `CampaignSafetyService`
- `CampaignSuspensionService`
- `CampaignAbortService`
- `CampaignRecoveryService`
- `CampaignLeaseService`
- `CampaignEvidenceService`
- `CampaignAuditService`

These names identify responsibilities. They do not authorize a package layout, class set, or persistence schema.

---

# 7. Trust Boundaries and Dependency Direction

```text
Phase 13 → Phase 14 → Phase 15 → Phase 16 governance
  → Phase 18 campaign orchestration (application / control plane)
  → Phase 16 production authority / grant / lease
  → Production Write Gateway (final mutation boundary)
  → certified Phase 17 resolver / adapter / transport
  → vendor interface (UNRESOLVED for production Ericsson write)
```

Trust boundaries:

1. Campaign application/control plane — orchestration, review, release, health, safety accounting. **No vendor write credentials.**
2. Phase 16 production-change plane — individual grant, consume, lease, expected-state, verification, rollback governance.
3. Production Write Gateway — final preflight, campaign-binding enforcement for `executionOrigin = PRODUCTION_CAMPAIGN`, credential resolution, vendor send.
4. Phase 17 certification/onboarding — currentness only; not campaign progression.
5. Phase 12 canonical reconciliation — authoritative synchronization path; not campaign mutation.
6. Humans — review, authorize, release, pause, abort, resume, rollback request.

Dependency direction is downward only. The gateway is not the campaign progression engine. The campaign plane is not a grant writer.

---

# 8. ProductionChangeCampaign Aggregate

A `ProductionChangeCampaign` is a revisioned aggregate.

Once authorized, a campaign revision is **immutable**.

Material change requires:

```text
new revision
  → new fingerprint
  → new review
  → new authorization
```

One campaign binds **exactly one** `ProductionNetworkTarget`.

Campaign authorization freezes the complete `AuthorizedCampaignScope`. That scope identifies every intended campaign item and **MUST NOT** be enlarged after authorization. Removal, replacement, mutation, or reordering of an authorized item stales existing campaign authority and requires new governance.

Every authorized campaign item **MUST** eventually receive a governed terminal accounting. A campaign **MUST NOT** complete by silently leaving authorized items unreleased or unaccounted.

Campaign validity **MUST NOT** resurrect stale, revoked, or otherwise non-executable authoritative Phase 14, Phase 15, or Phase 16 state.

---

# 9. CampaignExecutionItem

A campaign item references existing governed lineage. It does **not** create arbitrary mutation authority.

Bind or reference as applicable:

- Phase 14 plan ID / version / fingerprint
- Phase 15 execution ID / fingerprint
- Phase 16 production change ID / fingerprint
- `ProductionNetworkTarget`
- cell
- parameter
- expected value
- desired value
- exact governed rollback value

Campaign mutation identity **MUST** bind target, cell, parameter, expected value, desired value, and exact governed rollback value.

Mutation equality **MUST** use canonical typed value and unit semantics across campaign, Phase 16, and gateway boundaries.

Initial Phase 18 **prohibits** duplicate `(productionTargetId, cellId, parameter)` within one campaign revision. Sequential repeated forward changes to the same target/cell/parameter in one revision are deferred.

---

# 10. Campaign Fingerprint

The campaign fingerprint is deterministic and binds at least:

- campaign ID
- revision
- objective
- external change-control reference
- target
- vendor
- platform
- environment
- network domain
- ordered campaign items
- ordered cohorts
- item fingerprints
- cell
- parameter
- expected value
- desired value
- rollback value
- Phase 14 identities / fingerprints
- Phase 15 identities / fingerprints
- Phase 16 identities / fingerprints
- Phase 17 transport profile
- certification
- target onboarding
- safety policy identity / version
- progression policy identity / version
- health policy identity / version
- observation policy identity / version
- assurance policy identity / version where applicable
- verification policy
- recovery / rollback policy
- windows
- rate limits
- blast limits
- authorization generation

Material change makes existing authority stale. Material fingerprint mismatch denies progression.

Canonicalization **MUST**:

- preserve governed order for ordered collections
- apply deterministic canonical ordering for set-like collections
- distinguish null, absent, and empty
- use canonical typed values and units
- be independent of locale, process, database result ordering, and platform-specific serialization

Campaign objective text is descriptive governance metadata only. It is **NEVER** executable input.

External change-ticket currentness **MUST** be represented explicitly and **MUST NOT** be silently inferred from reference presence alone.

Material health-policy or observation-policy change **MUST** invalidate future progression where existing authority no longer binds the governing policy. Material policy change **MUST NOT** rewrite historical execution or observation evidence.

---

# 11. Cohorts and Progressive Delivery

Cohorts are **release boundaries**. They are not batch vendor mutation commands.

Initial production behavior:

```text
canary
  → one cell
  → individual execution
  → vendor verification
  → canonical reconciliation
  → governed observation
  → health evaluation
  → human decision
  → later cohort release
```

Every initial Phase 18 production campaign **MUST** begin with a mandatory canary of exactly one cell. A canary remains subject to the same individual Phase 16/17 production controls as every later item.

Production mutation concurrency remains exactly one. Campaign items execute sequentially. A subsequent item **MUST NOT** become send-eligible until all predecessor conditions required by policy are satisfied. A subsequent cohort **MUST NOT** become progression-eligible until the preceding cohort satisfies required verification, reconciliation, observation, and safety conditions.

A later cohort **cannot** be validly pre-released. A human release authorizes only the specifically governed cohort and does **not** pre-authorize future cohorts.

The only executable production progression mode in initial Phase 18 is **MANUAL**. Phase 18 **MUST NOT** introduce an executable dormant or hidden automatic production progression mode.

Canary success **MUST NOT** be represented as statistical proof that later campaign cells will behave identically, nor as statistical representativeness of later campaign scope.

Unreleased campaign work possesses **no** production mutation capability. Phase 16 production grants **MUST NOT** be pre-minted for unreleased future campaign items. A production grant **MUST** be created only sufficiently late for an individually eligible item after current campaign safety has been revalidated.

A released item still requires individual production eligibility and Phase 16 authority before mutation. Duplicate human release requests **MUST** be idempotent and **MUST NOT** duplicate mutation capability.

---

# 12. CohortReleaseFingerprint

`CohortReleaseFingerprint` is immutable once the cohort is released. It binds at least:

- `campaignId`
- `campaignRevision`
- `campaignFingerprint`
- `cohortId`
- ordered item IDs
- ordered item fingerprints
- `releaseGeneration`
- `campaignControlGeneration`
- `campaignFencingToken`
- applicable network-health evidence digest
- applicable operational-safety evidence digest
- `authorizationGeneration`
- `releasedBy`
- `releasedAt`
- `releaseFingerprint`

Released membership is frozen. Add, remove, replace, or reorder requires new governed authority. Released cohort membership **MUST NOT** be enlarged, reduced, replaced, or reordered under the existing release authority.

---

# 13. CampaignExecutionBinding

`CampaignExecutionBinding` is a critical Phase 18 security primitive.

It is immutable and tamper-evident once created for an eligible/released campaign item. It binds at least:

- `campaignId`
- `campaignRevision`
- `campaignFingerprint`
- `campaignControlGeneration`
- `cohortId`
- `cohortReleaseFingerprint`
- `releaseGeneration`
- `itemId`
- `itemSequence`
- `itemFingerprint`
- `campaignFencingToken`
- `productionTargetId`
- Phase 14 plan ID / version / fingerprint
- Phase 15 execution ID / fingerprint
- Phase 16 `productionChangeId`
- Phase 16 production fingerprint
- `cellId`
- `parameter`
- `expectedValue`
- `desiredValue`
- `rollbackValue`
- `bindingCreatedAt`
- `bindingDigest`

Require:

```text
CampaignExecutionBinding mutation identity
  == Phase 16 authorized mutation identity
  == Phase 16 grant mutation identity
  == Gateway mutation identity
  == Vendor adapter mutation identity
```

Mismatch = **DENY**.

---

# 14. Gateway Campaign-Binding Enforcement

Application-side validation is **insufficient**.

For authoritative `executionOrigin = PRODUCTION_CAMPAIGN`, the Production Write Gateway **MUST** independently establish trustworthy campaign execution context and independently verify the campaign execution binding.

Missing, invalid, stale, substituted, or mismatched campaign binding: **DENY**.

A caller-supplied self-asserted JSON object is **not** sufficient trust. The exact authoritative-state lookup or cryptographic attestation mechanism is deferred to the implementation specification. This architecture does **not** invent that wire protocol.

The campaign application never receives vendor write credentials. Application-side campaign eligibility does **not** substitute for gateway final preflight. The gateway remains the final trusted mutation boundary.

---

# 15. Standalone Anti-Downgrade

A Phase 16 production change already bound to a campaign **cannot** later be executed as standalone work merely to omit campaign controls.

Execution origin **MUST NOT** be caller-forgeable and **MUST NOT** be a mechanism for bypassing campaign controls.

```text
campaign-bound productionChangeId
  + standalone execution origin
  = DENY
```

---

# 16. Cardinality

Require:

```text
one campaign-originated Phase 16 execution
  → exactly one CampaignExecutionBinding
  → exactly one campaign item
```

and:

```text
one campaign item
  → at most one active forward production execution lineage
```

Replay or crash recovery **MUST NOT** create duplicate execution authority or duplicate active forward execution lineages.

---

# 17. Campaign Lease and Fencing

A durable campaign progression lease conceptually contains:

- `campaignId`
- `holderId`
- `fencingToken`
- `acquiredAt`
- `expiresAt`

Campaign lease protects **orchestration ownership**. It is **NOT** the Phase 16 production execution lease.

Lost campaign fencing before `MAY_HAVE_SENT` means **NO NEW CAMPAIGN-ORIGINATED SEND**.

The campaign fencing token **MUST** be bound to the production execution handoff. A stale campaign fencing token **MUST** deny a new campaign-originated production send. Current fencing **MUST** be validated at a trusted production boundary.

---

# 18. CampaignControlGeneration

`CampaignControlGeneration` is a durable, non-reusable campaign safety epoch.

It advances or becomes authoritatively obsolete on applicable:

- abort
- safety suspension
- campaign authorization revocation
- material campaign invalidation
- certification revocation
- onboarding revocation
- security-profile revocation
- credential-profile revocation
- external change-control invalidation where current authority is required
- revision invalidation
- material health/observation policy invalidation

A stale generation can **never** become current again. No ABA reuse. Campaign control and release generations are **never** reused or reset such that stale authority can become current again.

`CampaignExecutionBinding` carries the governing generation. Current campaign control generation **MUST** be checked at a trusted production pre-send boundary. Abort **MUST** advance or otherwise authoritatively invalidate the campaign execution/control generation before any later new send.

---

# 19. ReleaseGeneration

Unused release authority is **not** perpetual. Release generations are non-reusable.

Applicable suspension, abort, revocation, material invalidation, authorization invalidation, or policy invalidation makes unused release authority stale.

Safety suspension invalidates unused cohort release authority. Applicable revocation invalidates unused cohort release authority. Unused release authority becomes stale when its bound campaign authorization generation is no longer current.

Resumption **never** resurrects an old release. Unexecuted work requires a new explicit human release and a new release generation.

---

# 20. CampaignExecutionHandoffEvidence

Durable campaign → Phase 16 handoff evidence binds at least:

- campaign
- revision
- cohort
- item
- `CampaignExecutionBinding` digest
- campaign fencing token
- campaign control generation
- release generation
- Phase 16 `productionChangeId`
- Phase 16 `executionId` where assigned
- handoff state
- handoff timestamp

Conceptual handoff states include at least:

```text
ELIGIBLE
HANDOFF_PENDING
HANDED_OFF
PRE_SEND
MAY_HAVE_SENT
VENDOR_ACCEPTED
VERIFYING
VERIFIED
```

Handoff is **not** send. `HANDED_OFF` does **not** imply external send.

Crash recovery **MUST** consult authoritative Phase 16 production execution state before determining whether another action is permissible. It **MUST NOT** create a speculative second execution. It **MUST NOT** infer successful external mutation from local intent or handoff state alone.

`UNKNOWN` = **BLOCK**. Failure to reconcile authoritative execution state after crash **MUST** block further progression. Duplicate release or recovery processing after restart **MUST** be idempotent.

---

# 21. MAY_HAVE_SENT Semantics

Before `MAY_HAVE_SENT`:

```text
invalidation → deny / cancel future send
```

At or after `MAY_HAVE_SENT`:

```text
invalidation
  → stop additional sends
  → preserve historical authority / evidence
  → continue mandatory outcome resolution
  → continue required verification / evidence collection where safe
  → suspend / stale campaign as applicable
```

Never rewrite `MAY_HAVE_SENT` into `NOT_SENT` without authoritative evidence.

An ambiguous child execution outcome **MUST** immediately block later campaign mutation. Phase 18 **MUST NOT** automatically retry an ambiguous production mutation.

A child outcome observed at desired state may become `VERIFIED` only through authoritative verification evidence. A child outcome observed at expected pre-change state **MUST NOT** automatically cause a retry; any later attempt requires separately governed authority. A third/unexpected observed state **MUST** require manual intervention or equivalent fail-closed governance. Verification/readback unavailable after a possible send **MUST** remain unresolved and **MUST** block progression.

Safety invalidation after `MAY_HAVE_SENT` **MUST NOT** terminate mandatory outcome resolution for the possibly sent operation.

Historical outcome resolution uses its bound historical evidence/policy identity while current authoritative policy governs future progression.

---

# 22. Runtime Currentness and Final Send Eligibility

Every production send **MUST** revalidate:

- global production enablement
- target enablement and target operational state
- campaign enablement and campaign non-aborted / non-suspended state
- current campaign revision and fingerprint
- campaign authorization currentness
- campaign lease ownership and fencing currentness
- cohort release currentness
- item eligibility
- campaign and production-change windows
- external change-control currentness where policy requires it
- applicable Phase 17 certification currentness
- target onboarding currentness
- security-profile and credential-profile currentness
- applicable blast-radius and rate-limit safety
- Phase 16 production authorization currentness
- a current legal Phase 16 production execution grant
- a current Phase 16 execution lease and fencing authority

The Production Write Gateway independently performs final production preflight and remains the final trusted mutation boundary.

`UNKNOWN`, `STALE`, `ERROR`, unavailable, or unverifiable required currentness **MUST** deny a new production send. Campaign progression **MUST** stop when required predecessor state is `UNKNOWN`, `STALE`, `ERROR`, unresolved, or otherwise non-progressable.

---

# 23. Campaign Item Lifecycle

Architecturally distinguish at least:

```text
PLANNED
RELEASED
ELIGIBLE
HANDOFF_PENDING
HANDED_OFF
PRE_SEND
MAY_HAVE_SENT
VENDOR_ACCEPTED
VERIFYING
PRODUCTION_VERIFIED
RECONCILIATION_PENDING
CANONICAL_RECONCILED
OBSERVING
OBSERVATION_HEALTHY
COMPLETED
```

Exceptional states include at least:

```text
BLOCKED
STALE
SUSPENDED
NOT_SENT
VENDOR_REJECTED
VERIFICATION_FAILED
OUTCOME_UNRESOLVED
RECOVERY_REQUIRED
MANUAL_INTERVENTION_REQUIRED
```

Do not collapse:

```text
VENDOR_ACCEPTED != PRODUCTION_VERIFIED
PRODUCTION_VERIFIED != CANONICAL_RECONCILED
CANONICAL_RECONCILED != OBSERVATION_HEALTHY
OBSERVATION_HEALTHY != COMPLETED
```

Vendor acceptance **MUST NOT** constitute successful production verification. Successful production execution **MUST** require independent direct vendor readback according to inherited Phase 16/17 verification policy.

`PRODUCTION_VERIFIED` remains distinct from `CANONICAL_RECONCILED`. Phase 18 **MUST NOT** directly mutate Phase 12 canonical network state to make campaign execution appear reconciled. Canonical reconciliation **MUST** occur through the authoritative synchronization path.

Verification mismatch **MUST** block further forward campaign progression. Unresolved production outcome **MUST** block further campaign mutation.

The exact executable transition matrix belongs to the implementation specification (A18-R3-02).

---

# 24. PostChangeObservationBoundary

A governed `PostChangeObservationBoundary` binds at least:

- `campaignId`
- `campaignRevision`
- `cohortId`
- `itemId`
- `productionChangeId`
- `executionId`
- `cellId`
- `parameter`
- `verifiedObservedValue`
- `vendorVerifiedAt`
- `observationStart`
- `observationEnd`
- `requiredTelemetrySource`
- `minimumMeasurementTime`
- `minimumSourceWatermark`
- `maximumAcceptedIngestionLag`
- required canonical checkpoint / version / watermark
- `healthPolicyVersion`
- `observationPolicyVersion`
- `boundaryDigest`

Initial Phase 18 requires:

```text
observationStart >= vendorVerifiedAt
```

Initial Phase 18 post-change observation begins no earlier than successful direct vendor verification.

Telemetry ingestion time alone is **not** proof of post-change measurement. Campaign progression **MUST** require a governed post-change observation interval where policy requires observation. Observation timeout **MUST** block progression rather than infer success. Required observation evidence **MUST** be bound to the relevant campaign item and production execution.

---

# 25. Observation Provenance

Progression evidence **MUST** establish as required:

- correct network object
- correct telemetry source
- measurement belongs to the governed post-change interval
- required source watermark reached
- acceptable ingestion delay
- required canonical checkpoint reached
- applicable policy versions
- absence of unresolved interference invalidating the evidence

If required provenance cannot be established:

```text
NetworkObservationHealth = UNKNOWN or STALE
```

and progression is blocked.

Post-change telemetry **MUST** satisfy explicit freshness requirements. Stale telemetry **MUST NOT** satisfy campaign health progression. Telemetry unavailable or provenance unavailable **MUST** produce non-progressable health rather than assumed health. Campaign progression **MUST** use current health evidence and **MUST NOT** reuse stale prior `HEALTHY` state.

Absence of detected same-parameter interference is **not** represented as proof of exclusive causal attribution. Do not claim exclusive causality merely because no same-parameter interference was detected.

---

# 26. External Interference

`ExternalInterferenceDetected` is first-class durable safety evidence.

Potential triggers include:

- unexpected pre-send state
- unexpected post-verification state
- canonical drift
- unexpected additional parameter transition
- conflicting authoritative change reference
- incompatible authoritative network state

Same target/cell/parameter interference during the governed interval invalidates attribution **by default**.

Detected external interference **MUST** invalidate affected campaign progression until authoritative certainty is restored.

Superseded conflicting observation or reconciliation evidence **MUST NOT** satisfy progression merely because it was previously valid. Superseded evidence cannot later be reused merely because it was once valid.

---

# 27. Dual Health Model

Separate:

```text
NetworkObservationHealth:
  HEALTHY
  DEGRADED
  UNHEALTHY
  UNKNOWN
  STALE
```

from:

```text
OperationalSafetyHealth:
  SAFE
  UNSAFE
  UNKNOWN
  STALE
```

Initial progression requires:

```text
NetworkObservationHealth == HEALTHY
AND
OperationalSafetyHealth == SAFE
```

Only `HEALTHY` network-observation state permits initial Phase 18 progression. A healthy KPI **cannot** override unsafe certification, authorization, target, audit, security, credential, window, rate/blast, or other operational state.

Assurance conditions required by campaign policy **MUST** be evaluated before progression. `CANONICAL_RECONCILED` remains distinct from post-change observation health. Healthy observation creates at most progression eligibility; an explicit human release is still required.

---

# 28. Human Governance and Separation of Duties

Architecturally required permissions cover at least:

- `CAMPAIGN_VIEW`
- `CAMPAIGN_CREATE`
- `CAMPAIGN_REVIEW`
- `CAMPAIGN_AUTHORIZE`
- `RELEASE_COHORT`
- `PAUSE`
- `ABORT`
- `VIEW_EVIDENCE`
- resumption review
- resumption authorization

Preserve at least:

```text
creator != campaign authorizer
```

and independent governed release/resumption authority. Campaign authorization and production cohort release **MUST** be separated so campaign authorization does not automatically release production work.

Human campaign authority derives from authenticated actor identity, not caller-supplied principal fields. Null, blank, malformed, unknown, or unauthorized governance principals fail closed.

Agent, MCP, service, and scheduler identities **cannot** masquerade as humans.

Campaign authorization **MUST NOT** execute a production mutation. Campaign creation **MUST NOT** execute a production mutation. Opening a campaign or change window **MUST NOT** execute a production mutation.

Campaign governance actions **MUST** have durable idempotency/replay protection so duplicate requests cannot create duplicate authority. Human campaign review, authorization, release, pause, abort, and resumption governance **MUST** have durable idempotency/replay protection.

---

# 29. Resumption

Resumption binds at least:

- campaign / revision
- suspension
- suspension cause
- remediation evidence
- current fingerprint
- current certification / onboarding / security state
- current operational safety
- reviewer
- authorizer
- new applicable generation

Resumption requires explicit review and authorization.

Resumption does **NOT**:

- release a cohort
- restore stale release authority
- override an unresolved production outcome
- reopen `ForwardProgressionClosure`

Campaign resumption **MUST NOT** make progression executable while a blocking production outcome remains unresolved.

---

# 30. Abort

Abort means:

```text
NO ADDITIONAL FORWARD MUTATIONS.
```

Abort is **not** rollback. Abort does **not** itself constitute rollback authorization and does **not** automatically initiate rollback.

Abort advances or invalidates applicable campaign control authority before any later new send.

If `MAY_HAVE_SENT` has occurred, mandatory outcome resolution continues.

---

# 31. Forward Safety Exposure

Forward campaign safety exposure is **not** mutation authority.

Forward safety check + reservation **MUST** eventually be implemented atomically in durable shared state.

Conceptual reservation states may include:

```text
RESERVED
CONSUMED
RELEASED
EXPIRED
```

`MAY_HAVE_SENT` conservatively consumes exposure. Safety-budget exposure reaching `MAY_HAVE_SENT` **MUST NOT** be blindly returned. Capacity cannot be returned merely because a timer expired. Uncertain external exposure **MUST** be accounted for conservatively.

Campaign safety controls **MUST** be durable/shared and **MUST NOT** depend solely on process-local state. Campaign safety **MUST** enforce a policy-defined maximum campaign scope. Campaign implementation **MUST** additionally have a hard maximum campaign-size ceiling independent of mutable runtime policy. Effective campaign-size allowance **MUST** never exceed the stricter applicable hard or policy limit.

Campaign safety **MUST** bound cells, parameters, and mutation operations; bound production mutation rate over governed time intervals; track verification failures; track unresolved/ambiguous production outcomes; and support policy-defined consecutive-failure stopping conditions.

Rate and blast-radius enforcement **MUST** be concurrency-safe. Safety-limit failure or inability to establish authoritative counters **MUST** fail closed.

Automatic safety suspension is permitted when a safety condition becomes non-progressable. Automatic safety suspension **MUST NOT** itself initiate a production mutation, automatically initiate rollback, or automatically resume the campaign.

Typed machine-readable safety and eligibility reason codes **MUST** be available so clients do not infer safety state from free-form text.

Campaign safety-budget reservation **MUST NOT** constitute mutation authority.

---

# 32. Recovery Safety Exposure

`CampaignSafetyExposure` separates at least:

- `forwardMutationExposure`
- `recoveryMutationExposure`
- `distinctCellsExposed`
- `unresolvedForwardExposure`
- `unresolvedRecoveryExposure`

A separately governed `RecoverySafetyBudget` exists.

```text
RecoverySafetyBudget != rollback authority
ForwardSafetyBudget != rollback authority
```

Recovery capacity **cannot** be borrowed for forward mutation. Forward capacity **cannot** authorize recovery.

A recovery operation is still a production mutation. Campaign safety **MUST** track recovery/rollback events. A recovery mutation reaching `MAY_HAVE_SENT` conservatively consumes recovery exposure.

---

# 33. Recovery Reservation

Recovery safety check + reservation **MUST** be atomic in the future implementation.

Reservation can release or expire capacity only when authoritative execution evidence proves the associated operation did not reach `MAY_HAVE_SENT`. Uncertainty retains conservative exposure.

Exact numeric limits are deferred to the implementation specification.

---

# 34. Combined Mutation Concurrency

Initial Phase 18 requires:

```text
active forward production mutations
  + active recovery production mutations
  <= 1
```

Do **not** model one forward slot plus one independent recovery slot. Phase 18 **MUST NOT** introduce parallel production campaign mutation under the initial production model.

---

# 35. Recovery Scope

Initial campaign recovery may target only an object already within applicable governed campaign mutation exposure.

Recovery **cannot** be used to mutate unrelated cells.

Recovery uses the exact governed rollback value inherited from the existing lifecycle. No invented recovery value.

Campaign recovery planning **MUST NOT** itself constitute rollback authority. Every actual rollback **MUST** remain separately requested, reviewed, authorized, executed, and verified through authoritative production controls.

Rollback verification remains distinct from vendor acknowledgement. Recovery ordering may be represented, including reverse-order recovery where governed, but representation **MUST NOT** authorize the constituent rollbacks. Recovery state **MUST** be durably represented and reconstructible after crash.

---

# 36. No Recursive Recovery

Failed or ambiguous rollback **MUST NOT** automatically trigger:

- retry
- second rollback
- rollback-of-rollback

It returns to human governance. Phase 18 **MUST NOT** automatically execute rollback and **MUST NOT** automatically retry rollback. Ambiguous rollback outcome **MUST** block additional campaign-associated mutation and return to human governance.

---

# 37. ForwardProgressionClosure

`ForwardProgressionClosure` is durable and monotonic.

Once a campaign item enters `RECOVERY_REQUIRED`, or a campaign-associated rollback reaches authorized execution:

```text
ForwardProgressionClosure: OPEN → CLOSED
```

For the same campaign revision:

```text
CLOSED → OPEN
```

is **forbidden**.

Successful recovery does **not** reopen forward progression for that campaign revision.

---

# 38. Suspension versus Forward Closure

Distinguish:

```text
SAFETY_SUSPENSION
```

from:

```text
FORWARD_PROGRESSION_CLOSED
```

Operator pause is distinguishable from safety suspension.

Safety suspension may be remediated through governed review/authorization. `ForwardProgressionClosure` is permanent for that campaign revision.

Successful rollback does **not** reopen it. Human resumption does **not** reopen it.

---

# 39. Recovery Finalization and New Optimization

After forward progression is closed, allowed campaign activity is limited to safe governance/finalization including:

- outcome resolution
- recovery planning
- recovery request / review / authorization
- recovery execution
- recovery verification
- canonical reconciliation
- post-recovery observation
- evidence
- audit
- final disposition

No next forward item. No next forward cohort.

Later forward optimization requires **new** governed authority based on current authoritative state:

```text
new campaign revision or campaign
  → new fingerprint
  → new review
  → new authorization
  → new applicable canary
  → new releases
  → new execution bindings
```

Old releases/bindings **cannot** be inherited as mutation authority.

---

# 40. Campaign Completion

Completion **MUST** account for **EVERY** item in immutable `AuthorizedCampaignScope`.

A campaign **MUST NOT** become `COMPLETED` while an authorized item remains silently `PLANNED`/unreleased. No successful final campaign state may silently omit an item from immutable authorized campaign scope. Campaign completion **MUST NOT** conceal unresolved mutation or recovery state.

Strong completion requires as applicable:

- every authorized item terminally accounted for
- no unresolved production outcome
- required vendor verification complete
- required canonical reconciliation complete
- required observation complete
- health criteria satisfied
- audit integrity valid

Possible final dispositions include:

```text
COMPLETED
ABORTED_NO_RECOVERY_REQUIRED
ABORTED_RECOVERY_REQUIRED
RECOVERED
PARTIALLY_RECOVERED
MANUAL_INTERVENTION_REQUIRED
EXPIRED
```

Campaign final disposition **MUST** distinguish complete recovery, partial recovery, unresolved/manual-intervention, abort, and expiry semantics as applicable.

Exact executable transition semantics are deferred to the implementation specification.

---

# 41. Kill Hierarchy

Preserve the independent hierarchy:

```text
global production state
  → target state
  → campaign state
  → cohort release
  → item eligibility
  → Phase 16 production authority
  → gateway final preflight
```

These are independent hierarchical controls rather than substitutes for one another.

The gateway remains the final production mutation authority boundary.

Kill/suspension **MUST** stop new sends but **MUST NOT** claim to cancel a vendor request already in flight.

---

# 42. Revocation Cascade

Applicable Phase 17:

- interface-definition withdrawal
- transport-profile invalidation
- certification revocation / expiry / required staleness
- onboarding revocation / expiry
- security-profile invalidation
- credential-profile invalidation

**MUST** propagate into campaign safety.

Effects include as applicable:

- campaign `SUSPENDED` / `STALE`
- control generation invalidated
- unused release stale
- unreleased work blocked
- new production grants prohibited
- eligible unconsumed grants handled only through existing Phase 16 legal authority/state transitions
- gateway denies new send
- consumed grants/history unchanged
- `MAY_HAVE_SENT` outcome resolution continues

External change-control invalidation participates in campaign control-generation invalidation whenever current external change-control authority is required.

Revocation **MUST NOT** rewrite consumed Phase 16 grants or historical execution evidence.

Phase 18 does **NOT** create a second Phase 16 grant writer.

---

# 43. Evidence and Bidirectional Correlation

Campaign evidence **MUST** bind as applicable:

- campaign revision / fingerprint
- review
- authorization
- external change control
- target
- Phase 17 certification
- transport profile
- onboarding
- cohorts
- release fingerprints
- lease / fencing
- control generation
- execution bindings
- handoff evidence
- forward safety exposure
- recovery safety exposure
- Phase 16 production changes
- execution / grant identities
- gateway attempts
- vendor outcomes
- verification
- reconciliation
- observation boundary
- health
- interference
- pause / suspension
- abort
- `ForwardProgressionClosure`
- recovery
- rollback
- final disposition

No secrets. Campaign evidence and audit **MUST NOT** contain vendor credentials or secrets.

Require bidirectional correlation:

```text
Campaign
  → Cohort
  → Item
  → CampaignExecutionBinding
  → Phase16 ProductionChange
  → Execution
  → Grant
  → Gateway Attempt
  → Vendor Outcome
  → Verification
```

and:

```text
Phase16 campaign-originated execution
  → CampaignExecutionBinding
  → Item
  → Cohort
  → Campaign Revision
```

A valid unrelated production execution **cannot** be substituted. Campaign audit/evidence **MUST** cryptographically bind the relevant Phase 16 execution fingerprint/identity. Campaign-originated production execution evidence **MUST** link back to the applicable campaign execution binding.

Historical execution, verification, reconciliation, health, suspension, abort, and recovery evidence **MUST** remain distinguishable rather than overwritten into a simplified final state.

Required active/historical campaign evidence has retention and referential-integrity semantics sufficient to make missing evidence detectable and fail closed where required.

---

# 44. Tamper-Evident Campaign Audit

Use campaign-scoped tamper-evident audit architecture containing concepts such as:

- `campaignId`
- `sequence`
- `previousEventHash`
- `eventHash`
- `eventType`
- `actor`
- `timestamp`
- `canonicalPayloadDigest`

Require deterministic canonicalization and concurrency-safe ordering.

```text
INVALID
UNKNOWN
GAP
HASH_MISMATCH
```

block progression.

Claim: **tamper-evident**.

Do **NOT** claim immutable storage unless independently provided.

Required missing evidence must be detectable and fail closed where necessary.

---

# 45. Agents, MCP, Schedulers and Events

Agents may:

- observe
- summarize
- explain
- recommend

Agents may **NOT**:

- act as human reviewer
- authorize campaign
- release cohort
- authorize resumption
- mint production grant
- execute production mutation
- authorize rollback
- execute rollback

MCP has equivalent restrictions.

Schedulers may perform safe control-plane operations such as:

- expiry
- freshness evaluation
- health evaluation
- safety suspension

Schedulers may **NOT**:

- release cohort
- create mutation authority
- execute mutation
- resume campaign
- execute rollback

Events cannot trigger production mutation.

No closed-loop production autonomy.

Agent identities **MUST NOT** satisfy human campaign review, authorization, release, resumption, execution, or rollback authority. MCP identities **MUST NOT** satisfy those authorities. Scheduler activity **MUST NOT** authorize or release a cohort or initiate a production mutation. Event consumption **MUST NOT** authorize or release a cohort or initiate a production mutation.

---

# 46. Credential and Network Boundary

Phase 10 remains authoritative. Phase 10 credential late-resolution and no-secret-persistence requirements remain authoritative.

Campaign components:

- do not resolve vendor write credentials
- do not persist vendor write credentials
- do not log vendor write credentials
- do not expose vendor write credentials
- do not broaden production egress

Do not introduce:

- arbitrary host
- arbitrary URL
- arbitrary port
- `0.0.0.0/0`
- SSH
- CLI
- generic command execution

The Production Write Gateway remains the vendor-write credential/session boundary.

---

# 47. Vendor Protocol Boundary

The architecture explicitly preserves:

```text
ERICSSON PRODUCTION WRITE PROTOCOL = UNRESOLVED
ERICSSON PRODUCTION TRANSPORT = UNCONFIGURED / NOT IMPLEMENTED
ERICSSON PRODUCTION ENDPOINT = NONE / NOT CONFIGURED
ERICSSON PRODUCTION AUTH METHOD = UNRESOLVED / EXTERNALLY CONFIGURED
ERICSSON PRODUCTION CREDENTIAL = NONE
NOKIA = DEFERRED
```

Phase 18 **MUST NOT** infer, guess, invent, or embed an Ericsson production-write protocol, endpoint, credential mechanism, CLI, SSH operation, SDK call, or generic command mechanism.

No campaign API or internal contract **SHALL** accept arbitrary vendor commands, arbitrary endpoints, or free-form production mutation payloads.

This architecture does **not** invent a protocol, endpoint, authentication mechanism, command syntax, or vendor API.

---

# 48. Deployment Boundary

Preserve:

```text
campaign orchestration  → application / control plane
production mutation     → separate Production Write Gateway
vendor write credential → gateway boundary only
```

Campaign orchestration remains in the application/control plane and **MUST NOT** move vendor-write credentials into that plane.

The gateway is **not** the campaign progression engine.

The application does **not** possess production vendor-write credentials.

---

# 49. External Readiness and Certification

Preserve Phase 17 readiness:

```text
P17 L1 = NOT EXECUTED
P17 L2 = NOT EXECUTED
P17 L3 = NOT SATISFIED
P17 L4 = NOT SATISFIED
```

Phase 18 conceptual campaign qualification:

```text
C0 = software correctness
C1 = simulator qualification
C2 = vendor-lab campaign qualification
C3 = pre-production progressive-delivery qualification
C4 = target-specific campaign operational approval
```

Current state:

```text
P18 C1 = NOT EXECUTED
P18 C2 = NOT EXECUTED
P18 C3 = NOT EXECUTED
P18 C4 = NOT SATISFIED
REAL PRODUCTION EXECUTION = NOT AUTHORIZED
REAL PRODUCTION CAMPAIGN EXECUTION = NOT AUTHORIZED
```

Campaign software correctness qualification remains distinct from vendor-lab qualification. C1, C2, C3, and C4 remain distinct from one another and do **not** replace Phase 17 L-level transport/target readiness.

Phase 18 **MUST NOT** claim L1, L2, L3, L4, C1, C2, C3, or C4 evidence unless independently executed/satisfied.

Default CI **MUST** remain independent of Azure credentials, vendor credentials, real vendor endpoints, and external certification environments.

Software existence never implies production authorization. Real production campaign execution **MUST** remain unauthorized until all inherited external certification, target-onboarding, production-authorization, and Phase 18 readiness requirements are independently satisfied.

---

# 50. Failure Model

Preserve:

> When SNIP loses certainty, SNIP loses permission to progress, not responsibility to resolve an already possible external mutation.

Before `MAY_HAVE_SENT` uncertainty:

```text
DENY NEW SEND
```

After `MAY_HAVE_SENT` uncertainty:

```text
STOP NEW SENDS
  → RESOLVE OUTCOME
  → RECORD EVIDENCE
  → REQUIRE HUMAN GOVERNANCE
```

The architecture **MUST** fail closed for required uncertainty involving at least:

- database unavailable
- campaign lease unavailable
- stale fencing
- stale control generation
- stale release generation
- invalid campaign authorization
- campaign revision mismatch
- fingerprint mismatch
- campaign binding mismatch
- standalone downgrade attempt
- campaign aborted
- campaign suspended
- `ForwardProgressionClosure`
- target disabled
- global kill
- certification stale/revoked
- onboarding stale/revoked
- security profile invalid
- credential profile invalid
- external change-control invalid where required
- campaign window closed
- production-change window closed
- safety budget unavailable
- forward budget exhausted
- recovery budget unavailable
- rate limit exhausted
- blast limit exceeded
- audit invalid
- required evidence missing
- precondition mismatch
- vendor reject
- verification mismatch
- outcome unknown
- reconciliation unavailable
- telemetry unavailable
- telemetry stale
- observation incomplete
- external interference unresolved
- health unknown
- safety health unsafe/unknown
- crash/recovery uncertainty
- duplicate release
- duplicate execution lineage
- ambiguous recovery

`UNKNOWN` / `STALE` / `ERROR` = **DENY** for new progression/send.

---

# 51. Conceptual Future Persistence

Future implementation is expected to require a migration such as:

```text
V19__phase18_production_change_campaigns.sql
```

and conceptual durable persistence for:

- campaign
- revision
- authorized scope
- item
- cohort
- review
- authorization
- release
- lease
- control generation
- release generation
- `CampaignExecutionBinding`
- handoff evidence
- health snapshots
- observation boundary
- interference evidence
- forward safety exposure
- recovery safety exposure
- `ForwardProgressionClosure`
- suspension
- resumption
- recovery
- evidence
- audit

This is conceptual architecture only.

**V19 is NOT CREATED by this document.** Final table count, SQL, constraints, and JPA entities belong to the later implementation specification and implementation.

---

# 52. High-Risk Future Implementation Evidence

The later implementation specification **cannot** satisfy high-risk safety requirements using structural evidence alone.

Future behavioral, integration, database, security, concurrency, and failure-injection evidence will be required as appropriate for at least:

- `CampaignExecutionBinding` enforcement
- gateway campaign-binding enforcement
- stale fencing
- campaign control generation
- release-generation invalidation
- anti-downgrade
- one-binding / one-item cardinality
- duplicate execution prevention
- atomic forward safety reservation
- atomic recovery safety reservation
- `MAY_HAVE_SENT` exposure accounting
- crash recovery
- observation provenance
- stale / superseded evidence rejection
- external interference
- authorized-scope completion
- forward / recovery budget separation
- combined mutation concurrency
- `ForwardProgressionClosure` monotonicity
- recovery cannot reopen forward progression

Those tests are **not** implemented by this document.

A18-R3-01 and A18-R3-02 remain implementation-specification obligations and do not alter this accepted architecture.

---

# 53. Threat Catalogue

Authoritative threat definitions. Each identifier appears exactly once in this section.

**T18-01 — Per-change authorization bypass:** Campaign authority is incorrectly treated as sufficient authority to mutate the network without the required individual production authorization/grant.

**T18-02 — Post-authorization cell injection:** A cell is added to campaign scope after the campaign revision was reviewed/authorized.

**T18-03 — Post-release cohort enlargement:** A cohort gains additional items after human release.

**T18-04 — Item reordering:** Campaign item ordering is changed after authorization/release to alter progression semantics.

**T18-05 — Stale certification use:** A campaign progresses using expired, revoked, stale, or otherwise non-current Phase 17 certification.

**T18-06 — Stale onboarding use:** A campaign progresses using expired, revoked, stale, or otherwise non-current target onboarding.

**T18-07 — Window grandfathering:** A campaign or item is allowed to mutate after the applicable execution window closes because it was previously authorized/released.

**T18-08 — Duplicate item execution:** Replay, retry, crash recovery, or duplicate processing causes one campaign item to execute more than once.

**T18-09 — Concurrent orchestrators:** Multiple campaign orchestrators concurrently progress the same campaign without authoritative lease/fencing protection.

**T18-10 — Grant pre-minting or replay:** Production execution grants are created for unreleased future work or replayed beyond their legal single-use/currentness semantics.

**T18-11 — Progression after ambiguous outcome:** A later campaign item executes while an earlier production mutation outcome remains ambiguous or unresolved.

**T18-12 — Verification failure ignored:** Campaign progression continues after required independent production verification fails or mismatches.

**T18-13 — Stale telemetry accepted as healthy:** Old telemetry or health state is reused to satisfy post-change campaign observation.

**T18-14 — Automatic resumption:** A suspended campaign automatically resumes production progression when a condition appears healthy again.

**T18-15 — Abort triggers unauthorized rollback:** Campaign abort is incorrectly treated as authority to execute rollback.

**T18-16 — Recovery bypasses rollback governance:** Campaign recovery planning or orchestration executes rollback without separately governed rollback authority.

**T18-17 — Audit tampering:** Campaign audit/evidence is altered, reordered, removed, substituted, or made inconsistent without progression being blocked.

**T18-18 — Cross-target injection:** An item or execution for another production target is inserted into a single-target campaign.

**T18-19 — Mutation substitution:** Cell, parameter, expected value, desired value, or rollback value differs between the authorized campaign item and executed production mutation.

**T18-20 — Transport/certification revocation mid-campaign:** Phase 17 transport/certification authority becomes invalid after campaign authorization or release but before later production mutation.

**T18-21 — Credential/security revocation mid-campaign:** Security or credential profile becomes invalid while the campaign still holds unused progression authority.

**T18-22 — Kill-switch race:** A production send crosses the external mutation boundary despite a concurrent global, target, or campaign safety stop.

**T18-23 — Crash during mutation:** Application or gateway failure around the external send boundary causes incorrect inference of whether the vendor mutation occurred.

**T18-24 — Crash during progression:** Campaign orchestrator failure causes lost, duplicated, or incorrectly reconstructed progression authority.

**T18-25 — Duplicate cohort release:** Replayed or concurrent human release requests create duplicate release authority or duplicate production work.

**T18-26 — Agent/MCP release:** An Agent or MCP pathway acquires human cohort release or campaign authorization authority.

**T18-27 — Scheduler/event mutation:** A scheduler or event automatically releases, grants, or executes a production mutation.

**T18-28 — Rate-limit race:** Concurrent campaign operations exceed shared rate/blast safety limits because checks and reservations are non-atomic.

**T18-29 — Observation evidence substitution:** Telemetry/health evidence for another object, source, interval, or execution is attached to the campaign item.

**T18-30 — Reconciliation mistaken for verification:** Canonical synchronization is incorrectly treated as proof of immediate vendor mutation success.

**T18-31 — Completion with unresolved item:** Campaign reaches a successful final state while an authorized item has unresolved execution, verification, reconciliation, observation, or recovery state.

**T18-32 — Wrong rollback value:** Recovery uses a newly calculated, guessed, stale, or otherwise different value instead of the exact governed rollback value.

**T18-33 — Evidence bundle substitution:** A valid but unrelated execution, verification, health, certification, or evidence bundle is substituted for the campaign's actual lineage.

**T18-34 — Expired campaign authorization:** A later cohort executes under campaign authorization that is expired, revoked, stale, or otherwise non-current.

**T18-35 — Lost lease holder continues:** An orchestrator continues progression after losing its authoritative campaign lease/fencing ownership.

**T18-36 — Old revision progresses:** A superseded campaign revision continues to release or execute work.

**T18-37 — Cached health beyond freshness:** Previously healthy campaign evidence is reused after its permitted freshness interval.

**T18-38 — Release while suspended:** A cohort is released or executed while the campaign is safety-suspended.

**T18-39 — Safety-counter divergence:** Durable rate, blast, unresolved-outcome, failure, or recovery counters diverge so safety exposure is understated.

**T18-40 — Partial recovery reported as recovered:** Campaign is reported as successfully recovered although one or more required recovery outcomes remain incomplete, failed, ambiguous, or unverified.

**T18-41 — Campaign item substitution between release and execution:** The released campaign item is replaced by a different but otherwise valid Phase 16 production mutation before execution.

**T18-42 — Stale campaign holder crosses into Phase 16:** A campaign orchestrator loses its lease/fencing authority after eligibility evaluation but still initiates a production send.

**T18-43 — Old release reused after resumption:** A cohort release created before suspension/revocation is reused after remediation/resumption.

**T18-44 — Abort races with eligible execution:** A campaign abort occurs after an item becomes eligible but before external send, and stale authority continues execution.

**T18-45 — Pre-change telemetry accepted post-change:** Telemetry measured before the mutation but ingested afterward is incorrectly accepted as post-change observation evidence.

**T18-46 — External change corrupts attribution:** An independent network change during the observation interval invalidates attribution but campaign health continues as though the campaign were the sole relevant change.

**T18-47 — Safety budget returned after possible send:** Capacity is returned even though the operation reached or may have reached `MAY_HAVE_SENT`.

**T18-48 — Completion by omission:** Campaign completes by never releasing or terminally accounting for part of immutable authorized scope.

**T18-49 — Unrelated valid execution substituted:** A valid Phase 16 execution unrelated to the campaign item is attached as campaign execution evidence.

**T18-50 — Health-policy change without invalidation:** Material health-policy change occurs but existing progression authority/evidence remains treated as current.

**T18-51 — Duplicate target/cell/parameter:** A campaign revision contains multiple forward items for the same target, cell, and parameter, making expected-state and observation attribution ambiguous.

**T18-52 — External change-control becomes invalid:** External ticket/change authority ceases to be current before a later cohort but progression continues.

**T18-53 — Resumption resurrects stale authority:** Campaign resumption restores old release, binding, or execution authority instead of requiring current governance.

**T18-54 — Healthy KPI masks unsafe operation:** Network observation appears healthy while certification, security, target, audit, credential, or other operational safety state is unsafe.

**T18-55 — Recovery omitted from safety exposure:** Rollback/recovery mutation bypasses or disappears from campaign safety-exposure accounting.

**T18-56 — Recovery budget reused for forward mutation:** Recovery capacity is borrowed to continue forward optimization after forward capacity is exhausted.

**T18-57 — Forward progression resumes after recovery:** The campaign continues later forward items after recovery changed or invalidated the assumptions under which the revision was authorized.

**T18-58 — Campaign execution downgraded to standalone:** A campaign-bound Phase 16 production change is executed through a standalone path after campaign suspension, abort, closure, or other invalidation.

**T18-59 — Reservation returned despite unresolved send:** A safety reservation expires/releases capacity while the associated operation may already have reached `MAY_HAVE_SENT`.

**T18-60 — One execution attached to multiple items:** A single valid production execution is reused as the execution lineage for multiple campaign items.

**T18-61 — One item replayed into multiple executions:** Crash recovery, replay, or duplicate processing creates multiple active forward production execution lineages for one campaign item.

**T18-62 — Superseded evidence reused:** Observation or reconciliation evidence known to be superseded by conflicting authoritative state is reused for progression.

**T18-63 — Automation masquerades as human:** An Agent, service, scheduler, automation identity, or caller-supplied principal field is treated as authenticated human campaign authority.

**T18-64 — Value/unit canonicalization mismatch:** Numeric or unit representation differences defeat mutation-binding equality or permit semantically different mutation values to be treated as equivalent.

**Threat count: 64**

---

# 54. Invariant Catalogue

Authoritative invariant definitions. Each identifier appears exactly once in this section.

**I18-01 — Campaign authority is not mutation authority.** No campaign authorization, state, release, or orchestration decision by itself authorizes a production network mutation.

**I18-02 — Campaign release is not a Phase 16 production execution grant.** Every production mutation still requires the authoritative individual production execution authority.

**I18-03 — Campaign lease is not the production execution lease.** Campaign orchestration fencing and Phase 16 mutation fencing remain separate authorities.

**I18-04 — Vendor acknowledgement is not production verification.** Vendor acceptance alone never proves the desired network state.

**I18-05 — Production verification is not canonical reconciliation.** Direct vendor readback and Phase 12 canonical synchronization remain distinct evidence.

**I18-06 — Canonical reconciliation is not observation health.** Canonical state convergence alone does not establish acceptable post-change network behavior.

**I18-07 — Healthy is not released.** Healthy observation creates at most progression eligibility; an explicit human release is still required.

**I18-08 — Abort is not rollback.** Aborting a campaign stops further forward mutation but does not authorize or automatically perform recovery.

**I18-09 — Recovery planning is not rollback authorization.** Every actual rollback remains separately governed.

**I18-10 — Automatic safety suspension is not automatic resumption.** Recovery of the triggering condition never automatically restores production progression authority.

**I18-11 — Campaign start is not future window authorization.** Every production send must satisfy current applicable campaign and production-change windows.

**I18-12 — Software capability is not external certification.** Implementation or local evidence cannot substitute for required vendor/lab/pre-production certification.

**I18-13 — External certification is not production authorization.** Certification/readiness alone does not authorize a production mutation or campaign.

**I18-14 — Unknown, stale, or erroneous required safety state denies progression.** Loss of certainty removes permission for new progression/send.

**I18-15 — No ambiguous outcome may be followed by another campaign mutation.** A possible external mutation must be resolved or governed to a blocking terminal/manual state before further mutation.

**I18-16 — Every production mutation is individually governed.** Campaign orchestration never converts multiple changes into one blanket mutation authority.

**I18-17 — Unreleased campaign work has no mutation capability.** Future work cannot hold a pre-minted production capability merely because it belongs to an authorized campaign.

**I18-18 — Consumed grants and historical execution evidence are never rewritten to manufacture reusable authority.**

**I18-19 — Phase 18 cannot broaden Phase 17 vendor protocol knowledge.** Campaign orchestration does not infer or invent vendor write mechanisms.

**I18-20 — Real production campaign execution remains unauthorized until independently authorized.** Architecture/software existence does not change that state.

**I18-21** — Released campaign item identity must equal the mutation identity authorized and executed by Phase 16/17.

**I18-22** — A stale campaign lease holder cannot initiate a new campaign-originated production send.

**I18-23** — Abort, suspension, revocation, and material invalidation invalidate unused release authority.

**I18-24** — Safety invalidation after `MAY_HAVE_SENT` cannot terminate mandatory outcome resolution.

**I18-25** — Post-change health evidence must be attributable to the verified post-change observation boundary.

**I18-26** — `MAY_HAVE_SENT` safety-budget exposure cannot be blindly returned.

**I18-27** — Campaign completion must account for every item in immutable authorized campaign scope.

**I18-28** — Network observation health and operational safety health are independent and both must permit progression.

**I18-29** — Campaign-to-production execution correlation must be bidirectional and tamper-evident.

**I18-30** — External interference invalidates affected campaign assumptions until authoritative reconciliation restores certainty.

**I18-31** — Forward and recovery mutations have separate durable safety-exposure accounting; neither safety budget grants mutation authority, recovery capacity cannot be borrowed for forward progression, and any `MAY_HAVE_SENT` operation conservatively consumes its applicable exposure.

**I18-32** — Once a campaign revision requires recovery or begins a campaign-associated rollback execution, forward progression for that revision is permanently closed; successful recovery cannot reopen it, and any later forward optimization requires new governed authority.

**Invariant count: 32**

---

# 55. Architecture Acceptance Gates

Authoritative gate definitions. Each identifier appears exactly once in this section.

**G18-001** — Phase 18 SHALL use immutable Phase 17 implementation baseline `d1751cca70391babf712bce3c6bcc29238ce0c86` as its parent and SHALL NOT alter frozen Phase 17 artifacts.

**G18-002** — Phase 18 SHALL add campaign coordination and progressive-delivery governance without weakening or bypassing authoritative Phase 13–17 controls.

**G18-003** — Campaign authority SHALL NOT constitute, imply, mint, or substitute for production mutation authority.

**G18-004** — Campaign release SHALL NOT constitute or substitute for a Phase 16 `ProductionExecutionGrant`.

**G18-005** — Initial Phase 18 production campaign scope SHALL be limited to one registered `ProductionNetworkTarget` per campaign.

**G18-006** — Initial Phase 18 mutation scope SHALL remain CELL object type and `txPower` parameter only, with one parameter and one forward mutation per child production execution.

**G18-007** — Ericsson SHALL remain the first production-vendor context for Phase 18; Nokia SHALL remain deferred.

**G18-008** — Phase 18 SHALL NOT infer, guess, invent, or embed an Ericsson production-write protocol, endpoint, credential mechanism, CLI, SSH operation, SDK call, or generic command mechanism.

**G18-009** — Real production campaign execution SHALL remain unauthorized until all inherited external certification, target-onboarding, production-authorization, and Phase 18 readiness requirements are independently satisfied.

**G18-010** — Software implementation, local tests, architecture gates, or campaign qualification evidence SHALL NOT by themselves constitute production authorization.

**G18-011** — A `ProductionChangeCampaign` SHALL be revisioned, and an authorized revision SHALL be immutable.

**G18-012** — Every material campaign change SHALL require a new revision and new campaign fingerprint.

**G18-013** — A material campaign revision SHALL require new review and new campaign authorization before production progression.

**G18-014** — Campaign authorization SHALL freeze the complete `AuthorizedCampaignScope`.

**G18-015** — Authorized campaign scope SHALL identify every intended campaign item and SHALL NOT be enlarged after authorization.

**G18-016** — Removal, replacement, mutation, or reordering of an authorized item SHALL stale the existing campaign authority and require new governance.

**G18-017** — A campaign item SHALL reference governed Phase 14, Phase 15, and Phase 16 lineage rather than construct arbitrary production mutations.

**G18-018** — Campaign mutation identity SHALL bind target, cell, parameter, expected value, desired value, and exact governed rollback value.

**G18-019** — The campaign fingerprint SHALL bind all material campaign, target, item, cohort, inherited governance, policy, window, safety, certification, onboarding, and authorization identities.

**G18-020** — Campaign fingerprint canonicalization SHALL be deterministic and independent of locale, process, database result ordering, and platform-specific serialization.

**G18-021** — Ordered campaign collections SHALL preserve their governed order in fingerprint canonicalization.

**G18-022** — Set-like campaign collections SHALL use a deterministic canonical ordering.

**G18-023** — Material fingerprint mismatch SHALL make existing campaign authority stale and SHALL deny progression.

**G18-024** — Campaign objectives SHALL be descriptive governance metadata only and SHALL NOT be executable input to a vendor mutation path.

**G18-025** — No campaign API or internal contract SHALL accept arbitrary vendor commands, arbitrary endpoints, or free-form production mutation payloads.

**G18-026** — Campaign review, authorization, cohort release, pause, abort, evidence access, and resumption governance SHALL use explicit least-privilege permissions.

**G18-027** — Campaign creator and campaign authorizer SHALL be different authenticated human authorities.

**G18-028** — Campaign authorization and production cohort release SHALL be separated so campaign authorization does not automatically release production work.

**G18-029** — Human governance authority SHALL be derived from authenticated identity rather than untrusted request metadata.

**G18-030** — Null, blank, malformed, unknown, or unauthorized governance principals SHALL fail closed.

**G18-031** — Agent identities SHALL NOT satisfy human campaign review, authorization, release, resumption, execution, or rollback authority.

**G18-032** — MCP identities SHALL NOT satisfy human campaign review, authorization, release, resumption, execution, or rollback authority.

**G18-033** — Campaign authorization SHALL NOT execute a production mutation.

**G18-034** — Campaign creation SHALL NOT execute a production mutation.

**G18-035** — Opening a campaign or change window SHALL NOT execute a production mutation.

**G18-036** — A cohort SHALL require an explicit human release before any contained item can become production-execution eligible.

**G18-037** — Health becoming eligible SHALL NOT automatically release a cohort.

**G18-038** — Scheduler activity SHALL NOT authorize or release a cohort or initiate a production mutation.

**G18-039** — Event consumption SHALL NOT authorize or release a cohort or initiate a production mutation.

**G18-040** — Campaign governance actions SHALL have durable idempotency/replay protection so duplicate requests cannot create duplicate authority.

**G18-041** — Every initial Phase 18 production campaign SHALL begin with a mandatory canary.

**G18-042** — The initial canary SHALL contain exactly one cell.

**G18-043** — A canary SHALL remain subject to the same individual Phase 16/17 production controls as every later item.

**G18-044** — Canary success SHALL NOT be represented as statistical proof that later campaign cells will behave identically.

**G18-045** — Cohorts SHALL be governance/release boundaries and SHALL NOT constitute batch vendor mutation commands.

**G18-046** — Initial Phase 18 production mutation concurrency SHALL be exactly one campaign-associated production mutation at a time.

**G18-047** — Campaign items SHALL execute sequentially under the initial Phase 18 production model.

**G18-048** — A subsequent item SHALL NOT become send-eligible until all predecessor conditions required by policy are satisfied.

**G18-049** — A subsequent cohort SHALL NOT become progression-eligible until the preceding cohort satisfies required verification, reconciliation, observation, and safety conditions.

**G18-050** — The only executable production progression mode in initial Phase 18 SHALL be MANUAL.

**G18-051** — Phase 18 SHALL NOT introduce an executable dormant or hidden automatic production progression mode.

**G18-052** — A human release SHALL authorize only the specifically governed cohort and SHALL NOT pre-authorize future cohorts.

**G18-053** — Cohort membership SHALL be frozen when the cohort is released.

**G18-054** — Released cohort membership SHALL NOT be enlarged, reduced, replaced, or reordered under the existing release authority.

**G18-055** — A released item SHALL still require individual production eligibility and Phase 16 authority before mutation.

**G18-056** — Unreleased campaign work SHALL possess no production mutation capability.

**G18-057** — Phase 16 production grants SHALL NOT be pre-minted for unreleased future campaign items.

**G18-058** — A production grant SHALL be created only sufficiently late for an individually eligible item after current campaign safety has been revalidated.

**G18-059** — Duplicate human release requests SHALL be idempotent and SHALL NOT duplicate mutation capability.

**G18-060** — Campaign progression SHALL stop when required predecessor state is UNKNOWN, STALE, ERROR, unresolved, or otherwise non-progressable.

**G18-061** — Every production send SHALL revalidate global production enablement.

**G18-062** — Every production send SHALL revalidate target enablement and target operational state.

**G18-063** — Every production send SHALL revalidate campaign enablement and campaign non-aborted/non-suspended state.

**G18-064** — Every production send SHALL revalidate the current campaign revision and fingerprint.

**G18-065** — Every production send SHALL revalidate campaign authorization currentness.

**G18-066** — Every production send SHALL revalidate campaign lease ownership and fencing currentness.

**G18-067** — Every production send SHALL revalidate cohort release currentness.

**G18-068** — Every production send SHALL revalidate item eligibility.

**G18-069** — Every production send SHALL revalidate campaign and production-change windows.

**G18-070** — Every production send SHALL revalidate external change-control currentness where policy requires it.

**G18-071** — Every production send SHALL revalidate applicable Phase 17 certification currentness.

**G18-072** — Every production send SHALL revalidate target onboarding currentness.

**G18-073** — Every production send SHALL revalidate security-profile and credential-profile currentness.

**G18-074** — Every production send SHALL revalidate applicable blast-radius and rate-limit safety.

**G18-075** — Every production send SHALL revalidate Phase 16 production authorization currentness.

**G18-076** — Every production send SHALL require a current legal Phase 16 production execution grant.

**G18-077** — Every production send SHALL require a current Phase 16 execution lease and fencing authority.

**G18-078** — The Production Write Gateway SHALL independently perform final production preflight and SHALL remain the final trusted mutation boundary.

**G18-079** — Application-side campaign eligibility SHALL NOT substitute for gateway final preflight.

**G18-080** — UNKNOWN, STALE, ERROR, unavailable, or unverifiable required currentness SHALL deny a new production send.

**G18-081** — Vendor acceptance SHALL NOT constitute successful production verification.

**G18-082** — Successful production execution SHALL require independent direct vendor readback according to inherited Phase 16/17 verification policy.

**G18-083** — `PRODUCTION_VERIFIED` SHALL remain distinct from `CANONICAL_RECONCILED`.

**G18-084** — Phase 18 SHALL NOT directly mutate Phase 12 canonical network state to make campaign execution appear reconciled.

**G18-085** — Canonical reconciliation SHALL occur through the authoritative synchronization path.

**G18-086** — `CANONICAL_RECONCILED` SHALL remain distinct from post-change observation health.

**G18-087** — Campaign progression SHALL require a governed post-change observation interval where policy requires observation.

**G18-088** — Post-change telemetry SHALL satisfy explicit freshness requirements.

**G18-089** — Stale telemetry SHALL NOT satisfy campaign health progression.

**G18-090** — Telemetry unavailable or provenance unavailable SHALL produce non-progressable health rather than assumed health.

**G18-091** — Campaign health SHALL distinguish HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN, and STALE network-observation states.

**G18-092** — Only HEALTHY network-observation state SHALL permit initial Phase 18 progression.

**G18-093** — Assurance conditions required by campaign policy SHALL be evaluated before progression.

**G18-094** — Verification mismatch SHALL block further forward campaign progression.

**G18-095** — Unresolved production outcome SHALL block further campaign mutation.

**G18-096** — Observation timeout SHALL block progression rather than infer success.

**G18-097** — Required observation evidence SHALL be bound to the relevant campaign item and production execution.

**G18-098** — Campaign progression SHALL use current health evidence and SHALL NOT reuse stale prior HEALTHY state.

**G18-099** — Observation and health policy identities SHALL be versioned/bound sufficiently to detect material policy change.

**G18-100** — Material health or observation policy change SHALL invalidate future progression where the existing authorization no longer binds the governing policy.

**G18-101** — Campaign safety controls SHALL be durable/shared and SHALL NOT depend solely on process-local state.

**G18-102** — Campaign safety SHALL enforce a policy-defined maximum campaign scope.

**G18-103** — Campaign implementation SHALL additionally have a hard maximum campaign-size ceiling independent of mutable runtime policy.

**G18-104** — Effective campaign-size allowance SHALL never exceed the stricter applicable hard or policy limit.

**G18-105** — Campaign safety SHALL bound cells, parameters, and mutation operations.

**G18-106** — Campaign safety SHALL bound production mutation rate over governed time intervals.

**G18-107** — Campaign safety SHALL track verification failures.

**G18-108** — Campaign safety SHALL track unresolved/ambiguous production outcomes.

**G18-109** — Campaign safety SHALL track recovery/rollback events.

**G18-110** — Campaign safety SHALL support policy-defined consecutive-failure stopping conditions.

**G18-111** — Rate and blast-radius enforcement SHALL be concurrency-safe.

**G18-112** — Safety-limit failure or inability to establish authoritative counters SHALL fail closed.

**G18-113** — Automatic safety suspension SHALL be permitted when a safety condition becomes non-progressable.

**G18-114** — Automatic safety suspension SHALL NOT itself initiate a production mutation.

**G18-115** — Automatic safety suspension SHALL NOT automatically initiate rollback.

**G18-116** — Automatic safety suspension SHALL NOT automatically resume the campaign.

**G18-117** — Operator pause SHALL be distinguishable from safety suspension.

**G18-118** — Global kill, target kill, campaign suspension, cohort release, and item eligibility SHALL form independent hierarchical controls rather than substitutes for one another.

**G18-119** — Kill/suspension SHALL stop new sends but SHALL NOT claim to cancel a vendor request already in flight.

**G18-120** — Typed machine-readable safety and eligibility reason codes SHALL be available so clients do not infer safety state from free-form text.

**G18-121** — Campaign abort SHALL prohibit additional forward campaign mutations.

**G18-122** — Campaign abort SHALL NOT itself constitute rollback authorization.

**G18-123** — Campaign abort SHALL NOT automatically initiate rollback.

**G18-124** — An ambiguous child execution outcome SHALL immediately block later campaign mutation.

**G18-125** — Phase 18 SHALL NOT automatically retry an ambiguous production mutation.

**G18-126** — A child outcome observed at desired state may become VERIFIED only through authoritative verification evidence.

**G18-127** — A child outcome observed at expected pre-change state SHALL NOT automatically cause a retry; any later attempt requires separately governed authority.

**G18-128** — A third/unexpected observed state SHALL require manual intervention or equivalent fail-closed governance.

**G18-129** — Verification/readback unavailable after a possible send SHALL remain unresolved and SHALL block progression.

**G18-130** — Campaign recovery planning SHALL NOT itself constitute rollback authority.

**G18-131** — Every actual rollback SHALL remain separately requested, reviewed, authorized, executed, and verified through authoritative production controls.

**G18-132** — Rollback SHALL use the exact governed rollback value; Phase 18 SHALL NOT invent a recovery value.

**G18-133** — Recovery ordering may be represented, including reverse-order recovery where governed, but representation SHALL NOT authorize the constituent rollbacks.

**G18-134** — Phase 18 SHALL NOT automatically execute rollback.

**G18-135** — Phase 18 SHALL NOT automatically retry rollback.

**G18-136** — Rollback verification SHALL remain distinct from vendor acknowledgement.

**G18-137** — Ambiguous rollback outcome SHALL block additional campaign-associated mutation and return to human governance.

**G18-138** — Recovery state SHALL be durably represented and reconstructible after crash.

**G18-139** — Campaign final disposition SHALL distinguish complete recovery, partial recovery, unresolved/manual-intervention, abort, and expiry semantics as applicable.

**G18-140** — Campaign completion SHALL NOT conceal unresolved mutation or recovery state.

**G18-141** — Phase 17 interface-definition withdrawal SHALL invalidate affected campaign progression.

**G18-142** — Phase 17 transport-profile revocation or expiry SHALL invalidate affected campaign progression.

**G18-143** — Phase 17 certification revocation, expiry, or required staleness SHALL invalidate affected campaign progression.

**G18-144** — Target-onboarding revocation or expiry SHALL invalidate affected campaign progression.

**G18-145** — Security-profile invalidation SHALL invalidate affected campaign progression.

**G18-146** — Credential-profile invalidation SHALL invalidate affected campaign progression.

**G18-147** — Revocation/invalidation SHALL prohibit new campaign-originated grants/sends where affected.

**G18-148** — Revocation SHALL NOT rewrite consumed Phase 16 grants or historical execution evidence.

**G18-149** — Revocation after `MAY_HAVE_SENT` SHALL stop future sends while preserving mandatory outcome resolution.

**G18-150** — Campaign evidence SHALL provide bidirectional correlation to the authoritative production execution lineage.

**G18-151** — Campaign audit SHALL be tamper-evident using deterministic canonical event hashing.

**G18-152** — Campaign audit ordering/sequence SHALL be concurrency-safe.

**G18-153** — Audit `INVALID`, `UNKNOWN`, `GAP`, or `HASH_MISMATCH` SHALL block progression.

**G18-154** — Phase 18 SHALL claim tamper evidence only and SHALL NOT claim immutable storage unless such storage is independently established.

**G18-155** — Campaign evidence and audit SHALL NOT contain vendor credentials or secrets.

**G18-156** — Crash recovery SHALL reconstruct authority only from durable authoritative state.

**G18-157** — Crash recovery SHALL NOT infer successful external mutation from local intent or handoff state alone.

**G18-158** — Duplicate release or recovery processing after restart SHALL be idempotent.

**G18-159** — Failure to reconcile authoritative execution state after crash SHALL block further progression.

**G18-160** — Historical execution, verification, reconciliation, health, suspension, abort, and recovery evidence SHALL remain distinguishable rather than overwritten into a simplified final state.

**G18-161** — Campaign orchestration SHALL remain in the application/control plane and SHALL NOT move vendor-write credentials into that plane.

**G18-162** — The separate Production Write Gateway SHALL remain the production vendor-write credential/session boundary.

**G18-163** — The Production Write Gateway SHALL NOT become the campaign progression engine.

**G18-164** — Phase 10 credential late-resolution and no-secret-persistence requirements SHALL remain authoritative.

**G18-165** — Phase 18 SHALL NOT broaden production egress to arbitrary hosts, ports, URLs, or `0.0.0.0/0`.

**G18-166** — Phase 18 SHALL NOT introduce SSH, generic CLI execution, or arbitrary command transport.

**G18-167** — Default CI SHALL remain independent of Azure credentials, vendor credentials, real vendor endpoints, and external certification environments.

**G18-168** — Campaign software correctness qualification SHALL remain distinct from vendor-lab qualification.

**G18-169** — Phase 18 C1 simulator qualification SHALL remain distinct from C2 vendor-lab qualification.

**G18-170** — Phase 18 C2 vendor-lab qualification SHALL remain distinct from C3 pre-production progressive-delivery qualification.

**G18-171** — Phase 18 C3 qualification SHALL remain distinct from C4 target-specific campaign operational approval.

**G18-172** — Phase 18 C-level qualification SHALL NOT replace Phase 17 L-level transport/target readiness.

**G18-173** — Phase 18 SHALL NOT claim L1, L2, L3, L4, C1, C2, C3, or C4 evidence unless independently executed/satisfied.

**G18-174** — Agent, MCP, scheduler, and event mechanisms SHALL NOT obtain production campaign mutation authority.

**G18-175** — Phase 18 SHALL NOT introduce closed-loop production autonomy.

**G18-176** — Phase 18 SHALL NOT introduce parallel production campaign mutation under the initial production model.

**G18-177** — Phase 18 SHALL NOT introduce cross-target production campaigns.

**G18-178** — Phase 18 SHALL NOT introduce arbitrary/multi-parameter production campaign items.

**G18-179** — Phase 18 SHALL preserve Ericsson-first/Nokia-deferred vendor readiness without implying vendor certification.

**G18-180** — Completion of Phase 18 software SHALL NOT authorize real production execution or real production campaign execution.

**G18-181** — `CampaignExecutionBinding` SHALL be immutable once created for an eligible/released campaign item.

**G18-182** — `CampaignExecutionBinding` SHALL include the authoritative Phase 16 production fingerprint.

**G18-183** — The gateway SHALL independently verify the campaign execution binding for campaign-originated mutation.

**G18-184** — The campaign fencing token SHALL be bound to the production execution handoff.

**G18-185** — The campaign control generation SHALL be bound to the production execution handoff.

**G18-186** — A stale campaign fencing token SHALL deny a new campaign-originated production send.

**G18-187** — Abort SHALL advance or otherwise authoritatively invalidate the campaign execution/control generation before any later new send.

**G18-188** — Safety suspension SHALL invalidate unused cohort release authority.

**G18-189** — Applicable revocation SHALL invalidate unused cohort release authority.

**G18-190** — Resumption SHALL require a new release generation for unexecuted work and SHALL NOT resurrect stale release authority.

**G18-191** — Cohort release SHALL have an immutable membership fingerprint binding its ordered released items and governing authority.

**G18-192** — Post-change observation SHALL have an explicit authoritative observation boundary.

**G18-193** — Post-change health SHALL use measurement provenance and SHALL NOT use ingestion time alone as proof of post-change measurement.

**G18-194** — Observation evidence SHALL be bound to the applicable production execution and campaign item.

**G18-195** — Detected external interference SHALL invalidate affected campaign progression until authoritative certainty is restored.

**G18-196** — Network observation health SHALL be distinct from operational safety health.

**G18-197** — Initial production progression SHALL require both network observation health to permit progression and operational safety health to be SAFE.

**G18-198** — The campaign item lifecycle SHALL explicitly distinguish planning, release, eligibility, handoff, send possibility, vendor acceptance, verification, reconciliation, observation, completion, and exceptional states.

**G18-199** — Campaign safety-budget check and reservation SHALL be atomic in durable shared state.

**G18-200** — Safety-budget exposure reaching `MAY_HAVE_SENT` SHALL NOT be blindly returned.

**G18-201** — Authorized campaign scope SHALL be immutable under the applicable campaign authorization.

**G18-202** — Campaign completion SHALL account for every item in authorized campaign scope.

**G18-203** — Campaign handoff to Phase 16 SHALL be durably recorded.

**G18-204** — Crash recovery SHALL consult authoritative Phase 16 production execution state before determining whether another action is permissible.

**G18-205** — Campaign audit/evidence SHALL cryptographically bind the relevant Phase 16 execution fingerprint/identity.

**G18-206** — Campaign-originated production execution evidence SHALL link back to the applicable campaign execution binding.

**G18-207** — Initial Phase 18 SHALL prohibit duplicate `(productionTargetId, cellId, parameter)` entries within one campaign revision.

**G18-208** — Campaign authority/fingerprint SHALL bind the applicable health-policy version.

**G18-209** — Campaign authority/fingerprint SHALL bind the applicable observation-policy version.

**G18-210** — External change-ticket currentness SHALL be represented explicitly and SHALL NOT be silently inferred from reference presence alone.

**G18-211** — Campaign objective text SHALL remain non-executable governance metadata.

**G18-212** — Campaign canonicalization SHALL explicitly distinguish ordered and set-like semantics and SHALL remain deterministic.

**G18-213** — Campaign size SHALL be bounded by both policy and an implementation hard ceiling.

**G18-214** — Canary success SHALL NOT be represented as statistical representativeness of later campaign scope.

**G18-215** — A material observation-policy change SHALL invalidate future progression where existing authority no longer binds the governing policy.

**G18-216** — A material health-policy change SHALL invalidate future progression where existing authority no longer binds the governing policy.

**G18-217** — Safety and eligibility decisions SHALL expose typed machine-readable reason codes.

**G18-218** — Human campaign review, authorization, release, pause, abort, and resumption governance SHALL have durable idempotency/replay protection.

**G18-219** — Campaign resumption SHALL require explicit governed review and authorization.

**G18-220** — Campaign resumption SHALL bind the suspension cause and remediation evidence.

**G18-221** — Campaign resumption SHALL NOT itself release a cohort.

**G18-222** — Safety invalidation after `MAY_HAVE_SENT` SHALL NOT terminate mandatory outcome resolution for the possibly sent operation.

**G18-223** — Current campaign control generation SHALL be checked at a trusted production pre-send boundary.

**G18-224** — Campaign execution origin SHALL NOT be a caller-forgeable mechanism for bypassing campaign controls.

**G18-225** — Campaign safety-budget reservation SHALL NOT constitute mutation authority.

**G18-226** — Uncertain external exposure SHALL be accounted for conservatively.

**G18-227** — Detected interference on the same target/cell/parameter during the relevant observation interval SHALL invalidate attribution by default.

**G18-228** — Material policy change SHALL NOT rewrite historical execution or observation evidence.

**G18-229** — Unused release authority SHALL become stale when its bound campaign authorization generation is no longer current.

**G18-230** — No successful final campaign state SHALL silently omit an item from immutable authorized campaign scope.

**G18-231** — A Phase 16 production change bound to a campaign cannot subsequently be executed through a standalone-origin path that omits the campaign binding.

**G18-232** — Campaign control and release generations are never reused or reset such that stale authority can become current again.

**G18-233** — A campaign safety-budget reservation may expire or release only when authoritative execution evidence establishes that the associated operation did not reach `MAY_HAVE_SENT`; uncertainty retains conservative exposure.

**G18-234** — A campaign-originated Phase 16 production execution maps to exactly one `CampaignExecutionBinding` and one campaign item.

**G18-235** — A campaign item cannot obtain duplicate active forward execution lineages through replay or crash recovery.

**G18-236** — Superseded conflicting observation or reconciliation evidence cannot satisfy progression merely because it was previously valid.

**G18-237** — Campaign resumption cannot make progression executable while a blocking production outcome remains unresolved.

**G18-238** — Campaign safety exposure separately accounts for forward and recovery mutation exposure.

**G18-239** — Recovery mutation capacity cannot be used for forward mutation.

**G18-240** — Forward mutation capacity does not authorize recovery mutation.

**G18-241** — A recovery mutation reaching `MAY_HAVE_SENT` conservatively consumes recovery exposure.

**G18-242** — `RECOVERY_REQUIRED` permanently closes forward progression for the current campaign revision.

**G18-243** — A campaign-associated rollback reaching authorized execution permanently closes forward progression for the current campaign revision.

**G18-244** — Successful recovery does not reopen forward progression for that campaign revision.

**G18-245** — Forward optimization after recovery requires new governed campaign authority and a new applicable canary lifecycle.

**G18-246** — Campaign validity cannot resurrect stale, revoked, or otherwise non-executable authoritative Phase 14, Phase 15, or Phase 16 state.

**G18-247** — Required active/historical campaign evidence has retention and referential-integrity semantics sufficient to make missing evidence detectable and fail closed where required.

**G18-248** — External change-control invalidation participates in campaign control-generation invalidation whenever current external change-control authority is required.

**G18-249** — Human campaign authority derives from authenticated actor identity, not caller-supplied principal fields.

**G18-250** — Campaign mutation equality uses canonical typed value and unit semantics across campaign, Phase 16, and gateway boundaries.

**G18-251** — Initial Phase 18 post-change observation begins no earlier than successful direct vendor verification.

**G18-252** — A future cohort cannot be validly pre-released before predecessor verification, reconciliation, observation, and safety requirements have been satisfied.

**G18-253** — Historical outcome resolution uses its bound historical evidence/policy identity while current authoritative policy governs future progression.

**G18-254** — Absence of detected same-parameter interference is not represented as proof of exclusive causal attribution.

**Architecture gate count: 254**

---

# 56. Architecture Traceability

This section is architecture traceability. It is **not** implementation evidence.

## 56.1 Gate → normative section

Every gate is established by at least one normative section.

| Gate | Normative section(s) |
|---|---|
| G18-001 | §2 |
| G18-002 | §2 |
| G18-003 | §5 |
| G18-004 | §5 |
| G18-005 | §3 |
| G18-006 | §3 |
| G18-007 | §3 |
| G18-008 | §47 |
| G18-009 | §49 |
| G18-010 | §49 |
| G18-011 | §8 |
| G18-012 | §8, §10 |
| G18-013 | §8 |
| G18-014 | §8 |
| G18-015 | §8 |
| G18-016 | §8 |
| G18-017 | §9 |
| G18-018 | §9 |
| G18-019 | §10 |
| G18-020 | §10 |
| G18-021 | §10 |
| G18-022 | §10 |
| G18-023 | §10 |
| G18-024 | §10 |
| G18-025 | §4, §47 |
| G18-026 | §28 |
| G18-027 | §28 |
| G18-028 | §28 |
| G18-029 | §28 |
| G18-030 | §28 |
| G18-031 | §45 |
| G18-032 | §45 |
| G18-033 | §28 |
| G18-034 | §28 |
| G18-035 | §28 |
| G18-036 | §11 |
| G18-037 | §11, §27 |
| G18-038 | §45 |
| G18-039 | §45 |
| G18-040 | §28 |
| G18-041 | §11 |
| G18-042 | §11 |
| G18-043 | §11 |
| G18-044 | §11 |
| G18-045 | §11 |
| G18-046 | §11, §34 |
| G18-047 | §11 |
| G18-048 | §11 |
| G18-049 | §11 |
| G18-050 | §11 |
| G18-051 | §11 |
| G18-052 | §11 |
| G18-053 | §12 |
| G18-054 | §12 |
| G18-055 | §11, §13 |
| G18-056 | §11 |
| G18-057 | §11 |
| G18-058 | §11, §22 |
| G18-059 | §11, §28 |
| G18-060 | §22, §50 |
| G18-061 | §22, §41 |
| G18-062 | §22, §41 |
| G18-063 | §22, §41 |
| G18-064 | §22 |
| G18-065 | §22 |
| G18-066 | §17, §22 |
| G18-067 | §12, §22 |
| G18-068 | §22 |
| G18-069 | §22 |
| G18-070 | §22 |
| G18-071 | §22, §42 |
| G18-072 | §22, §42 |
| G18-073 | §22, §42 |
| G18-074 | §22, §31 |
| G18-075 | §22 |
| G18-076 | §22 |
| G18-077 | §17, §22 |
| G18-078 | §14, §22 |
| G18-079 | §14, §22 |
| G18-080 | §22, §50 |
| G18-081 | §5, §23 |
| G18-082 | §23 |
| G18-083 | §23 |
| G18-084 | §23 |
| G18-085 | §23 |
| G18-086 | §23, §27 |
| G18-087 | §24 |
| G18-088 | §25 |
| G18-089 | §25 |
| G18-090 | §25 |
| G18-091 | §27 |
| G18-092 | §27 |
| G18-093 | §27 |
| G18-094 | §23 |
| G18-095 | §21, §23 |
| G18-096 | §24, §25 |
| G18-097 | §24, §25 |
| G18-098 | §25, §27 |
| G18-099 | §10, §24 |
| G18-100 | §10, §18 |
| G18-101 | §31 |
| G18-102 | §31 |
| G18-103 | §31 |
| G18-104 | §31 |
| G18-105 | §31 |
| G18-106 | §31 |
| G18-107 | §31 |
| G18-108 | §31 |
| G18-109 | §32 |
| G18-110 | §31 |
| G18-111 | §31 |
| G18-112 | §31, §50 |
| G18-113 | §31, §38 |
| G18-114 | §31 |
| G18-115 | §31 |
| G18-116 | §29, §31 |
| G18-117 | §38 |
| G18-118 | §41 |
| G18-119 | §41 |
| G18-120 | §31, §50 |
| G18-121 | §30 |
| G18-122 | §30 |
| G18-123 | §30 |
| G18-124 | §21, §50 |
| G18-125 | §21, §36 |
| G18-126 | §21 |
| G18-127 | §21 |
| G18-128 | §21 |
| G18-129 | §21 |
| G18-130 | §35 |
| G18-131 | §35 |
| G18-132 | §35 |
| G18-133 | §35 |
| G18-134 | §36 |
| G18-135 | §36 |
| G18-136 | §35 |
| G18-137 | §36 |
| G18-138 | §35 |
| G18-139 | §40 |
| G18-140 | §40 |
| G18-141 | §42 |
| G18-142 | §42 |
| G18-143 | §42 |
| G18-144 | §42 |
| G18-145 | §42 |
| G18-146 | §42 |
| G18-147 | §42 |
| G18-148 | §42 |
| G18-149 | §21, §42 |
| G18-150 | §43 |
| G18-151 | §44 |
| G18-152 | §44 |
| G18-153 | §44 |
| G18-154 | §44 |
| G18-155 | §43, §46 |
| G18-156 | §20, §50 |
| G18-157 | §20 |
| G18-158 | §20, §28 |
| G18-159 | §20, §50 |
| G18-160 | §43 |
| G18-161 | §48 |
| G18-162 | §46, §48 |
| G18-163 | §48 |
| G18-164 | §46 |
| G18-165 | §46 |
| G18-166 | §46, §47 |
| G18-167 | §49 |
| G18-168 | §49 |
| G18-169 | §49 |
| G18-170 | §49 |
| G18-171 | §49 |
| G18-172 | §49 |
| G18-173 | §49 |
| G18-174 | §45 |
| G18-175 | §45 |
| G18-176 | §34 |
| G18-177 | §3 |
| G18-178 | §3 |
| G18-179 | §47 |
| G18-180 | §49 |
| G18-181 | §13 |
| G18-182 | §13 |
| G18-183 | §14 |
| G18-184 | §17, §20 |
| G18-185 | §18, §20 |
| G18-186 | §17 |
| G18-187 | §18, §30 |
| G18-188 | §19, §38 |
| G18-189 | §19, §42 |
| G18-190 | §19, §29 |
| G18-191 | §12 |
| G18-192 | §24 |
| G18-193 | §25 |
| G18-194 | §25 |
| G18-195 | §26 |
| G18-196 | §27 |
| G18-197 | §27 |
| G18-198 | §23 |
| G18-199 | §31 |
| G18-200 | §31 |
| G18-201 | §8 |
| G18-202 | §40 |
| G18-203 | §20 |
| G18-204 | §20 |
| G18-205 | §43, §44 |
| G18-206 | §43 |
| G18-207 | §9 |
| G18-208 | §10 |
| G18-209 | §10 |
| G18-210 | §10, §22 |
| G18-211 | §10 |
| G18-212 | §10 |
| G18-213 | §31 |
| G18-214 | §11 |
| G18-215 | §10, §18 |
| G18-216 | §10, §18 |
| G18-217 | §31, §50 |
| G18-218 | §28 |
| G18-219 | §29 |
| G18-220 | §29 |
| G18-221 | §29 |
| G18-222 | §21 |
| G18-223 | §18, §22 |
| G18-224 | §15 |
| G18-225 | §31 |
| G18-226 | §31, §33 |
| G18-227 | §26 |
| G18-228 | §10, §43 |
| G18-229 | §19 |
| G18-230 | §40 |
| G18-231 | §15 |
| G18-232 | §18, §19 |
| G18-233 | §31, §33 |
| G18-234 | §16 |
| G18-235 | §16 |
| G18-236 | §26 |
| G18-237 | §29 |
| G18-238 | §32 |
| G18-239 | §32 |
| G18-240 | §32 |
| G18-241 | §32, §33 |
| G18-242 | §37 |
| G18-243 | §37 |
| G18-244 | §37, §38 |
| G18-245 | §39 |
| G18-246 | §8, §9 |
| G18-247 | §43 |
| G18-248 | §18, §42 |
| G18-249 | §28 |
| G18-250 | §9, §13 |
| G18-251 | §24 |
| G18-252 | §11 |
| G18-253 | §21, §43 |
| G18-254 | §25, §26 |

## 56.2 Threat → principal gates

| Threat | Principal gates |
|---|---|
| T18-01 | G18-003, G18-004, G18-055, G18-076 |
| T18-02 | G18-011–G18-016, G18-201 |
| T18-03 | G18-053, G18-054, G18-191 |
| T18-04 | G18-016, G18-021, G18-054 |
| T18-05 | G18-071, G18-143, G18-147 |
| T18-06 | G18-072, G18-144, G18-147 |
| T18-07 | G18-069, G18-080 |
| T18-08 | G18-158, G18-203, G18-204, G18-235 |
| T18-09 | G18-066, G18-186 |
| T18-10 | G18-057, G18-058, G18-076 |
| T18-11 | G18-060, G18-095, G18-124 |
| T18-12 | G18-082, G18-094 |
| T18-13 | G18-088–G18-090, G18-098 |
| T18-14 | G18-116, G18-190, G18-219–G18-221 |
| T18-15 | G18-121–G18-123 |
| T18-16 | G18-130–G18-134 |
| T18-17 | G18-151–G18-154 |
| T18-18 | G18-005, G18-018 |
| T18-19 | G18-018, G18-181–G18-183, G18-250 |
| T18-20 | G18-071, G18-142, G18-143, G18-147–G18-149 |
| T18-21 | G18-073, G18-145–G18-149 |
| T18-22 | G18-061–G18-080, G18-118–G18-119, G18-223 |
| T18-23 | G18-124–G18-129, G18-156–G18-159, G18-222 |
| T18-24 | G18-066, G18-156–G18-159, G18-203–G18-204 |
| T18-25 | G18-059, G18-158, G18-218 |
| T18-26 | G18-031–G18-032, G18-174 |
| T18-27 | G18-038–G18-039, G18-174–G18-175 |
| T18-28 | G18-101, G18-106, G18-111–G18-112, G18-199 |
| T18-29 | G18-097, G18-192–G18-195 |
| T18-30 | G18-081–G18-086 |
| T18-31 | G18-140, G18-202, G18-230 |
| T18-32 | G18-132 |
| T18-33 | G18-150, G18-205–G18-206 |
| T18-34 | G18-065, G18-080, G18-229 |
| T18-35 | G18-066, G18-184, G18-186, G18-223 |
| T18-36 | G18-011–G18-016, G18-064 |
| T18-37 | G18-088–G18-090, G18-098, G18-236 |
| T18-38 | G18-063, G18-188, G18-190 |
| T18-39 | G18-101–G18-112, G18-199–G18-200 |
| T18-40 | G18-139–G18-140, G18-202 |
| T18-41 | G18-181–G18-183, G18-205–G18-206 |
| T18-42 | G18-184–G18-186, G18-223 |
| T18-43 | G18-188–G18-190, G18-229, G18-232 |
| T18-44 | G18-187, G18-222–G18-223 |
| T18-45 | G18-192–G18-194, G18-251 |
| T18-46 | G18-195, G18-227, G18-254 |
| T18-47 | G18-199–G18-200, G18-226, G18-233 |
| T18-48 | G18-201–G18-202, G18-230 |
| T18-49 | G18-182–G18-183, G18-205–G18-206 |
| T18-50 | G18-208, G18-216, G18-228 |
| T18-51 | G18-207 |
| T18-52 | G18-210, G18-248 |
| T18-53 | G18-188–G18-190, G18-219–G18-221, G18-229 |
| T18-54 | G18-196–G18-197 |
| T18-55 | G18-238, G18-240–G18-241 |
| T18-56 | G18-238–G18-240 |
| T18-57 | G18-242–G18-245 |
| T18-58 | G18-224, G18-231 |
| T18-59 | G18-200, G18-226, G18-233 |
| T18-60 | G18-181–G18-183, G18-234 |
| T18-61 | G18-203–G18-204, G18-235 |
| T18-62 | G18-195, G18-236 |
| T18-63 | G18-029–G18-032, G18-249 |
| T18-64 | G18-018, G18-250 |

## 56.3 Invariant → principal gates

| Invariant | Principal gates |
|---|---|
| I18-01 | G18-003, G18-033 |
| I18-02 | G18-004, G18-055, G18-076 |
| I18-03 | G18-066, G18-077, G18-184 |
| I18-04 | G18-081–G18-082 |
| I18-05 | G18-083–G18-085 |
| I18-06 | G18-086–G18-092 |
| I18-07 | G18-036–G18-037, G18-052 |
| I18-08 | G18-121–G18-123 |
| I18-09 | G18-130–G18-134 |
| I18-10 | G18-113–G18-116, G18-219–G18-221 |
| I18-11 | G18-069 |
| I18-12 | G18-167–G18-173 |
| I18-13 | G18-172–G18-173, G18-180 |
| I18-14 | G18-060, G18-080, G18-112 |
| I18-15 | G18-095, G18-124–G18-129 |
| I18-16 | G18-003–G18-004, G18-055 |
| I18-17 | G18-056–G18-058 |
| I18-18 | G18-148, G18-160 |
| I18-19 | G18-008, G18-166, G18-179 |
| I18-20 | G18-009–G18-010, G18-180 |
| I18-21 | G18-181–G18-183, G18-205–G18-206 |
| I18-22 | G18-184, G18-186, G18-223 |
| I18-23 | G18-187–G18-190, G18-229 |
| I18-24 | G18-149, G18-222 |
| I18-25 | G18-192–G18-195, G18-251 |
| I18-26 | G18-200, G18-226, G18-233 |
| I18-27 | G18-201–G18-202, G18-230 |
| I18-28 | G18-196–G18-197 |
| I18-29 | G18-150, G18-205–G18-206 |
| I18-30 | G18-195, G18-227, G18-236, G18-254 |
| I18-31 | G18-225–G18-226, G18-233, G18-238–G18-241 |
| I18-32 | G18-242–G18-245 |

---

# 57. Review History

## R1

```text
C18 = 3
B18 = 12
A18 = 10
D18 = 0
```

Targeted correction addressed C18-01–03 and B18-01–12 and incorporated A18-01–10.

## R2

```text
Original C18: 3/3 CLOSED
Original B18: 12/12 CLOSED

C18-R2 = 0
B18-R2 = 2
A18-R2 = 16
D18-R2 = 0
```

Final targeted correction addressed B18-R2-01 and B18-R2-02 and incorporated A18-R2-01 through A18-R2-16.

The final correction expanded the catalogue to:

```text
G18-254
T18-64
I18-32
```

## R3 — Final adversarial architecture closure review

```text
C18-R3 = 0
B18-R3 = 0
D18-R3 = 0
A18-R3 = 2
```

### A18-R3-01

The implementation specification must define deterministic durable transaction/lock ordering and crash boundaries for interacting campaign controls.

### A18-R3-02

The implementation specification must define an explicit executable state-transition matrix for campaign/cohort/item/release/safety reservation/suspension/resumption/forward-closure/recovery lifecycles.

These are implementation-specification obligations. They do not alter the accepted architecture.

---

# 58. Architecture Acceptance State

```text
OPEN CRITICAL ARCHITECTURE FINDINGS = 0
OPEN BLOCKING ARCHITECTURE FINDINGS = 0
ARCHITECTURAL CONTRADICTIONS = 0
ARCHITECTURE GATES = 254
THREATS = 64
EXPLICIT INVARIANTS = 32

ARCHITECTURE STATUS:
ACCEPTED FOR DOCUMENT FREEZE
```

This document is **not** frozen in Git, **not** Git baselined, and does **not** authorize implementation.

---

# 59. Implementation Lifecycle (after future independent review)

```text
architecture materialization (this document)
→ independent materialization / conformance review
→ architecture freeze (when authorized)
→ architecture Git baseline (when authorized)
→ implementation specification (when authorized)
→ implementation (when authorized)
→ C0 / default CI
→ C1 simulator qualification (when authorized)
→ C2 vendor-lab campaign qualification (when authorized)
→ C3 pre-production progressive-delivery qualification (when authorized)
→ C4 target-specific campaign operational approval (when authorized)
```

This document does **not** authorize those later steps.

---

# 60. Final Status

```text
PHASE 18 ARCHITECTURE STATUS:
ACCEPTED FOR DOCUMENT FREEZE

PHASE 18 GIT ARCHITECTURE BASELINE:
NOT YET AUTHORIZED

PHASE 18 IMPLEMENTATION SPECIFICATION:
NOT STARTED

PHASE 18 IMPLEMENTATION:
NOT AUTHORIZED

V19:
NOT CREATED

PHASE 19:
NOT STARTED

REAL PRODUCTION EXECUTION:
NOT AUTHORIZED

REAL PRODUCTION CAMPAIGN EXECUTION:
NOT AUTHORIZED

ERICSSON PRODUCTION WRITE PROTOCOL:
UNRESOLVED

ERICSSON PRODUCTION TRANSPORT:
UNCONFIGURED / NOT IMPLEMENTED

ERICSSON PRODUCTION ENDPOINT:
NONE / NOT CONFIGURED

ERICSSON PRODUCTION AUTH METHOD:
UNRESOLVED / EXTERNALLY CONFIGURED

ERICSSON PRODUCTION CREDENTIAL:
NONE

NOKIA:
DEFERRED

P17 L1:
NOT EXECUTED

P17 L2:
NOT EXECUTED

P17 L3:
NOT SATISFIED

P17 L4:
NOT SATISFIED

P18 C1:
NOT EXECUTED

P18 C2:
NOT EXECUTED

P18 C3:
NOT EXECUTED

P18 C4:
NOT SATISFIED

AGENT / MCP / SCHEDULED / EVENT EXECUTION:
NOT AUTHORIZED

AUTOMATIC ROLLBACK:
NOT AUTHORIZED

AUTOMATIC COHORT RELEASE / PROGRESSION / RESUMPTION:
NOT AUTHORIZED

CLOSED-LOOP OPTIMIZATION:
NOT AUTHORIZED

PHASE 17 IMPLEMENTATION BASELINE (IMMUTABLE):
d1751cca70391babf712bce3c6bcc29238ce0c86

PHASE 17 ARCHITECTURE BASELINE:
77fd24c0fd32c920c97ff5169f4bc8a93a77b208

PHASE 17 ARCHITECTURE SHA-256:
ea92c6e9183234485da83798ab4fc91c224cfbd1dad80bc464d41009fce576a0
```

---

*End of Phase 18 architecture document (accepted for document freeze; not yet a Git architecture baseline).*
