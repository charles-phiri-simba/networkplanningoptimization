# SNIP Phase 13 --- Network Change Intelligence, Optimization Proposals & Governed Recommendations Implementation Specification

**Repository:** `charles-phiri-simba/networkplanningoptimization`\
**Parent immutable baseline:**
`2f51bc1fd746633ec051a2ea933aa339c0ddc804`\
**Parent phase:** Phase 12 --- Continuous Synchronization, Drift &
Network Knowledge Confidence\
**Accepted architecture:**
`docs/architecture/SNIP-PHASE-13-NETWORK-CHANGE-INTELLIGENCE-OPTIMIZATION-PROPOSALS-GOVERNED-RECOMMENDATIONS-ARCHITECTURE.md`\
**Architecture status:** `ACCEPTED`\
**Implementation status:** `NOT STARTED`\
**Specification status:** `AUTHORIZED FOR IMPLEMENTATION`\
**Expected next Flyway migration:** `V14`\
**Document date:** 2026-08-28

------------------------------------------------------------------------

## 1. Purpose

This specification instructs Cursor to implement SNIP Phase 13 exactly
within the accepted architecture.

Phase 13 establishes the **Network Change Intelligence Plane**. It
converts trusted network state and existing analytical evidence into
bounded, simulated, deterministic, explainable, governed network-change
recommendations.

The defining implementation rule is:

> **Phase 13 may create recommendation authority. It must not create
> execution authority.**

A proposal may become `APPROVED`, but neither approval nor any other
Phase 13 state may cause a real vendor-network mutation.

------------------------------------------------------------------------

## 2. Immutable Parent Baseline

All Phase 13 implementation work is based on:

``` text
2f51bc1fd746633ec051a2ea933aa339c0ddc804
```

Do not amend, rewrite, rebase, squash, or otherwise alter the Phase 12
baseline.

------------------------------------------------------------------------

## 3. Architecture Is Authoritative

The accepted Phase 13 architecture is authoritative over this
specification.

If repository inspection reveals a conflict between implementation
convenience and an accepted architecture invariant:

1.  preserve the architecture;
2.  do not invent a workaround that weakens it;
3.  document the conflict in the completion report;
4.  stop before making an architectural change unless the change is
    explicitly authorized.

All 53 Phase 13 architecture acceptance gates remain mandatory.

------------------------------------------------------------------------

## 4. Cursor Execution Contract

Cursor is authorized to:

-   inspect the repository;
-   implement Phase 13;
-   create the V14 migration if required by the final persistence
    design;
-   add or modify Phase 13 tests;
-   make narrowly necessary integration changes to existing code;
-   update appropriate Phase 13 implementation/completion documentation;
-   run Maven and Go verification;
-   produce a completion report.

Cursor is NOT authorized to:

-   weaken the accepted architecture;
-   implement Phase 14;
-   introduce real vendor writes;
-   configure production ENM transport;
-   introduce closed-loop optimization;
-   commit;
-   push;
-   tag;
-   amend the Phase 12 baseline;
-   create a Phase 13 Git baseline;
-   claim architectural acceptance of its own implementation.

After implementation and verification, Cursor MUST stop.

------------------------------------------------------------------------

## 5. Required Pre-Implementation Repository Inspection

Before editing application code, re-confirm the concrete shapes and APIs
of:

-   `AssuranceCaseEntity`;
-   `AssuranceDetectionService`;
-   `DecisionIntelligenceService`;
-   Phase 4 `ProposedAction`;
-   `ActionPolicyEvaluator`;
-   `CapabilityRegistry`;
-   `AgentPermissionGuard`;
-   `AgentProposalAdapter`;
-   `SimulatableParameterRegistry`;
-   `DigitalTwinSimulationService`;
-   Phase 6 simulation result/persistence/confidence types;
-   `NetworkKnowledgeConfidenceEvaluator`;
-   `NetworkKnowledgeStatusEntity`;
-   `NetworkDriftService`;
-   Phase 12 synchronization/source/snapshot identifiers;
-   `IntegrationController` query patterns;
-   `VendorImportAuthorizer` authorization style;
-   existing audit abstraction;
-   existing metrics abstraction;
-   current Flyway migrations through V13;
-   controller/error-response conventions;
-   JPA transaction/locking/versioning conventions;
-   architecture-isolation test patterns.

Reuse existing concepts where they satisfy the required semantics.

Do not create duplicate abstractions merely to match names in this
specification.

------------------------------------------------------------------------

## 6. Required Package Boundary

Create a coherent Phase 13 package/domain using repository naming
conventions.

Conceptually it should contain responsibilities equivalent to:

``` text
changeintelligence/
  api/
  model/
  persistence/
  repository/
  service/
  policy/
  authorization/
```

Exact package decomposition may follow existing repository style.

The important boundary is that Phase 13 code must remain independent of
vendor connector/transport/credential implementations.

------------------------------------------------------------------------

## 7. Authoritative Existing Dependencies

Phase 13 MUST reuse:

``` text
SimulatableParameterRegistry
DigitalTwinSimulationService
NetworkKnowledgeConfidenceEvaluator
NetworkKnowledgeStatusEntity
NetworkDriftService
```

where their current APIs provide the required behavior.

Do not create:

``` text
Phase13SimulatableParameterRegistry
Phase13DigitalTwinSimulationService
Phase13NetworkKnowledgeConfidenceEvaluator
Phase13NetworkDriftService
```

or semantically duplicate equivalents.

------------------------------------------------------------------------

## 8. Initial Supported Optimization Parameter

Phase 13 initially supports only:

``` text
txPower
```

`SimulatableParameterRegistry` is the authoritative source of parameter
support and range.

Repository inspection currently indicates the Phase 6 range is:

``` text
20–50 dBm
```

Do not hard-code a second independent Phase 13 range.

Candidate validation must delegate to or derive from the authoritative
Phase 6 registry.

------------------------------------------------------------------------

## 9. Initial Optimization Scope

Initial Phase 13 scope is:

``` text
one proposal
one target entity/cell
one parameter
one current value
one bounded candidate set
```

Do not implement coupled multi-cell optimization, network-wide search,
grouped execution, rollback groups, or change ordering.

------------------------------------------------------------------------

## 10. OptimizationOpportunity Semantics

`OptimizationOpportunity` is a **logical architectural concept**, not a
mandated new persistent entity.

Prefer deriving proposal input from existing Phase 3 assurance/decision
objects.

Only introduce a dedicated opportunity class if needed as an internal
value object or request model.

Do not create a dedicated database table solely because the architecture
uses the term `OptimizationOpportunity`.

------------------------------------------------------------------------

## 11. Phase 3 Integration

Use existing Phase 3 evidence where available:

``` text
AssuranceCaseEntity
AssuranceDetectionService
DecisionIntelligenceService
```

A proposal must retain safe identifiers/references to the relevant
assurance and/or decision evidence used in generation.

Do not duplicate Phase 3 detection or decision algorithms inside Phase
13.

------------------------------------------------------------------------

## 12. Phase 4 ProposedAction Boundary

`NetworkChangeProposal` and Phase 4 `ProposedAction` are distinct
concepts.

``` text
NetworkChangeProposal
= analytical, simulated, governed recommendation

ProposedAction
= existing Phase 4 governed-action concept
```

There MUST NOT be an automatic Phase 13 mapping:

``` text
NetworkChangeProposal -> ProposedAction
```

including after `APPROVED`.

No Phase 13 lifecycle transition may invoke Phase 4 to perform a
real-network action.

------------------------------------------------------------------------

## 13. Phase 5 Agent Boundary

`AgentProposalAdapter` may be reused only as an initiation/request
boundary if appropriate.

An Agent may request Phase 13 analysis.

An Agent MUST NOT:

-   directly persist an authoritative `NetworkChangeProposal`;
-   assign authoritative proposal status;
-   assign authoritative risk;
-   assign authoritative score;
-   override confidence;
-   approve or reject a proposal;
-   call `EnmTransport`;
-   call vendor connectors;
-   resolve credentials;
-   create vendor commands;
-   mutate canonical network state.

The authoritative Phase 13 service owns proposal creation.

------------------------------------------------------------------------

## 14. Phase 6 Digital Twin Boundary

`DigitalTwinSimulationService` is the authoritative simulation service.

Do not create a second simulation engine.

Every candidate eligible for recommendation must be evaluated through
the existing Phase 6 simulation path.

Preserve the current synthetic/non-vendor-calibrated model limitations.

------------------------------------------------------------------------

## 15. Simulation Confidence

Use the existing `SimulationConfidence`.

Repository inspection indicates the current synthetic model yields:

``` text
SimulationConfidence.LOW
```

Tests and user-facing evidence must reflect this reality.

Do not artificially upgrade simulation confidence to make
recommendations appear stronger.

------------------------------------------------------------------------

## 16. Phase 12 Knowledge Boundary

Use existing Phase 12 authoritative state/services for:

-   network knowledge confidence;
-   freshness;
-   drift;
-   source/synchronization state.

Do not reimplement Phase 12 confidence or drift algorithms in Phase 13.

------------------------------------------------------------------------

## 17. Three Confidence Domains

Preserve these as separate concepts:

``` text
assurance.Confidence
NetworkKnowledgeConfidence
SimulationConfidence
```

They MUST NOT be:

-   collapsed into one `confidence`;
-   averaged;
-   implicitly converted;
-   substituted for one another.

Persist or expose them distinctly where relevant.

------------------------------------------------------------------------

## 18. Hard Network-Knowledge Gate

The initial recommendation eligibility policy is:

``` text
HIGH    -> recommendation may proceed
MEDIUM  -> recommendation may proceed with explicit degradation
LOW     -> RECOMMENDED prohibited
UNKNOWN -> RECOMMENDED prohibited
```

This is a hard gate.

No proposal score, benefit, LLM output, assurance confidence, or
simulation result may compensate for:

``` text
NetworkKnowledgeConfidence = LOW
```

or:

``` text
NetworkKnowledgeConfidence = UNKNOWN
```

------------------------------------------------------------------------

## 19. Proposal Domain Model

Implement a first-class persistent `NetworkChangeProposal` concept.

It must represent at least:

``` text
proposal identity
proposal type
target entity type
target entity ID
parameter
current value
proposed value
unit
source system
source snapshot identifier
source synchronization execution identifier
network knowledge confidence
knowledge reason/evidence
assurance evidence references
decision evidence references
simulation evidence references
benefit assessment
risk assessment
proposal score
status
generation initiator/method
creation time
evaluation time
expiration time
version/concurrency information
supersession/invalidation information
```

Exact Java names should match repository conventions.

------------------------------------------------------------------------

## 20. Proposal Type

Initial proposal type:

``` text
RADIO_TX_POWER_OPTIMIZATION
```

Use an enum or equivalent strongly typed representation.

Do not create generic executable command proposal types.

------------------------------------------------------------------------

## 21. Proposal Status

Implement a deterministic lifecycle equivalent to:

``` text
DRAFT
VALIDATING
INVALID
SIMULATING
SIMULATION_FAILED
EVALUATED
RECOMMENDED
REJECTED
APPROVED
EXPIRED
SUPERSEDED
INVALIDATED
```

If repository style favors fewer persisted transient states, Cursor may
keep `VALIDATING`/`SIMULATING` as execution-state semantics rather than
durable states, but the externally meaningful outcomes and safety
behavior must remain equivalent.

Document any such choice.

------------------------------------------------------------------------

## 22. Forbidden Statuses

Do not introduce:

``` text
EXECUTING
EXECUTED
APPLIED
DEPLOYED
ROLLING_BACK
ROLLED_BACK
REMEDIATING
```

or semantic equivalents.

------------------------------------------------------------------------

## 23. Proposal Value Representation

For the initial `txPower` case, use a deterministic numeric
representation suitable for exact domain comparison.

Do not store executable vendor syntax.

Unit must be represented safely, e.g.:

``` text
dBm
```

according to existing repository conventions.

------------------------------------------------------------------------

## 24. Snapshot Binding

Every proposal must persist enough source identity to prove the network
state used for analysis.

At minimum, where available from current Phase 12 data:

``` text
sourceSystem
sourceSnapshotId
sourceSynchronizationExecutionId
knowledgeEvaluatedAt
```

If the existing model uses differently named stable identifiers, use
those rather than inventing redundant IDs.

------------------------------------------------------------------------

## 25. Current-Value Binding

Persist:

``` text
currentValue
proposedValue
```

The proposal's `currentValue` is an authoritative precondition for later
review validity.

It must not be overwritten when the canonical state changes.

------------------------------------------------------------------------

## 26. Canonical State Integrity

Creating, recommending, approving, rejecting, expiring, invalidating, or
superseding a Phase 13 proposal MUST NOT mutate canonical network state.

Example:

``` text
canonical txPower = 40
proposal          = 40 -> 38
proposal status   = APPROVED
```

Canonical state remains:

``` text
40
```

until the established trusted observation/reconciliation path observes
and reconciles a real source-state change.

------------------------------------------------------------------------

## 27. Candidate Model

Represent candidate evaluations durably or as proposal evidence
sufficient to reproduce/explain:

``` text
candidate value
validation outcome
validation reason
simulation run reference
benefit result
risk result
score
ranking
```

Do not mandate a separate candidate table if a simpler normalized
persistence design satisfies these requirements.

------------------------------------------------------------------------

## 28. Candidate Generation Policy

Candidate generation must be deterministic.

Use:

-   authoritative current value;
-   `SimulatableParameterRegistry`;
-   configured step;
-   configured maximum delta and/or bounded candidate count;
-   supported parameter constraints.

Do not permit arbitrary user- or LLM-supplied candidate envelopes to
bypass registry constraints.

------------------------------------------------------------------------

## 29. Candidate Configuration

Introduce configuration only for SNIP-owned optimization policy, such
as:

``` text
candidate step
maximum candidate delta
maximum candidate count
proposal validity duration
```

Do not encode guessed Ericsson-specific operational limits.

Defaults must be deterministic and documented.

------------------------------------------------------------------------

## 30. Candidate Includes Current Value

The implementation may simulate the current value as a baseline
candidate if this is compatible with Phase 6.

If so, distinguish baseline/no-change from actual change candidates.

A no-change baseline must not become a network-change recommendation.

------------------------------------------------------------------------

## 31. Constraint Validator

Implement a deterministic validator/service that rejects candidates
when:

-   parameter unsupported;
-   target unsupported;
-   candidate outside `SimulatableParameterRegistry`;
-   candidate violates configured Phase 13 envelope;
-   required canonical current state missing;
-   knowledge gate fails;
-   required Twin state unavailable/incompatible;
-   candidate is otherwise invalid.

Return stable reason codes.

------------------------------------------------------------------------

## 32. Digital Twin Compatibility

Before candidate simulation/recommendation, validate compatibility
between:

``` text
proposal trusted source state
```

and:

``` text
Digital Twin source state
```

Use existing Phase 6 metadata if available.

Do not fabricate a snapshot identity if Phase 6 does not currently
persist one.

If exact compatibility cannot be proven with current metadata, implement
the strongest deterministic safe check available and document the
limitation in the completion report.

Do not silently treat unknown compatibility as perfect compatibility.

------------------------------------------------------------------------

## 33. Stale Twin Behavior

If Twin state is stale/incompatible beyond accepted policy:

-   do not silently recommend;
-   return/persist a stable blocked or invalid reason such as
    `TWIN_STATE_STALE`;
-   preserve diagnostic evidence.

Do not automatically rebuild the entire Twin unless an existing explicit
Phase 6 API is deliberately invoked by an authorized Phase 13 workflow
and doing so does not change Phase 6 semantics.

------------------------------------------------------------------------

## 34. Simulation Execution

For each valid change candidate:

1.  prepare the supported Phase 6 simulation request;
2.  invoke `DigitalTwinSimulationService`;
3.  capture the simulation result reference;
4.  capture simulation confidence;
5.  classify failure safely;
6.  continue or stop according to deterministic policy.

Do not persist fabricated successful simulations.

------------------------------------------------------------------------

## 35. Simulation Failure

A simulation failure must not become a successful candidate.

If no eligible candidate can be evaluated successfully:

``` text
SIMULATION_FAILED
```

or equivalent final failure outcome must result.

Do not promote such a proposal to `RECOMMENDED`.

------------------------------------------------------------------------

## 36. Benefit Assessment

Implement a deterministic benefit assessment using only metrics actually
supported by existing Phase 3/6 evidence.

Do not invent unavailable RF KPIs.

The assessment must produce:

-   stable benefit dimensions;
-   normalized/deterministic values where justified;
-   stable reason codes;
-   enough evidence for explanation.

------------------------------------------------------------------------

## 37. Risk Assessment

Implement deterministic risk assessment.

Use:

``` text
LOW
MEDIUM
HIGH
CRITICAL
```

or an existing compatible risk enum.

Risk may consider:

-   magnitude of parameter change;
-   network knowledge confidence;
-   simulation confidence;
-   negative predicted impact;
-   missing/degraded evidence;
-   target scope;
-   Twin compatibility.

Do not use an LLM as risk authority.

------------------------------------------------------------------------

## 38. MEDIUM Knowledge Degradation

A `MEDIUM` knowledge-confidence proposal may become `RECOMMENDED`, but
must carry explicit degradation evidence/reason code.

The score must not hide the degraded confidence.

------------------------------------------------------------------------

## 39. Simulation LOW Confidence

Because Phase 6 currently uses a synthetic model, a valid recommendation
may have:

``` text
NetworkKnowledgeConfidence = HIGH
SimulationConfidence = LOW
```

This must be exposed explicitly in evidence and explanation.

------------------------------------------------------------------------

## 40. Deterministic Scoring

Implement a deterministic scoring policy.

Before coding the formula, inspect existing Phase 3/6 scoring/value
semantics.

The formula must:

-   use only deterministic inputs;
-   be reproducible;
-   have explicit weights/constants in code/config;
-   penalize risk/uncertainty as designed;
-   never bypass the hard knowledge gate;
-   never let an LLM alter the authoritative result.

Document the exact implemented formula in the completion report.

------------------------------------------------------------------------

## 41. Deterministic Ranking

For the same authoritative inputs, candidate ordering must be
reproducible.

Define deterministic tie-breaking, for example:

1.  higher authoritative score;
2.  lower risk;
3.  smaller absolute parameter delta;
4.  stable numeric candidate ordering.

Use a stable rule and test it.

------------------------------------------------------------------------

## 42. Recommendation Selection

Only an eligible simulated candidate may become the selected proposed
value.

The selected candidate must have:

-   passed constraints;
-   passed knowledge gate;
-   valid simulation evidence;
-   benefit assessment;
-   risk assessment;
-   deterministic score;
-   deterministic rank.

------------------------------------------------------------------------

## 43. No-Improvement Outcome

If no candidate produces sufficient deterministic benefit relative to
baseline, do not manufacture a recommendation.

Return/persist an evaluated non-recommended outcome with a stable reason
such as:

``` text
NO_BENEFICIAL_CANDIDATE
```

if this fits the selected state model.

------------------------------------------------------------------------

## 44. Proposal Generation Initiators

Support strongly typed initiators equivalent to:

``` text
MANUAL
ASSURANCE_TRIGGERED
AGENT_REQUESTED
```

All initiators converge on the same authoritative proposal-generation
service.

No initiator gets a privileged bypass path.

If `ASSURANCE_TRIGGERED` or `AGENT_REQUESTED` cannot be safely wired
without broad unrelated changes, implement the shared model/service and
test the boundary, then document the deferred trigger wiring.

------------------------------------------------------------------------

## 45. Authoritative Generation Service

Implement one authoritative orchestration service responsible for the
proposal pipeline.

Conceptually:

``` text
request
  -> resolve evidence/current state
  -> evaluate network knowledge
  -> generate candidates
  -> validate constraints
  -> validate Twin compatibility
  -> simulate
  -> assess benefit
  -> assess risk
  -> score
  -> rank
  -> persist proposal/evidence
```

Controllers and Agents must not reproduce this pipeline.

------------------------------------------------------------------------

## 46. Proposal Validity Service

Implement a deterministic validity service that can evaluate whether an
existing proposal remains valid.

It must consider at least:

-   latest trusted canonical current value;
-   relevant source/snapshot progression;
-   Phase 12 knowledge confidence;
-   relevant drift;
-   expiration;
-   supersession state.

------------------------------------------------------------------------

## 47. Approval-Time Revalidation

Immediately before authoritative approval:

1.  reload proposal under concurrency-safe transaction semantics;
2.  verify it is in an approvable state;
3.  revalidate current trusted canonical value;
4.  revalidate knowledge-confidence gate;
5.  check expiration;
6.  check invalidation/supersession;
7.  persist approval only if still valid.

Approval must fail closed if validity cannot be established.

------------------------------------------------------------------------

## 48. Newer Current Value Invalidation

If proposal expected:

``` text
txPower = 40
```

but latest trusted canonical state is:

``` text
txPower = 39
```

the proposal must not remain approvable.

Transition it to `INVALIDATED` or deterministically return invalidation
and persist the transition according to the lifecycle design.

------------------------------------------------------------------------

## 49. Drift Re-Evaluation

Consume Phase 12 drift authority.

Do not implement a second drift detector.

When relevant drift exists for the same source/entity/parameter domain,
re-evaluate proposal validity.

Initial matching may be deliberately conservative.

------------------------------------------------------------------------

## 50. Confidence Degradation

If network knowledge falls to LOW or UNKNOWN after recommendation but
before approval:

-   approval must be blocked;
-   proposal must become invalidated/expired/otherwise non-approvable
    according to the selected deterministic lifecycle;
-   stable reason code must identify confidence degradation.

Do not leave an APPROVABLE recommendation based on failed knowledge
trust.

------------------------------------------------------------------------

## 51. Expiration

Implement proposal expiration using UTC `Instant`.

Expiration duration must be configuration-driven.

An expired proposal cannot be approved.

Expiration preserves historical evidence.

------------------------------------------------------------------------

## 52. Supersession

When a newer proposal deliberately replaces an older proposal for the
same target/parameter, preserve lineage.

Use either:

-   immutable proposal revisions; or
-   separate proposal rows linked through predecessor/supersession
    fields.

Prefer separate immutable proposal records unless repository conventions
strongly favor explicit version rows.

Document the selected strategy.

------------------------------------------------------------------------

## 53. Historical Integrity

Do not rewrite historical proposal inputs/evidence when later network
state changes.

Invalidation, expiration, rejection, or supersession changes lifecycle
metadata, not the historical evidence used to create the original
recommendation.

------------------------------------------------------------------------

## 54. Review Model

Persist governed review evidence sufficient to answer:

``` text
who/subject reference
decision
time
reason code
optional safe comment
proposal version/state reviewed
```

Follow existing identity/audit conventions.

Do not persist authentication headers or secrets.

------------------------------------------------------------------------

## 55. Proposal Governance Permissions

Use distinct proposal-governance permissions conceptually equivalent to:

``` text
VIEW_NETWORK_CHANGE_PROPOSALS
GENERATE_NETWORK_CHANGE_PROPOSAL
REVIEW_NETWORK_CHANGE_PROPOSAL
APPROVE_NETWORK_CHANGE_PROPOSAL
REJECT_NETWORK_CHANGE_PROPOSAL
```

Align actual naming with repository conventions.

Do not treat vendor-import authorization as proposal-approval authority.

------------------------------------------------------------------------

## 56. Authorization Implementation Style

The repository currently uses a header-based `VendorImportAuthorizer`
pattern and does not use Spring Security.

Do not introduce Spring Security solely for Phase 13.

Implement a narrow proposal-governance authorizer consistent with
current project style.

Keep import permissions and proposal permissions semantically distinct.

------------------------------------------------------------------------

## 57. Approval/Generation Separation

A principal allowed to generate a proposal is not automatically
authorized to approve it.

Tests must prove this.

Do not implement implicit self-approval.

------------------------------------------------------------------------

## 58. Agent Approval Prohibition

Regardless of Agent permissions elsewhere, Phase 13 Agents cannot
approve or reject authoritative proposals.

Architecture/isolation tests must protect this boundary.

------------------------------------------------------------------------

## 59. Approval Has No Execution Side Effect

The approval transaction may persist:

``` text
status = APPROVED
review evidence
audit event
```

It MUST NOT:

-   call Phase 4 action execution;
-   create a `ProposedAction` automatically;
-   call MCP;
-   call a vendor connector;
-   call `EnmTransport`;
-   resolve credentials;
-   alter canonical state;
-   create vendor commands.

------------------------------------------------------------------------

## 60. Rejection

Rejection must:

-   require authorization;
-   validate lifecycle;
-   preserve evidence;
-   persist safe reviewer reason;
-   produce audit evidence;
-   have no vendor/canonical side effect.

------------------------------------------------------------------------

## 61. Concurrency

Use repository-standard JPA transaction/locking mechanisms.

Prevent contradictory outcomes such as:

``` text
APPROVED and REJECTED
```

from concurrent reviewers.

Use optimistic locking (`@Version`) or an equivalent existing convention
where appropriate.

Do not introduce a new distributed lease subsystem for proposal review.

------------------------------------------------------------------------

## 62. Persistence Migration

Expected next migration:

``` text
V14
```

Create one forward-only Phase 13 migration if persistence changes
require it.

Do not edit V1--V13.

------------------------------------------------------------------------

## 63. Persistence Decomposition

Do not blindly create one table per architectural concept.

Design the smallest clear durable model that satisfies:

-   proposal lifecycle;
-   candidate/evaluation evidence;
-   simulation references;
-   risk/benefit;
-   provenance;
-   review;
-   invalidation/expiration/supersession;
-   concurrency/versioning.

Likely concepts include:

``` text
network_change_proposal
network_change_candidate
change_proposal_review
```

Risk/benefit/evidence may be normalized or embedded according to
existing database conventions.

Document the final schema in the completion report.

------------------------------------------------------------------------

## 64. Persistence Safety

Do not persist:

-   raw ENM payloads;
-   passwords;
-   tokens;
-   Key Vault values;
-   credential handles;
-   vendor sessions;
-   arbitrary vendor endpoints;
-   vendor command strings.

------------------------------------------------------------------------

## 65. API Surface

Implement safe `/api/v1/...` controllers consistent with existing flat
controller style.

Expected capabilities:

``` text
POST /api/v1/change-intelligence/proposals
GET  /api/v1/change-intelligence/proposals
GET  /api/v1/change-intelligence/proposals/{proposalId}
GET  /api/v1/change-intelligence/proposals/{proposalId}/evidence
POST /api/v1/change-intelligence/proposals/{proposalId}/approve
POST /api/v1/change-intelligence/proposals/{proposalId}/reject
```

Exact request/response DTOs may follow repository conventions.

------------------------------------------------------------------------

## 66. Generation API

The generation API must accept only safe SNIP-domain inputs.

Prefer identifiers for existing assurance/decision/target state rather
than caller-supplied authoritative network facts.

Do not trust caller-supplied current value or confidence when those can
be resolved from authoritative state.

------------------------------------------------------------------------

## 67. Forbidden API Inputs

Reject or do not model inputs for:

``` text
vendor endpoint
vendor username
vendor password
access token
Key Vault URI
credential handle
vendor command
HTTP method
arbitrary protocol operation
fencing token
lease owner
checkpoint/cursor override
authoritative risk override
authoritative confidence override
authoritative score override
```

------------------------------------------------------------------------

## 68. Read APIs

Read responses may expose:

-   proposal identity/type/status;
-   target;
-   current/proposed value;
-   confidence domains;
-   risk;
-   benefit summary;
-   score/rank;
-   safe evidence identifiers;
-   lifecycle timestamps;
-   invalidation/expiration reason;
-   review outcome.

Do not expose secret/raw vendor material.

------------------------------------------------------------------------

## 69. Error Taxonomy

Use stable Phase 13 reason/error codes.

At minimum cover:

``` text
UNSUPPORTED_PARAMETER
UNSUPPORTED_TARGET
NETWORK_KNOWLEDGE_LOW
NETWORK_KNOWLEDGE_UNKNOWN
CURRENT_STATE_UNAVAILABLE
CANDIDATE_OUT_OF_RANGE
NO_VALID_CANDIDATES
TWIN_STATE_UNAVAILABLE
TWIN_STATE_STALE
SIMULATION_FAILED
NO_BENEFICIAL_CANDIDATE
PROPOSAL_EXPIRED
PROPOSAL_INVALIDATED
PROPOSAL_SUPERSEDED
CURRENT_VALUE_CHANGED
KNOWLEDGE_CONFIDENCE_DEGRADED
INVALID_PROPOSAL_STATE
PROPOSAL_GENERATION_FORBIDDEN
PROPOSAL_REVIEW_FORBIDDEN
PROPOSAL_APPROVAL_FORBIDDEN
PROPOSAL_REJECTION_FORBIDDEN
CONCURRENT_REVIEW_CONFLICT
```

Align with existing exception/error response style.

------------------------------------------------------------------------

## 70. Explainability

Implement deterministic evidence assembly sufficient for a caller to
understand:

-   why the target was selected;
-   source snapshot/execution;
-   current value;
-   proposed value;
-   candidates considered;
-   constraints;
-   simulation references/results;
-   benefit;
-   risk;
-   score/rank;
-   all confidence domains;
-   invalidation conditions.

An LLM-generated narrative is optional and not required for Phase 13
acceptance.

------------------------------------------------------------------------

## 71. LLM Role

If an LLM/Agent is used for narrative explanation, it may:

-   summarize evidence;
-   explain trade-offs;
-   describe deterministic ranking.

It may not authoritatively determine:

-   canonical state;
-   network knowledge confidence;
-   simulation confidence;
-   candidate legality;
-   risk;
-   score;
-   ranking;
-   approval;
-   vendor execution.

Phase 13 must remain fully functional without LLM authority.

------------------------------------------------------------------------

## 72. Audit

Use existing audit infrastructure if available.

Emit safe events for:

``` text
proposal generation requested
proposal generation blocked
proposal generated/evaluated
recommendation produced
proposal invalidated
proposal expired
proposal superseded
approval attempted
proposal approved
proposal rejected
```

Do not log secrets/raw payloads.

------------------------------------------------------------------------

## 73. Metrics

Use existing metrics infrastructure.

Low-cardinality metrics may count:

``` text
generation attempts
generation blocked
evaluated proposals
recommended proposals
approvals
rejections
invalidations
expirations
simulation failures
risk categories
knowledge-confidence categories
evaluation duration
```

Do not label metrics with proposal IDs, entity IDs, raw error text,
endpoints, credentials, or source cursors.

------------------------------------------------------------------------

## 74. Vendor Isolation

Phase 13 code must not inject or directly invoke:

``` text
EnmTransport
Ericsson connector implementations
Nokia connector implementations
production credential providers
Azure Key Vault clients
```

Use architecture source-walk tests to prove this.

------------------------------------------------------------------------

## 75. No Vendor Write Capability

Do not add any capability equivalent to:

``` text
CONFIGURATION_WRITE
COMMAND_EXECUTE
NETWORK_MUTATE
REMEDIATION_WRITE
```

to Phase 13 or connector registries.

------------------------------------------------------------------------

## 76. No New Egress

Phase 13 should not require new external egress.

Do not add broad `0.0.0.0/0` network policy or any new vendor
destination.

------------------------------------------------------------------------

## 77. Production ENM State

Preserve:

``` text
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
```

Do not guess the Ericsson interface.

------------------------------------------------------------------------

## 78. Real Vendor Continuous Synchronization

Preserve:

``` text
REAL VENDOR CONTINUOUS SYNCHRONIZATION: NOT YET VERIFIED
```

Phase 13 acceptance uses synthetic/canonical evidence.

------------------------------------------------------------------------

## 79. Default CI

Default CI must remain:

``` text
Azure-independent
Key-Vault-independent
Ericsson-independent
Nokia-independent
real-vendor-independent
```

Do not introduce cloud credentials or vendor connectivity into ordinary
CI.

------------------------------------------------------------------------

## 80. Go Simulator

Phase 13 should not modify the Go simulator unless a narrowly necessary
synthetic scenario is required for a Phase 13 test and the change does
not alter prior-phase semantics.

Regardless of whether Go code changes, run:

``` text
go test ./...
go build ./cmd/simulator
```

from the simulator module/path used by the repository.

------------------------------------------------------------------------

## 81. Architecture Isolation Tests

Add/update a `Phase13...ArchitectureIsolationTest` using the
repository's source-walk pattern.

It must prove at least:

1.  no Phase 13 dependency on `EnmTransport`;
2.  no Phase 13 dependency on vendor connector implementation;
3.  no Phase 13 dependency on credential resolution/Key Vault;
4.  no duplicate Phase 13 `SimulatableParameterRegistry`;
5.  no duplicate Phase 13 Digital Twin simulation service;
6.  no duplicate Phase 13 knowledge-confidence evaluator;
7.  no duplicate Phase 13 drift engine;
8.  three confidence domains are not collapsed;
9.  `NetworkChangeProposal` is not Phase 4 `ProposedAction`;
10. no automatic proposal-to-`ProposedAction` conversion;
11. Agent path cannot approve/reject;
12. approval path cannot invoke MCP;
13. approval path cannot invoke Phase 4 execution;
14. approval path cannot invoke vendor connector/transport;
15. approval does not mutate canonical state;
16. no vendor write capability added;
17. no executable vendor command field persisted;
18. no Phase 14 implementation package/work introduced.

Use robust semantic/source checks consistent with existing tests; avoid
brittle checks where possible.

------------------------------------------------------------------------

## 82. Mandatory Behavioral Test Matrix

The following matrix is mandatory. Cursor may split it across unit,
repository, controller, integration, and architecture tests, but every
item must have automated PASS evidence or a documented,
architecture-approved reason why an equivalent stronger test proves it.

### A. Parameter and candidate generation

1.  `txPower` is accepted through `SimulatableParameterRegistry`.
2.  Unsupported parameter is rejected.
3.  Candidate below authoritative range is rejected.
4.  Candidate above authoritative range is rejected.
5.  Candidate generation is deterministic.
6.  Candidate generation respects configured step.
7.  Candidate generation respects configured maximum delta/count.
8.  No arbitrary LLM/caller candidate bypass.
9.  Baseline/no-change candidate cannot become change recommendation.
10. Stable deterministic candidate ordering.

### B. Knowledge confidence

11. HIGH knowledge confidence permits evaluation.
12. MEDIUM permits evaluation with degradation evidence.
13. LOW blocks `RECOMMENDED`.
14. UNKNOWN blocks `RECOMMENDED`.
15. High benefit cannot override LOW.
16. High score cannot override UNKNOWN.
17. Simulation confidence cannot replace network knowledge confidence.
18. Assurance confidence cannot replace network knowledge confidence.
19. All three confidence domains remain independently observable.

### C. Phase 3 evidence

20. Existing assurance evidence can be referenced.
21. Existing decision evidence can be referenced.
22. Proposal generation does not duplicate Phase 3 decision algorithm.
23. No mandatory `OptimizationOpportunity` persistence table is required
    merely by architecture naming.

### D. Snapshot/current-state binding

24. Proposal persists source identity.
25. Proposal persists synchronization/snapshot identity where available.
26. Proposal persists expected current value.
27. Historical current value remains unchanged after later sync.
28. Caller cannot override authoritative current value.
29. Caller cannot override authoritative confidence.

### E. Digital Twin

30. Valid candidate invokes `DigitalTwinSimulationService`.
31. No duplicate Phase 13 simulation engine.
32. Existing `SimulationConfidence.LOW` is preserved.
33. Simulation result reference is retained.
34. Simulation failure cannot produce recommendation.
35. Stale/incompatible Twin blocks/degrades according to policy.
36. Unknown Twin compatibility is not silently treated as perfect.
37. Phase 13 does not automatically rebuild entire Twin after every
    synchronization.

### F. Benefit/risk/score/ranking

38. Benefit assessment is deterministic.
39. Risk assessment is deterministic.
40. Risk uses stable reason codes.
41. Score is deterministic.
42. Score is repeatable for same inputs.
43. Hard knowledge gate executes before score can authorize
    recommendation.
44. Ranking is deterministic.
45. Tie-breaker is deterministic.
46. LLM cannot override risk.
47. LLM cannot override score.
48. LLM cannot override ranking.
49. No beneficial candidate produces no fabricated recommendation.

### G. Proposal lifecycle

50. Successful eligible evaluation reaches `RECOMMENDED`.
51. Invalid candidate/evidence reaches safe non-recommended state.
52. Simulation failure reaches safe failure state.
53. `RECOMMENDED` may be rejected by authorized reviewer.
54. `RECOMMENDED` may be approved by authorized reviewer.
55. Invalid lifecycle transition is rejected.
56. Forbidden execution-like statuses do not exist.
57. Expired proposal cannot be approved.
58. Invalidated proposal cannot be approved.
59. Superseded proposal cannot be approved.
60. Historical evidence survives rejection.
61. Historical evidence survives invalidation.
62. Historical evidence survives expiration.
63. Historical evidence survives supersession.

### H. Revalidation and drift

64. Unchanged trusted current value permits approval revalidation.
65. Changed current value invalidates proposal.
66. Relevant Phase 12 drift triggers validity re-evaluation.
67. Phase 13 does not create a duplicate drift detector.
68. Confidence degradation HIGH/MEDIUM -\> LOW blocks approval.
69. Confidence degradation -\> UNKNOWN blocks approval.
70. Newer compatible state does not silently rewrite historical proposal
    evidence.
71. Proposal expiration is UTC/deterministic.
72. Supersession preserves lineage.

### I. Authorization/governance

73. Viewer can read but cannot generate unless granted generation
    permission.
74. Generator can generate but cannot approve without approval
    permission.
75. Reviewer permission does not imply approval unless explicitly
    configured.
76. Unauthorized approval is rejected.
77. Unauthorized rejection is rejected.
78. Authorized approval succeeds only after revalidation.
79. Authorized rejection persists safe review evidence.
80. Concurrent approve/reject cannot create contradictory authoritative
    outcomes.
81. Agent cannot approve.
82. Agent cannot reject.
83. Agent request converges on authoritative Phase 13 service if wired.
84. Assurance-triggered request converges on authoritative service if
    wired.
85. Vendor-import authorization alone does not grant proposal approval.

### J. No execution/canonical mutation

86. Approval does not create Phase 4 `ProposedAction`.
87. Approval does not call `ActionPolicyEvaluator` for vendor execution.
88. Approval does not call MCP.
89. Approval does not call `EnmTransport`.
90. Approval does not call vendor connector.
91. Approval does not resolve credentials.
92. Approval does not generate executable vendor command.
93. Approval does not modify canonical txPower.
94. Rejection does not modify canonical state.
95. Recommendation does not modify canonical state.
96. Canonical state remains observation/reconciliation-owned.

### K. API safety

97. Generation API resolves authoritative current state rather than
    trusting caller override.
98. API has no vendor endpoint input.
99. API has no secret/token input.
100. API has no credential-handle input.
101. API has no arbitrary HTTP/protocol operation input.
102. API has no fencing-token/lease override input.
103. API has no authoritative risk override.
104. API has no authoritative score override.
105. API has no authoritative confidence override.
106. Read API exposes safe evidence.
107. Read API does not expose raw vendor payload.
108. Error responses use stable safe reason codes.

### L. Persistence/security/audit/metrics

109. V14 is forward-only.
110. V1--V13 remain unchanged.
111. No raw ENM payload persisted.
112. No credential/token persisted.
113. No arbitrary vendor endpoint persisted.
114. No executable vendor command persisted.
115. Review concurrency/versioning works.
116. Audit records safe proposal lifecycle events.
117. Audit does not leak secrets/raw payloads.
118. Metrics are low-cardinality.
119. Metrics do not use proposal/entity IDs as labels.
120. Metrics do not leak endpoints/secrets/raw errors.

### M. Architecture boundaries

121. `SimulatableParameterRegistry` remains authoritative.
122. `DigitalTwinSimulationService` remains authoritative.
123. Phase 12 confidence evaluator remains authoritative.
124. Phase 12 drift service remains authoritative.
125. Three confidence domains remain separate.
126. `NetworkChangeProposal` remains distinct from `ProposedAction`.
127. No automatic conversion to `ProposedAction`.
128. `AgentProposalAdapter` cannot authoritatively persist/approve
     proposal.
129. Phase 13 has no vendor connector/transport dependency.
130. No vendor write capability introduced.
131. Production ENM transport remains unconfigured/fail-closed.
132. Closed-loop optimization remains absent.
133. No Phase 14 implementation exists.

### N. Regression and environment independence

134. Existing Phase 1--12 Maven tests remain green.
135. All Phase 13 Maven tests pass.
136. Default Maven tests require no Azure credentials.
137. Default Maven tests require no Key Vault.
138. Default Maven tests require no Ericsson system.
139. Default Maven tests require no Nokia system.
140. `go test ./...` passes.
141. `go build ./cmd/simulator` passes.
142. `git diff --check` is clean except harmless repository line-ending
     warnings if already established.

**Mandatory matrix size: 142 items.**

------------------------------------------------------------------------

## 83. Acceptance Scenario 1 --- HIGH-Confidence Recommendation

Automate an end-to-end application-level test equivalent to:

``` text
trusted canonical cell
txPower = 40 dBm
NetworkKnowledgeConfidence = HIGH
        |
Phase 3 evidence/opportunity
        |
bounded txPower candidates
        |
DigitalTwinSimulationService
        |
deterministic benefit/risk/score/ranking
        |
selected candidate
        |
NetworkChangeProposal = RECOMMENDED
```

Assert:

-   current value remains 40;
-   canonical state remains 40;
-   simulation confidence remains the actual Phase 6 value;
-   evidence/provenance is persisted;
-   no vendor path is called.

------------------------------------------------------------------------

## 84. Acceptance Scenario 2 --- Human Approval Without Execution

Automate:

``` text
RECOMMENDED
   |
authorized approval
   |
APPROVED
```

Assert:

``` text
no ProposedAction
no MCP
no EnmTransport
no vendor connector
no credential resolution
no canonical mutation
```

------------------------------------------------------------------------

## 85. Acceptance Scenario 3 --- Stale Current Value

Automate:

``` text
proposal expects 40
latest trusted canonical state becomes 39
approval attempted
```

Expected:

``` text
approval rejected
proposal invalidated/non-approvable
historical proposal still records 40 -> proposed value
canonical remains 39
```

------------------------------------------------------------------------

## 86. Acceptance Scenario 4 --- Knowledge Confidence Degradation

Automate:

``` text
proposal RECOMMENDED at HIGH
Phase 12 knowledge becomes LOW
approval attempted
```

Expected:

``` text
approval blocked
stable degradation reason
no execution
```

Repeat for UNKNOWN.

------------------------------------------------------------------------

## 87. Acceptance Scenario 5 --- MEDIUM Confidence

Automate:

``` text
NetworkKnowledgeConfidence = MEDIUM
```

Expected:

-   evaluation allowed;
-   explicit degraded-confidence reason/evidence;
-   recommendation may occur if all other deterministic gates pass;
-   confidence is not hidden by score.

------------------------------------------------------------------------

## 88. Acceptance Scenario 6 --- Synthetic Simulation Limitation

Automate a successful recommendation with:

``` text
NetworkKnowledgeConfidence = HIGH
SimulationConfidence = LOW
```

Expected:

-   recommendation may exist if deterministic policy permits;
-   evidence explicitly reports LOW simulation confidence;
-   no production-certainty wording/state is produced.

------------------------------------------------------------------------

## 89. Acceptance Scenario 7 --- No Beneficial Candidate

Automate legal candidate evaluation where none beats baseline
sufficiently.

Expected:

-   no fabricated `RECOMMENDED` change;
-   stable no-beneficial-candidate result;
-   simulation/evaluation evidence retained.

------------------------------------------------------------------------

## 90. Acceptance Scenario 8 --- Concurrent Review

Create a recommended proposal and race approve/reject or two terminal
review decisions.

Expected:

-   exactly one authoritative review outcome;
-   second operation receives safe conflict/invalid-state response;
-   no contradictory durable state.

------------------------------------------------------------------------

## 91. Acceptance Scenario 9 --- Agent Request Boundary

If agent-trigger wiring is implemented:

``` text
Agent -> AgentProposalAdapter -> Phase 13 authoritative generation service
```

Assert Agent cannot:

-   directly save proposal;
-   set status;
-   approve;
-   bypass confidence;
-   bypass simulation.

If trigger wiring is deferred, architecture isolation tests must still
prove no direct authority path.

------------------------------------------------------------------------

## 92. Acceptance Scenario 10 --- Phase 4 Isolation

Given an `APPROVED` Phase 13 proposal, assert no Phase 4
`ProposedAction` is automatically created.

This is a mandatory architectural boundary test.

------------------------------------------------------------------------

## 93. Transaction Boundaries

Use explicit transactional boundaries for:

-   proposal creation/evidence persistence;
-   terminal review transition;
-   invalidation;
-   supersession;
-   expiration if persisted proactively.

Do not hold database transactions across unnecessarily long
external-like simulation work if current Phase 6 semantics make that
unsafe.

Prefer:

1.  establish durable analysis intent/state;
2.  perform simulation;
3.  persist result in a bounded transaction;
4.  revalidate before terminal governance transition.

Preserve consistency without inventing distributed transactions.

------------------------------------------------------------------------

## 94. Failure Atomicity

A failed candidate simulation must not partially persist a successful
candidate result.

A failed proposal-generation run must not leave a `RECOMMENDED` proposal
without complete required evidence.

If diagnostic failure records are persisted, they must be explicitly
distinguishable from successful recommendation state.

------------------------------------------------------------------------

## 95. Idempotency

Where practical, protect against accidental duplicate processing of the
same explicit generation request.

Do not over-engineer global idempotency if no existing request
identifier convention exists.

At minimum, repeated evaluation must not corrupt lifecycle/evidence.

Document the chosen behavior.

------------------------------------------------------------------------

## 96. Time and Clock Testability

Use injectable/testable clock semantics if existing project patterns
support them.

Expiration tests must not rely on arbitrary sleeps.

------------------------------------------------------------------------

## 97. Configuration Validation

Validate Phase 13 policy configuration at startup or first use.

Reject invalid combinations such as:

-   non-positive candidate step;
-   negative validity duration;
-   invalid maximum candidate count;
-   impossible delta policy.

Do not silently normalize dangerous configuration.

------------------------------------------------------------------------

## 98. Logging

Log safe identifiers and reason codes only.

Never log:

-   credentials;
-   tokens;
-   raw vendor payloads;
-   authorization header contents;
-   arbitrary endpoints.

Avoid logging complete request objects if they may later grow sensitive
fields.

------------------------------------------------------------------------

## 99. Documentation During Implementation

Create:

``` text
docs/implementation/SNIP-PHASE-13-NETWORK-CHANGE-INTELLIGENCE-OPTIMIZATION-PROPOSALS-GOVERNED-RECOMMENDATIONS-COMPLETION-REPORT.md
```

Do not create a second implementation specification; this file is the
specification.

Keep the accepted architecture copies unchanged unless a separately
authorized architecture correction is required.

------------------------------------------------------------------------

## 100. Completion Report Required Contents

The completion report must contain:

1.  parent baseline;
2.  architecture file;
3.  specification file;
4.  implementation summary;
5.  exact files added/modified grouped by concern;
6.  final domain model;
7.  V14 schema summary;
8.  proposal lifecycle implemented;
9.  exact candidate-generation policy;
10. exact risk policy;
11. exact benefit policy;
12. exact scoring formula;
13. exact ranking/tie-break rule;
14. confidence-domain handling;
15. Twin compatibility rule;
16. invalidation rule;
17. expiration policy;
18. supersession/versioning strategy;
19. authorization model;
20. concurrency strategy;
21. Phase 3 integration;
22. Phase 4 isolation;
23. Phase 5 Agent integration/isolation;
24. Phase 6 integration;
25. Phase 12 integration;
26. API endpoints;
27. audit/metrics;
28. architecture-isolation evidence;
29. mandatory test matrix mapping for all 142 items;
30. Maven test totals;
31. Maven failures/errors/skips;
32. Go test result;
33. Go build result;
34. `git diff --check` result;
35. secret/vendor-endpoint/write-capability searches;
36. known limitations/debt;
37. confirmation no real vendor write exists;
38. confirmation production ENM transport remains unconfigured;
39. confirmation real-vendor continuous sync remains unverified;
40. confirmation Phase 14 was not started.

------------------------------------------------------------------------

## 101. Test Traceability Format

The completion report must include a table with one row for every
mandatory test item:

``` text
Matrix ID | Requirement | Test class | Test method | Result
```

All 142 IDs must appear exactly once or be explicitly mapped to a
stronger test with justification.

Do not claim `142/142` without traceable automated evidence.

------------------------------------------------------------------------

## 102. Required Maven Verification

Run from the repository's Maven root:

``` text
mvn -B test
```

Report exact:

``` text
Tests run:
Failures:
Errors:
Skipped:
BUILD SUCCESS/FAILURE
```

Do not rely only on IDE test execution.

------------------------------------------------------------------------

## 103. Required Go Verification

Run using the repository's established simulator module/path:

``` text
go test ./...
go build ./cmd/simulator
```

Report both independently.

------------------------------------------------------------------------

## 104. Required Diff Verification

Run:

``` text
git diff --check
```

Report actual errors separately from harmless existing CRLF warnings.

Do not silently ignore whitespace errors.

------------------------------------------------------------------------

## 105. Required Security/Boundary Searches

Before completion, search the Phase 13 diff/repository as appropriate
for:

-   real Ericsson hosts/endpoints;
-   real Nokia hosts/endpoints;
-   passwords/tokens/secrets;
-   Key Vault secret values;
-   trust-all TLS;
-   `0.0.0.0/0`;
-   vendor mutation verbs/capabilities;
-   `EnmTransport` references from Phase 13;
-   vendor connector references from Phase 13;
-   automatic `ProposedAction` creation;
-   MCP execution from approval;
-   canonical state mutation from proposal lifecycle;
-   Phase 14 implementation.

Report results.

------------------------------------------------------------------------

## 106. Regression Protection

Do not weaken or delete prior-phase tests to make Phase 13 pass.

If an existing test conflicts with Phase 13, determine whether Phase 13
has exposed a genuine architecture conflict.

Do not simply modify the old test expectation without documenting why.

------------------------------------------------------------------------

## 107. No Test-Only Architecture Bypass

Production code must implement the real deterministic gates.

Do not create test-only shortcuts that bypass:

-   confidence;
-   candidate validation;
-   simulation;
-   authorization;
-   approval revalidation;
-   canonical-state protection.

Test hooks may observe deterministic boundaries but must not become
production bypasses.

------------------------------------------------------------------------

## 108. No Real Vendor E2E Requirement

Phase 13 implementation acceptance does not require real Ericsson or
Nokia access.

Do not request real vendor credentials.

Do not request production endpoints.

Do not use a developer workstation to obtain vendor credentials for
Phase 13.

------------------------------------------------------------------------

## 109. No Azure E2E Requirement

Phase 13 does not require Azure/Key Vault E2E.

Do not introduce an Azure-dependent acceptance gate.

Phase 10 remains the security authority for credential resolution where
vendor integration eventually uses it.

------------------------------------------------------------------------

## 110. Source of Canonical Truth

Phase 13 proposal state is never canonical network truth.

Canonical network state continues to be established by the existing
trusted observation/reconciliation path.

Even a future successful execution must eventually be confirmed through
observation/reconciliation before canonical state reflects the external
change.

Phase 13 must preserve this principle now.

------------------------------------------------------------------------

## 111. No Closed Loop

The following is forbidden:

``` text
observe
 -> detect
 -> propose
 -> approve automatically
 -> execute
 -> verify
 -> remediate
```

Phase 13 stops at governed proposal approval/rejection.

------------------------------------------------------------------------

## 112. No Phase 14 Work

Do not create:

-   Phase 14 packages;
-   Phase 14 docs;
-   execution-intent models;
-   vendor command translators;
-   change-window schedulers;
-   rollback engines;
-   canary execution;
-   four-eyes execution tokens.

These belong to future architecture decisions.

------------------------------------------------------------------------

## 113. Expected Implementation Sequence

Implement in this order unless repository dependencies require a minor
documented adjustment:

1.  inspect repository and record integration choices;
2.  create Phase 13 domain enums/value objects;
3.  design V14 persistence;
4.  implement repositories;
5.  implement proposal policy/config;
6.  implement candidate generator;
7.  implement constraint validator;
8.  integrate Phase 12 confidence;
9.  integrate Phase 6 simulation;
10. implement benefit assessment;
11. implement risk assessment;
12. implement deterministic scoring/ranking;
13. implement authoritative generation service;
14. implement validity/invalidation/expiration/supersession;
15. implement governance authorizer;
16. implement approval/rejection service with concurrency control;
17. implement safe API;
18. integrate Phase 3 evidence;
19. integrate optional Agent/assurance request paths only if safe;
20. implement audit/metrics;
21. implement architecture isolation tests;
22. implement mandatory behavioral matrix;
23. run full Maven regression;
24. run Go tests/build;
25. run diff/security/boundary checks;
26. produce completion report;
27. STOP.

------------------------------------------------------------------------

## 114. Implementation Decision Discipline

Where this specification intentionally allows implementation choice,
prefer:

1.  reuse existing SNIP abstractions;
2.  smallest design satisfying the architecture;
3.  deterministic behavior;
4.  explicit safety;
5.  testability;
6.  forward-only persistence;
7.  no speculative vendor functionality.

Document material choices in the completion report.

------------------------------------------------------------------------

## 115. Prohibited Scope Expansion

Do not use Phase 13 as an opportunity to:

-   refactor unrelated earlier phases;
-   migrate security frameworks;
-   redesign the entire domain model;
-   replace existing Digital Twin infrastructure;
-   replace Phase 12 synchronization;
-   add new vendor integrations;
-   add production transport;
-   add Kubernetes/Azure infrastructure;
-   add frontend features unrelated to the required safe API;
-   introduce autonomous execution.

Keep the diff Phase 13-focused.

------------------------------------------------------------------------

## 116. Architecture Acceptance Gate Traceability

The completion report must explicitly state that all **53 accepted Phase
13 architecture gates** were reviewed against the implementation.

For each gate, provide:

``` text
Gate | Implementation evidence | Test evidence | Result
```

This is separate from the 142-item behavioral matrix.

------------------------------------------------------------------------

## 117. Completion Status Before Review

Cursor MUST NOT mark the implementation architecturally accepted.

After successful implementation/testing, the completion report status
must be:

``` text
PHASE 13 ARCHITECTURE STATUS: ACCEPTED
PHASE 13 IMPLEMENTATION STATUS: COMPLETE — PENDING ARCHITECTURAL CONFORMANCE REVIEW
SIMULATOR/CONTRACT STATUS: VERIFIED
REAL VENDOR WRITE CAPABILITY: NOT AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT AUTHORIZED
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
REAL VENDOR CONTINUOUS SYNCHRONIZATION: NOT YET VERIFIED
PHASE 13 GIT BASELINE: NOT YET ESTABLISHED
PHASE 14 STATUS: NOT STARTED
```

If mandatory tests fail, use an accurate incomplete/failed status
instead.

------------------------------------------------------------------------

## 118. Git Prohibition

During this implementation task Cursor MUST NOT:

``` text
git add
git commit
git commit --amend
git push
git tag
git rebase
git reset --hard
```

Read-only Git inspection is allowed.

The Phase 13 Git baseline will be established only after architectural
conformance review and explicit authorization.

------------------------------------------------------------------------

## 119. Stop Condition

After:

-   implementation;
-   V14 migration if required;
-   tests;
-   Maven verification;
-   Go verification;
-   diff/security checks;
-   completion report;

Cursor must stop.

Do not begin remediation beyond ordinary implementation/test fixes
needed to satisfy this specification unless doing so would require an
architecture change.

If an architecture change is required, report it instead of implementing
it.

------------------------------------------------------------------------

## 120. Required Final Cursor Response

Cursor's final response must summarize:

``` text
Parent baseline
Architecture status
Implementation status
Files changed
V14 status
Phase 13 test count
Total Maven test count
Maven result
Go test result
Go build result
git diff --check result
53-gate architecture traceability result
142-item mandatory matrix result
Real vendor write capability
Closed-loop optimization
Production ENM transport
Real vendor continuous synchronization
Git baseline
Phase 14 status
Completion report path
```

The final line of the completion report and Cursor response must be
exactly:

``` text
PHASE 13 STATUS: IMPLEMENTED — PENDING ARCHITECTURAL ACCEPTANCE
```

------------------------------------------------------------------------

## 121. Final Implementation Invariants

The implementation is conformant only if all of these remain true:

``` text
Phase 12 immutable baseline remains unchanged.

Phase 13 architecture remains ACCEPTED.

txPower remains the only initial supported optimization parameter.

SimulatableParameterRegistry remains authoritative.

DigitalTwinSimulationService remains authoritative.

Phase 12 remains authoritative for network knowledge confidence and drift.

Assurance confidence, network knowledge confidence, and simulation confidence remain separate.

LOW/UNKNOWN network knowledge cannot become RECOMMENDED.

Candidate generation, validation, risk, scoring, and ranking are deterministic.

LLMs/Agents cannot override deterministic authority.

NetworkChangeProposal remains distinct from Phase 4 ProposedAction.

APPROVED remains recommendation/governance state only.

APPROVED does not mutate canonical network state.

Canonical state remains observation/reconciliation-owned.

No Phase 13 path reaches vendor mutation.

No real vendor write capability exists.

Closed-loop optimization remains unauthorized.

Production ENM transport remains unconfigured.

Default CI remains Azure/vendor independent.

Phase 14 remains not started.
```

------------------------------------------------------------------------

## 122. Formal Authorization

Phase 13 implementation is authorized **only within this specification
and the accepted architecture**.

No execution capability is authorized.

No real network mutation is authorized.

No Phase 14 work is authorized.

Proceed with Phase 13 implementation, testing, and completion reporting,
then stop.

**PHASE 13 IMPLEMENTATION SPECIFICATION: AUTHORIZED**
