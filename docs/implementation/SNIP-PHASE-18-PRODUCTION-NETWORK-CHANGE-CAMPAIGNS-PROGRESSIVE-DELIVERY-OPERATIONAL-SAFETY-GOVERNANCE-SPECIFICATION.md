# SNIP Phase 18 --- Production Network Change Campaigns, Progressive Delivery & Operational Safety Governance

## Repository-Specific Implementation Specification --- ACCEPTED / FROZEN

**Status:** ACCEPTED --- FROZEN\
**Date:** 2026-09-05\
**Accepted/frozen:** 2026-09-08\
**Phase:** 18\
**Frozen architecture Git baseline:**
`f38a62ad0e3f80522a95830322079b1380289719`\
**Frozen architecture content SHA-256:**
`19865a242141c6ab4f5e9233056eebada22dbc9c9b456b68676c736a07763a32`\
**Architecture exact-SHA CI:** `33958028479` --- SUCCESS\
**Parent Phase 17 immutable implementation baseline:**
`d1751cca70391babf712bce3c6bcc29238ce0c86`\
**R1 review (historical):** C18-SR1=1, B18-SR1=8 --- CLOSED; A18-R3-01 CLOSED BY ADVERSARIAL REVIEW R2\
**R2 review (historical):** C18-SR2=0, B18-SR2=4, A18-SR2=5, D18-SR2=0 --- CLOSED BY TARGETED ADVERSARIAL REVIEW R3\
**Targeted Adversarial Review R3:** PASSED --- C18-SR3=0, B18-SR3=0, D18-SR3=0, A18-SR3=2 NON-BLOCKING IMPLEMENTATION EVIDENCE OBLIGATIONS\
**Git specification baseline:** NOT YET CREATED\
**Exact-SHA specification CI:** NOT YET EXECUTED\
**Implementation:** NOT AUTHORIZED\
**V19:** NOT CREATED\
**Phase 19:** NOT STARTED\
**Real production execution:** NOT AUTHORIZED\
**Real production campaign execution:** NOT AUTHORIZED

------------------------------------------------------------------------

# 0. Authority, Purpose, and Specification Rule

This specification translates the frozen Phase 18 architecture into
deterministic repository-specific implementation requirements. The
frozen architecture remains superior authority. Any conflict requires
correction of this specification before implementation.

Historical R2 candidate (2026-09-05) incorporated every Blocking and
Advisory finding from Implementation Specification Adversarial Review
R2. Targeted Adversarial Review R3 has now PASSED. This specification
is **ACCEPTED** and **FROZEN**. It does **not** authorize
implementation.

A18-R3-01 is **CLOSED BY ADVERSARIAL REVIEW R2**. A18-R3-02 is
**CLOSED BY TARGETED ADVERSARIAL REVIEW R3**.

Defining rule:

> When SNIP loses certainty, SNIP loses permission to progress, not
> responsibility to resolve an already possible external mutation.

No generic state setter exists for any Phase 18 lifecycle. Any
transition not explicitly listed in the applicable matrix is **DENIED**
with a typed reason code.

# 1. Hard Scope

Initial Phase 18 is limited to Ericsson context, exactly one
`ProductionNetworkTarget` per campaign, `CELL`/`txPower`, one parameter
per item, one forward mutation per child Phase 16 execution, a mandatory
one-cell canary, sequential item execution, MANUAL progression, and
combined forward+recovery campaign mutation concurrency of exactly one.

Forbidden: cross-target campaigns, duplicate target/cell/parameter
forward items in one revision, generic vendor commands, arbitrary
endpoints/payloads, SSH/CLI, automatic
release/progression/rollback/resumption/retry of ambiguous mutation,
Agent/MCP/scheduler/event mutation authority, closed-loop production
autonomy, vendor credentials in the application, inferred Ericsson
protocol, and Nokia production write.

Preserved invariants (non-negotiable):

- CAMPAIGN AUTHORITY != MUTATION AUTHORITY
- CAMPAIGN RELEASE != PHASE16 EXECUTION GRANT
- CAMPAIGN LEASE != PHASE16 EXECUTION LEASE
- FORWARD SAFETY BUDGET != MUTATION AUTHORITY
- RECOVERY SAFETY BUDGET != ROLLBACK AUTHORITY
- VENDOR_ACCEPTED != PRODUCTION_VERIFIED
- PRODUCTION_VERIFIED != CANONICAL_RECONCILED
- ABORT != ROLLBACK
- SUCCESSFUL RECOVERY != FORWARD PROGRESSION REOPENED
- No grant preminting
- No second Phase 16 grant writer
- Gateway remains the final trusted mutation boundary

# 2. Repository Structure and Reuse

Preserve: `production-change-protocol`; `production-write-gateway`;
`snip-npo-app`; migrations under
`snip-npo-app/src/main/resources/db/migration/`; latest predecessor
migration V18; PostgreSQL 16 integration tests; Java 17; Go 1.22;
`mvn -B test`; `go test ./...`.

Phase 18 bounded context: `com.simba.snip.npo.productioncampaign`.

Logical dependency direction:

`API → Application → Domain → Ports → persistence/inherited-phase adapters`.

Campaign code MUST NOT depend on vendor transport/credentials or
arbitrary network endpoints.

Reuse Phase 16 authorities including `ProductionNetworkChangeEntity`,
production review/authorization, `ProductionExecutionGrantEntity`,
`ProductionExecutionLeaseEntity`, `ProductionLeaseService`, gateway
`ProductionGrantConsumeService`, `ExpectedStateObservationService`,
production verification, rollback services,
`ProductionChangeAuditService`, `ProductionRateLimitService`, and
`ProductionExecutionOrchestrationService`. No second grant writer.

# 3. Repository-Specific Decisions

## 3.1 txPower and unit rejection

Campaign mutation numeric type is `BigDecimal`; canonical unit is `dBm`.
Equality is `compareTo(...) == 0`, never floating-point equality. Hash
canonicalization uses `stripTrailingZeros().toPlainString()`, with zero
rendered as `0`, UTF-8, locale-independent, via existing `Sha256Hex` /
`CanonicalJson` conventions.

Alternate units are **not** accepted at the Phase 18 authority boundary.
Existing import normalization may normalize vendor source values before
they become campaign-authoritative. No general unit-conversion
framework is introduced.

Normative unit-rejection tests (A18-SR1-05) MUST prove the Phase 18
authority boundary rejects, with typed
`UNSUPPORTED_PARAMETER_UNIT` (or equivalent existing code if later
introduced as an alias):

- `W`
- `mW`
- `TENTHS_DBM`
- blank unit
- null unit
- unsupported case variants (`dbm`, `DBM `, `Dbm`, `dBM`)
- caller-defined units

unless an upstream existing import normalization process has already
converted the value into canonical campaign-authoritative `dBm` **before**
Phase 18 governance begins.

## 3.2 Campaign hard maximum

`HARD_MAX_CAMPAIGN_ITEMS = 25`.

This is a SNIP Phase 18 software safety ceiling. It is **not** an
Ericsson limit.

Effective maximum is `min(25, configuredPolicyMaximum)`. Runtime policy
may lower but never raise it.

Normative tests (A18-SR1-01) MUST prove that policy/configuration values
`26`, `100`, `Integer.MAX_VALUE`, malformed, `0`, and negative cannot
increase the hard maximum above 25. Invalid configuration MUST fail
closed (`CAMPAIGN_SIZE_LIMIT` / configuration-fail-closed). Zero and
negative configured maxima are invalid and fail closed; they do not
disable the hard ceiling.

## 3.3 Gateway trust

Campaign context is established from authoritative shared PostgreSQL
state, not self-asserted request JSON. `GatewayExecuteRequest` MUST NOT
become a trusted carrier of campaign fields. Gateway derives campaign
binding from authoritative `productionChangeId` / grant lineage /
`CampaignHandoffId`.

Caller cannot choose execution origin. Campaign-bound production change
executed via standalone path = `STANDALONE_DOWNGRADE_DENIED`.

## 3.4 Authenticated human trust contract

Domain port `AuthenticatedActorProvider` is normative and fail-closed.

```text
AuthenticatedActor {
    actorId
    actorType
    authorities
    authenticationSource
    authenticated
}
```

Human campaign authority requires **all** of:

1. `authenticated == true`
2. `actorType == HUMAN`
3. `authenticationSource` belongs to an explicitly configured trusted
   authentication-source allow-list
4. required Phase 18 authority is present
5. applicable SoD checks pass

Raw caller-controlled request fields or request-body principal fields
MUST NOT establish human authority. In particular raw
`X-SNIP-ACTOR-PRINCIPAL-ID`, `X-SNIP-PRODUCTION-CHANGE-PERMISSION`,
`X-SNIP-PERMISSIONS`, or equivalent externally supplied headers MUST
NOT directly establish Phase 18 human authority.

If existing headers are adapted behind `AuthenticatedActorProvider`,
the adapter is valid **only** when trusted ingress guarantees those
headers were stripped/replaced and cannot be caller-forged.

That ingress trust requirement MUST be:

- explicit configuration (trusted authentication source + ingress
  guarantee flag);
- fail closed when absent, blank, unknown, or unconfigured
  (`UNTRUSTED_ACTOR_SOURCE`, `UNAUTHENTICATED_HUMAN_ACTOR`);
- security tested;
- represented in C0 evidence.

Agent, MCP, service, scheduler, automation, and unknown identities
cannot satisfy human governance. Phase 18 does **not** invent a new
OIDC provider or identity platform.

## 3.5 Phase 12 reconciliation

Introduce read-only `CanonicalReconciliationReader` over existing
synchronization/checkpoint and `RadioConfigurationEntity` state. Phase
18 never writes canonical state.

## 3.6 Audit

Reuse `Sha256Hex`, `CanonicalJson` / `ProductionAuditCanonical`
conventions and the existing `FOR UPDATE + UNIQUE(sequence)` pattern.

# 4. V19 Persistence Contract

V19 is a **future implementation** migration. **V19 is NOT CREATED by
this specification correction.**

When implementation is later authorized, create only:

`V19__phase18_production_change_campaigns.sql`

V1--V18 remain unchanged. This document does **not** claim or authorize
migration execution.

## 4.1 Campaign-origin metadata and backward compatibility

Preferred schema is `production_network_change.execution_origin` with
`STANDALONE` / `PRODUCTION_CAMPAIGN`, plus a durable one-to-one binding
link and durable `campaign_handoff_id`.

V19 backward-compatibility rules (A18-SR1-02):

1. Historical pre-V19 production changes remain `STANDALONE`.
2. Migration MUST set `execution_origin = STANDALONE` for every existing
   row. It MUST NOT infer `PRODUCTION_CAMPAIGN` from nullable fields,
   comments, fingerprints, or absent campaign columns.
3. Default/NULL ambiguity MUST NOT campaign-bind historical rows.
   `execution_origin` is NOT NULL after migration with server default
   `STANDALONE` only for the migration backfill.
4. `execution_origin` is server-controlled and immutable once written
   for a row. The caller MUST NOT select execution origin.
5. Campaign binding requires authoritative campaign handoff/binding
   evidence (`CampaignExecutionBinding` + `CampaignHandoffId`). A
   nullable campaign FK alone is not binding evidence.
6. Uniqueness constraints enforce campaign lineage (see §4.2 and §9).
7. Migration is non-destructive to historical production evidence.
   Consumed grants, attempts, verification, rollback, and audit rows
   are not rewritten to manufacture campaign authority.

## 4.2 Required constraints

- unique `(campaign_id, revision_number)`
- unique `(revision_id, item_sequence)`
- unique `(revision_id, production_target_id, cell_id, parameter)`
- unique `(revision_id, cohort_sequence)`
- unique `(campaign_id, audit_sequence)`
- unique `CampaignHandoffId`
- unique binding per item
- unique binding per campaign-originated production change
- unique `campaign_handoff_id` on campaign-originated
  `production_network_change`
- `PRODUCTION_CAMPAIGN` rows MUST have non-null `campaign_handoff_id`
- `STANDALONE` rows MUST have null `campaign_handoff_id`
- at most one active forward lineage per item
- one active campaign lease per campaign
- one combined `CampaignMutationSlot` row per campaign
- unique durable governance idempotency identity
- monotonic/non-reusable generation/fencing semantics
- `ForwardProgressionClosure` cannot transition CLOSED→OPEN
- SHA-256 format constraints consistent with `Sha256Hex` lowercase hex
- FKs sufficient to make missing evidence/correlation detectable

# 5. Core Domain Records

`ProductionChangeCampaign`: campaignId, currentRevision,
productionTargetId, vendor/platform/environment/domain, objective,
external change-control reference, state, enabled, creator, timestamps,
version.

`CampaignRevision`: campaign/revision/fingerprint,
authorizationGeneration, policy versions, windows, external-ticket
currentness requirement, state.

`CampaignExecutionItem`: item/revision/cohort/sequence, P14 plan
identity/version/fingerprint, P15 execution identity/fingerprint, P16
productionChangeId/fingerprint, target, CELL, cellId, txPower,
expected/desired/rollback `BigDecimal`, unit dBm, itemFingerprint,
state/version.

`CampaignCohort`:
campaign/revision/cohortId/sequence/type(CANARY|STANDARD)/state.

`CampaignExecutionBinding`: immutable
campaign/revision/fingerprint/controlGeneration/cohort/releaseFingerprint/releaseGeneration/item/sequence/itemFingerprint/campaignFence/target/P14/P15/P16
identities and
fingerprints/cell/parameter/expected/desired/rollback/unit/createdAt/bindingDigest.

`CampaignHandoffId`: SHA-256 lowercase hex identity defined in §9.

`CampaignMutationSlot`: one row per campaign; see §17.

`CampaignControlGeneration`: one authoritative row per campaign; see
§11.2.

`CampaignReleaseGeneration`: one authoritative campaign-global row;
see §11.3.

`PostChangeObservationBoundary`: campaign/revision/cohort/item/P16
change+execution/cell/parameter/verified
value/vendorVerifiedAt/observationStart/end/source/minimumMeasurementTime/watermark
type+value/max ingestion lag/required canonical checkpoint/policy
versions/boundaryDigest.

`CampaignSafetyExposure`: separate forwardMutationExposure,
recoveryMutationExposure, distinctCellsExposed,
unresolvedForwardExposure, unresolvedRecoveryExposure,
verificationFailureCount, consecutiveFailureCount.

# 6. Fingerprints and Mutation Equality

Campaign fingerprint binds every material architecture field:
target/vendor/platform/environment/domain; ordered items/cohorts;
values; P14/P15/P16 lineage; P17 transport/cert/onboarding;
safety/progression/health/observation/assurance/verification/recovery
policy identities; windows; rate/blast; external ticket; authorization
generation.

Ordered collections preserve governed order. Set-like collections sort
by stable canonical key. Null/absent/empty differ. Timestamps use UTC
ISO-8601. UUIDs use canonical lowercase textual form. BigDecimal uses
§3.1.

Required mutation equality:

`CampaignExecutionBinding == Phase16 authorized mutation == Phase16 grant mutation == Gateway mutation == vendor-adapter mutation`.

Mismatch = DENY.

# 7. Cohorts, Canary, Release

Cohorts are release boundaries, never batch writes. Canary is first and
exactly one cell. Later cohorts cannot become release-eligible before
predecessor verification, reconciliation, observation, and safety
completion.

`CohortReleaseFingerprint` binds campaign/revision/fingerprint, cohort,
ordered item IDs/fingerprints, releaseGeneration, controlGeneration,
campaignFence, network-health digest, operational-safety digest,
authorizationGeneration, human releaser/time, fingerprint.

Release membership is immutable. Release does not send and does not mint
a grant. Future cohorts cannot be pre-released.

# 8. Gateway Campaign Binding and Anti-Downgrade

For `PRODUCTION_CAMPAIGN`, gateway independently queries authoritative
shared DB and verifies exact binding, `CampaignHandoffId`,
campaign/revision/fingerprint, non-aborted/non-suspended state, forward
closure (for forward send), current control/release generations,
campaign fence, release fingerprint, item identity, mutation identity,
P16 authorization/grant/lease/fence, P17 currentness, windows, safety,
and audit integrity.

Caller cannot choose execution origin. Campaign-bound production change
executed via standalone path = `STANDALONE_DOWNGRADE_DENIED`.

Cardinality invariant:

one campaign item
↔ one `CampaignExecutionBinding`
↔ one `CampaignHandoffId`
↔ one campaign-originated Phase 16 forward execution lineage

# 9. Durable Phase18 → Phase16 Handoff and Idempotency

## 9.1 CampaignHandoffId

Handoff identity is deterministic and durable.

Canonical material is `CanonicalJson` of this ordered map (UTF-8,
locale-independent):

```text
campaignId          = lowercase UUID textual form
campaignRevision    = decimal integer, no leading zeros
itemId              = lowercase UUID textual form
bindingDigest       = Sha256Hex lowercase hex of the immutable binding
```

```text
CampaignHandoffId = Sha256Hex.hash(canonicalMaterial)
```

Zero `BigDecimal` inside any contributing digest is rendered `0`.
`Sha256Hex` is the existing repository convention
(`production-change-protocol` /
`com.simba.snip.npo.productionchange.audit`).

The caller MUST NOT generate a new handoff identity after uncertainty.
The identity is a function of the immutable binding already committed
in the local Phase 18 authority transaction.

## 9.2 Persistence and uniqueness

Campaign-originated Phase 16 execution MUST persist `CampaignHandoffId`
under an authoritative database uniqueness constraint.

Required uniqueness / cardinality:

- `UNIQUE(campaign_handoff_id)` on handoff evidence
- `UNIQUE(campaign_handoff_id)` on campaign-originated
  `production_network_change`
- `UNIQUE(item_id)` on `CampaignExecutionBinding`
- `UNIQUE(production_change_id)` on `CampaignExecutionBinding`
- at most one active forward lineage per item
- `STANDALONE` rows have null `campaign_handoff_id`

Violation of uniqueness is `DUPLICATE_CAMPAIGN_LINEAGE` or
`HANDOFF_IDEMPOTENCY_CONFLICT` as applicable.

## 9.3 Local authority transaction before Phase 16

No distributed ACID transaction is claimed across
Phase18 → Phase16 → gateway → vendor.

Phase 18 locks MUST NOT be held while invoking Phase 16.

Before calling Phase 16, one local transaction MUST:

1. lock/validate current Phase 18 authority in §12 order;
2. require `ForwardProgressionClosure = OPEN` for forward handoff;
3. atomically acquire `CampaignMutationSlot` (FORWARD) and safety
   reservation;
4. create immutable `CampaignExecutionBinding` and `bindingDigest`;
5. compute and persist `CampaignHandoffId`;
6. persist `HANDOFF_PENDING` handoff evidence;
7. append campaign audit;
8. record durable idempotency outcome;
9. commit;
10. **then** release locks.

Only after commit may Phase 16 typed orchestration be invoked.

## 9.4 Create-or-return-existing

The Phase 16 handoff operation MUST have create-or-return-existing
semantics keyed by `CampaignHandoffId`:

| Observed | Result |
|---|---|
| same `CampaignHandoffId` + same immutable digest/identity | return/reconcile the authoritative existing lineage |
| same `CampaignHandoffId` + different digest/identity | FAIL CLOSED `HANDOFF_IDEMPOTENCY_CONFLICT` |
| no row | create exactly one campaign-originated lineage |

A lost response MUST NEVER permit creation of a second Phase 16
lineage.

## 9.5 Crash after Phase 16 commit before Phase 18 correlation

1. query Phase 16 using the durable `CampaignHandoffId`;
2. recover the existing lineage;
3. verify exact binding equality;
4. persist Phase 18 correlation;
5. NEVER create speculative replacement execution.

## 9.6 Governance command idempotency

Human review/authorization/release/pause/abort/resumption commands
require `Idempotency-Key`. Persist actor, operation, scope, key,
canonical request digest, result reference/status. Same key+same digest
replays result; same key+different digest = conflict
(`GOVERNANCE_IDEMPOTENCY_CONFLICT` or existing equivalent).

# 10. Human Governance and SoD

Permissions: CAMPAIGN_VIEW, CAMPAIGN_CREATE, CAMPAIGN_REVIEW,
CAMPAIGN_AUTHORIZE, RELEASE_COHORT, CAMPAIGN_PAUSE, CAMPAIGN_ABORT,
VIEW_CAMPAIGN_EVIDENCE, CAMPAIGN_RESUMPTION_REVIEW,
CAMPAIGN_RESUMPTION_AUTHORIZE, CAMPAIGN_RECOVERY_REQUEST,
CAMPAIGN_RECOVERY_REVIEW, CAMPAIGN_RECOVERY_AUTHORIZE.

Minimum SoD (comparisons use immutable `actorPrincipalId` only, as in
Phase 16 §14):

- creator != campaign authorizer
- campaign authorizer != cohort releaser
- resumption reviewer != resumption authorizer
- campaign recovery requester != campaign recovery reviewer
- campaign recovery reviewer != campaign recovery authorizer
- campaign recovery requester != campaign recovery authorizer
- inherited Phase 16 rollback SoD remains authoritative and is never
  weakened: requester != production authorizer; reviewer != authorizer;
  authorizer != executor; change-control validator != requester when
  that Phase 16 rule applies
- campaign recovery authorizer != Phase 16 rollback authorizer for the
  same recovery lineage
- campaign recovery authorizer != Phase 16 rollback executor for the
  same recovery lineage
- cohort releaser / execution operator != Phase 16 rollback authorizer
  for the same recovery lineage

One human MUST NOT create an unreviewed self-authorized rollback path.
Campaign recovery governance is not Phase 16 rollback mutation
authorization. Recovery safety budget is not rollback authority.
Unknown, null, blank, service, Agent, MCP, scheduler, and automation
identities fail closed (`UNAUTHENTICATED_HUMAN_ACTOR` /
`UNTRUSTED_ACTOR_SOURCE` / `PRODUCTION_SOD_VIOLATION`).

Authority is derived only from `AuthenticatedActor` (§3.4).
Agent/MCP/service/scheduler/unknown identities cannot satisfy human
governance.

# 11. Campaign Lease, Generations, and Fencing

Campaign lease is orchestration authority, not Phase 16 execution lease.
Durable fields: campaignId, holderId, fencingToken, acquiredAt,
expiresAt, status/version. One active lease per campaign. Fence
monotonically increases and is never reused.

## 11.1 Campaign lease

Authoritative owning row: `CampaignLease` (one active per campaign).
Acquisition uses PostgreSQL row lock / conditional SQL consistent with
`ProductionLeaseService`. Process-local locking is not authoritative.

## 11.2 CampaignControlGeneration allocation

`CampaignControlGeneration` is **campaign-global**.

- Authoritative owning row: exactly one `CampaignControlGeneration` row
  per campaign (`campaign_id` PK).
- Initial value: `1`.
- Allocation/invalidation occurs only inside an authority-bearing
  transaction that already holds rank-1 campaign lock and then rank-4
  `FOR UPDATE` on this row.
- Advance is monotonic `BIGINT` increment: `new = current + 1` performed
  by the database on the locked row.
- Application-side unlocked `current + 1` is forbidden.
- No decrement. No reuse. No ABA. A stale generation can never become
  current again.
- Overflow (`current = BIGINT` maximum) = fail closed
  `GENERATION_OVERFLOW`.
- Invalidation events remain those required by frozen architecture:
  abort; safety suspension; campaign authorization revocation; material
  campaign invalidation; certification/onboarding/security/credential
  revocation; required external change-control invalidation; revision
  invalidation; material health/observation policy invalidation.

## 11.3 ReleaseGeneration allocation

`ReleaseGeneration` is **campaign-global**, not cohort-scoped.

Rationale: frozen architecture treats release generations as
non-reusable campaign epochs; repository locking uses one monotonic
BIGINT owner row (same pattern as fencing / control generation); a
campaign-global counter prevents ABA across cohorts and resumptions
without per-cohort reset.

- Authoritative owning row: exactly one `CampaignReleaseGeneration` row
  per campaign (`campaign_id` PK).
- Initial value: `0` meaning no release has been allocated
  (`Release.state = NONE`).
- Each explicit human cohort release, while holding rank-1 campaign
  lock and rank-6 `FOR UPDATE` on this row, increments
  `new = current + 1` and binds that value immutably to the new
  `CohortRelease`.
- No decrement. No reuse. No ABA. Stale generations never become
  current again.
- Overflow = fail closed `GENERATION_OVERFLOW`.
- Suspension/abort/revocation/material/authorization-generation
  invalidation stales unused release. Resumption never resurrects an
  old release; a new explicit release allocates a new generation.

# 12. A18-R3-01 --- Lock Order

Authority-bearing Phase 18 transactions acquire durable locks only in
this order:

1. ProductionChangeCampaign
2. CampaignRevision / AuthorizedCampaignScope
3. CampaignLease
4. CampaignControlGeneration
5. CampaignCohort
6. CampaignReleaseGeneration / CohortRelease
7. CampaignExecutionItem
8. ForwardProgressionClosure
9. CampaignMutationSlot
10. CampaignSafetyExposure
11. CampaignSafetyBudgetReservation
12. CampaignExecutionBinding
13. CampaignExecutionHandoffEvidence
14. observation / suspension / resumption / recovery / evidence
15. CampaignAuditHead / latest event

Once rank N is acquired, no lower rank may subsequently be acquired.
Multiple rows at the same rank lock ascending stable PK. Use existing
PostgreSQL/JPA pessimistic / `FOR UPDATE` / atomic SQL patterns.
Process-local locking is not authoritative.

The same local transaction that establishes executable reservation
ownership MUST establish `CampaignMutationSlot` ownership (ranks 9-11
together).

DB unavailable, lock timeout, or unreconciled deadlock outcome = fail
closed. No blind retry of authority-bearing operations. Retried client
operation uses the same durable idempotency identity and re-reads
authoritative state.

Phase 18 locks MUST NOT be held across Phase 16, gateway, or vendor
network calls.

# 13. Transaction and Crash Boundaries

No distributed ACID transaction exists across
Phase18 → Phase16 → gateway → vendor.

Local authority transaction atomically validates, mutates Phase 18
state, appends audit, and records idempotency outcome.

When certainty is lost: new progression is denied; already possible
external mutation is resolved.

| Crash / uncertainty | Required behavior |
|---|---|
| Before Phase 18 local commit | No authority assumed. No binding, handoff, slot, or reservation is durable. |
| After `HANDOFF_PENDING` commit, before Phase 16 call | Recover the same `CampaignHandoffId`. Do not mint a replacement identity. Retry create-or-return-existing. |
| After Phase 16 call starts, response lost | Query Phase 16 by `CampaignHandoffId`. Never speculative second execution. |
| After Phase 16 commit, before Phase 18 correlation | Query Phase 16 by `CampaignHandoffId`; verify binding equality; persist correlation. |
| After Phase 16 response, before Phase 18 correlation commit | Same as previous row. Authoritative Phase 16 lineage wins. |
| Before gateway send | Final gateway preflight revalidates current fence, control/release generations, closure, grant, lease, P17 currentness, windows, safety. |
| At / after `MAY_HAVE_SENT` | Conservatively consume applicable exposure and slot; stop new mutation; resolve outcome. Never rewrite to `NOT_SENT`. |
| Vendor response lost; local persistence lost | Authoritative P16 / gateway evidence wins. |
| Verification completed; local campaign state not persisted | Reconstruct from P16 verification evidence. Never resend. |
| Reconciliation / observation persistence lost | Reconstruct from Phase 12 / telemetry readers. Never manufacture canonical writes. Never resend. |
| Rollback / recovery crash | Authoritative P16 rollback state decides. Ambiguity blocks additional campaign-associated mutation. |
| Governance command crash | Durable idempotency key + generation decide replay vs conflict. |
| Audit append failure | Fail closed. State transition and audit are atomic; neither commits alone. |
| DB unavailable | Fail closed. No new mutation authority. |
| Lock timeout / deadlock uncertainty | Fail closed. No blind retry of authority-bearing work. |

# 14. A18-R3-02 --- Common Matrix Fields and Denial Rule

Every executable matrix below uses these fields. Where a field does not
apply, the cell is `N/A`.

| Field | Meaning |
|---|---|
| FROM | Required current durable state |
| TRIGGER | Command or authoritative event |
| AUTHORITY | `AuthenticatedActor` requirement or `SYSTEM` / `SCHEDULER_EVAL` |
| PRECONDITIONS | Domain predicates |
| CURRENTNESS | Fingerprint / policy / cert / ticket / target currentness |
| LEASE/FENCE | Campaign lease and fencing requirement |
| GENERATIONS | Control and/or release generation requirement |
| LOCKS | Ranks from §12 acquired in order |
| ATOMIC WRITES | Durable mutations in the same local transaction |
| TO | Result state |
| AUDIT | Required campaign audit event type |
| IDEMPOTENCY | Durable identity / replay rule |
| FAILURE | Typed fail-closed reason |

Any transition absent from the applicable matrix MUST return a typed
denial (`INVALID_CAMPAIGN_TRANSITION`, `INVALID_RECOVERY_TRANSITION`,
or the more specific code if listed). No generic state setter is
allowed.

# 15. Campaign Transition Matrix

Principal states: DRAFT, UNDER_REVIEW, APPROVED, AUTHORIZED, READY,
CANARY_ACTIVE, CANARY_OBSERVING, CANARY_VERIFIED, PAUSED_FOR_RELEASE,
COHORT_ACTIVE, COHORT_OBSERVING, COHORT_VERIFIED, FINAL_OBSERVATION,
COMPLETED.

Exceptional states: SUSPENDED, STALE, EXPIRED, ABORTED,
RECOVERY_REQUIRED, OUTCOME_UNRESOLVED, MANUAL_INTERVENTION_REQUIRED.

Terminal campaign states: COMPLETED, ABORTED, EXPIRED. Exceptional
blocking states are not successful completion.

After `ForwardProgressionClosure = CLOSED`, every forward-progression
transition (new release, new forward handoff, READY→CANARY_ACTIVE,
PAUSED_FOR_RELEASE→COHORT_ACTIVE, or any new forward mutation
eligibility) is forbidden (`FORWARD_PROGRESSION_CLOSED`). Outcome
resolution, recovery, reconciliation, post-recovery observation,
evidence, audit, and final disposition remain allowed.

## 15.1 Principal campaign transitions

**CM-01** none → DRAFT\
TRIGGER=`CREATE_CAMPAIGN`; AUTHORITY=`HUMAN`+`CAMPAIGN_CREATE`;
PRECONDITIONS=one target, CELL/txPower, size<=effective max, canary
exactly one, no duplicate (target,cell,parameter); CURRENTNESS=target
exists; LEASE/FENCE=N/A; GENERATIONS=control=1, release=0;
LOCKS=1,2,4,15; ATOMIC WRITES=campaign+revision+items+cohorts+closure
OPEN+slot EMPTY+exposure zero+audit+idempotency; TO=`DRAFT`;
AUDIT=`CAMPAIGN_CREATED`; IDEMPOTENCY=`Idempotency-Key`;
FAILURE=`CAMPAIGN_SIZE_LIMIT` / `UNSUPPORTED_OBJECT_OR_PARAMETER` /
`UNAUTHENTICATED_HUMAN_ACTOR`.

**CM-02** DRAFT → UNDER_REVIEW\
TRIGGER=`SUBMIT_FOR_REVIEW`; AUTHORITY=`HUMAN`+`CAMPAIGN_CREATE`;
PRECONDITIONS=scope complete; submitter is the campaign creator;
CURRENTNESS=fingerprint current; LEASE/FENCE=N/A; GENERATIONS=current
control; LOCKS=1,2,15; ATOMIC WRITES=state+audit+idempotency;
TO=`UNDER_REVIEW`; AUDIT=`CAMPAIGN_SUBMITTED`;
IDEMPOTENCY=`Idempotency-Key`; FAILURE=`INVALID_CAMPAIGN_TRANSITION`.

**CM-03** UNDER_REVIEW → APPROVED\
TRIGGER=`REVIEW_APPROVE`; AUTHORITY=`HUMAN`+`CAMPAIGN_REVIEW`;
PRECONDITIONS=review recorded; CURRENTNESS=fingerprint current;
LEASE/FENCE=N/A; GENERATIONS=current control; LOCKS=1,2,15;
ATOMIC WRITES=review+state+audit+idempotency; TO=`APPROVED`;
AUDIT=`CAMPAIGN_REVIEWED`; IDEMPOTENCY=`Idempotency-Key`;
FAILURE=`UNAUTHENTICATED_HUMAN_ACTOR` / SoD.

**CM-04** UNDER_REVIEW → DRAFT\
TRIGGER=`REVIEW_RETURN`; AUTHORITY=`HUMAN`+`CAMPAIGN_REVIEW`;
PRECONDITIONS=no authorization exists; CURRENTNESS=N/A; LEASE/FENCE=N/A;
GENERATIONS=current; LOCKS=1,2,15; ATOMIC WRITES=state+audit;
TO=`DRAFT`; AUDIT=`CAMPAIGN_REVIEW_RETURNED`;
IDEMPOTENCY=`Idempotency-Key`; FAILURE=`INVALID_CAMPAIGN_TRANSITION`.

**CM-05** APPROVED → AUTHORIZED\
TRIGGER=`AUTHORIZE_CAMPAIGN`; AUTHORITY=`HUMAN`+`CAMPAIGN_AUTHORIZE`;
PRECONDITIONS=SoD creator != authorizer; freeze
`AuthorizedCampaignScope`; CURRENTNESS=fingerprint, policies, windows,
ticket-if-required, P17 currentness; LEASE/FENCE=N/A;
GENERATIONS=bind authorizationGeneration + current control;
LOCKS=1,2,4,15; ATOMIC WRITES=authorization+immutable scope+state+audit;
TO=`AUTHORIZED`; AUDIT=`CAMPAIGN_AUTHORIZED`;
IDEMPOTENCY=`Idempotency-Key`; FAILURE=`PRODUCTION_SOD_VIOLATION` /
currentness codes.

**CM-06** AUTHORIZED → READY\
TRIGGER=`EVALUATE_READY`; AUTHORITY=`SYSTEM` (deterministic);
PRECONDITIONS=authorization current, canary planned, closure OPEN;
CURRENTNESS=all send-relevant currentness except grant (no premint);
LEASE/FENCE=optional orchestration lease; GENERATIONS=current control;
LOCKS=1,2,4,8,15; ATOMIC WRITES=state+audit; TO=`READY`;
AUDIT=`CAMPAIGN_READY`; IDEMPOTENCY=deterministic re-eval same result;
FAILURE=currentness / `FORWARD_PROGRESSION_CLOSED`.

**CM-07** READY → CANARY_ACTIVE\
TRIGGER=`RELEASE_CANARY`; AUTHORITY=`HUMAN`+`RELEASE_COHORT`;
PRECONDITIONS=canary exactly one cell, authorizer != releaser, closure
OPEN, no unresolved outcome; CURRENTNESS=auth+cert+onboarding+ticket+
health/safety current; LEASE/FENCE=current campaign lease+fence;
GENERATIONS=allocate new campaign-global release generation; bind
current control; LOCKS=1-8,15; ATOMIC WRITES=release row+generation
increment+canary items PLANNED→RELEASED+state+audit;
TO=`CANARY_ACTIVE`; AUDIT=`COHORT_RELEASED`;
IDEMPOTENCY=`Idempotency-Key` (duplicate release does not remint);
FAILURE=`FORWARD_PROGRESSION_CLOSED` / SoD / currentness.

**CM-08** CANARY_ACTIVE → CANARY_OBSERVING\
TRIGGER=`CANARY_ITEMS_ENTER_OBSERVING`; AUTHORITY=`SYSTEM`;
PRECONDITIONS=canary item reached OBSERVING via item matrix;
CURRENTNESS=observation boundary valid; LEASE/FENCE=N/A for this
transition; GENERATIONS=bound release/control still current for
observation policy; LOCKS=1,5,7,15; ATOMIC WRITES=campaign state+audit;
TO=`CANARY_OBSERVING`; AUDIT=`CAMPAIGN_CANARY_OBSERVING`;
IDEMPOTENCY=state-guarded; FAILURE=`INVALID_CAMPAIGN_TRANSITION`.

**CM-09** CANARY_OBSERVING → CANARY_VERIFIED\
TRIGGER=`CANARY_HEALTH_AND_SAFETY_PASS`; AUTHORITY=`SYSTEM`;
PRECONDITIONS=Network=HEALTHY **and** Safety=SAFE, current (not stale
historical HEALTHY/SAFE), observation complete, no interference;
CURRENTNESS=current health/observation/safety policy;
LEASE/FENCE=N/A; GENERATIONS=bound policy versions current;
LOCKS=1,5,7,14,15; ATOMIC WRITES=state+health snapshot+audit;
TO=`CANARY_VERIFIED`; AUDIT=`CAMPAIGN_CANARY_VERIFIED`;
IDEMPOTENCY=state-guarded;
FAILURE=health/safety/provenance codes.

**CM-10** CANARY_VERIFIED → PAUSED_FOR_RELEASE\
TRIGGER=`CANARY_TERMINALLY_ACCOUNTED`; AUTHORITY=`SYSTEM`;
PRECONDITIONS=canary items terminally accounted, no unresolved;
CURRENTNESS=N/A; LEASE/FENCE=N/A; GENERATIONS=N/A; LOCKS=1,5,7,15;
ATOMIC WRITES=state+audit; TO=`PAUSED_FOR_RELEASE`;
AUDIT=`CAMPAIGN_PAUSED_FOR_RELEASE`; IDEMPOTENCY=state-guarded;
FAILURE=`INVALID_CAMPAIGN_TRANSITION`.

**CM-11** PAUSED_FOR_RELEASE → COHORT_ACTIVE\
TRIGGER=`RELEASE_STANDARD_COHORT`; AUTHORITY=`HUMAN`+`RELEASE_COHORT`;
PRECONDITIONS=predecessor verification+reconciliation+observation+safety
complete; future cohort not pre-released; closure OPEN; authorizer !=
releaser; CURRENTNESS=full currentness; LEASE/FENCE=current lease+fence;
GENERATIONS=new campaign-global release generation; current control;
LOCKS=1-8,15; ATOMIC WRITES=release+generation+item RELEASED+state+audit;
TO=`COHORT_ACTIVE`; AUDIT=`COHORT_RELEASED`;
IDEMPOTENCY=`Idempotency-Key`;
FAILURE=`FORWARD_PROGRESSION_CLOSED` / predecessor incomplete.

**CM-12** COHORT_ACTIVE → COHORT_OBSERVING\
TRIGGER=`COHORT_ITEMS_ENTER_OBSERVING`; AUTHORITY=`SYSTEM`;
PRECONDITIONS=every mutation item of the active standard cohort that
required send has reached OBSERVING via the item matrix, or the
policy-required observing subset has reached OBSERVING; no unresolved
item outcome; PRECONDITIONS also require ForwardProgressionClosure=OPEN
for continued forward observation accounting;
CURRENTNESS=each item PostChangeObservationBoundary valid and
observationStart >= vendorVerifiedAt; LEASE/FENCE=N/A for this
transition; GENERATIONS=bound release generation and control generation
still current for the observation policy; LOCKS=1,5,7,15;
ATOMIC WRITES=campaign state COHORT_OBSERVING + audit;
TO=`COHORT_OBSERVING`; AUDIT=`CAMPAIGN_COHORT_OBSERVING`;
IDEMPOTENCY=state-guarded; FAILURE=`INVALID_CAMPAIGN_TRANSITION` /
`FORWARD_PROGRESSION_CLOSED`.

**CM-13** COHORT_OBSERVING → COHORT_VERIFIED\
TRIGGER=`COHORT_HEALTH_AND_SAFETY_PASS`; AUTHORITY=`SYSTEM`;
PRECONDITIONS=required observation complete for the active cohort;
NetworkObservationHealth=HEALTHY and OperationalSafetyHealth=SAFE,
both **current** (historical HEALTHY/SAFE is insufficient); no
superseding interference; no unresolved outcome;
CURRENTNESS=current health-policy version, observation-policy version,
certification/onboarding/security/credential/ticket currentness;
LEASE/FENCE=N/A; GENERATIONS=bound policy versions still current;
LOCKS=1,5,7,14,15; ATOMIC WRITES=state+current health snapshot+audit;
TO=`COHORT_VERIFIED`; AUDIT=`CAMPAIGN_COHORT_VERIFIED`;
IDEMPOTENCY=state-guarded;
FAILURE=health/safety/provenance codes / stale historical HEALTHY.

**CM-14** COHORT_VERIFIED → PAUSED_FOR_RELEASE\
TRIGGER=`MORE_AUTHORIZED_COHORTS_REMAIN`; AUTHORITY=`SYSTEM`;
PRECONDITIONS=at least one authorized cohort remains unreleased;
ForwardProgressionClosure=OPEN; no item OUTCOME_UNRESOLVED,
RECOVERY_REQUIRED, or MANUAL_INTERVENTION_REQUIRED;
CURRENTNESS=campaign fingerprint and authorization current;
LEASE/FENCE=N/A; GENERATIONS=current control; unused prior release
must not be treated as authority for the next cohort;
LOCKS=1,2,5,8,15; ATOMIC WRITES=state PAUSED_FOR_RELEASE+audit;
TO=`PAUSED_FOR_RELEASE`; AUDIT=`CAMPAIGN_PAUSED_FOR_RELEASE`;
IDEMPOTENCY=state-guarded; FAILURE=`FORWARD_PROGRESSION_CLOSED` /
unresolved codes.

**CM-15** COHORT_VERIFIED → FINAL_OBSERVATION\
TRIGGER=`LAST_AUTHORIZED_COHORT_ACCOUNTED`; AUTHORITY=`SYSTEM`;
PRECONDITIONS=no remaining unreleased authorized cohorts; every
authorized mutation item is either terminally accounted or in a
required final observation interval; no unresolved outcome;
CURRENTNESS=observation policy current for items still observing;
LEASE/FENCE=N/A; GENERATIONS=bound observation policy versions;
LOCKS=1,2,5,7,15; ATOMIC WRITES=state FINAL_OBSERVATION+audit;
TO=`FINAL_OBSERVATION`; AUDIT=`CAMPAIGN_FINAL_OBSERVATION`;
IDEMPOTENCY=state-guarded; FAILURE=`INVALID_CAMPAIGN_TRANSITION` /
`CAMPAIGN_SCOPE_INCOMPLETE`.

**CM-16** FINAL_OBSERVATION → COMPLETED\
TRIGGER=`COMPLETE_CAMPAIGN`; AUTHORITY=`SYSTEM`;
PRECONDITIONS=every AuthorizedCampaignScope item terminally accounted
exactly once; no item in OUTCOME_UNRESOLVED, RECOVERY_REQUIRED, or
MANUAL_INTERVENTION_REQUIRED; required verification/reconciliation/
observation/health present; audit valid; CURRENTNESS=N/A for historical
accounting; LOCKS=1,2,7,15; ATOMIC WRITES=disposition COMPLETED+audit;
TO=`COMPLETED`; AUDIT=`CAMPAIGN_COMPLETED`;
FAILURE=`CAMPAIGN_SCOPE_INCOMPLETE` / unresolved codes.

## 15.2 Legal entries into SUSPENDED

From DRAFT, UNDER_REVIEW, APPROVED, AUTHORIZED, READY, CANARY_ACTIVE,
CANARY_OBSERVING, CANARY_VERIFIED, PAUSED_FOR_RELEASE, COHORT_ACTIVE,
COHORT_OBSERVING, COHORT_VERIFIED, FINAL_OBSERVATION:

**CM-S1** TRIGGER=`OPERATOR_PAUSE`; AUTHORITY=`HUMAN`+`CAMPAIGN_PAUSE`;
PRECONDITIONS=not already terminal; CURRENTNESS=N/A;
LEASE/FENCE=current lease recommended; GENERATIONS=advance control
generation; stale unused release; LOCKS=1,3,4,6,8,15;
ATOMIC WRITES=suspension OPERATOR_PAUSE + control increment + unused
release STALE + state SUSPENDED + audit;
TO=`SUSPENDED`; AUDIT=`CAMPAIGN_OPERATOR_PAUSED`;
IDEMPOTENCY=`Idempotency-Key`;
FAILURE=`INVALID_CAMPAIGN_TRANSITION` from COMPLETED/ABORTED/EXPIRED.

**CM-S2** TRIGGER=`SAFETY_SUSPEND`; AUTHORITY=`SYSTEM` or
`SCHEDULER_EVAL` (evaluate only; not human governance);
PRECONDITIONS=safety/health/currentness non-progressable;
GENERATIONS=advance control; stale unused release; LOCKS=1,3,4,6,8,15;
ATOMIC WRITES=suspension SAFETY_SUSPENSION + control increment + unused
release STALE + state SUSPENDED + audit;
TO=`SUSPENDED`; AUDIT=`CAMPAIGN_SAFETY_SUSPENDED`;
IDEMPOTENCY=cause-keyed; does **not** mutate, rollback, resume, or
close forward progression by itself.

Suspension does not cancel in-flight possible mutation and does not
auto-rollback.

## 15.3 Legal entries into STALE

From AUTHORIZED, READY, CANARY_*, PAUSED_FOR_RELEASE, COHORT_*,
FINAL_OBSERVATION, SUSPENDED:

**CM-ST** TRIGGER=`MATERIAL_INVALIDATION` (fingerprint mismatch,
authorization generation stale, required P17/ticket/policy invalidation,
superseded revision); AUTHORITY=`SYSTEM`;
PRECONDITIONS=material authority no longer current; unused release
stale; GENERATIONS=advance or obsolete control;
LOCKS=1,2,4,6,15; TO=`STALE`; AUDIT=`CAMPAIGN_STALE`;
FAILURE=N/A (fail-closed destination). STALE cannot progress until new
governed revision/authorization. STALE does not rewrite consumed
history and does not terminate MAY_HAVE_SENT resolution.

## 15.4 Legal entries into OUTCOME_UNRESOLVED

From CANARY_ACTIVE, CANARY_OBSERVING, COHORT_ACTIVE, COHORT_OBSERVING,
or any state whose active item/recovery reached unresolved send
possibility:

**CM-OU** TRIGGER=`UNRESOLVED_MUTATION_OUTCOME`; AUTHORITY=`SYSTEM`;
PRECONDITIONS=authoritative evidence cannot establish send outcome;
ATOMIC WRITES=unresolved exposure retained/consumed + slot held +
campaign OUTCOME_UNRESOLVED + audit; TO=`OUTCOME_UNRESOLVED`;
AUDIT=`CAMPAIGN_OUTCOME_UNRESOLVED`;
FAILURE=N/A. Blocks all additional campaign-associated mutation.
Resumption cannot become EFFECTIVE while this remains.

## 15.5 Legal entries into RECOVERY_REQUIRED

From any non-terminal campaign state when an item or campaign
**determines** `RECOVERY_REQUIRED`. Determination is not rollback
authorization and is not a Phase 16 grant.

**CM-RR** TRIGGER=`DETERMINED_RECOVERY_REQUIRED`; AUTHORITY=`SYSTEM`
from the item matrix (or equivalent governed failure determination);
PRECONDITIONS=item TO=`RECOVERY_REQUIRED`; this transition MUST NOT
wait for Phase 16 rollback authorization;
LOCKS=1,7,8,15;
ATOMIC WRITES=**ForwardProgressionClosure OPEN→CLOSED in this same
authoritative local transaction** + campaign RECOVERY_REQUIRED + audit;
TO=`RECOVERY_REQUIRED`; AUDIT=`FORWARD_PROGRESSION_CLOSED`;
IDEMPOTENCY=once-only closure; FAILURE=`INVALID_CAMPAIGN_TRANSITION`
if already COMPLETED without recovery accounting.

A campaign-associated Phase 16 rollback MUST NOT be newly authorized
or handed off while `ForwardProgressionClosure = OPEN`. See §19.

## 15.6 Abort transitions

**CM-AB** NOT_ABORTED (any non-terminal, non-ABORTED state) → ABORTED\
TRIGGER=`ABORT_CAMPAIGN`; AUTHORITY=`HUMAN`+`CAMPAIGN_ABORT`;
PRECONDITIONS=not already ABORTED; GENERATIONS=advance control
**before** any later new send; unused release STALE; closure unchanged
unless a concurrent recovery trigger applies;
LOCKS=1,3,4,6,8,15; ATOMIC WRITES=abort flag+control increment+unused
release STALE+state ABORTED+audit; TO=`ABORTED`;
AUDIT=`CAMPAIGN_ABORTED`; IDEMPOTENCY=`Idempotency-Key`;
No reverse. Abort is not rollback and does not auto-initiate rollback.
Outcome resolution continues.

## 15.7 Recovery-related campaign transitions

From RECOVERY_REQUIRED the campaign remains RECOVERY_REQUIRED until
recovery matrix reaches a terminal recovery disposition, then:

- all recovered → campaign final disposition `RECOVERED`
- mixed → `PARTIALLY_RECOVERED`
- failed/unresolved → remain `RECOVERY_REQUIRED` /
  `OUTCOME_UNRESOLVED` / `MANUAL_INTERVENTION_REQUIRED`

Successful recovery MUST NOT reopen forward progression or
`ForwardProgressionClosure`.

## 15.8 Expiry and manual intervention

**CM-EX** → EXPIRED only from states with **no** possible send and **no**
unresolved outcome, when required window/authorization/ticket has
expired. Timer expiry during MAY_HAVE_SENT is not EXPIRED.
TRIGGER=`AUTHORITY_EXPIRED`; AUTHORITY=`SYSTEM`/`SCHEDULER_EVAL`;
GENERATIONS=control obsolete; TO=`EXPIRED`; AUDIT=`CAMPAIGN_EXPIRED`.

**CM-MI** → MANUAL_INTERVENTION_REQUIRED when item/recovery matrix so
determines (third/unexpected state). Blocks additional mutation.

## 15.9 Final dispositions

See §22. COMPLETED is refused if any authorized item is
OUTCOME_UNRESOLVED, RECOVERY_REQUIRED, MANUAL_INTERVENTION_REQUIRED, or
lacks required successful verification/reconciliation/observation.

# 16. Cohort Transition Matrix

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| none | CREATE_REVISION | HUMAN CREATE | canary first, one cell | N/A | N/A | control current | 1,2,5,15 | cohort PLANNED | PLANNED | COHORT_PLANNED | revision create | size/canary |
| PLANNED | PREDECESSOR_INCOMPLETE | SYSTEM | predecessor not complete | N/A | N/A | N/A | 1,5,15 | state | WAITING_PREDECESSOR | COHORT_WAITING | state-guard | N/A |
| PLANNED | CANARY_OR_PREDECESSOR_COMPLETE | SYSTEM | canary or predecessor verified+reconciled+observed+safe | current | N/A | current control | 1,5,8,15 | state | RELEASE_ELIGIBLE | COHORT_RELEASE_ELIGIBLE | state-guard | FORWARD_PROGRESSION_CLOSED |
| WAITING_PREDECESSOR | PREDECESSOR_COMPLETE | SYSTEM | pred verification+reconciliation+observation+safety | current | N/A | current control | 1,5,8,15 | state | RELEASE_ELIGIBLE | COHORT_RELEASE_ELIGIBLE | state-guard | predecessor incomplete |
| RELEASE_ELIGIBLE | RELEASE_COHORT | HUMAN RELEASE_COHORT | SoD; closure OPEN; not future pre-release | full | current lease+fence | allocate campaign-global release gen | 1-8,15 | release+items RELEASED | RELEASED | COHORT_RELEASED | Idempotency-Key | FORWARD_PROGRESSION_CLOSED |
| RELEASED | ITEM_PROGRESSES | SYSTEM | a contained item leaves RELEASED | bound release current | N/A | bound gens | 1,5,7,15 | state | ACTIVE | COHORT_ACTIVE | state-guard | INVALID |
| ACTIVE | ITEMS_OBSERVING | SYSTEM | required items OBSERVING | observation current | N/A | bound | 1,5,7,15 | state | OBSERVING | COHORT_OBSERVING | state-guard | INVALID |
| OBSERVING | HEALTH_SAFE_PASS | SYSTEM | HEALTHY and SAFE current | current policies | N/A | bound | 1,5,14,15 | state | VERIFIED | COHORT_VERIFIED | state-guard | health/safety |
| VERIFIED | ITEMS_ACCOUNTED | SYSTEM | all cohort items terminal | N/A | N/A | N/A | 1,5,7,15 | state | COMPLETED | COHORT_COMPLETED | state-guard | scope incomplete |
| applicable | MATERIAL_INVALIDATION | SYSTEM | unused release | N/A | N/A | stale unused release | 1,5,6,15 | release STALE | STALE | COHORT_STALE | state-guard | N/A |
| applicable | OPERATOR_PAUSE or SAFETY_SUSPEND | see §15.2 | not terminal | N/A | see §15.2 | control++ | 1,4,5,6,15 | state | SUSPENDED | COHORT_SUSPENDED | see §15.2 | N/A |
| applicable | BLOCKING_CONDITION | SYSTEM | eligibility lost pre-send | current | N/A | current | 1,5,7,15 | state | BLOCKED | COHORT_BLOCKED | state-guard | N/A |

Resumption never restores RELEASE_ELIGIBLE from a stale release.
A new human release and new generation are required.

# 17. Combined Mutation Slot

Exactly one authoritative durable combined mutation slot per campaign.
There MUST NOT be independent forward and recovery mutation slots.

Conceptual persisted state `CampaignMutationSlot`:

```text
campaignId
holderType = FORWARD | RECOVERY | NONE
holderReferenceId
reservationId
campaignFencingToken
campaignControlGeneration
state = EMPTY | ACQUIRED | HELD_MAY_HAVE_SENT
acquiredAt
version
```

Java field names may follow repository conventions; semantics are
mandatory.

`ExternalMutationOutcomeResolution` and
`CampaignMutationSerializationRelease` are different concepts.
Resolving a vendor response or send-outcome ambiguity MUST NOT, by
itself, release serialization capacity for another campaign mutation.

Invariant (at all times):

`activeForwardCampaignMutations + activeRecoveryCampaignMutations <= 1`

Acquisition MUST be atomic using PostgreSQL row locking / conditional
SQL consistent with `ProductionRateLimitService`. The same local
transaction that establishes executable reservation ownership MUST
establish slot ownership.

Concurrent acquisition: one wins; all others fail closed
`MUTATION_SLOT_UNAVAILABLE`.

DB unavailable / lock timeout / unresolved transaction outcome: no new
mutation authority.

Slot release MUST NOT occur merely because a timer elapsed when the
associated mutation may have reached `MAY_HAVE_SENT`
(`SAFETY_EXPOSURE_ALREADY_ACCOUNTED` / conservative retain).

Stale fence/generation on the slot = `MUTATION_SLOT_STALE`.

# 18. Item Transition Matrix

Item states: PLANNED, RELEASED, ELIGIBLE, HANDOFF_PENDING, HANDED_OFF,
PRE_SEND, MAY_HAVE_SENT, VENDOR_ACCEPTED, VERIFYING,
PRODUCTION_VERIFIED, RECONCILIATION_PENDING, CANONICAL_RECONCILED,
OBSERVING, OBSERVATION_HEALTHY, COMPLETED, BLOCKED, STALE, SUSPENDED,
NOT_SENT, VENDOR_REJECTED, VERIFICATION_FAILED, OUTCOME_UNRESOLVED,
RECOVERY_REQUIRED, MANUAL_INTERVENTION_REQUIRED.

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| PLANNED | CURRENT_EXACT_COHORT_RELEASE | HUMAN RELEASE | item in released membership | full | current | new release gen + current control | 1-8,7,15 | item RELEASED | RELEASED | ITEM_RELEASED | release key | FORWARD_PROGRESSION_CLOSED |
| RELEASED | INDIVIDUAL_ELIGIBILITY | SYSTEM | predecessor policy; closure OPEN | full send currentness except grant | current | bound gens current | 1,3,4,6,7,8,15 | item ELIGIBLE | ELIGIBLE | ITEM_ELIGIBLE | state-guard | predecessor / CLOSED |
| ELIGIBLE | START_HANDOFF | SYSTEM orchestrator | slot+reservation atomic; closure OPEN | full | current fence | bound gens | 1-13,15 | slot ACQUIRED FORWARD; reservation RESERVED; binding; CampaignHandoffId; HANDOFF_PENDING | HANDOFF_PENDING | ITEM_HANDOFF_PENDING | CampaignHandoffId | MUTATION_SLOT_UNAVAILABLE / CLOSED |
| HANDOFF_PENDING | P16_LINEAGE_CORRELATED | SYSTEM | exact binding equality | bound | bound fence | bound | 1,7,12,13,15 after P16 returns (new tx) | correlate P16 ids | HANDED_OFF | ITEM_HANDED_OFF | CampaignHandoffId create-or-return | HANDOFF_IDEMPOTENCY_CONFLICT |
| HANDED_OFF | PRE_SEND_READY | SYSTEM/gateway evidence | trusted pre-send readiness; not send | full preflight | current P16+campaign | current | no P18 lock across remote | state | PRE_SEND | ITEM_PRE_SEND | state-guard | currentness |
| PRE_SEND | GATEWAY_SEND_BOUNDARY | SYSTEM | authoritative gateway send-boundary evidence | N/A after possibility | N/A | N/A | 1,7,8,9,10,11,15 | reservation CONSUMED; exposure++; slot HELD_MAY_HAVE_SENT | MAY_HAVE_SENT | ITEM_MAY_HAVE_SENT | send-evidence idempotent | SAFETY_EXPOSURE_ALREADY_ACCOUNTED |
| PRE_SEND | AUTHORITATIVE_NO_SEND | SYSTEM | proof no send occurred | N/A | N/A | N/A | 1,7,9,10,11,15 | reservation RELEASED; slot EMPTY | NOT_SENT | ITEM_NOT_SENT | evidence-keyed | unknown != NOT_SENT |
| MAY_HAVE_SENT | ACCEPTANCE_EVIDENCE | SYSTEM | authoritative acceptance | N/A | N/A | N/A | 1,7,15 | state | VENDOR_ACCEPTED | ITEM_VENDOR_ACCEPTED | evidence-keyed | N/A |
| MAY_HAVE_SENT | ACCEPTANCE_UNAVAILABLE_VERIFY_REQUIRED | SYSTEM | verification required | N/A | N/A | N/A | 1,7,15 | state | VERIFYING | ITEM_VERIFYING | evidence-keyed | N/A |
| MAY_HAVE_SENT | OUTCOME_UNESTABLISHABLE | SYSTEM | cannot establish outcome | N/A | N/A | N/A | 1,7,9,10,15 | unresolved exposure | OUTCOME_UNRESOLVED | ITEM_OUTCOME_UNRESOLVED | evidence-keyed | N/A |
| VENDOR_ACCEPTED | START_VERIFY | SYSTEM | P16 verification required | N/A | N/A | N/A | 1,7,15 | state | VERIFYING | ITEM_VERIFYING | state-guard | N/A |
| VERIFYING | DIRECT_DESIRED_READBACK | SYSTEM | desired-state readback | P16 verification policy | N/A | N/A | 1,7,15 | state | PRODUCTION_VERIFIED | ITEM_PRODUCTION_VERIFIED | evidence-keyed | N/A |
| VERIFYING | EXPECTED_PRECHANGE | SYSTEM | P16 expected-prechange evidence | P16 | N/A | N/A | 1,7,15 | NOT_SENT or MANUAL_INTERVENTION per P16; never auto-retry | NOT_SENT or MANUAL_INTERVENTION_REQUIRED | ITEM_EXPECTED_PRECHANGE | evidence-keyed | no auto-retry |
| VERIFYING | THIRD_UNEXPECTED_STATE | SYSTEM | unexpected observed state | P16 | N/A | N/A | 1,7,15 | state | MANUAL_INTERVENTION_REQUIRED | ITEM_MANUAL_INTERVENTION | evidence-keyed | N/A |
| VERIFYING | GOVERNED_FAILURE | SYSTEM/human P16 | governed failure determination | P16 | N/A | N/A | 1,7,8,15 | closure CLOSED if RECOVERY_REQUIRED | VERIFICATION_FAILED or RECOVERY_REQUIRED | ITEM_VERIFICATION_FAILED | evidence-keyed | N/A |
| VERIFYING | SEND_UNRESOLVED | SYSTEM | possible send unresolved | N/A | N/A | N/A | 1,7,10,15 | unresolved exposure | OUTCOME_UNRESOLVED | ITEM_OUTCOME_UNRESOLVED | evidence-keyed | N/A |
| PRODUCTION_VERIFIED | BEGIN_RECONCILE | SYSTEM | P12 reader available | N/A | N/A | N/A | 1,7,15 | state | RECONCILIATION_PENDING | ITEM_RECONCILIATION_PENDING | state-guard | N/A |
| RECONCILIATION_PENDING | PROOF_FORM_A_OR_B | SYSTEM | §23 proof | Phase12 current | N/A | N/A | 1,7,14,15 | state | CANONICAL_RECONCILED | ITEM_CANONICAL_RECONCILED | evidence-keyed | RECONCILIATION_PROVENANCE_UNAVAILABLE |
| CANONICAL_RECONCILED | OPEN_OBSERVATION | SYSTEM | boundary valid; start>=vendorVerifiedAt | observation policy | N/A | bound observation policy | 1,7,14,15 | persist boundary | OBSERVING | ITEM_OBSERVING | state-guard | provenance |
| OBSERVING | INTERVAL_HEALTHY_SAFE | SYSTEM | complete interval; HEALTHY and SAFE **current** | current policies/freshness | N/A | bound vs current | 1,7,14,15 | snapshot | OBSERVATION_HEALTHY | ITEM_OBSERVATION_HEALTHY | evidence-keyed | stale historical HEALTHY |
| OBSERVATION_HEALTHY | COMPLETE_ITEM | SYSTEM | all required evidence/audit | N/A | N/A | N/A | 1,7,15 | state | COMPLETED | ITEM_COMPLETED | state-guard | evidence gap |

Pre-send invalidation may lead to BLOCKED / STALE / SUSPENDED / NOT_SENT
only when no possible send exists. Post-MAY_HAVE_SENT invalidation
never rewrites to NOT_SENT; outcome resolution continues.
RECOVERY_REQUIRED permanently closes forward progression first (§19).

`VENDOR_REJECTED` is a no-send terminal ONLY when authoritative
evidence proves the operation did not reach `MAY_HAVE_SENT` (the
`AUTHORITATIVE_PRE_SEND_REJECTION` / `AUTHORITATIVE_NOT_SENT` paths).
If `MAY_HAVE_SENT` has already occurred, a later vendor rejection MUST
NOT rewrite the operation to `NOT_SENT`, return consumed exposure,
automatically release serialization capacity, permit speculative retry,
or permit another mutation before required outcome resolution.
Historical send-boundary evidence is preserved.

# 19. ForwardProgressionClosure

Only `OPEN → CLOSED` is legal. `CLOSED → OPEN` is impossible.

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| OPEN | DETERMINED_RECOVERY_REQUIRED | SYSTEM | item/campaign determines RECOVERY_REQUIRED; Phase 16 rollback is not yet requested | N/A | N/A | N/A | 1,7,8,15 | closure CLOSED **in the same transaction** that persists RECOVERY_REQUIRED | CLOSED | FORWARD_PROGRESSION_CLOSED | once-only | N/A |
| CLOSED | any reopen attempt including resumption, restart, policy change, or successful rollback | any | N/A | N/A | N/A | N/A | N/A | none | CLOSED | N/A | N/A | FORWARD_PROGRESSION_CLOSED |

Persistence / domain invariant:

No campaign-associated Phase 16 rollback execution may be newly
authorized or handed off while the corresponding campaign revision has
`ForwardProgressionClosure = OPEN`.

Authoritative ordering (Phase 16 grant/consume/preflight order remains
the inherited Phase 16 repository semantics; no second grant writer):

1. item/campaign determines `RECOVERY_REQUIRED`
2. same authoritative local transaction durably performs
   `ForwardProgressionClosure OPEN → CLOSED`
3. commit closure
4. campaign recovery request (`CAMPAIGN_RECOVERY_REQUEST`)
5. campaign recovery review (`CAMPAIGN_RECOVERY_REVIEW`)
6. campaign recovery authorization (`CAMPAIGN_RECOVERY_AUTHORIZE`)
7. inherited Phase 16 rollback request / review / authorization
8. recovery safety reservation
9. acquire the single combined mutation slot for RECOVERY (or
   `REBIND_HOLDER_TO_RECOVERY` if the slot is already held by the
   failed forward)
10. durable recovery `CampaignHandoffId` / binding
11. Phase 16 rollback handoff
12. Phase 16 rollback grant
13. gateway final preflight
14. possible external rollback send
15. verification
16. canonical reconciliation
17. post-recovery observation
18. final disposition

While OPEN, forbidden: campaign recovery AUTHORIZED or stronger;
recovery safety reservation; combined recovery mutation-slot
acquisition; Phase 16 rollback handoff; rollback grant path; gateway
rollback send. All fail `RECOVERY_REQUIRES_FORWARD_CLOSURE`.

Successful rollback MUST NOT reopen closure. Human resumption MUST NOT
reopen closure. Restart MUST NOT reopen closure. Policy change MUST NOT
reopen closure. Later forward optimization requires new governed
authority and a new canary lifecycle according to the frozen
architecture.

# 20. Release Transition Matrix

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| NONE | RELEASE_COHORT | HUMAN RELEASE_COHORT | §16 RELEASE_ELIGIBLE; closure OPEN | full | current | increment campaign-global ReleaseGeneration | 1-8,15 | new release row ACTIVE | ACTIVE | COHORT_RELEASED | Idempotency-Key | FORWARD_PROGRESSION_CLOSED |
| ACTIVE | MEMBERSHIP_TERMINALLY_ACCOUNTED | SYSTEM | all released items terminal | N/A | N/A | bound gen | 1,6,7,15 | CONSUMED | CONSUMED | RELEASE_CONSUMED | state-guard | N/A |
| ACTIVE | SUSPEND/ABORT/REVOKE/MATERIAL/AUTH_GEN | SYSTEM or human abort/pause | unused | N/A | N/A | bound gen stale; control++ if required | 1,4,6,15 | STALE | STALE | RELEASE_STALE | state-guard | N/A |
| STALE | resurrect | any | N/A | N/A | N/A | N/A | N/A | none | STALE | N/A | N/A | INVALID / stale release |
| CONSUMED | reactivate | any | N/A | N/A | N/A | N/A | N/A | none | CONSUMED | N/A | N/A | INVALID |

STALE→ACTIVE forbidden. CONSUMED→ACTIVE forbidden. Resumption requires
a new release row and a new campaign-global generation.

# 21. Safety Reservation, Exposure, and Slot Matrices

## 21.1 Safety reservation

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| none | RESERVE_FOR_ELIGIBLE_ITEM | SYSTEM | item ELIGIBLE; closure OPEN for forward | full | current | bound | 1,8-11,15 | create RESERVED + slot ACQUIRED | RESERVED | SAFETY_RESERVED | reservation id | MUTATION_SLOT_UNAVAILABLE |
| RESERVED | MAY_HAVE_SENT_OR_IRREVERSIBLE | SYSTEM | authoritative send-boundary evidence | N/A | bound | bound | 1,8-11,15 | §21.3 atomic consume | CONSUMED | SAFETY_CONSUMED | send-evidence once | SAFETY_EXPOSURE_ALREADY_ACCOUNTED |
| RESERVED | AUTHORITATIVE_NO_MAY_HAVE_SENT | SYSTEM | proof operation did **not** reach MAY_HAVE_SENT | N/A | bound | bound | 1,9-11,15 | RELEASED or EXPIRED; slot EMPTY | RELEASED or EXPIRED | SAFETY_RELEASED | evidence-keyed | timer insufficient |
| CONSUMED | release/expire | any | N/A | N/A | N/A | N/A | N/A | none | CONSUMED | N/A | N/A | SAFETY_EXPOSURE_ALREADY_ACCOUNTED |

Timer expiry alone is insufficient for RELEASED/EXPIRED. Unknown
outcome conservatively retains/consumes exposure. Released/expired
reservation is never revived; create a new governed reservation.
CONSUMED → RELEASED/EXPIRED forbidden. Safety budget is not mutation
authority. Recovery safety budget is not rollback authority.

## 21.2 Safety exposure transitions

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| counters | CONSUME_FORWARD | SYSTEM | reservation CONSUMED path | N/A | bound | bound | 1,9,10,11,15 | forwardMutationExposure += 1 exactly once; distinctCells; unresolvedForward if needed | updated | SAFETY_EXPOSURE_FORWARD | send-evidence id | already accounted |
| counters | CONSUME_RECOVERY | SYSTEM | recovery MAY_HAVE_SENT; closure CLOSED | N/A | bound | bound | 1,8-11,15 | recoveryMutationExposure += 1 exactly once; unresolvedRecovery if needed | updated | SAFETY_EXPOSURE_RECOVERY | send-evidence id | already accounted |
| unresolved* | OUTCOME_RESOLVED | SYSTEM | authoritative resolution | N/A | N/A | N/A | 1,10,15 | decrement unresolved* only; never decrement consumed exposure | updated | SAFETY_UNRESOLVED_CLEARED | evidence-keyed | N/A |

Forward capacity does not authorize recovery. Recovery capacity cannot
be used for forward mutation.

## 21.3 Atomic MAY_HAVE_SENT accounting

When authoritative evidence establishes MAY_HAVE_SENT or equivalent
irreversible exposure, the local campaign transaction MUST atomically:

1. lock authoritative campaign/safety state in §12 order;
2. validate reservation ownership/currentness
   (`SAFETY_RESERVATION_STALE` if stale);
3. transition reservation RESERVED → CONSUMED;
4. increment **exactly one** of `forwardMutationExposure` or
   `recoveryMutationExposure` once;
5. update `distinctCellsExposed` per §21.5 (unique-cell insert);
6. update `unresolvedForwardExposure` or `unresolvedRecoveryExposure`
   where outcome remains unresolved;
7. update combined mutation-slot state to `HELD_MAY_HAVE_SENT`;
8. append authoritative campaign audit;
9. commit.

Processing the same send-boundary evidence more than once MUST be
idempotent and MUST NOT increment exposure twice.

## 21.4 Combined mutation-slot matrix

Slot states: `EMPTY`, `ACQUIRED`, `HELD_MAY_HAVE_SENT`.

`CampaignMutationSerializationRelease` is the only legal path from
`HELD_MAY_HAVE_SENT` to `EMPTY` for a forward holder, and it requires
the exact predicates below. `ExternalMutationOutcomeResolution` may
clear `unresolved*` counters without changing the slot to `EMPTY`.

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| EMPTY | ACQUIRE_FORWARD | SYSTEM | closure OPEN; holder NONE; no unresolved campaign mutation; item individually eligible | full | current fence | current control | 1,8,9,11,15 | holder FORWARD + reservation | ACQUIRED | SLOT_ACQUIRED_FORWARD | with reservation | MUTATION_SLOT_UNAVAILABLE / FORWARD_PROGRESSION_CLOSED |
| EMPTY | ACQUIRE_RECOVERY | SYSTEM | closure already CLOSED; campaign recovery AUTHORIZED; inherited P16 rollback authorized; no unresolved recovery outcome | full recovery currentness | current fence | current control | 1,8,9,11,15 | holder RECOVERY + reservation | ACQUIRED | SLOT_ACQUIRED_RECOVERY | with reservation | RECOVERY_REQUIRES_FORWARD_CLOSURE |
| ACQUIRED | AUTHORITATIVE_NOT_SENT | SYSTEM | authoritative proof the operation never reached MAY_HAVE_SENT | N/A | bound | bound | 1,9-11,15 | reservation RELEASED/EXPIRED; holder NONE | EMPTY | SLOT_RELEASED_NOT_SENT | evidence-keyed | unknown != NOT_SENT |
| ACQUIRED | AUTHORITATIVE_PRE_SEND_REJECTION | SYSTEM | authoritative proof no possible external mutation (pre-send reject) | N/A | bound | bound | 1,9-11,15 | reservation RELEASED; holder NONE | EMPTY | SLOT_RELEASED_PRE_SEND_REJECT | evidence-keyed | MAY_HAVE_SENT already recorded |
| ACQUIRED | MAY_HAVE_SENT | SYSTEM | §21.3 send-boundary evidence | N/A | bound | bound | 1,9-11,15 | HELD_MAY_HAVE_SENT; exposure consumed | HELD_MAY_HAVE_SENT | SLOT_HELD | send-evidence | N/A |
| HELD_MAY_HAVE_SENT | OUTCOME_UNRESOLVED | SYSTEM | send outcome cannot be established | N/A | N/A | N/A | 1,9,10,15 | unresolved* retained; slot unchanged | HELD_MAY_HAVE_SENT | SLOT_UNRESOLVED | evidence-keyed | MUTATION_SLOT_UNAVAILABLE for later mutation |
| HELD_MAY_HAVE_SENT | VENDOR_ACCEPTED_NOT_VERIFIED | SYSTEM | vendor accepted; not PRODUCTION_VERIFIED | N/A | N/A | N/A | 1,9,15 | none that empties slot | HELD_MAY_HAVE_SENT | SLOT_HELD_UNVERIFIED | evidence-keyed | MUTATION_SLOT_UNAVAILABLE |
| HELD_MAY_HAVE_SENT | PRODUCTION_VERIFIED_NOT_RECONCILED | SYSTEM | PRODUCTION_VERIFIED; not CANONICAL_RECONCILED | N/A | N/A | N/A | 1,9,15 | none that empties slot | HELD_MAY_HAVE_SENT | SLOT_HELD_UNRECONCILED | evidence-keyed | MUTATION_SLOT_UNAVAILABLE |
| HELD_MAY_HAVE_SENT | RECONCILED_OBSERVATION_INCOMPLETE | SYSTEM | CANONICAL_RECONCILED; required observation incomplete | N/A | N/A | N/A | 1,9,15 | none that empties slot | HELD_MAY_HAVE_SENT | SLOT_HELD_OBSERVING | evidence-keyed | MUTATION_SLOT_UNAVAILABLE |
| HELD_MAY_HAVE_SENT | OBSERVATION_NOT_HEALTHY | SYSTEM | observation interval complete; NetworkObservationHealth != HEALTHY | current health evidence | N/A | N/A | 1,9,14,15 | none that empties slot | HELD_MAY_HAVE_SENT | SLOT_HELD_UNHEALTHY | evidence-keyed | MUTATION_SLOT_UNAVAILABLE |
| HELD_MAY_HAVE_SENT | OBSERVATION_NOT_SAFE | SYSTEM | observation interval complete; OperationalSafetyHealth != SAFE | current safety evidence | N/A | N/A | 1,9,14,15 | none that empties slot | HELD_MAY_HAVE_SENT | SLOT_HELD_UNSAFE | evidence-keyed | MUTATION_SLOT_UNAVAILABLE |
| HELD_MAY_HAVE_SENT | SERIALIZATION_RELEASE_FORWARD | SYSTEM | required observation complete AND NetworkObservationHealth==HEALTHY AND OperationalSafetyHealth==SAFE AND item terminally accountable for the applicable forward lifecycle AND not RECOVERY_REQUIRED AND not MANUAL_INTERVENTION_REQUIRED AND not OUTCOME_UNRESOLVED AND closure OPEN | current (not historical) HEALTHY/SAFE | N/A | bound policy versions current | 1,8,9,10,15 | holder NONE | EMPTY | SLOT_SERIALIZATION_RELEASED | item+execution keyed once | any missing predicate denies |
| HELD_MAY_HAVE_SENT | DETERMINED_RECOVERY_REQUIRED | SYSTEM | item/campaign RECOVERY_REQUIRED; closure CLOSED in that tx | N/A | N/A | N/A | 1,8,9,15 | slot remains held; holder stays FORWARD until REBIND | HELD_MAY_HAVE_SENT | SLOT_HELD_FOR_RECOVERY | once | must not become EMPTY |
| HELD_MAY_HAVE_SENT | REBIND_HOLDER_TO_RECOVERY | SYSTEM | closure CLOSED; campaign recovery AUTHORIZED; P16 rollback authorized; recovery reservation created | full recovery currentness | current fence | current control | 1,8-11,15 | holderType FORWARD→RECOVERY; state ACQUIRED or HELD as recovery proceeds | ACQUIRED or HELD_MAY_HAVE_SENT | SLOT_REBOUND_RECOVERY | reservation+slot | RECOVERY_REQUIRES_FORWARD_CLOSURE |
| HELD_MAY_HAVE_SENT | MANUAL_INTERVENTION_REQUIRED | SYSTEM | item/recovery TO=MANUAL_INTERVENTION_REQUIRED | N/A | N/A | N/A | 1,9,15 | none that empties slot | HELD_MAY_HAVE_SENT | SLOT_HELD_MANUAL | evidence-keyed | MUTATION_SLOT_UNAVAILABLE; no later campaign mutation |
| HELD_MAY_HAVE_SENT | RECOVERY_MAY_HAVE_SENT | SYSTEM | recovery send-boundary; §21.3 recovery path | N/A | bound | bound | 1,8-11,15 | consume recovery exposure; remain held | HELD_MAY_HAVE_SENT | SLOT_HELD_RECOVERY | send-evidence | N/A |
| HELD_MAY_HAVE_SENT | RECOVERY_OUTCOME_UNRESOLVED | SYSTEM | recovery outcome cannot be established | N/A | N/A | N/A | 1,9,10,15 | unresolvedRecovery retained | HELD_MAY_HAVE_SENT | SLOT_RECOVERY_UNRESOLVED | evidence-keyed | blocks all further campaign-associated mutation |
| HELD_MAY_HAVE_SENT | SUCCESSFUL_RECOVERY_COMPLETE | SYSTEM | recovery RECOVERED; required post-recovery observation complete; current HEALTHY and SAFE | current | N/A | N/A | 1,8,9,15 | holder NONE; closure remains CLOSED | EMPTY | SLOT_CLEARED_AFTER_RECOVERY | evidence-keyed | MUST NOT reopen forward; ACQUIRE_FORWARD denied |
| any | timer expiry | SYSTEM | N/A | N/A | N/A | N/A | N/A | none that returns capacity | unchanged | N/A | N/A | conservative retain |

Forbidden implicit releases: "EMPTY if allowed", "when safe", "when
appropriate", or "outcome resolved" without the predicates above.

## 21.5 distinctCellsExposed

`distinctCellsExposed` is the number of unique canonical cells within
the campaign revision for which at least one campaign-associated
forward or recovery operation reached `MAY_HAVE_SENT`.

It is a unique-cell blast-radius measure, not an operation counter.
`forwardMutationExposure` and `recoveryMutationExposure` remain
separate operation counters.

The same canonical cell MUST NOT increase `distinctCellsExposed` more
than once for the same campaign revision because send evidence is
replayed, forward execution is later recovered, multiple evidence
processors observe the same mutation, or recovery touches the same
already-exposed cell.

Durable uniqueness mechanism (required in V19, not created now):

```text
campaign_exposed_cell (
  campaign_id,
  revision_number,
  cell_id
)
PRIMARY KEY / UNIQUE (campaign_id, revision_number, cell_id)
```

On first authoritative `MAY_HAVE_SENT` for that cell, insert the row.
A conflicting insert is ignored (idempotent). `distinctCellsExposed`
equals the count of rows for that campaign revision. Replay MUST NOT
increment the counter.

# 22. Suspension, Resumption, and Abort Matrices

## 22.1 Suspension

Types: `OPERATOR_PAUSE`, `SAFETY_SUSPENSION`.

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| inactive | OPERATOR_PAUSE | HUMAN CAMPAIGN_PAUSE | campaign not terminal | N/A | current recommended | control++ | 1,3,4,6,15 | suspension active | ACTIVE | CAMPAIGN_OPERATOR_PAUSED | Idempotency-Key | INVALID |
| inactive | SAFETY_SUSPEND | SYSTEM/SCHEDULER_EVAL | non-progressable safety | triggering evidence | N/A | control++ | 1,4,6,15 | suspension active | ACTIVE | CAMPAIGN_SAFETY_SUSPENDED | cause-keyed | N/A |
| ACTIVE | (no auto-resume) | any auto-resume | N/A | N/A | N/A | N/A | N/A | none | ACTIVE | N/A | N/A | DENY |

Active suspension invalidates control generation and unused release,
blocks new sends, does not cancel in-flight possible mutation, does not
auto-rollback, and does not by itself close forward progression.

## 22.2 Resumption

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| none | REQUEST_RESUMPTION | HUMAN CAMPAIGN_RESUMPTION_REVIEW or PAUSE holder | suspension active | N/A | N/A | N/A | 1,14,15 | REQUESTED | REQUESTED | RESUMPTION_REQUESTED | Idempotency-Key | INVALID |
| REQUESTED | START_REVIEW | HUMAN CAMPAIGN_RESUMPTION_REVIEW | N/A | N/A | N/A | N/A | 1,14,15 | UNDER_REVIEW | UNDER_REVIEW | RESUMPTION_UNDER_REVIEW | Idempotency-Key | UNAUTHENTICATED_HUMAN_ACTOR |
| UNDER_REVIEW | REVIEW_APPROVE | HUMAN CAMPAIGN_RESUMPTION_REVIEW | remediation evidence bound | current fingerprint | N/A | N/A | 1,14,15 | REVIEWED | REVIEWED | RESUMPTION_REVIEWED | Idempotency-Key | SoD |
| UNDER_REVIEW | REVIEW_REJECT | HUMAN CAMPAIGN_RESUMPTION_REVIEW | N/A | N/A | N/A | N/A | 1,14,15 | REJECTED | REJECTED | RESUMPTION_REJECTED | Idempotency-Key | N/A |
| REVIEWED | AUTHORIZE | HUMAN CAMPAIGN_RESUMPTION_AUTHORIZE | reviewer != authorizer | full currentness | N/A | N/A | 1,4,14,15 | AUTHORIZED | AUTHORIZED | RESUMPTION_AUTHORIZED | Idempotency-Key | SoD / currentness |
| AUTHORIZED | MAKE_EFFECTIVE | SYSTEM | §22.4 destination predicates all true; no unresolved MAY_HAVE_SENT; closure not reopened | full currentness | current lease | new control already allocated; **no** cohort release by this transition | 1,3,4,8,14,15 | EFFECTIVE; campaign state = exact §22.4 destination | EFFECTIVE | RESUMPTION_EFFECTIVE | Idempotency-Key | G18-237 / FORWARD_PROGRESSION_CLOSED / STALE |
| applicable | MATERIAL_INVALIDATION | SYSTEM | N/A | stale | N/A | N/A | 1,14,15 | STALE | STALE | RESUMPTION_STALE | state-guard | N/A |

Resumption itself MUST NOT release a cohort and MUST NOT reopen
forward closure. Old release resurrection is denied. The phrase
"last progressable non-release state" is not a destination; §22.4 is.

## 22.4 Resumption Destination Matrix

`preState` is the durable campaign state immediately before
suspension. `EFFECTIVE` is denied unless every listed requirement
passes. Destination is exact; implementation MUST NOT infer another
state.

Observation-boundary rule (not inferred):

An existing `PostChangeObservationBoundary` remains valid for continued
observation if and only if all of the following remain true: bound
production execution unchanged; `vendorVerifiedAt` unchanged;
observation-policy version still current; health-policy version still
current; no superseding interference; `observationStart >= vendorVerifiedAt`;
required source/watermark still satisfied. Otherwise a **new** governed
`PostChangeObservationBoundary` is required before HEALTHY/SAFE may
again be established. Historical HEALTHY/SAFE does not establish future
admissibility when governing currentness has changed.

| preState | suspension type | unresolved MAY_HAVE_SENT | closure | currentness | observation boundary | old release | new release mandatory | exact destination | forbidden destinations | denial reason |
|---|---|---|---|---|---|---|---|---|---|---|
| READY | OPERATOR_PAUSE or SAFETY_SUSPENSION | none allowed | OPEN required | fingerprint/auth/cert/onboarding/security/credential/ticket/target current | N/A | none or unused stale | no | READY | CANARY_ACTIVE, any released/active/observing/completed | unresolved / CLOSED / STALE |
| READY | either | none | CLOSED | any | N/A | any | N/A | DENY EFFECTIVE | any forward-progressing state | FORWARD_PROGRESSION_CLOSED |
| CANARY_ACTIVE | either | none allowed | OPEN | full currentness | N/A for unsent items | unused release stale | yes, for remaining unexecuted items | PAUSED_FOR_RELEASE | CANARY_ACTIVE, COHORT_ACTIVE, READY | resurrecting stale release |
| CANARY_ACTIVE | either | present | any | any | any | any | N/A | DENY EFFECTIVE | any | unresolved outcome |
| CANARY_ACTIVE | either | none | OPEN | full; all canary items already terminally accounted with still-valid verification/reconciliation/observation | existing boundary still valid per rule above | consumed | no | CANARY_VERIFIED | CANARY_ACTIVE, PAUSED_FOR_RELEASE | evidence no longer current → STALE deny |
| CANARY_OBSERVING | either | none allowed | OPEN | full currentness | existing valid → keep; else NEW_OBSERVATION_BOUNDARY_REQUIRED | bound release historical only | no for already-sent items; yes for any unexecuted remainder | CANARY_OBSERVING | CANARY_VERIFIED, COMPLETED, CANARY_ACTIVE | unresolved / CLOSED / stale currentness |
| CANARY_VERIFIED | either | none | OPEN | required canary evidence still authoritative and current | historical evidence bound; not reused as future HEALTHY without currentness | consumed | no | CANARY_VERIFIED then CM-10 may proceed later | CANARY_ACTIVE | STALE if evidence/currentness lost |
| PAUSED_FOR_RELEASE | either | none allowed | OPEN | full currentness | N/A | unused stale | yes before later mutation | PAUSED_FOR_RELEASE | COHORT_ACTIVE, CANARY_ACTIVE | resurrecting stale release / CLOSED |
| COHORT_ACTIVE | either | none allowed | OPEN | full currentness | N/A for unsent | unused release stale | yes for remaining unexecuted items | PAUSED_FOR_RELEASE | COHORT_ACTIVE | resurrecting stale release |
| COHORT_ACTIVE | either | present | any | any | any | any | N/A | DENY EFFECTIVE | any | unresolved outcome |
| COHORT_ACTIVE | either | none | OPEN | full; all active-cohort items terminally accounted with still-valid evidence | existing valid | consumed | no | COHORT_VERIFIED | COHORT_ACTIVE | evidence/currentness lost → STALE |
| COHORT_OBSERVING | either | none allowed | OPEN | full currentness | existing valid or NEW_OBSERVATION_BOUNDARY_REQUIRED | historical only | no for already-sent; yes for unexecuted remainder | COHORT_OBSERVING | COHORT_VERIFIED, COHORT_ACTIVE | unresolved / CLOSED |
| COHORT_VERIFIED | either | none | OPEN | evidence still authoritative | bound historical | consumed | no | COHORT_VERIFIED; later CM-14 or CM-15 only | COHORT_ACTIVE | STALE if currentness lost |
| FINAL_OBSERVATION | either | none allowed | OPEN | full currentness | existing valid or NEW_OBSERVATION_BOUNDARY_REQUIRED | consumed | no | FINAL_OBSERVATION | COMPLETED, any ACTIVE | unresolved / CLOSED / stale HEALTHY |
| any listed | either | any unresolved MAY_HAVE_SENT | any | any | any | any | N/A | DENY EFFECTIVE | any | unresolved outcome |
| any listed | either | none | CLOSED | any | any | any | N/A | DENY any forward-progressing destination | READY, *ACTIVE, *OBSERVING, *VERIFIED, PAUSED_FOR_RELEASE, FINAL_OBSERVATION, COMPLETED | FORWARD_PROGRESSION_CLOSED |
| any listed | either | none | OPEN | revision/fingerprint/authorization stale requiring new governance | any | any | N/A | DENY EFFECTIVE | any restored progress state | STALE; resumption cannot repair |

Rules A–J: (A) unresolved MAY_HAVE_SENT cannot be bypassed; (B) stale
release is not resurrected; (C) resumption does not release a cohort;
(D) stale prior release means later mutation requires a new human
release and new `ReleaseGeneration`; (E) pre-release READY may resume
to READY when currentness remains valid; (F) completed canary/cohort
evidence may restore only the exact non-release state justified by
that still-valid evidence; (G) observation-boundary validity is the
explicit rule above; (H) historical HEALTHY/SAFE is not future
admissibility; (I) CLOSED prohibits any forward-progressing
destination; (J) stale campaign authority cannot be repaired by
resumption.

## 22.3 Abort

| FROM | TRIGGER | AUTHORITY | PRECONDITIONS | CURRENTNESS | LEASE/FENCE | GENERATIONS | LOCKS | ATOMIC WRITES | TO | AUDIT | IDEMPOTENCY | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| NOT_ABORTED | ABORT_CAMPAIGN | HUMAN CAMPAIGN_ABORT | not already ABORTED | N/A | current recommended | control++ before later send | 1,3,4,6,15 | ABORTED; unused release STALE | ABORTED | CAMPAIGN_ABORTED | Idempotency-Key | INVALID |
| ABORTED | reverse | any | N/A | N/A | N/A | N/A | N/A | none | ABORTED | N/A | N/A | INVALID_CAMPAIGN_TRANSITION |

# 23. Phase 12 Reconciliation Provenance

`CanonicalReconciliationReader` is read-only and returns target/source
scope, cell, parameter, canonical value/unit, source version,
checkpoint type/value/status, lastObservedAt, freshness/confidence if
available, evidence digest.

`CANONICAL_RECONCILED` MUST NOT be established merely because the
canonical value currently equals the verified desired value.

## 23.1 Proof Form A (mandatory support)

All of:

- canonical target/source scope matches
- cell matches
- parameter == `txPower`
- unit == `dBm`
- canonical `BigDecimal` value == directly vendor-verified value
  (`compareTo == 0`)
- authoritative synchronization/checkpoint status is successful
- canonical `lastObservedAt >= vendorVerifiedAt`
- no superseding conflict/interference exists

## 23.2 Proof Form B (optional, repository-conditional)

Permitted **only if** existing Phase 12 authoritative source semantics
provide a monotonic source observation sequence/version that proves the
represented source observation itself occurred after the direct vendor
verification boundary.

## 23.3 Insufficient by themselves

The following MUST NOT by themselves prove reconciliation:

- `checkpointCreatedAt >= vendorVerifiedAt`
- `ingestedAt >= vendorVerifiedAt`
- `rowUpdatedAt >= vendorVerifiedAt`
- current value equality

If the repository cannot prove post-verification source provenance:

`RECONCILIATION_PROVENANCE_UNAVAILABLE`

and campaign progression remains blocked.

Phase 18 MUST NOT write Phase 12 canonical state to manufacture
reconciliation.

# 24. Observation Provenance, Interference, and Dual Health

Persist `PostChangeObservationBoundary` with `vendorVerifiedAt`,
start/end, required source, minimumMeasurementTime, max ingestion lag,
watermark/checkpoint and policy versions.
`observationStart >= vendorVerifiedAt`.

Existing telemetry `observedAt` and `ingestedAt` remain distinct.
Ingestion time alone never proves post-change measurement.

Watermark type: `SOURCE_NATIVE_WATERMARK` or `PHASE12_CHECKPOINT`.
Never fabricate a watermark. If policy requires one and no
authoritative proof exists, health UNKNOWN/STALE and block.

Same target/cell/txPower interference during the governed interval
invalidates attribution by default. Persist first-class interference
evidence. Superseded evidence cannot later satisfy progression.
Absence of detected interference is not exclusive causality proof.

NetworkObservationHealth = HEALTHY/DEGRADED/UNHEALTHY/UNKNOWN/STALE.
OperationalSafetyHealth = SAFE/UNSAFE/UNKNOWN/STALE.

Progression requires HEALTHY **and** SAFE. Healthy KPI cannot override
unsafe authorization/certification/target/audit/security/credential/
window/rate/blast/outcome state. Healthy observation creates
eligibility only, never release.

## 24.1 Historical health vs current admissibility

A previously persisted `NetworkObservationHealth = HEALTHY` or
`OperationalSafetyHealth = SAFE` remains **historical evidence**.

It MUST NOT authorize future progression if its governing policy,
authorization, ticket, certification, onboarding, security, credential,
target, generation, freshness, or observation provenance is no longer
current.

Historical outcome resolution uses historically bound evidence/policy.
Future progression uses current authoritative policy/currentness.

# 25. Recovery Transition Matrix

Recovery uses the exact governed rollback value and existing Phase 16
`GrantType.ROLLBACK` governance/gateway. Recovery safety budget is
**not** rollback authority. Combined mutation slot + inherited Phase 16
rollback authority are both required for executable recovery. Phase 18
recovery permissions (`CAMPAIGN_RECOVERY_REQUEST`,
`CAMPAIGN_RECOVERY_REVIEW`, `CAMPAIGN_RECOVERY_AUTHORIZE`) govern
campaign recovery only and MUST NOT replace Phase 16
`AUTHORIZE_PRODUCTION_CHANGE` / rollback SoD. No automatic recovery,
automatic retry, recursive rollback, or rollback-of-rollback.
Successful recovery MUST NOT reopen forward progression.

Unlisted recovery transitions are `INVALID_RECOVERY_TRANSITION`.

| FROM | TRIGGER | AUTHORITY | SoD | PRECONDITIONS | CLOSURE | RESERVATION | SLOT | P16 ROLLBACK AUTH | LOCKS / TX | TO | AUDIT | IDEMPOTENCY | CRASH | FAILURE |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| NOT_REQUIRED | DETERMINED_RECOVERY_REQUIRED | SYSTEM | N/A | item TO=RECOVERY_REQUIRED | OPEN→CLOSED same tx; then commit | none | none | none | 1,7,8,15 | REQUIRED | FORWARD_PROGRESSION_CLOSED | once | reconstruct from item | N/A |
| REQUIRED | REQUEST_RECOVERY | HUMAN + CAMPAIGN_RECOVERY_REQUEST | requester != reviewer and != campaign recovery authorizer (enforced at later steps); identity HUMAN authenticated | closure already durably CLOSED | CLOSED | none | none | planning only; not P16 rollback auth | 1,8,14,15 | REQUESTED | RECOVERY_REQUESTED | Idempotency-Key | replay | RECOVERY_REQUIRES_FORWARD_CLOSURE / UNAUTHENTICATED_HUMAN_ACTOR |
| REQUESTED | START_REVIEW | HUMAN + CAMPAIGN_RECOVERY_REVIEW | reviewer != requester; HUMAN authenticated | CLOSED | CLOSED | none | none | none | 1,14,15 | UNDER_REVIEW | RECOVERY_UNDER_REVIEW | Idempotency-Key | replay | UNAUTHENTICATED_HUMAN_ACTOR / PRODUCTION_SOD_VIOLATION |
| UNDER_REVIEW | APPROVE_REVIEW | HUMAN + CAMPAIGN_RECOVERY_REVIEW | reviewer != requester; exact governed rollback value bound | CLOSED | CLOSED | none | none | not yet | 1,14,15 | REVIEWED (not AUTHORIZED) | RECOVERY_REVIEWED | Idempotency-Key | replay | RECOVERY_REQUIRES_FORWARD_CLOSURE / PRODUCTION_SOD_VIOLATION |
| REVIEWED | AUTHORIZE_RECOVERY | HUMAN + CAMPAIGN_RECOVERY_AUTHORIZE | authorizer != reviewer AND != requester AND != later P16 rollback authorizer/executor | CLOSED already committed | CLOSED | none | none | not a substitute for P16 rollback authorization | 1,8,14,15 | AUTHORIZED | RECOVERY_AUTHORIZED | Idempotency-Key | replay | RECOVERY_REQUIRES_FORWARD_CLOSURE / PRODUCTION_SOD_VIOLATION |
| UNDER_REVIEW | REJECT | HUMAN + CAMPAIGN_RECOVERY_REVIEW | reviewer != requester | CLOSED | CLOSED | none | none | none | 1,14,15 | REJECTED | RECOVERY_REJECTED | Idempotency-Key | replay | N/A |
| AUTHORIZED | RESERVE_RECOVERY | SYSTEM | closure CLOSED; inherited P16 rollback separately requested/reviewed/authorized per Phase 16 SoD | CLOSED | create recovery reservation | ACQUIRE RECOVERY or REBIND | required current P16 rollback authority | 1,8-11,15 | RESERVED | RECOVERY_RESERVED | reservation+slot | no lock across P16 | MUTATION_SLOT_UNAVAILABLE / RECOVERY_REQUIRES_FORWARD_CLOSURE |
| RESERVED | HANDOFF | SYSTEM | N/A | create-or-return CampaignHandoffId for recovery lineage | CLOSED | RESERVED | ACQUIRED | required | local tx then P16 | HANDOFF_PENDING | RECOVERY_HANDOFF_PENDING | CampaignHandoffId | §9.5 | HANDOFF_IDEMPOTENCY_CONFLICT |
| HANDOFF_PENDING | SEND_BOUNDARY | SYSTEM | N/A | gateway evidence | CLOSED | CONSUMED | HELD_MAY_HAVE_SENT | consumed grant | §21.3 | MAY_HAVE_SENT | RECOVERY_MAY_HAVE_SENT | send-evidence | conservative consume | N/A |
| MAY_HAVE_SENT | START_VERIFY | SYSTEM | N/A | P16 verify | CLOSED | CONSUMED | held | N/A | 1,14,15 | VERIFYING | RECOVERY_VERIFYING | evidence | P16 wins | N/A |
| VERIFYING | VERIFIED_ROLLBACK | SYSTEM | N/A | exact rollback value readback | CLOSED | CONSUMED | held | N/A | 1,14,15 | VERIFIED | RECOVERY_VERIFIED | evidence | never reopen | N/A |
| VERIFYING | UNRESOLVED | SYSTEM | N/A | cannot establish | CLOSED | retain | held | N/A | 1,10,15 | OUTCOME_UNRESOLVED | RECOVERY_OUTCOME_UNRESOLVED | evidence | block all mutation | N/A |
| VERIFYING | FAILED | SYSTEM | N/A | governed failure | CLOSED | retain | held | N/A | 1,14,15 | FAILED | RECOVERY_FAILED | evidence | no recursive rollback | N/A |
| VERIFYING | UNEXPECTED | SYSTEM | N/A | third state | CLOSED | retain | held | N/A | 1,14,15 | MANUAL_INTERVENTION_REQUIRED | RECOVERY_MANUAL | evidence | block | N/A |
| VERIFIED | RECONCILE | SYSTEM | N/A | §23 | CLOSED | N/A | N/A | N/A | 1,14,15 | RECONCILING | RECOVERY_RECONCILING | evidence | no canonical write | RECONCILIATION_PROVENANCE_UNAVAILABLE |
| RECONCILING | PROOF | SYSTEM | N/A | §23 | CLOSED | N/A | N/A | N/A | 1,14,15 | OBSERVING | RECOVERY_OBSERVING | evidence | reconstruct | provenance |
| OBSERVING | HEALTHY_SAFE | SYSTEM | N/A | current HEALTHY and SAFE | CLOSED | N/A | N/A | N/A | 1,14,15 | RECOVERED or PARTIALLY_RECOVERED | RECOVERY_OBSERVED | evidence | never reopen | stale historical |
| any executable | auto-retry / auto-recover / rollback-of-rollback | any | N/A | N/A | N/A | N/A | N/A | N/A | N/A | unchanged | N/A | N/A | N/A | INVALID_RECOVERY_TRANSITION |

REVIEWED, AUTHORIZED, and every stronger executable recovery state
require durable `ForwardProgressionClosure = CLOSED` already committed.
Campaign recovery AUTHORIZED is not Phase 16 rollback authorization.

# 26. Terminal Item Accounting and Final Disposition

Every item in `AuthorizedCampaignScope` MUST be accounted exactly once.
Historical states are not collapsed.

Item terminal classes:

- **Successful forward terminal:** COMPLETED
- **No-send terminal:** NOT_SENT, VENDOR_REJECTED only when
  authoritative evidence proves no MAY_HAVE_SENT, STALE /
  BLOCKED / SUSPENDED that never reached MAY_HAVE_SENT and are
  terminally abandoned under abort/expiry
- **Recovery terminals:** RECOVERED (via recovery matrix),
  PARTIALLY_RECOVERED (campaign aggregation), FAILED,
  MANUAL_INTERVENTION_REQUIRED
- **Blocking non-completion:** OUTCOME_UNRESOLVED, RECOVERY_REQUIRED,
  MANUAL_INTERVENTION_REQUIRED, VERIFYING without resolution

Campaign final disposition aggregation:

| Disposition | Aggregation rule |
|---|---|
| COMPLETED | Every authorized item is successful-forward-terminal with required verification, reconciliation, observation, and current-at-the-time evidence; none of OUTCOME_UNRESOLVED, RECOVERY_REQUIRED, MANUAL_INTERVENTION_REQUIRED; audit valid |
| ABORTED_NO_RECOVERY_REQUIRED | Campaign ABORTED; every item terminally accounted; no item required recovery; no unresolved send |
| ABORTED_RECOVERY_REQUIRED | Campaign ABORTED and at least one item RECOVERY_REQUIRED / recovery not complete |
| RECOVERED | Closure CLOSED; every item that required recovery is recovery-terminal RECOVERED; no unresolved |
| PARTIALLY_RECOVERED | Closure CLOSED; some required recoveries RECOVERED and at least one FAILED / not recovered / not verified |
| MANUAL_INTERVENTION_REQUIRED | Any authorized item remains MANUAL_INTERVENTION_REQUIRED |
| EXPIRED | Authority expired with no possible send and no unresolved outcome; items terminally accounted as not-sent/abandoned |

A campaign MUST NOT become COMPLETED if any item is
OUTCOME_UNRESOLVED, RECOVERY_REQUIRED, MANUAL_INTERVENTION_REQUIRED, or
otherwise lacks required successful
verification/reconciliation/observation evidence.

# 27. Reason Codes and API

Reuse existing `{error, reasonCode}` production convention and existing
`ProductionReasonCode` values where they already name the same failure
(`PRODUCTION_SOD_VIOLATION`, `PRODUCTION_FENCING_TOKEN_STALE`,
`PRODUCTION_OUTCOME_UNRESOLVED`, `MANUAL_INTERVENTION_REQUIRED`,
`PRODUCTION_AUDIT_CHAIN_INVALID`, etc.).

New Phase 18 typed codes (no existing equivalent):

- `HANDOFF_IDEMPOTENCY_CONFLICT`
- `DUPLICATE_CAMPAIGN_LINEAGE`
- `INVALID_CAMPAIGN_TRANSITION`
- `INVALID_RECOVERY_TRANSITION`
- `MUTATION_SLOT_UNAVAILABLE`
- `MUTATION_SLOT_STALE`
- `SAFETY_RESERVATION_STALE`
- `SAFETY_EXPOSURE_ALREADY_ACCOUNTED`
- `FORWARD_PROGRESSION_CLOSED`
- `RECOVERY_REQUIRES_FORWARD_CLOSURE`
- `RECONCILIATION_PROVENANCE_UNAVAILABLE`
- `UNTRUSTED_ACTOR_SOURCE`
- `UNAUTHENTICATED_HUMAN_ACTOR`
- `GENERATION_OVERFLOW`
- `EVIDENCE_MAP_INVALID`
- `EXTERNAL_CERTIFICATION_NOT_EXECUTED`
- `STANDALONE_DOWNGRADE_DENIED`
- `CAMPAIGN_SIZE_LIMIT`
- `UNSUPPORTED_PARAMETER_UNIT`
- `UNSUPPORTED_OBJECT_OR_PARAMETER`
- `GOVERNANCE_IDEMPOTENCY_CONFLICT`
- `CAMPAIGN_SCOPE_INCOMPLETE`

Also retain the draft's required families: stale
campaign/revision/fingerprint/auth/release/generation/fence, closure,
predecessor incomplete, health/safety,
observation/reconciliation/interference, safety budget/concurrency,
unresolved outcome, Phase 16 unavailable, binding mismatch/not found,
window/cert/onboarding/security/credential/ticket currentness, audit
invalidity, DB/lock unavailable.

Recommended API base: `/api/v1/production-campaigns`.
Authority-bearing commands require `Idempotency-Key`.

No API accepts arbitrary vendor
command/endpoint/port/credential/SSH/CLI/generic mutation
payload/caller-selected origin/fence/generation.

# 28. Automation, Revocation, and Qualification Inference

Schedulers may evaluate expiry/freshness/health and safety-suspend.
They cannot authorize/release/grant/mutate/resume/rollback. Events
cannot initiate mutation. Agents/MCP may observe/explain/recommend
only.

P17 interface/transport/cert/onboarding/security/credential
invalidation and required external ticket invalidation cascade to
suspension/stale, control-generation invalidation, unused release
stale, new grants/sends prohibited, while consumed history remains and
MAY_HAVE_SENT resolution continues.

## 28.1 No certification inference

Software, unit, integration, or simulator test existence MUST NOT infer
any of: P17 L1/L2/L3/L4 PASS; P18 C1/C2/C3/C4 PASS.

External qualification changes status only when independently executed
evidence exists for that qualification level. Otherwise the evidence
map MUST record `NOT_EXECUTED` / `NOT_SATISFIED` and
`EXTERNAL_CERTIFICATION_NOT_EXECUTED` if a PASS is attempted.

# 29. Testing and Failure Injection

Use JUnit 5, Spring Boot tests, PostgreSQL 16 Testcontainers, existing
gateway failure injection, and fixture-owned cleanup. Tests must be
order-independent and clean their own durable state.

High-risk gates require
behavioral/database/integration/security/concurrency/failure evidence,
not structural-only evidence.

Mandatory adversarial scenarios include the prior draft list plus:

- hard-max policy values 26 / 100 / `Integer.MAX_VALUE` / malformed /
  0 / negative cannot raise the ceiling;
- unit rejection at the Phase 18 boundary (§3.1);
- `CampaignHandoffId` create-or-return and conflict;
- combined slot exclusivity;
- atomic exposure consume-once;
- closure before recovery AUTHORIZED;
- Proof Form A vs insufficient timestamps;
- untrusted actor headers;
- historical HEALTHY/SAFE cannot admit future progression;
- evidence-map validator failures listed in §32;
- no inference of C1-C4 / L1-L4 from software tests.

# 30. CI and Qualification

Default CI remains Java 17 `mvn -B test` plus Go 1.22 `go test ./...`;
no Azure / Key Vault / workload identity / vendor
credentials / endpoints / certification environment. Do not claim a Go
application build unless separately added.

C0 software correctness is distinct from C1 simulator, C2 vendor-lab,
C3 preprod, C4 target operational approval. P17 L1--L4 remain
independent. Software existence authorizes nothing.

# 31. Machine-Readable Evidence Map

Existing repository convention is **JSON**, not YAML:
`docs/implementation/phase16-gate-evidence-map.json` and
`docs/implementation/phase17-gate-evidence-map.json`. Phase 18 MUST
preserve JSON.

Future implementation artifact (NOT created by this correction):

`docs/implementation/phase18-gate-evidence-map.json`

Required document schema:

```text
phase
architectureBaseline
architectureContentSha256
implementationBaseline          # when available; otherwise null
generatedAt
gates[]                         # exactly one entry per G18-001..G18-254
threats[]                       # T18-01..T18-64 mapped to gates/tests
invariants[]                    # I18-01..I18-32 mapped to gates/enforcement
```

Allowed gate statuses: `PASS`, `FAIL`, `NOT_EXECUTED`, `NOT_APPLICABLE`.

Allowed evidence types: `structural`, `behavioral`, `database`,
`integration`, `security`, `concurrency`, `failure_injection`,
`external_certification`.

Each evidence entry MUST identify enough information to resolve the
actual implementation/test evidence, including as applicable: module,
path, test class, test method/scenario, evidence type, qualification
level, notes.

Implementation MUST provide a validator that fails for:

- missing G18 gate; duplicate G18 gate; unknown G18 gate
- missing T18 threat; duplicate T18 threat; unknown T18 threat
- missing I18 invariant; duplicate I18 invariant; unknown I18 invariant
- threat → gate mapping different from the frozen Phase 18 architecture
- invariant → gate mapping different from the frozen Phase 18 architecture
- nonexistent referenced evidence path
- nonexistent referenced test identifier
- invalid status; invalid evidence type
- high-risk gate marked PASS with structural-only evidence
- external-certification PASS without independently executed external
  evidence

The frozen Phase 18 architecture remains the authoritative source for
254 gates, 64 threats, 32 invariants, and the threat→gate and
invariant→gate mappings. Catalogue membership alone is insufficient;
mapping integrity MUST match the frozen architecture exactly.

Validator failure reason: `EVIDENCE_MAP_INVALID`.
C1/C2/C3/C4 and P17 L1-L4 MUST NOT become PASS merely because software
tests exist (`EXTERNAL_CERTIFICATION_NOT_EXECUTED`).

This specification freeze does **not** create the JSON map or the
validator. A18-SR3-02 remains NOT EXECUTED --- IMPLEMENTATION EVIDENCE
REQUIRED.

# 32. Audit and Evidence Correlation

Campaign audit uses SHA-256 canonical chain with campaignId, sequence,
previousEventHash, eventHash, eventType, authenticated actor, timestamp,
canonicalPayloadDigest/safe payload. Sequence allocation follows
existing `FOR UPDATE` latest + unique sequence pattern. Audit transition
is atomic with the authoritative local state transition.

`INVALID` / `UNKNOWN` / `GAP` / `HASH_MISMATCH` blocks progression.
Claim tamper-evident only.

Evidence must bidirectionally correlate
campaign → cohort → item → binding → `CampaignHandoffId` → P16
change/execution → grant → gateway attempt → vendor outcome →
verification, and reverse from a campaign-originated P16 execution to
binding/item/cohort/revision. No secrets.

# 33. Specification Acceptance Gates

ARCHITECTURE BASELINE:
VERIFIED

ARCHITECTURE CONTENT SHA-256:
VERIFIED

G18 CATALOGUE:
254 / COMPLETE

T18 CATALOGUE:
64 / COMPLETE

I18 CATALOGUE:
32 / COMPLETE

A18-R3-01:
CLOSED BY ADVERSARIAL REVIEW R2

A18-R3-02:
CLOSED BY TARGETED ADVERSARIAL REVIEW R3

OPEN CRITICAL SPECIFICATION FINDINGS:
0

OPEN BLOCKING SPECIFICATION FINDINGS:
0

ARCHITECTURAL CONTRADICTIONS:
0

SPECIFICATION:
ACCEPTED
FROZEN

A18-SR3-01 and A18-SR3-02 do **not** block specification freeze.
They **MUST** block future Phase 18 implementation conformance
acceptance until their required evidence exists and passes.

The following specification-freeze predicates remain satisfied:

- exact architecture baseline/hash recorded
- 254 gates, 64 threats, 32 invariants preserved
- A18-R3-01 lock/crash contract CLOSED BY ADVERSARIAL REVIEW R2
- A18-R3-02 lifecycle matrices CLOSED BY TARGETED ADVERSARIAL REVIEW R3
- BigDecimal/dBm semantics explicit
- authoritative gateway binding + anti-downgrade explicit
- one binding/item/`CampaignHandoffId`/lineage cardinality explicit
- durable governance idempotency explicit
- authenticated actor trust boundary explicit
- Phase 12 read-only reconciliation proof explicit
- observation provenance/interference fail closed
- separate forward/recovery exposure + combined concurrency explicit
- permanent forward closure explicit
- V19 obligations explicit and migration not executed
- evidence map machine-checkable (validator obligation A18-SR3-02)
- CI external-independent
- no protocol/credential invention
- production / C1-C4 / Phase 19 remain unauthorized / not started
- Git specification baseline NOT YET CREATED
- exact-SHA specification CI NOT YET EXECUTED
- Phase 18 implementation NOT AUTHORIZED

# 34. Review Incorporation Record

R1 preserved and CLOSED (historical): C18-SR1-01; B18-SR1-01 through
B18-SR1-08; A18-SR1-01 through A18-SR1-08. D18-SR1=0.

R2 incorporated (historical findings remain recorded): B18-SR2-01
through B18-SR2-04; A18-SR2-01 through A18-SR2-05. C18-SR2=0.
D18-SR2=0.

TARGETED ADVERSARIAL REVIEW R3:

C18-SR3:
0

B18-SR3:
0

D18-SR3:
0

A18-SR3:
2 NON-BLOCKING IMPLEMENTATION EVIDENCE OBLIGATIONS

B18-SR2-01:
CLOSED BY TARGETED ADVERSARIAL REVIEW R3

B18-SR2-02:
CLOSED BY TARGETED ADVERSARIAL REVIEW R3

B18-SR2-03:
CLOSED BY TARGETED ADVERSARIAL REVIEW R3

B18-SR2-04:
CLOSED BY TARGETED ADVERSARIAL REVIEW R3

A18-R3-01:
CLOSED BY ADVERSARIAL REVIEW R2

A18-R3-02:
CLOSED BY TARGETED ADVERSARIAL REVIEW R3

OPEN CRITICAL SPECIFICATION FINDINGS:
0

OPEN BLOCKING SPECIFICATION FINDINGS:
0

ARCHITECTURAL CONTRADICTIONS:
0

SPECIFICATION DISPOSITION:
ACCEPTED FOR DOCUMENT FREEZE

## 34.1 A18-SR3-01 --- Combined mutation serialization evidence

Classification:

NON-BLOCKING FOR SPECIFICATION FREEZE

MANDATORY FOR PHASE 18 IMPLEMENTATION CONFORMANCE ACCEPTANCE

Status:

NOT EXECUTED --- IMPLEMENTATION EVIDENCE REQUIRED

Required future implementation evidence:

real PostgreSQL concurrency/failure-injection evidence for the combined
campaign mutation serialization mechanism, including at minimum:

1. two simultaneous forward acquisitions;
2. forward versus recovery acquisition;
3. duplicate MAY_HAVE_SENT processing;
4. crash between reservation and slot persistence;
5. recovery holder rebind;
6. concurrent suspension/control-generation invalidation;
7. serialization release racing a new acquisition.

The evidence must prove the authoritative invariant:

`activeForwardCampaignMutations + activeRecoveryCampaignMutations <= 1`

These tests are **not** implemented by this freeze. This obligation is
**not** marked PASS. No implementation evidence is created by this
document freeze.

## 34.2 A18-SR3-02 --- Evidence-map validator acceptance infrastructure

Classification:

NON-BLOCKING FOR SPECIFICATION FREEZE

MANDATORY FOR PHASE 18 IMPLEMENTATION CONFORMANCE ACCEPTANCE

Status:

NOT EXECUTED --- IMPLEMENTATION EVIDENCE REQUIRED

The machine-readable Phase 18 evidence-map validator must become
executable acceptance infrastructure during implementation.

Future implementation/conformance evidence must prove that the
validator executes and rejects at minimum:

1. missing catalogue entries;
2. duplicate catalogue entries;
3. unknown catalogue entries;
4. threat -> gate mapping divergence;
5. invariant -> gate mapping divergence;
6. nonexistent evidence references;
7. invalid evidence types;
8. invalid evidence statuses;
9. high-risk structural-only PASS;
10. unsupported external-certification PASS.

The validator is **not** created by this freeze. The Phase 18 evidence
map is **not** created by this freeze. This obligation is **not**
marked PASS.

# 35. Current Readiness State

```text
ERICSSON PRODUCTION WRITE PROTOCOL: UNRESOLVED
ERICSSON PRODUCTION TRANSPORT: UNCONFIGURED / NOT IMPLEMENTED
ERICSSON PRODUCTION ENDPOINT: NONE / NOT CONFIGURED
ERICSSON PRODUCTION AUTH METHOD: UNRESOLVED / EXTERNALLY CONFIGURED
ERICSSON PRODUCTION CREDENTIAL: NONE
NOKIA: DEFERRED
P17 L1/L2: NOT EXECUTED
P17 L3/L4: NOT SATISFIED
P18 C1/C2/C3: NOT EXECUTED
P18 C4: NOT SATISFIED
REAL PRODUCTION EXECUTION: NOT AUTHORIZED
REAL PRODUCTION CAMPAIGN EXECUTION: NOT AUTHORIZED
AGENT/MCP/SCHEDULED/EVENT EXECUTION: NOT AUTHORIZED
AUTOMATIC ROLLBACK: NOT AUTHORIZED
AUTOMATIC COHORT RELEASE: NOT AUTHORIZED
AUTOMATIC PROGRESSION: NOT AUTHORIZED
AUTOMATIC RESUMPTION: NOT AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT AUTHORIZED
```

------------------------------------------------------------------------

# 36. Frozen Architecture Gate Catalogue --- Normative

**G18-001 ---** Phase 18 SHALL use immutable Phase 17 implementation
baseline `d1751cca70391babf712bce3c6bcc29238ce0c86` as its parent and
SHALL NOT alter frozen Phase 17 artifacts.

**G18-002 ---** Phase 18 SHALL add campaign coordination and
progressive-delivery governance without weakening or bypassing
authoritative Phase 13--17 controls.

**G18-003 ---** Campaign authority SHALL NOT constitute, imply, mint, or
substitute for production mutation authority.

**G18-004 ---** Campaign release SHALL NOT constitute or substitute for
a Phase 16 `ProductionExecutionGrant`.

**G18-005 ---** Initial Phase 18 production campaign scope SHALL be
limited to one registered `ProductionNetworkTarget` per campaign.

**G18-006 ---** Initial Phase 18 mutation scope SHALL remain CELL object
type and `txPower` parameter only, with one parameter and one forward
mutation per child production execution.

**G18-007 ---** Ericsson SHALL remain the first production-vendor
context for Phase 18; Nokia SHALL remain deferred.

**G18-008 ---** Phase 18 SHALL NOT infer, guess, invent, or embed an
Ericsson production-write protocol, endpoint, credential mechanism, CLI,
SSH operation, SDK call, or generic command mechanism.

**G18-009 ---** Real production campaign execution SHALL remain
unauthorized until all inherited external certification,
target-onboarding, production-authorization, and Phase 18 readiness
requirements are independently satisfied.

**G18-010 ---** Software implementation, local tests, architecture
gates, or campaign qualification evidence SHALL NOT by themselves
constitute production authorization.

**G18-011 ---** A `ProductionChangeCampaign` SHALL be revisioned, and an
authorized revision SHALL be immutable.

**G18-012 ---** Every material campaign change SHALL require a new
revision and new campaign fingerprint.

**G18-013 ---** A material campaign revision SHALL require new review
and new campaign authorization before production progression.

**G18-014 ---** Campaign authorization SHALL freeze the complete
`AuthorizedCampaignScope`.

**G18-015 ---** Authorized campaign scope SHALL identify every intended
campaign item and SHALL NOT be enlarged after authorization.

**G18-016 ---** Removal, replacement, mutation, or reordering of an
authorized item SHALL stale the existing campaign authority and require
new governance.

**G18-017 ---** A campaign item SHALL reference governed Phase 14, Phase
15, and Phase 16 lineage rather than construct arbitrary production
mutations.

**G18-018 ---** Campaign mutation identity SHALL bind target, cell,
parameter, expected value, desired value, and exact governed rollback
value.

**G18-019 ---** The campaign fingerprint SHALL bind all material
campaign, target, item, cohort, inherited governance, policy, window,
safety, certification, onboarding, and authorization identities.

**G18-020 ---** Campaign fingerprint canonicalization SHALL be
deterministic and independent of locale, process, database result
ordering, and platform-specific serialization.

**G18-021 ---** Ordered campaign collections SHALL preserve their
governed order in fingerprint canonicalization.

**G18-022 ---** Set-like campaign collections SHALL use a deterministic
canonical ordering.

**G18-023 ---** Material fingerprint mismatch SHALL make existing
campaign authority stale and SHALL deny progression.

**G18-024 ---** Campaign objectives SHALL be descriptive governance
metadata only and SHALL NOT be executable input to a vendor mutation
path.

**G18-025 ---** No campaign API or internal contract SHALL accept
arbitrary vendor commands, arbitrary endpoints, or free-form production
mutation payloads.

**G18-026 ---** Campaign review, authorization, cohort release, pause,
abort, evidence access, and resumption governance SHALL use explicit
least-privilege permissions.

**G18-027 ---** Campaign creator and campaign authorizer SHALL be
different authenticated human authorities.

**G18-028 ---** Campaign authorization and production cohort release
SHALL be separated so campaign authorization does not automatically
release production work.

**G18-029 ---** Human governance authority SHALL be derived from
authenticated identity rather than untrusted request metadata.

**G18-030 ---** Null, blank, malformed, unknown, or unauthorized
governance principals SHALL fail closed.

**G18-031 ---** Agent identities SHALL NOT satisfy human campaign
review, authorization, release, resumption, execution, or rollback
authority.

**G18-032 ---** MCP identities SHALL NOT satisfy human campaign review,
authorization, release, resumption, execution, or rollback authority.

**G18-033 ---** Campaign authorization SHALL NOT execute a production
mutation.

**G18-034 ---** Campaign creation SHALL NOT execute a production
mutation.

**G18-035 ---** Opening a campaign or change window SHALL NOT execute a
production mutation.

**G18-036 ---** A cohort SHALL require an explicit human release before
any contained item can become production-execution eligible.

**G18-037 ---** Health becoming eligible SHALL NOT automatically release
a cohort.

**G18-038 ---** Scheduler activity SHALL NOT authorize or release a
cohort or initiate a production mutation.

**G18-039 ---** Event consumption SHALL NOT authorize or release a
cohort or initiate a production mutation.

**G18-040 ---** Campaign governance actions SHALL have durable
idempotency/replay protection so duplicate requests cannot create
duplicate authority.

**G18-041 ---** Every initial Phase 18 production campaign SHALL begin
with a mandatory canary.

**G18-042 ---** The initial canary SHALL contain exactly one cell.

**G18-043 ---** A canary SHALL remain subject to the same individual
Phase 16/17 production controls as every later item.

**G18-044 ---** Canary success SHALL NOT be represented as statistical
proof that later campaign cells will behave identically.

**G18-045 ---** Cohorts SHALL be governance/release boundaries and SHALL
NOT constitute batch vendor mutation commands.

**G18-046 ---** Initial Phase 18 production mutation concurrency SHALL
be exactly one campaign-associated production mutation at a time.

**G18-047 ---** Campaign items SHALL execute sequentially under the
initial Phase 18 production model.

**G18-048 ---** A subsequent item SHALL NOT become send-eligible until
all predecessor conditions required by policy are satisfied.

**G18-049 ---** A subsequent cohort SHALL NOT become
progression-eligible until the preceding cohort satisfies required
verification, reconciliation, observation, and safety conditions.

**G18-050 ---** The only executable production progression mode in
initial Phase 18 SHALL be MANUAL.

**G18-051 ---** Phase 18 SHALL NOT introduce an executable dormant or
hidden automatic production progression mode.

**G18-052 ---** A human release SHALL authorize only the specifically
governed cohort and SHALL NOT pre-authorize future cohorts.

**G18-053 ---** Cohort membership SHALL be frozen when the cohort is
released.

**G18-054 ---** Released cohort membership SHALL NOT be enlarged,
reduced, replaced, or reordered under the existing release authority.

**G18-055 ---** A released item SHALL still require individual
production eligibility and Phase 16 authority before mutation.

**G18-056 ---** Unreleased campaign work SHALL possess no production
mutation capability.

**G18-057 ---** Phase 16 production grants SHALL NOT be pre-minted for
unreleased future campaign items.

**G18-058 ---** A production grant SHALL be created only sufficiently
late for an individually eligible item after current campaign safety has
been revalidated.

**G18-059 ---** Duplicate human release requests SHALL be idempotent and
SHALL NOT duplicate mutation capability.

**G18-060 ---** Campaign progression SHALL stop when required
predecessor state is UNKNOWN, STALE, ERROR, unresolved, or otherwise
non-progressable.

**G18-061 ---** Every production send SHALL revalidate global production
enablement.

**G18-062 ---** Every production send SHALL revalidate target enablement
and target operational state.

**G18-063 ---** Every production send SHALL revalidate campaign
enablement and campaign non-aborted/non-suspended state.

**G18-064 ---** Every production send SHALL revalidate the current
campaign revision and fingerprint.

**G18-065 ---** Every production send SHALL revalidate campaign
authorization currentness.

**G18-066 ---** Every production send SHALL revalidate campaign lease
ownership and fencing currentness.

**G18-067 ---** Every production send SHALL revalidate cohort release
currentness.

**G18-068 ---** Every production send SHALL revalidate item eligibility.

**G18-069 ---** Every production send SHALL revalidate campaign and
production-change windows.

**G18-070 ---** Every production send SHALL revalidate external
change-control currentness where policy requires it.

**G18-071 ---** Every production send SHALL revalidate applicable Phase
17 certification currentness.

**G18-072 ---** Every production send SHALL revalidate target onboarding
currentness.

**G18-073 ---** Every production send SHALL revalidate security-profile
and credential-profile currentness.

**G18-074 ---** Every production send SHALL revalidate applicable
blast-radius and rate-limit safety.

**G18-075 ---** Every production send SHALL revalidate Phase 16
production authorization currentness.

**G18-076 ---** Every production send SHALL require a current legal
Phase 16 production execution grant.

**G18-077 ---** Every production send SHALL require a current Phase 16
execution lease and fencing authority.

**G18-078 ---** The Production Write Gateway SHALL independently perform
final production preflight and SHALL remain the final trusted mutation
boundary.

**G18-079 ---** Application-side campaign eligibility SHALL NOT
substitute for gateway final preflight.

**G18-080 ---** UNKNOWN, STALE, ERROR, unavailable, or unverifiable
required currentness SHALL deny a new production send.

**G18-081 ---** Vendor acceptance SHALL NOT constitute successful
production verification.

**G18-082 ---** Successful production execution SHALL require
independent direct vendor readback according to inherited Phase 16/17
verification policy.

**G18-083 ---** `PRODUCTION_VERIFIED` SHALL remain distinct from
`CANONICAL_RECONCILED`.

**G18-084 ---** Phase 18 SHALL NOT directly mutate Phase 12 canonical
network state to make campaign execution appear reconciled.

**G18-085 ---** Canonical reconciliation SHALL occur through the
authoritative synchronization path.

**G18-086 ---** `CANONICAL_RECONCILED` SHALL remain distinct from
post-change observation health.

**G18-087 ---** Campaign progression SHALL require a governed
post-change observation interval where policy requires observation.

**G18-088 ---** Post-change telemetry SHALL satisfy explicit freshness
requirements.

**G18-089 ---** Stale telemetry SHALL NOT satisfy campaign health
progression.

**G18-090 ---** Telemetry unavailable or provenance unavailable SHALL
produce non-progressable health rather than assumed health.

**G18-091 ---** Campaign health SHALL distinguish HEALTHY, DEGRADED,
UNHEALTHY, UNKNOWN, and STALE network-observation states.

**G18-092 ---** Only HEALTHY network-observation state SHALL permit
initial Phase 18 progression.

**G18-093 ---** Assurance conditions required by campaign policy SHALL
be evaluated before progression.

**G18-094 ---** Verification mismatch SHALL block further forward
campaign progression.

**G18-095 ---** Unresolved production outcome SHALL block further
campaign mutation.

**G18-096 ---** Observation timeout SHALL block progression rather than
infer success.

**G18-097 ---** Required observation evidence SHALL be bound to the
relevant campaign item and production execution.

**G18-098 ---** Campaign progression SHALL use current health evidence
and SHALL NOT reuse stale prior HEALTHY state.

**G18-099 ---** Observation and health policy identities SHALL be
versioned/bound sufficiently to detect material policy change.

**G18-100 ---** Material health or observation policy change SHALL
invalidate future progression where the existing authorization no longer
binds the governing policy.

**G18-101 ---** Campaign safety controls SHALL be durable/shared and
SHALL NOT depend solely on process-local state.

**G18-102 ---** Campaign safety SHALL enforce a policy-defined maximum
campaign scope.

**G18-103 ---** Campaign implementation SHALL additionally have a hard
maximum campaign-size ceiling independent of mutable runtime policy.

**G18-104 ---** Effective campaign-size allowance SHALL never exceed the
stricter applicable hard or policy limit.

**G18-105 ---** Campaign safety SHALL bound cells, parameters, and
mutation operations.

**G18-106 ---** Campaign safety SHALL bound production mutation rate
over governed time intervals.

**G18-107 ---** Campaign safety SHALL track verification failures.

**G18-108 ---** Campaign safety SHALL track unresolved/ambiguous
production outcomes.

**G18-109 ---** Campaign safety SHALL track recovery/rollback events.

**G18-110 ---** Campaign safety SHALL support policy-defined
consecutive-failure stopping conditions.

**G18-111 ---** Rate and blast-radius enforcement SHALL be
concurrency-safe.

**G18-112 ---** Safety-limit failure or inability to establish
authoritative counters SHALL fail closed.

**G18-113 ---** Automatic safety suspension SHALL be permitted when a
safety condition becomes non-progressable.

**G18-114 ---** Automatic safety suspension SHALL NOT itself initiate a
production mutation.

**G18-115 ---** Automatic safety suspension SHALL NOT automatically
initiate rollback.

**G18-116 ---** Automatic safety suspension SHALL NOT automatically
resume the campaign.

**G18-117 ---** Operator pause SHALL be distinguishable from safety
suspension.

**G18-118 ---** Global kill, target kill, campaign suspension, cohort
release, and item eligibility SHALL form independent hierarchical
controls rather than substitutes for one another.

**G18-119 ---** Kill/suspension SHALL stop new sends but SHALL NOT claim
to cancel a vendor request already in flight.

**G18-120 ---** Typed machine-readable safety and eligibility reason
codes SHALL be available so clients do not infer safety state from
free-form text.

**G18-121 ---** Campaign abort SHALL prohibit additional forward
campaign mutations.

**G18-122 ---** Campaign abort SHALL NOT itself constitute rollback
authorization.

**G18-123 ---** Campaign abort SHALL NOT automatically initiate
rollback.

**G18-124 ---** An ambiguous child execution outcome SHALL immediately
block later campaign mutation.

**G18-125 ---** Phase 18 SHALL NOT automatically retry an ambiguous
production mutation.

**G18-126 ---** A child outcome observed at desired state may become
VERIFIED only through authoritative verification evidence.

**G18-127 ---** A child outcome observed at expected pre-change state
SHALL NOT automatically cause a retry; any later attempt requires
separately governed authority.

**G18-128 ---** A third/unexpected observed state SHALL require manual
intervention or equivalent fail-closed governance.

**G18-129 ---** Verification/readback unavailable after a possible send
SHALL remain unresolved and SHALL block progression.

**G18-130 ---** Campaign recovery planning SHALL NOT itself constitute
rollback authority.

**G18-131 ---** Every actual rollback SHALL remain separately requested,
reviewed, authorized, executed, and verified through authoritative
production controls.

**G18-132 ---** Rollback SHALL use the exact governed rollback value;
Phase 18 SHALL NOT invent a recovery value.

**G18-133 ---** Recovery ordering may be represented, including
reverse-order recovery where governed, but representation SHALL NOT
authorize the constituent rollbacks.

**G18-134 ---** Phase 18 SHALL NOT automatically execute rollback.

**G18-135 ---** Phase 18 SHALL NOT automatically retry rollback.

**G18-136 ---** Rollback verification SHALL remain distinct from vendor
acknowledgement.

**G18-137 ---** Ambiguous rollback outcome SHALL block additional
campaign-associated mutation and return to human governance.

**G18-138 ---** Recovery state SHALL be durably represented and
reconstructible after crash.

**G18-139 ---** Campaign final disposition SHALL distinguish complete
recovery, partial recovery, unresolved/manual-intervention, abort, and
expiry semantics as applicable.

**G18-140 ---** Campaign completion SHALL NOT conceal unresolved
mutation or recovery state.

**G18-141 ---** Phase 17 interface-definition withdrawal SHALL
invalidate affected campaign progression.

**G18-142 ---** Phase 17 transport-profile revocation or expiry SHALL
invalidate affected campaign progression.

**G18-143 ---** Phase 17 certification revocation, expiry, or required
staleness SHALL invalidate affected campaign progression.

**G18-144 ---** Target-onboarding revocation or expiry SHALL invalidate
affected campaign progression.

**G18-145 ---** Security-profile invalidation SHALL invalidate affected
campaign progression.

**G18-146 ---** Credential-profile invalidation SHALL invalidate
affected campaign progression.

**G18-147 ---** Revocation/invalidation SHALL prohibit new
campaign-originated grants/sends where affected.

**G18-148 ---** Revocation SHALL NOT rewrite consumed Phase 16 grants or
historical execution evidence.

**G18-149 ---** Revocation after `MAY_HAVE_SENT` SHALL stop future sends
while preserving mandatory outcome resolution.

**G18-150 ---** Campaign evidence SHALL provide bidirectional
correlation to the authoritative production execution lineage.

**G18-151 ---** Campaign audit SHALL be tamper-evident using
deterministic canonical event hashing.

**G18-152 ---** Campaign audit ordering/sequence SHALL be
concurrency-safe.

**G18-153 ---** Audit `INVALID`, `UNKNOWN`, `GAP`, or `HASH_MISMATCH`
SHALL block progression.

**G18-154 ---** Phase 18 SHALL claim tamper evidence only and SHALL NOT
claim immutable storage unless such storage is independently
established.

**G18-155 ---** Campaign evidence and audit SHALL NOT contain vendor
credentials or secrets.

**G18-156 ---** Crash recovery SHALL reconstruct authority only from
durable authoritative state.

**G18-157 ---** Crash recovery SHALL NOT infer successful external
mutation from local intent or handoff state alone.

**G18-158 ---** Duplicate release or recovery processing after restart
SHALL be idempotent.

**G18-159 ---** Failure to reconcile authoritative execution state after
crash SHALL block further progression.

**G18-160 ---** Historical execution, verification, reconciliation,
health, suspension, abort, and recovery evidence SHALL remain
distinguishable rather than overwritten into a simplified final state.

**G18-161 ---** Campaign orchestration SHALL remain in the
application/control plane and SHALL NOT move vendor-write credentials
into that plane.

**G18-162 ---** The separate Production Write Gateway SHALL remain the
production vendor-write credential/session boundary.

**G18-163 ---** The Production Write Gateway SHALL NOT become the
campaign progression engine.

**G18-164 ---** Phase 10 credential late-resolution and
no-secret-persistence requirements SHALL remain authoritative.

**G18-165 ---** Phase 18 SHALL NOT broaden production egress to
arbitrary hosts, ports, URLs, or `0.0.0.0/0`.

**G18-166 ---** Phase 18 SHALL NOT introduce SSH, generic CLI execution,
or arbitrary command transport.

**G18-167 ---** Default CI SHALL remain independent of Azure
credentials, vendor credentials, real vendor endpoints, and external
certification environments.

**G18-168 ---** Campaign software correctness qualification SHALL remain
distinct from vendor-lab qualification.

**G18-169 ---** Phase 18 C1 simulator qualification SHALL remain
distinct from C2 vendor-lab qualification.

**G18-170 ---** Phase 18 C2 vendor-lab qualification SHALL remain
distinct from C3 pre-production progressive-delivery qualification.

**G18-171 ---** Phase 18 C3 qualification SHALL remain distinct from C4
target-specific campaign operational approval.

**G18-172 ---** Phase 18 C-level qualification SHALL NOT replace Phase
17 L-level transport/target readiness.

**G18-173 ---** Phase 18 SHALL NOT claim L1, L2, L3, L4, C1, C2, C3, or
C4 evidence unless independently executed/satisfied.

**G18-174 ---** Agent, MCP, scheduler, and event mechanisms SHALL NOT
obtain production campaign mutation authority.

**G18-175 ---** Phase 18 SHALL NOT introduce closed-loop production
autonomy.

**G18-176 ---** Phase 18 SHALL NOT introduce parallel production
campaign mutation under the initial production model.

**G18-177 ---** Phase 18 SHALL NOT introduce cross-target production
campaigns.

**G18-178 ---** Phase 18 SHALL NOT introduce arbitrary/multi-parameter
production campaign items.

**G18-179 ---** Phase 18 SHALL preserve Ericsson-first/Nokia-deferred
vendor readiness without implying vendor certification.

**G18-180 ---** Completion of Phase 18 software SHALL NOT authorize real
production execution or real production campaign execution.

**G18-181 ---** `CampaignExecutionBinding` SHALL be immutable once
created for an eligible/released campaign item.

**G18-182 ---** `CampaignExecutionBinding` SHALL include the
authoritative Phase 16 production fingerprint.

**G18-183 ---** The gateway SHALL independently verify the campaign
execution binding for campaign-originated mutation.

**G18-184 ---** The campaign fencing token SHALL be bound to the
production execution handoff.

**G18-185 ---** The campaign control generation SHALL be bound to the
production execution handoff.

**G18-186 ---** A stale campaign fencing token SHALL deny a new
campaign-originated production send.

**G18-187 ---** Abort SHALL advance or otherwise authoritatively
invalidate the campaign execution/control generation before any later
new send.

**G18-188 ---** Safety suspension SHALL invalidate unused cohort release
authority.

**G18-189 ---** Applicable revocation SHALL invalidate unused cohort
release authority.

**G18-190 ---** Resumption SHALL require a new release generation for
unexecuted work and SHALL NOT resurrect stale release authority.

**G18-191 ---** Cohort release SHALL have an immutable membership
fingerprint binding its ordered released items and governing authority.

**G18-192 ---** Post-change observation SHALL have an explicit
authoritative observation boundary.

**G18-193 ---** Post-change health SHALL use measurement provenance and
SHALL NOT use ingestion time alone as proof of post-change measurement.

**G18-194 ---** Observation evidence SHALL be bound to the applicable
production execution and campaign item.

**G18-195 ---** Detected external interference SHALL invalidate affected
campaign progression until authoritative certainty is restored.

**G18-196 ---** Network observation health SHALL be distinct from
operational safety health.

**G18-197 ---** Initial production progression SHALL require both
network observation health to permit progression and operational safety
health to be SAFE.

**G18-198 ---** The campaign item lifecycle SHALL explicitly distinguish
planning, release, eligibility, handoff, send possibility, vendor
acceptance, verification, reconciliation, observation, completion, and
exceptional states.

**G18-199 ---** Campaign safety-budget check and reservation SHALL be
atomic in durable shared state.

**G18-200 ---** Safety-budget exposure reaching `MAY_HAVE_SENT` SHALL
NOT be blindly returned.

**G18-201 ---** Authorized campaign scope SHALL be immutable under the
applicable campaign authorization.

**G18-202 ---** Campaign completion SHALL account for every item in
authorized campaign scope.

**G18-203 ---** Campaign handoff to Phase 16 SHALL be durably recorded.

**G18-204 ---** Crash recovery SHALL consult authoritative Phase 16
production execution state before determining whether another action is
permissible.

**G18-205 ---** Campaign audit/evidence SHALL cryptographically bind the
relevant Phase 16 execution fingerprint/identity.

**G18-206 ---** Campaign-originated production execution evidence SHALL
link back to the applicable campaign execution binding.

**G18-207 ---** Initial Phase 18 SHALL prohibit duplicate
`(productionTargetId, cellId, parameter)` entries within one campaign
revision.

**G18-208 ---** Campaign authority/fingerprint SHALL bind the applicable
health-policy version.

**G18-209 ---** Campaign authority/fingerprint SHALL bind the applicable
observation-policy version.

**G18-210 ---** External change-ticket currentness SHALL be represented
explicitly and SHALL NOT be silently inferred from reference presence
alone.

**G18-211 ---** Campaign objective text SHALL remain non-executable
governance metadata.

**G18-212 ---** Campaign canonicalization SHALL explicitly distinguish
ordered and set-like semantics and SHALL remain deterministic.

**G18-213 ---** Campaign size SHALL be bounded by both policy and an
implementation hard ceiling.

**G18-214 ---** Canary success SHALL NOT be represented as statistical
representativeness of later campaign scope.

**G18-215 ---** A material observation-policy change SHALL invalidate
future progression where existing authority no longer binds the
governing policy.

**G18-216 ---** A material health-policy change SHALL invalidate future
progression where existing authority no longer binds the governing
policy.

**G18-217 ---** Safety and eligibility decisions SHALL expose typed
machine-readable reason codes.

**G18-218 ---** Human campaign review, authorization, release, pause,
abort, and resumption governance SHALL have durable idempotency/replay
protection.

**G18-219 ---** Campaign resumption SHALL require explicit governed
review and authorization.

**G18-220 ---** Campaign resumption SHALL bind the suspension cause and
remediation evidence.

**G18-221 ---** Campaign resumption SHALL NOT itself release a cohort.

**G18-222 ---** Safety invalidation after `MAY_HAVE_SENT` SHALL NOT
terminate mandatory outcome resolution for the possibly sent operation.

**G18-223 ---** Current campaign control generation SHALL be checked at
a trusted production pre-send boundary.

**G18-224 ---** Campaign execution origin SHALL NOT be a
caller-forgeable mechanism for bypassing campaign controls.

**G18-225 ---** Campaign safety-budget reservation SHALL NOT constitute
mutation authority.

**G18-226 ---** Uncertain external exposure SHALL be accounted for
conservatively.

**G18-227 ---** Detected interference on the same target/cell/parameter
during the relevant observation interval SHALL invalidate attribution by
default.

**G18-228 ---** Material policy change SHALL NOT rewrite historical
execution or observation evidence.

**G18-229 ---** Unused release authority SHALL become stale when its
bound campaign authorization generation is no longer current.

**G18-230 ---** No successful final campaign state SHALL silently omit
an item from immutable authorized campaign scope.

**G18-231 ---** A Phase 16 production change bound to a campaign cannot
subsequently be executed through a standalone-origin path that omits the
campaign binding.

**G18-232 ---** Campaign control and release generations are never
reused or reset such that stale authority can become current again.

**G18-233 ---** A campaign safety-budget reservation may expire or
release only when authoritative execution evidence establishes that the
associated operation did not reach `MAY_HAVE_SENT`; uncertainty retains
conservative exposure.

**G18-234 ---** A campaign-originated Phase 16 production execution maps
to exactly one `CampaignExecutionBinding` and one campaign item.

**G18-235 ---** A campaign item cannot obtain duplicate active forward
execution lineages through replay or crash recovery.

**G18-236 ---** Superseded conflicting observation or reconciliation
evidence cannot satisfy progression merely because it was previously
valid.

**G18-237 ---** Campaign resumption cannot make progression executable
while a blocking production outcome remains unresolved.

**G18-238 ---** Campaign safety exposure separately accounts for forward
and recovery mutation exposure.

**G18-239 ---** Recovery mutation capacity cannot be used for forward
mutation.

**G18-240 ---** Forward mutation capacity does not authorize recovery
mutation.

**G18-241 ---** A recovery mutation reaching `MAY_HAVE_SENT`
conservatively consumes recovery exposure.

**G18-242 ---** `RECOVERY_REQUIRED` permanently closes forward
progression for the current campaign revision.

**G18-243 ---** A campaign-associated rollback reaching authorized
execution permanently closes forward progression for the current
campaign revision.

**G18-244 ---** Successful recovery does not reopen forward progression
for that campaign revision.

**G18-245 ---** Forward optimization after recovery requires new
governed campaign authority and a new applicable canary lifecycle.

**G18-246 ---** Campaign validity cannot resurrect stale, revoked, or
otherwise non-executable authoritative Phase 14, Phase 15, or Phase 16
state.

**G18-247 ---** Required active/historical campaign evidence has
retention and referential-integrity semantics sufficient to make missing
evidence detectable and fail closed where required.

**G18-248 ---** External change-control invalidation participates in
campaign control-generation invalidation whenever current external
change-control authority is required.

**G18-249 ---** Human campaign authority derives from authenticated
actor identity, not caller-supplied principal fields.

**G18-250 ---** Campaign mutation equality uses canonical typed value
and unit semantics across campaign, Phase 16, and gateway boundaries.

**G18-251 ---** Initial Phase 18 post-change observation begins no
earlier than successful direct vendor verification.

**G18-252 ---** A future cohort cannot be validly pre-released before
predecessor verification, reconciliation, observation, and safety
requirements have been satisfied.

**G18-253 ---** Historical outcome resolution uses its bound historical
evidence/policy identity while current authoritative policy governs
future progression.

**G18-254 ---** Absence of detected same-parameter interference is not
represented as proof of exclusive causal attribution.

# 37. Frozen Threat Catalogue --- Normative

**T18-01 ---** Per-change authorization bypass:\*\* Campaign authority
is incorrectly treated as sufficient authority to mutate the network
without the required individual production authorization/grant.

**T18-02 ---** Post-authorization cell injection:\*\* A cell is added to
campaign scope after the campaign revision was reviewed/authorized.

**T18-03 ---** Post-release cohort enlargement:\*\* A cohort gains
additional items after human release.

**T18-04 ---** Item reordering:\*\* Campaign item ordering is changed
after authorization/release to alter progression semantics.

**T18-05 ---** Stale certification use:\*\* A campaign progresses using
expired, revoked, stale, or otherwise non-current Phase 17
certification.

**T18-06 ---** Stale onboarding use:\*\* A campaign progresses using
expired, revoked, stale, or otherwise non-current target onboarding.

**T18-07 ---** Window grandfathering:\*\* A campaign or item is allowed
to mutate after the applicable execution window closes because it was
previously authorized/released.

**T18-08 ---** Duplicate item execution:\*\* Replay, retry, crash
recovery, or duplicate processing causes one campaign item to execute
more than once.

**T18-09 ---** Concurrent orchestrators:\*\* Multiple campaign
orchestrators concurrently progress the same campaign without
authoritative lease/fencing protection.

**T18-10 ---** Grant pre-minting or replay:\*\* Production execution
grants are created for unreleased future work or replayed beyond their
legal single-use/currentness semantics.

**T18-11 ---** Progression after ambiguous outcome:\*\* A later campaign
item executes while an earlier production mutation outcome remains
ambiguous or unresolved.

**T18-12 ---** Verification failure ignored:\*\* Campaign progression
continues after required independent production verification fails or
mismatches.

**T18-13 ---** Stale telemetry accepted as healthy:\*\* Old telemetry or
health state is reused to satisfy post-change campaign observation.

**T18-14 ---** Automatic resumption:\*\* A suspended campaign
automatically resumes production progression when a condition appears
healthy again.

**T18-15 ---** Abort triggers unauthorized rollback:\*\* Campaign abort
is incorrectly treated as authority to execute rollback.

**T18-16 ---** Recovery bypasses rollback governance:\*\* Campaign
recovery planning or orchestration executes rollback without separately
governed rollback authority.

**T18-17 ---** Audit tampering:\*\* Campaign audit/evidence is altered,
reordered, removed, substituted, or made inconsistent without
progression being blocked.

**T18-18 ---** Cross-target injection:\*\* An item or execution for
another production target is inserted into a single-target campaign.

**T18-19 ---** Mutation substitution:\*\* Cell, parameter, expected
value, desired value, or rollback value differs between the authorized
campaign item and executed production mutation.

**T18-20 ---** Transport/certification revocation mid-campaign:\*\*
Phase 17 transport/certification authority becomes invalid after
campaign authorization or release but before later production mutation.

**T18-21 ---** Credential/security revocation mid-campaign:\*\* Security
or credential profile becomes invalid while the campaign still holds
unused progression authority.

**T18-22 ---** Kill-switch race:\*\* A production send crosses the
external mutation boundary despite a concurrent global, target, or
campaign safety stop.

**T18-23 ---** Crash during mutation:\*\* Application or gateway failure
around the external send boundary causes incorrect inference of whether
the vendor mutation occurred.

**T18-24 ---** Crash during progression:\*\* Campaign orchestrator
failure causes lost, duplicated, or incorrectly reconstructed
progression authority.

**T18-25 ---** Duplicate cohort release:\*\* Replayed or concurrent
human release requests create duplicate release authority or duplicate
production work.

**T18-26 ---** Agent/MCP release:\*\* An Agent or MCP pathway acquires
human cohort release or campaign authorization authority.

**T18-27 ---** Scheduler/event mutation:\*\* A scheduler or event
automatically releases, grants, or executes a production mutation.

**T18-28 ---** Rate-limit race:\*\* Concurrent campaign operations
exceed shared rate/blast safety limits because checks and reservations
are non-atomic.

**T18-29 ---** Observation evidence substitution:\*\* Telemetry/health
evidence for another object, source, interval, or execution is attached
to the campaign item.

**T18-30 ---** Reconciliation mistaken for verification:\*\* Canonical
synchronization is incorrectly treated as proof of immediate vendor
mutation success.

**T18-31 ---** Completion with unresolved item:\*\* Campaign reaches a
successful final state while an authorized item has unresolved
execution, verification, reconciliation, observation, or recovery state.

**T18-32 ---** Wrong rollback value:\*\* Recovery uses a newly
calculated, guessed, stale, or otherwise different value instead of the
exact governed rollback value.

**T18-33 ---** Evidence bundle substitution:\*\* A valid but unrelated
execution, verification, health, certification, or evidence bundle is
substituted for the campaign's actual lineage.

**T18-34 ---** Expired campaign authorization:\*\* A later cohort
executes under campaign authorization that is expired, revoked, stale,
or otherwise non-current.

**T18-35 ---** Lost lease holder continues:\*\* An orchestrator
continues progression after losing its authoritative campaign
lease/fencing ownership.

**T18-36 ---** Old revision progresses:\*\* A superseded campaign
revision continues to release or execute work.

**T18-37 ---** Cached health beyond freshness:\*\* Previously healthy
campaign evidence is reused after its permitted freshness interval.

**T18-38 ---** Release while suspended:\*\* A cohort is released or
executed while the campaign is safety-suspended.

**T18-39 ---** Safety-counter divergence:\*\* Durable rate, blast,
unresolved-outcome, failure, or recovery counters diverge so safety
exposure is understated.

**T18-40 ---** Partial recovery reported as recovered:\*\* Campaign is
reported as successfully recovered although one or more required
recovery outcomes remain incomplete, failed, ambiguous, or unverified.

**T18-41 ---** Campaign item substitution between release and
execution:\*\* The released campaign item is replaced by a different but
otherwise valid Phase 16 production mutation before execution.

**T18-42 ---** Stale campaign holder crosses into Phase 16:\*\* A
campaign orchestrator loses its lease/fencing authority after
eligibility evaluation but still initiates a production send.

**T18-43 ---** Old release reused after resumption:\*\* A cohort release
created before suspension/revocation is reused after
remediation/resumption.

**T18-44 ---** Abort races with eligible execution:\*\* A campaign abort
occurs after an item becomes eligible but before external send, and
stale authority continues execution.

**T18-45 ---** Pre-change telemetry accepted post-change:\*\* Telemetry
measured before the mutation but ingested afterward is incorrectly
accepted as post-change observation evidence.

**T18-46 ---** External change corrupts attribution:\*\* An independent
network change during the observation interval invalidates attribution
but campaign health continues as though the campaign were the sole
relevant change.

**T18-47 ---** Safety budget returned after possible send:\*\* Capacity
is returned even though the operation reached or may have reached
`MAY_HAVE_SENT`.

**T18-48 ---** Completion by omission:\*\* Campaign completes by never
releasing or terminally accounting for part of immutable authorized
scope.

**T18-49 ---** Unrelated valid execution substituted:\*\* A valid Phase
16 execution unrelated to the campaign item is attached as campaign
execution evidence.

**T18-50 ---** Health-policy change without invalidation:\*\* Material
health-policy change occurs but existing progression authority/evidence
remains treated as current.

**T18-51 ---** Duplicate target/cell/parameter:\*\* A campaign revision
contains multiple forward items for the same target, cell, and
parameter, making expected-state and observation attribution ambiguous.

**T18-52 ---** External change-control becomes invalid:\*\* External
ticket/change authority ceases to be current before a later cohort but
progression continues.

**T18-53 ---** Resumption resurrects stale authority:\*\* Campaign
resumption restores old release, binding, or execution authority instead
of requiring current governance.

**T18-54 ---** Healthy KPI masks unsafe operation:\*\* Network
observation appears healthy while certification, security, target,
audit, credential, or other operational safety state is unsafe.

**T18-55 ---** Recovery omitted from safety exposure:\*\*
Rollback/recovery mutation bypasses or disappears from campaign
safety-exposure accounting.

**T18-56 ---** Recovery budget reused for forward mutation:\*\* Recovery
capacity is borrowed to continue forward optimization after forward
capacity is exhausted.

**T18-57 ---** Forward progression resumes after recovery:\*\* The
campaign continues later forward items after recovery changed or
invalidated the assumptions under which the revision was authorized.

**T18-58 ---** Campaign execution downgraded to standalone:\*\* A
campaign-bound Phase 16 production change is executed through a
standalone path after campaign suspension, abort, closure, or other
invalidation.

**T18-59 ---** Reservation returned despite unresolved send:\*\* A
safety reservation expires/releases capacity while the associated
operation may already have reached `MAY_HAVE_SENT`.

**T18-60 ---** One execution attached to multiple items:\*\* A single
valid production execution is reused as the execution lineage for
multiple campaign items.

**T18-61 ---** One item replayed into multiple executions:\*\* Crash
recovery, replay, or duplicate processing creates multiple active
forward production execution lineages for one campaign item.

**T18-62 ---** Superseded evidence reused:\*\* Observation or
reconciliation evidence known to be superseded by conflicting
authoritative state is reused for progression.

**T18-63 ---** Automation masquerades as human:\*\* An Agent, service,
scheduler, automation identity, or caller-supplied principal field is
treated as authenticated human campaign authority.

**T18-64 ---** Value/unit canonicalization mismatch:\*\* Numeric or unit
representation differences defeat mutation-binding equality or permit
semantically different mutation values to be treated as equivalent.

# 38. Frozen Invariant Catalogue --- Normative

**I18-01 ---** Campaign authority is not mutation authority.\*\* No
campaign authorization, state, release, or orchestration decision by
itself authorizes a production network mutation.

**I18-02 ---** Campaign release is not a Phase 16 production execution
grant.\*\* Every production mutation still requires the authoritative
individual production execution authority.

**I18-03 ---** Campaign lease is not the production execution lease.\*\*
Campaign orchestration fencing and Phase 16 mutation fencing remain
separate authorities.

**I18-04 ---** Vendor acknowledgement is not production
verification.\*\* Vendor acceptance alone never proves the desired
network state.

**I18-05 ---** Production verification is not canonical
reconciliation.\*\* Direct vendor readback and Phase 12 canonical
synchronization remain distinct evidence.

**I18-06 ---** Canonical reconciliation is not observation health.\*\*
Canonical state convergence alone does not establish acceptable
post-change network behavior.

**I18-07 ---** Healthy is not released.\*\* Healthy observation creates
at most progression eligibility; an explicit human release is still
required.

**I18-08 ---** Abort is not rollback.\*\* Aborting a campaign stops
further forward mutation but does not authorize or automatically perform
recovery.

**I18-09 ---** Recovery planning is not rollback authorization.\*\*
Every actual rollback remains separately governed.

**I18-10 ---** Automatic safety suspension is not automatic
resumption.\*\* Recovery of the triggering condition never automatically
restores production progression authority.

**I18-11 ---** Campaign start is not future window authorization.\*\*
Every production send must satisfy current applicable campaign and
production-change windows.

**I18-12 ---** Software capability is not external certification.\*\*
Implementation or local evidence cannot substitute for required
vendor/lab/pre-production certification.

**I18-13 ---** External certification is not production
authorization.\*\* Certification/readiness alone does not authorize a
production mutation or campaign.

**I18-14 ---** Unknown, stale, or erroneous required safety state denies
progression.\*\* Loss of certainty removes permission for new
progression/send.

**I18-15 ---** No ambiguous outcome may be followed by another campaign
mutation.\*\* A possible external mutation must be resolved or governed
to a blocking terminal/manual state before further mutation.

**I18-16 ---** Every production mutation is individually governed.\*\*
Campaign orchestration never converts multiple changes into one blanket
mutation authority.

**I18-17 ---** Unreleased campaign work has no mutation capability.\*\*
Future work cannot hold a pre-minted production capability merely
because it belongs to an authorized campaign.

**I18-18 ---** Consumed grants and historical execution evidence are
never rewritten to manufacture reusable authority.\*\*

**I18-19 ---** Phase 18 cannot broaden Phase 17 vendor protocol
knowledge.\*\* Campaign orchestration does not infer or invent vendor
write mechanisms.

**I18-20 ---** Real production campaign execution remains unauthorized
until independently authorized.\*\* Architecture/software existence does
not change that state.

**I18-21 ---** Released campaign item identity must equal the mutation
identity authorized and executed by Phase 16/17.

**I18-22 ---** A stale campaign lease holder cannot initiate a new
campaign-originated production send.

**I18-23 ---** Abort, suspension, revocation, and material invalidation
invalidate unused release authority.

**I18-24 ---** Safety invalidation after `MAY_HAVE_SENT` cannot
terminate mandatory outcome resolution.

**I18-25 ---** Post-change health evidence must be attributable to the
verified post-change observation boundary.

**I18-26 ---** `MAY_HAVE_SENT` safety-budget exposure cannot be blindly
returned.

**I18-27 ---** Campaign completion must account for every item in
immutable authorized campaign scope.

**I18-28 ---** Network observation health and operational safety health
are independent and both must permit progression.

**I18-29 ---** Campaign-to-production execution correlation must be
bidirectional and tamper-evident.

**I18-30 ---** External interference invalidates affected campaign
assumptions until authoritative reconciliation restores certainty.

**I18-31 ---** Forward and recovery mutations have separate durable
safety-exposure accounting; neither safety budget grants mutation
authority, recovery capacity cannot be borrowed for forward progression,
and any `MAY_HAVE_SENT` operation conservatively consumes its applicable
exposure.

**I18-32 ---** Once a campaign revision requires recovery or begins a
campaign-associated rollback execution, forward progression for that
revision is permanently closed; successful recovery cannot reopen it,
and any later forward optimization requires new governed authority.

# 39. Evidence Traceability Requirements

The frozen architecture's threat→gate and invariant→gate mappings remain
authoritative and must be copied exactly into the eventual
machine-readable implementation evidence map. Implementation review must
prove: - gate definitions: 254 unique, no missing/duplicates; - threats:
64 unique, no missing/duplicates; - invariants: 32 unique, no
missing/duplicates; - every threat maps to principal gates and tests; -
every invariant maps to gates and enforcement evidence; - every
high-risk gate has non-structural evidence; - zero broken evidence
references.


# 40. Final Specification Status

``` text
PHASE 18 ARCHITECTURE GIT BASELINE:
f38a62ad0e3f80522a95830322079b1380289719

PHASE 18 ARCHITECTURE CONTENT SHA-256:
19865a242141c6ab4f5e9233056eebada22dbc9c9b456b68676c736a07763a32

PHASE 18 ARCHITECTURE EXACT-SHA CI:
33958028479 — SUCCESS

PHASE 18 IMPLEMENTATION SPECIFICATION:
ACCEPTED
FROZEN

TARGETED ADVERSARIAL REVIEW R3:
PASSED

C18-SR3:
0

B18-SR3:
0

D18-SR3:
0

A18-SR3:
2 NON-BLOCKING IMPLEMENTATION EVIDENCE OBLIGATIONS

OPEN CRITICAL SPECIFICATION FINDINGS:
0

OPEN BLOCKING SPECIFICATION FINDINGS:
0

ARCHITECTURAL CONTRADICTIONS:
0

A18-R3-01:
CLOSED BY ADVERSARIAL REVIEW R2

A18-R3-02:
CLOSED BY TARGETED ADVERSARIAL REVIEW R3

A18-SR3-01:
NOT EXECUTED — MANDATORY IMPLEMENTATION EVIDENCE

A18-SR3-02:
NOT EXECUTED — MANDATORY IMPLEMENTATION EVIDENCE

GIT SPECIFICATION BASELINE:
NOT YET CREATED

EXACT-SHA SPECIFICATION CI:
NOT YET EXECUTED

V19:
NOT CREATED

PHASE 18 IMPLEMENTATION:
NOT AUTHORIZED

PHASE 19:
NOT STARTED

REAL PRODUCTION EXECUTION:
NOT AUTHORIZED

REAL PRODUCTION CAMPAIGN EXECUTION:
NOT AUTHORIZED
```

*End of Phase 18 repository-specific implementation specification
ACCEPTED / FROZEN. This document does not authorize implementation.*