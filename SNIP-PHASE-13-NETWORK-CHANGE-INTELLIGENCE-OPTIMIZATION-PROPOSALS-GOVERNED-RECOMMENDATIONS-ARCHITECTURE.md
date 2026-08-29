# SNIP Phase 13 --- Network Change Intelligence, Optimization Proposals & Governed Recommendations Architecture

**Repository:** `networkplanningoptimization`\
**Parent immutable baseline:**
`2f51bc1fd746633ec051a2ea933aa339c0ddc804`\
**Parent phase:** Phase 12 --- Continuous Synchronization, Drift &
Network Knowledge Confidence\
**Architecture status:** `ACCEPTED`\
**Implementation status:** `NOT STARTED`\
**Document date:** 2026-08-28 (architecturally accepted)

------------------------------------------------------------------------

## 1. Executive Summary

Phase 13 establishes the **Network Change Intelligence Plane** for SNIP.

It converts trusted network knowledge, assurance findings, decision
intelligence, and Digital Twin evidence into structured, explainable,
ranked, governed **Network Change Proposals**.

Phase 13 does **not** authorize SNIP to modify a real network.

Its defining principle is:

> **Phase 13 teaches SNIP how to formulate, evaluate, rank, explain, and
> govern proposed network changes. It still does not teach SNIP how to
> execute those changes on a real network.**

An `APPROVED` proposal is an approved recommendation only. It is not an
executable vendor command, execution token, change request, or authority
to mutate Ericsson ENM, Nokia NetAct, or any other external
network-management system.

------------------------------------------------------------------------

## 2. Parent Baseline and Architectural Authority

Phase 13 is designed against the immutable Phase 12 baseline:

`2f51bc1fd746633ec051a2ea933aa339c0ddc804`

Phase 12 established the synchronization and knowledge-confidence
foundation on which Phase 13 depends:

-   durable synchronization checkpoints;
-   FULL / INCREMENTAL / RECOVERY_FULL synchronization modes;
-   freshness;
-   external-source health;
-   synchronization and source-state drift;
-   deterministic network knowledge confidence;
-   read-only vendor integration boundaries;
-   Phase 8 lease/fencing authority;
-   Phase 9/10 connector security;
-   Phase 11 read-only Ericsson connector abstraction;
-   production ENM transport remaining fail-closed.

Phase 13 MUST preserve those invariants.

------------------------------------------------------------------------

## 3. Architectural Problem

SNIP can now determine whether its understanding of the network is
current, complete, trustworthy, divergent, or recovering.

The next architectural question is:

> Given sufficiently trustworthy network knowledge, what bounded network
> changes could improve an identified condition, what are their
> predicted consequences, what risks do they introduce, and which
> proposals are suitable for human consideration?

Phase 13 answers this question without crossing into real-network
execution.

------------------------------------------------------------------------

## 4. Architectural Objective

Establish a production-grade change-intelligence capability that can:

1.  identify or receive optimization opportunities;
2.  bind analysis to trusted network state;
3.  generate bounded legal candidates;
4.  validate candidates against deterministic constraints;
5.  evaluate candidates through the existing Digital Twin;
6.  assess predicted benefit and risk;
7.  rank candidates deterministically;
8.  generate explainable recommendations;
9.  persist strong evidence and provenance;
10. support governed human review;
11. invalidate stale proposals when network knowledge changes;
12. expose proposal state safely to downstream intelligence;
13. prohibit all real-vendor mutation.

------------------------------------------------------------------------

## 5. Defining Safety Boundary

The Phase 13 authority boundary is:

``` text
Trusted Network Knowledge
        |
        v
Network Change Intelligence
        |
        v
Network Change Proposal
        |
        v
Human Governance
        |
        v
APPROVED
        |
        X
NO REAL NETWORK EXECUTION
```

No Phase 13 component may translate approval into vendor execution.

------------------------------------------------------------------------

## 6. End-to-End Logical Flow

``` text
Phase 12 Trusted Network Knowledge
              |
              v
      Assurance / Decision
              |
              v
    Optimization Opportunity
              |
              v
 Network Change Intelligence
              |
              +-- Current-state validation
              +-- Knowledge-confidence gate
              +-- Candidate generation
              +-- Constraint validation
              +-- Digital Twin evaluation
              +-- Risk assessment
              +-- Benefit assessment
              +-- Candidate comparison
              +-- Deterministic ranking
              +-- Explainability
              |
              v
     Network Change Proposal
              |
              v
       Governance Review
          /       \
         v         v
     REJECTED    APPROVED
                    |
                    X
             NO VENDOR EXECUTION
```

------------------------------------------------------------------------

## 7. Relationship to Earlier Phases

Phase 13 composes existing capabilities rather than replacing them.

-   Phase 3 remains authoritative for Assurance and Decision
    Intelligence.
-   Phase 4 remains authoritative for governed Action/MCP concepts and
    does not gain real-vendor execution authority.
-   Phase 5 Agents may request or interpret analysis but do not become
    authoritative proposal approvers or vendor executors.
-   Phase 6 remains authoritative for Digital Twin simulation.
-   Phases 7--11 retain integration, reconciliation, security, and
    connector boundaries.
-   Phase 12 remains authoritative for synchronization, freshness,
    drift, source health, and network knowledge confidence.

------------------------------------------------------------------------

## 8. Core Domain Concept --- NetworkChangeProposal

`NetworkChangeProposal` becomes a first-class SNIP domain object.

Conceptual fields:

``` text
proposalId
proposalType
targetScope
targetEntityType
targetEntityId

parameter
currentValue
proposedValue
unit

sourceSystem
sourceSnapshotId
sourceSynchronizationExecutionId

knowledgeConfidence
knowledgeReasonCodes
knowledgeEvaluatedAt

assuranceFindingIds
decisionIds
simulationRunIds

expectedBenefit
predictedImpact
riskLevel
riskReasonCodes
proposalScore

status
generatedBy
generationMethod
createdAt
evaluatedAt
expiresAt
supersededBy
version
```

The implementation specification may refine names after repository
inspection, but the semantics are mandatory.

`NetworkChangeProposal` is a **distinct** Phase 13 domain object from
Phase 4 `ProposedAction`.

-   `NetworkChangeProposal` represents analyzed, evidence-bound change
    **recommendation intent** for human governance.
-   `ProposedAction` remains the Phase 4 governed-action workflow object
    for policy, approval, and MCP execution within the frozen Phase 4
    envelope.

Phase 13 MUST NOT automatically convert between `NetworkChangeProposal`
and `ProposedAction` in either direction. No approved proposal may spawn,
approve, or execute a Phase 4 action without a future deliberate
architecture.

------------------------------------------------------------------------

## 9. Proposal Is Intent, Not Command

A proposal represents a vendor-neutral desired parameter change for
analysis and governance.

It MUST NOT contain:

-   vendor credentials;
-   access tokens;
-   Key Vault secret values;
-   credential handles;
-   vendor sessions;
-   arbitrary vendor endpoints;
-   vendor command strings;
-   arbitrary protocol operations;
-   raw ENM payloads;
-   executable scripts.

------------------------------------------------------------------------

## 10. Initial Proposal Type

Phase 13 initially supports only parameters already bounded by Phase 6.

The frozen Phase 6 `SimulatableParameterRegistry` is the **authoritative
supported-parameter and legal-range source** for Phase 13 candidate
generation and constraint validation.

Phase 13 MUST NOT introduce a duplicate parameter registry, parallel
range table, or shadow whitelist. Initial scope uses the registry's
existing `txPower` entry (currently the only enabled simulatable
parameter).

The initial proposal type is conceptually:

`RADIO_TX_POWER_OPTIMIZATION`

No other radio parameter becomes writable, executable, or automatically
optimizable merely because Phase 13 exists.

------------------------------------------------------------------------

## 11. Initial Scope

The initial optimization unit is deliberately narrow:

-   one proposal;
-   one target radio entity/cell;
-   one supported parameter;
-   one current value;
-   one proposed value;
-   one bounded candidate set;
-   one evidence chain.

Network-wide and coupled multi-cell optimization are deferred.

------------------------------------------------------------------------

## 12. Proposal Lifecycle

The conceptual lifecycle is:

``` text
DRAFT
  |
  v
VALIDATING
  |------> INVALID
  v
SIMULATING
  |------> SIMULATION_FAILED
  v
EVALUATED
  |
  v
RECOMMENDED
  |------> REJECTED
  v
APPROVED
  |------> EXPIRED
  |------> SUPERSEDED
  `------> INVALIDATED
```

The exact persistence state machine must be deterministic.

------------------------------------------------------------------------

## 13. Explicitly Forbidden Lifecycle States

Phase 13 MUST NOT introduce proposal states implying network mutation,
including:

-   EXECUTING;
-   EXECUTED;
-   APPLIED;
-   DEPLOYED;
-   ROLLING_BACK;
-   ROLLED_BACK;
-   REMEDIATING.

A future deliberate architecture phase is required before any such
execution lifecycle exists.

------------------------------------------------------------------------

## 14. Optimization Opportunity

`OptimizationOpportunity` is a **logical analysis concept**, not a
mandatory new persistent entity.

Proposal generation starts from an optimization opportunity expressed
through existing Phase 3 assurance/decision evidence where semantically
sufficient — for example an assurance case, finding, or decision
assessment — rather than requiring a dedicated `optimization_opportunity`
table by default.

An opportunity should identify:

-   affected entity/scope;
-   observed problem;
-   supporting assurance/decision evidence;
-   relevant source system;
-   current trusted network snapshot;
-   parameter domain that may be evaluated.

Phase 13 should reuse existing Phase 3 entities where semantically
sufficient rather than creating duplicates.

------------------------------------------------------------------------

## 15. Candidate Generation

Candidate generation MUST be bounded and deterministic.

For the initial `txPower` case, candidates are generated from:

-   current value;
-   configured candidate step;
-   configured maximum delta;
-   **`SimulatableParameterRegistry` legal bounds** (authoritative);
-   proposal strategy.

Candidate envelopes MUST remain within the registry's supported
parameter/range definitions. Phase 13 MUST NOT maintain a second source
of truth for supported parameters or ranges.

The architecture does not define universal Ericsson limits.

Vendor-specific legal ranges MUST NOT be invented.

------------------------------------------------------------------------

## 16. LLM Candidate Restrictions

An LLM may explain or compare legal candidates.

An LLM MUST NOT authoritatively create arbitrary parameter values
outside the deterministic candidate envelope.

The authoritative path is:

``` text
Optimization Opportunity
        |
        v
Deterministic Candidate Generator
        |
        v
Constraint Validator
        |
        v
Digital Twin Evaluation
```

------------------------------------------------------------------------

## 17. Constraint Validation

Every candidate must pass deterministic validation before simulation.

Validation may include:

-   supported parameter per **`SimulatableParameterRegistry`**;
-   supported entity type;
-   candidate within registry/configured bounds;
-   candidate differs meaningfully from current value;
-   source knowledge sufficiently trusted;
-   required canonical attributes present;
-   Digital Twin state available;
-   no proposal-level invariant violation.

Invalid candidates are not simulated as valid recommendations.

------------------------------------------------------------------------

## 18. Network Knowledge Confidence Gate

Phase 12 network knowledge confidence becomes an authoritative Phase 13
gate.

Initial policy:

``` text
HIGH    -> recommendation generation permitted
MEDIUM  -> recommendation generation permitted with explicit degradation
LOW     -> RECOMMENDED state prohibited
UNKNOWN -> RECOMMENDED state prohibited
```

LOW/UNKNOWN may permit a diagnostic analysis result if useful, but not a
governed recommendation.

`LOW` and `UNKNOWN` `NetworkKnowledgeConfidence` are **hard
recommendation gates**. Deterministic proposal scoring, benefit/risk
assessment, simulation success, or LLM narrative MUST NOT compensate for
or bypass these gates to reach `RECOMMENDED`.

------------------------------------------------------------------------

## 19. Confidence Is Deterministic

SNIP maintains **three distinct confidence domains**. Phase 13 MUST
preserve all three and MUST NOT collapse, merge, alias, or substitute
one for another:

``` text
Phase 3 assurance.Confidence          — finding/decision evidence quality
Phase 12 NetworkKnowledgeConfidence     — trusted network-state knowledge
Phase 6 SimulationConfidence          — hypothetical change prediction trust
```

Network knowledge confidence is **read** from Phase 12 authoritative
state via existing evaluators/services. Phase 13 MUST NOT duplicate
Phase 12 knowledge-confidence or drift algorithms.

Phase 13 MUST NOT:

-   recalculate network knowledge confidence with an LLM;
-   override network knowledge confidence;
-   manually upgrade network knowledge confidence;
-   substitute simulation confidence or assurance confidence for network
    knowledge confidence;
-   introduce a unified "overall confidence" score that hides domain
    separation.

------------------------------------------------------------------------

## 20. Snapshot Binding

Every proposal must be bound to the network knowledge used to generate
it.

At minimum:

``` text
sourceSystem
sourceSnapshotId
sourceSynchronizationExecutionId
knowledgeEvaluatedAt
```

A proposal must always be able to answer:

> What trusted network state did SNIP believe existed when this
> recommendation was generated?

------------------------------------------------------------------------

## 21. Current-Value Binding

For parameter-change proposals, the proposal records both:

``` text
currentValue
proposedValue
```

`currentValue` is evidence, not merely presentation data.

Approval validity depends on the latest trusted canonical value still
matching the proposal's expected current state.

------------------------------------------------------------------------

## 22. Optimistic Proposal Validity

Before recommendation promotion, approval, or any future consumption by
an execution phase, SNIP must validate that the proposal is still based
on compatible trusted state.

At minimum:

``` text
proposal.expectedCurrentValue == latestTrustedCanonicalValue
```

and the source snapshot must still satisfy the configured validity
policy.

------------------------------------------------------------------------

## 23. Proposal Invalidation

A proposal becomes `INVALIDATED` when a newer trusted observation makes
its assumptions materially false.

Example:

``` text
Proposal generated:
CELL-A txPower 40 -> 38

Later trusted synchronization:
CELL-A txPower = 39

Result:
previous proposal INVALIDATED
```

The old proposal is preserved for audit.

------------------------------------------------------------------------

## 24. Proposal Supersession

`SUPERSEDED` is distinct from `INVALIDATED`.

A proposal may be superseded when a newer proposal for the same
target/parameter replaces it based on newer evidence, even if the older
proposal was not intrinsically invalid at creation time.

The newer proposal should retain a lineage relationship where practical.

------------------------------------------------------------------------

## 25. Drift-Aware Validity

Phase 12 drift observations may trigger proposal validity re-evaluation.

Phase 13 MUST consume Phase 12 drift and knowledge-confidence authority
through existing query/evaluator services. Phase 13 MUST NOT reimplement
drift detection or knowledge-confidence scoring logic.

Initial scope should be conservative:

-   same source system;
-   same target entity;
-   same parameter/domain;
-   newer trusted observation.

Phase 13 does not require a generic dependency graph.

------------------------------------------------------------------------

## 26. Proposal Expiration

Every recommendation has a bounded validity period.

Conceptual fields:

``` text
createdAt
evaluatedAt
expiresAt
```

Expiration may result from:

-   time;
-   newer trusted snapshot;
-   relevant drift;
-   confidence degradation;
-   Digital Twin incompatibility;
-   superseding proposal.

------------------------------------------------------------------------

## 27. Digital Twin as Evaluation Authority

Candidates intended to become `RECOMMENDED` must be evaluated through the
existing Phase 6 **`DigitalTwinSimulationService`** and frozen Twin
simulation model unless the architecture explicitly defines a future
non-simulation proposal type.

`DigitalTwinSimulationService` is the **existing simulation authority**.
Phase 13 does not create a second simulation engine, parallel simulation
service, or alternate Twin evaluator.

------------------------------------------------------------------------

## 28. Digital Twin Snapshot Compatibility

Proposal evaluation must know whether the Twin state is compatible with
the trusted network state used by the proposal.

Conceptually:

``` text
proposal source snapshot
        vs
twin source snapshot
```

If incompatible beyond permitted policy:

`TWIN_STATE_STALE`

The proposal cannot silently proceed as fully evaluated.

------------------------------------------------------------------------

## 29. No Automatic Twin Rebuild

Phase 13 does not automatically rebuild or synchronize the complete
Digital Twin after every Phase 12 synchronization.

Existing Phase 6 synchronization semantics remain unless separately
changed by a future architecture.

------------------------------------------------------------------------

## 30. Simulation Confidence

Simulation confidence is distinct from network knowledge confidence and
from Phase 3 assurance confidence.

All three domains remain visible in proposal evidence and explainability.
Phase 13 MUST NOT collapse them into a single authoritative score or
allow an LLM to synthesize a replacement "overall confidence" judgment.

It answers:

> How much should SNIP trust the predicted consequence of this
> hypothetical change?

It does not answer whether the current real network state is
trustworthy.

------------------------------------------------------------------------

## 31. Initial Simulation Limitation

The existing Phase 6 simulation model remains synthetic and
non-vendor-calibrated.

Therefore Phase 13 MUST NOT present simulation output as production RF
certainty.

A valid proposal may have:

``` text
Network Knowledge Confidence: HIGH
Simulation Confidence: LOW
```

This is expected and must be visible.

------------------------------------------------------------------------

## 32. Simulation Evidence

Proposal evidence should retain references to all simulation runs
materially used in evaluation.

At minimum:

-   simulationRunId;
-   candidate identity/value;
-   simulation model/version if available;
-   input snapshot identity;
-   output summary;
-   simulation confidence;
-   completion status.

Full raw payload duplication is not required.

------------------------------------------------------------------------

## 33. Candidate Comparison

Multiple legal candidates may be evaluated for the same opportunity.

Candidate comparison must be reproducible and based on persisted or
reproducible evidence.

The system must retain enough evidence to explain why one candidate
ranked above another.

------------------------------------------------------------------------

## 34. Benefit Assessment

Introduce a deterministic `OptimizationBenefitAssessment` or equivalent
concept.

Potential dimensions include:

-   coverage benefit;
-   interference benefit;
-   capacity benefit;
-   quality benefit;
-   energy benefit;
-   stability benefit.

Phase 13 implementation should initially expose only dimensions actually
supported by Phase 6/Phase 3 evidence.

No fabricated KPI precision is allowed.

------------------------------------------------------------------------

## 35. Risk Assessment

Introduce a deterministic `ChangeRiskAssessment` or equivalent concept.

Conceptual risk levels:

``` text
LOW
MEDIUM
HIGH
CRITICAL
```

Potential inputs include:

-   parameter delta;
-   target scope;
-   knowledge confidence;
-   simulation confidence;
-   predicted negative impact;
-   affected entities;
-   incomplete evidence;
-   stale/aging knowledge;
-   Twin compatibility.

------------------------------------------------------------------------

## 36. Risk Authority

Risk level and reason codes are deterministic/domain-controlled.

LLMs may explain the risk assessment but MUST NOT authoritatively assign
or override risk.

------------------------------------------------------------------------

## 37. Proposal Scoring

Candidate ranking must be deterministic, explainable, and reproducible
for the same authoritative inputs.

Conceptually:

``` text
ProposalScore =
    benefit contribution
  - risk penalty
  - simulation uncertainty penalty
  - knowledge uncertainty penalty
```

The exact formula belongs in the implementation specification after
inspection of existing Phase 3 and Phase 6 scoring.

------------------------------------------------------------------------

## 38. LLM Ranking Restriction

An LLM MUST NOT secretly reorder authoritative proposal ranking.

If AI-generated narrative compares candidates, it must describe the
deterministic ranking rather than replace it.

------------------------------------------------------------------------

## 39. Explainability Contract

Every `RECOMMENDED` proposal must be able to answer:

-   Why was this target selected?
-   What condition was observed?
-   Which trusted snapshot was used?
-   What is the current value?
-   What change is proposed?
-   Which candidates were evaluated?
-   What constraints were applied?
-   What simulations were performed?
-   Why did this candidate rank highest?
-   What benefit is predicted?
-   What risks exist?
-   What is network knowledge confidence?
-   What is simulation confidence?
-   What evidence supports the recommendation?
-   What conditions would invalidate it?

------------------------------------------------------------------------

## 40. Explainability Safety

Explainability must not expose:

-   secrets;
-   access tokens;
-   credential identifiers that reveal sensitive material;
-   raw vendor authentication failures;
-   raw ENM payloads;
-   arbitrary endpoints;
-   internal security material.

------------------------------------------------------------------------

## 41. Proposal Provenance

Proposal provenance should link the evidence chain:

``` text
Source observation
      |
Synchronization execution
      |
Canonical state / snapshot
      |
Assurance finding
      |
Decision / opportunity
      |
Candidate
      |
Simulation
      |
Risk + benefit
      |
Recommendation
      |
Human review
```

------------------------------------------------------------------------

## 42. Provenance Requirements

Where applicable, retain identifiers for:

-   source system;
-   synchronization execution;
-   source snapshot;
-   knowledge status;
-   assurance finding;
-   decision;
-   optimization opportunity;
-   simulation run(s);
-   candidate evaluation;
-   risk assessment;
-   benefit assessment;
-   generation method/version;
-   reviewer decision.

------------------------------------------------------------------------

## 43. Proposal Generation Initiators

Conceptual generation initiators:

``` text
MANUAL
ASSURANCE_TRIGGERED
AGENT_REQUESTED
```

All initiators converge on the same authoritative change-intelligence
service.

No initiator bypasses confidence, constraints, simulation, risk, or
governance.

------------------------------------------------------------------------

## 44. Manual Generation

An authorized human may request proposal generation for a supported
opportunity or target.

The API cannot provide arbitrary vendor execution material.

Manual generation does not imply approval.

------------------------------------------------------------------------

## 45. Assurance-Triggered Generation

Phase 13 may support proposal generation from an eligible assurance
finding.

This means the finding initiates analysis.

It does not authorize automatic approval or execution.

If implementation scope requires, automatic triggering may be deferred
while retaining the architecture contract.

------------------------------------------------------------------------

## 46. Agent-Requested Generation

Phase 5 Agents may **initiate or request** proposal analysis through an
authorized application service.

`AgentProposalAdapter` and related Agent orchestration paths may request
or trigger change-intelligence **analysis** only. They MUST NOT
authoritatively create a `NetworkChangeProposal` record, promote a
proposal to `RECOMMENDED`, or approve/reject Phase 13 proposals. Those
remain governed application-service responsibilities with explicit
permissions and deterministic evaluation.

Agents may not:

-   call ENM transport;
-   obtain connector credentials;
-   acquire integration leases;
-   mutate synchronization checkpoints;
-   mutate canonical source state;
-   bypass candidate constraints;
-   override risk/confidence;
-   approve their own proposals;
-   execute vendor commands.

------------------------------------------------------------------------

## 47. Separation of Generation and Approval

Proposal generation and proposal approval are separate authorities.

A component or principal capable of generating a proposal does not
automatically possess approval authority.

This separation is mandatory even if the same human may hold both
permissions in a development environment.

------------------------------------------------------------------------

## 48. Human Governance

`RECOMMENDED` proposals require governed review before becoming
`APPROVED`.

Conceptual permissions:

``` text
VIEW_NETWORK_CHANGE_PROPOSALS
GENERATE_NETWORK_CHANGE_PROPOSAL
REVIEW_NETWORK_CHANGE_PROPOSAL
APPROVE_NETWORK_CHANGE_PROPOSAL
REJECT_NETWORK_CHANGE_PROPOSAL
```

Authorization MUST remain consistent with existing application patterns
(for example header/subject permission checks similar to
`VendorImportAuthorizer`), but Phase 13 proposal governance MUST use
**distinct proposal permissions** such as those listed above.

Vendor-import or synchronization permissions — including
`TRIGGER_VENDOR_IMPORT`, `TRIGGER_RECOVERY_SYNCHRONIZATION`, and
`VIEW_SYNCHRONIZATION_STATUS` — MUST NOT be treated as proposal
generation, review, approval, or rejection authority.

The implementation specification may align names with existing
authorization conventions.

------------------------------------------------------------------------

## 49. Approval Semantics

`APPROVED` means:

> An authorized reviewer accepted the recommendation as a valid proposed
> change based on the evidence available at review time.

It does NOT mean:

-   execute now;
-   generate a vendor command;
-   open a vendor session;
-   authorize Phase 4 execution;
-   authorize MCP execution;
-   modify canonical network state;
-   modify the canonical representation as if the real network changed.

`APPROVED` proposals MUST NOT write to, update, or invalidate canonical
Site/gNB/Cell/radio-configuration state. Recommendation and approval
state are analytical/governance state only.

Canonical network state changes **only** through the established trusted
observation and reconciliation path (Phase 7--12 vendor import/sync,
deterministic reconciliation, and related durable execution metadata) —
never from proposal generation, recommendation, review, approval, or
rejection.

------------------------------------------------------------------------

## 50. Rejection Semantics

A reviewer may reject a recommendation.

Rejection should preserve:

-   proposal evidence;
-   reviewer identity/subject reference according to existing audit
    conventions;
-   timestamp;
-   safe reason code;
-   optional safe comment.

Rejection must not mutate the real network.

------------------------------------------------------------------------

## 51. Relationship to Phase 4 Governed Actions

Phase 13 does not replace Phase 4.

`NetworkChangeProposal` and Phase 4 `ProposedAction` serve different
authorities and lifecycles. Phase 13 MUST NOT automatically map, mirror,
convert, or synchronize state between them.

There is no Phase 13 path:

``` text
APPROVED PROPOSAL -> PHASE 4 ACTION -> REAL NETWORK
NETWORK CHANGE PROPOSAL <-> PROPOSED ACTION   (automatic conversion)
```

A future architecture may define a deliberate conversion from approved
proposal to execution intent, but that boundary does not exist in Phase
13.

------------------------------------------------------------------------

## 52. MCP Boundary

MCP remains prohibited from using Phase 13 approval as direct vendor
authority.

There is no legal path:

``` text
APPROVED PROPOSAL -> MCP -> VENDOR COMMAND
```

Architecture tests must protect this boundary.

------------------------------------------------------------------------

## 53. Connector Boundary

Phase 13 components must not depend directly on:

-   `EricssonEnmConnector`;
-   `EnmTransport`;
-   vendor credential resolution;
-   Key Vault clients;
-   vendor protocol clients.

Phase 13 consumes canonical/trusted network state and existing
analytical services.

------------------------------------------------------------------------

## 54. Real Vendor Write Capability

Real vendor write capability remains:

`NOT AUTHORIZED`

No connector capability such as configuration write, command execution,
remediation, or mutation may be introduced by Phase 13.

------------------------------------------------------------------------

## 55. Production ENM Transport

Production ENM transport remains:

`NOT CONFIGURED`

Phase 13 does not resolve or guess the actual Ericsson production
interface.

No REST/Bulk CM/CLI/NETCONF/event-stream interface is assumed.

------------------------------------------------------------------------

## 56. Real Vendor Continuous Synchronization

Real vendor continuous synchronization remains:

`NOT YET VERIFIED`

Phase 13 must remain useful and testable without that verification.

------------------------------------------------------------------------

## 57. Persistence Model

Expected durable concepts include approximately:

``` text
network_change_proposal
network_change_candidate
change_risk_assessment
change_proposal_evidence
change_proposal_review
```

Repository inspection may justify combining some concepts or reusing
Phase 3/4/6 tables.

The expected next forward-only Flyway migration is **`V14`**, following
immutable **`V13__phase12_synchronization.sql`**. Exact table
decomposition, column naming, and reuse of existing Phase 3/4/6 tables
belong in the implementation specification after architecture
acceptance.

Historical migrations are immutable.

------------------------------------------------------------------------

## 58. Persistence Design Principles

Persistence must support:

-   immutable proposal identity;
-   proposal versioning;
-   lifecycle transitions;
-   snapshot binding;
-   candidate evidence;
-   simulation references;
-   deterministic scores;
-   risk/benefit evidence;
-   review decisions;
-   expiration/invalidation/supersession;
-   safe auditability.

No raw vendor payload is required.

------------------------------------------------------------------------

## 59. Proposal Versioning

Material re-evaluation based on new authoritative evidence should not
silently rewrite the historical reasoning of an already-reviewed
proposal.

Use either:

-   explicit proposal versions; or
-   a new proposal linked by `supersededBy` / predecessor relationship.

The implementation specification must select one consistent strategy.

------------------------------------------------------------------------

## 60. Concurrency and Lifecycle Integrity

Proposal lifecycle transitions must be concurrency-safe.

Two concurrent reviewers must not produce contradictory authoritative
terminal review outcomes.

The implementation should use existing transaction/optimistic-locking
conventions rather than introducing a new distributed locking subsystem.

------------------------------------------------------------------------

## 61. Time Semantics

Use UTC `Instant` semantics for:

-   generation;
-   evaluation;
-   review;
-   expiration;
-   invalidation;
-   supersession.

Display localization belongs at presentation boundaries.

------------------------------------------------------------------------

## 62. Safe API Surface

Conceptual APIs may include:

``` text
POST /api/v1/change-intelligence/proposals
GET  /api/v1/change-intelligence/proposals
GET  /api/v1/change-intelligence/proposals/{proposalId}
GET  /api/v1/change-intelligence/proposals/{proposalId}/evidence
POST /api/v1/change-intelligence/proposals/{proposalId}/approve
POST /api/v1/change-intelligence/proposals/{proposalId}/reject
```

Exact routes should follow repository conventions.

------------------------------------------------------------------------

## 63. API Input Restrictions

API callers MUST NOT supply:

-   vendor endpoint;
-   vendor username/password;
-   access token;
-   Key Vault URI;
-   credential handle;
-   vendor command;
-   arbitrary HTTP method;
-   arbitrary protocol operation;
-   fencing token;
-   integration lease ownership;
-   authoritative risk override;
-   authoritative confidence override;
-   arbitrary proposal score.

------------------------------------------------------------------------

## 64. Read APIs

Read APIs may expose safe proposal metadata, evidence summaries, scores,
reason codes, confidence, risk, lifecycle state, and review outcome.

They must not expose secrets or complete raw vendor payloads.

------------------------------------------------------------------------

## 65. Audit Requirements

Safe audit events should cover:

-   proposal generation requested;
-   generation blocked;
-   candidate generated;
-   candidate rejected by constraint;
-   simulation requested/completed/failed;
-   evaluation completed;
-   recommendation created;
-   proposal invalidated;
-   proposal expired;
-   proposal superseded;
-   review requested;
-   approved;
-   rejected.

Audit must contain identifiers and safe reason codes, not secrets/raw
payloads.

------------------------------------------------------------------------

## 66. Metrics

Low-cardinality metrics may include:

-   proposals generated;
-   generation blocked;
-   recommendations produced;
-   approvals;
-   rejections;
-   invalidations;
-   expirations;
-   simulation failures;
-   proposals by risk level;
-   proposals by confidence level;
-   evaluation duration.

Do not use entity IDs, proposal IDs, raw error text, credentials,
endpoints, or checkpoint values as metric labels.

------------------------------------------------------------------------

## 67. Security Model

Phase 13 inherits existing authentication and authorization
architecture.

Security principles:

-   least privilege;
-   explicit permissions;
-   no credential propagation into proposal domain;
-   no vendor session from proposal services;
-   no execution authority;
-   no secret-bearing API;
-   no trust-all TLS introduced;
-   no broad vendor egress introduced.

------------------------------------------------------------------------

## 68. No New Egress Requirement

Phase 13 itself should require no new outbound access to Ericsson,
Nokia, Azure Key Vault, or other production vendor systems.

Its runtime dependencies should be internal SNIP services/state and the
Digital Twin.

------------------------------------------------------------------------

## 69. Default CI Independence

Default CI remains:

-   Azure-independent;
-   Key-Vault-independent;
-   Ericsson-independent;
-   Nokia-independent;
-   real-vendor-independent.

Synthetic canonical state and the existing simulator/Digital Twin are
sufficient for Phase 13 contract verification.

------------------------------------------------------------------------

## 70. Architecture Isolation Tests

Phase 13 must include architecture/boundary tests proving:

-   change-intelligence packages do not inject `EnmTransport`;
-   proposal services do not inject vendor connectors;
-   Agents cannot approve proposals;
-   **`AgentProposalAdapter` / Agent orchestration cannot authoritatively
    create or approve Phase 13 proposals**;
-   MCP cannot execute approved proposals;
-   Phase 4 cannot convert approval into vendor mutation;
-   **no automatic conversion between `NetworkChangeProposal` and
    `ProposedAction`**;
-   **no duplicate `SimulatableParameterRegistry` or parallel supported-
    parameter source in Phase 13 packages**;
-   **Phase 13 does not duplicate Phase 12 knowledge-confidence or drift
    evaluator logic**;
-   **the three confidence domains are not collapsed in proposal
    persistence or API models**;
-   **`LOW` / `UNKNOWN` network knowledge confidence cannot reach
    `RECOMMENDED` via scoring or LLM compensation paths**;
-   **approval/recommendation services do not mutate canonical network
    state**;
-   no vendor write capability is advertised;
-   no executable vendor command is stored in proposal persistence.

Tests SHOULD follow the existing source-walk / reflection isolation
pattern used by `SynchronizationArchitectureIsolationTest` and
`EricssonEnmArchitectureIsolationTest`.

------------------------------------------------------------------------

## 71. Minimum Behavioral Test Obligations

The implementation specification must define a detailed traceability
matrix covering at least:

1.  HIGH-confidence proposal generation;
2.  MEDIUM-confidence degraded recommendation;
3.  LOW-confidence recommendation blocking;
4.  UNKNOWN-confidence recommendation blocking;
5.  deterministic bounded candidates;
6.  out-of-envelope candidate rejection;
7.  unsupported parameter rejection;
8.  current-value binding;
9.  snapshot binding;
10. Twin compatibility;
11. stale Twin handling;
12. candidate simulation;
13. simulation failure;
14. deterministic benefit;
15. deterministic risk;
16. deterministic score;
17. deterministic ranking;
18. repeatability;
19. evidence persistence;
20. proposal provenance;
21. explanation evidence;
22. no raw payload;
23. no secret persistence;
24. manual generation authorization;
25. assurance-triggered convergence if implemented;
26. agent-requested convergence if implemented;
27. generation/approval authority separation;
28. approval authorization;
29. rejection authorization;
30. concurrent review safety;
31. approval does not execute vendor action;
32. MCP isolation;
33. Phase 4 isolation;
34. connector/transport isolation;
35. newer canonical value invalidates proposal;
36. relevant drift causes validity re-evaluation;
37. confidence degradation invalidates/blocks as policy requires;
38. proposal expiration;
39. proposal supersession;
40. historical evidence retained after invalidation;
41. no hard mutation of source canonical state;
42. no vendor write capability;
43. production ENM remains fail-closed;
44. default CI remains Azure-independent;
45. default CI remains vendor-independent;
46. Phase 1--12 regression suite remains green.

The formal implementation specification may expand this matrix
substantially.

------------------------------------------------------------------------

## 72. Acceptance Scenario A --- Recommendation

``` text
Phase 12 trusted synchronization
        |
        v
CELL-A txPower = 40 dBm
Knowledge Confidence = HIGH
        |
        v
Assurance identifies opportunity
        |
        v
Bounded txPower candidates generated
        |
        v
Candidates validated
        |
        v
Digital Twin evaluates candidates
        |
        v
38 dBm ranks highest
        |
        v
Risk + benefit calculated
        |
        v
NetworkChangeProposal
40 dBm -> 38 dBm
        |
        v
RECOMMENDED
```

No network mutation occurs.

------------------------------------------------------------------------

## 73. Acceptance Scenario B --- Human Approval

``` text
RECOMMENDED
    |
authorized human review
    |
    v
APPROVED
    |
    X
NO ENM / NETACT / MCP / PHASE 4 EXECUTION
```

The approved proposal remains analytical/governance state only.

------------------------------------------------------------------------

## 74. Acceptance Scenario C --- Stale Proposal

``` text
Proposal:
CELL-A 40 -> 38

New trusted synchronization:
CELL-A current txPower = 39

Validity re-evaluation
        |
        v
INVALIDATED
```

The proposal is retained historically but is no longer valid for
approval/future consumption.

------------------------------------------------------------------------

## 75. Acceptance Scenario D --- Confidence Degradation

``` text
Source becomes stale
        |
        v
Knowledge Confidence = LOW
        |
        v
New RECOMMENDED proposal blocked
```

An LLM cannot override this gate.

------------------------------------------------------------------------

## 76. Acceptance Scenario E --- Simulation Uncertainty

``` text
Network Knowledge Confidence = HIGH
Simulation Confidence = LOW
        |
        v
Proposal may be analytically evaluated
        |
        v
Recommendation explicitly exposes LOW simulation confidence
```

The system must not claim production RF certainty.

------------------------------------------------------------------------

## 77. Bulk and Multi-Cell Optimization

Network-wide or coupled multi-cell optimization is not part of initial
Phase 13.

Deferred concerns include:

-   combinatorial candidate search;
-   inter-cell dependency;
-   grouped changes;
-   ordering;
-   partial application;
-   blast-radius management;
-   rollback groups;
-   constraint solving across many entities.

------------------------------------------------------------------------

## 78. Closed-Loop Optimization

Closed-loop optimization is:

`NOT AUTHORIZED`

No automatic sequence may observe, recommend, approve, execute, verify,
and remediate a real network without future explicit architecture.

------------------------------------------------------------------------

## 79. Real-Network Mutation

Phase 13 MUST NOT:

-   change txPower in Ericsson ENM;
-   change txPower in Nokia NetAct;
-   send vendor commands;
-   issue configuration writes;
-   invoke vendor mutation APIs;
-   create an execution-capable vendor payload;
-   perform rollback;
-   perform remediation.

------------------------------------------------------------------------

## 80. Future Execution Boundary

Phase 13 deliberately creates the future boundary:

``` text
APPROVED NETWORK CHANGE PROPOSAL
              |
      -------------------
       AUTHORITY BOUNDARY
      -------------------
              |
              v
         FUTURE PHASE
```

A future architecture may consider:

-   execution intent;
-   maintenance/change windows;
-   pre-flight validation;
-   vendor command translation;
-   four-eyes authorization;
-   dry run;
-   canary execution;
-   rollback planning;
-   post-change verification;
-   automatic rollback.

None are authorized by Phase 13.

------------------------------------------------------------------------

## 81. Explicit Non-Goals

Phase 13 does not implement:

-   real Ericsson/Nokia writes;
-   vendor commands;
-   closed-loop optimization;
-   automatic remediation;
-   automatic execution of approved proposals;
-   Agent-authorized network changes;
-   MCP-to-vendor execution;
-   Phase 4-to-vendor execution;
-   arbitrary LLM-generated parameter values;
-   initial network-wide/multi-cell optimization;
-   vendor-specific command generation;
-   rollback execution;
-   live vendor dependency in CI;
-   production ENM interface assumptions;
-   replacement of Phase 12 synchronization;
-   replacement of Phase 6 Digital Twin;
-   automatic full Twin rebuild after every synchronization;
-   production-calibrated RF claims from the synthetic model;
-   duplicate Phase 13 parameter registry;
-   duplicate Phase 12 knowledge-confidence or drift algorithms;
-   collapsing assurance, network-knowledge, and simulation confidence;
-   automatic `NetworkChangeProposal` ↔ `ProposedAction` conversion;
-   canonical mutation from proposal approval;
-   Phase 14 implementation.

------------------------------------------------------------------------

## 82. Architectural Acceptance Gates

Phase 13 architecture is **ACCEPTED**. All of the following remain
explicit:

1.  Parent baseline is exactly
    `2f51bc1fd746633ec051a2ea933aa339c0ddc804`.
2.  Phase 13 is proposal/recommendation intelligence, not network
    execution.
3.  `txPower` is the only initial optimization parameter.
4.  Initial proposal scope is one entity/one parameter.
5.  Candidate generation is bounded and deterministic.
6.  LLMs cannot authoritatively invent legal parameter values.
7.  Phase 12 knowledge confidence gates recommendations.
8.  LOW/UNKNOWN cannot become `RECOMMENDED`.
9.  Proposals bind to trusted source snapshot/execution.
10. Current value is part of proposal validity.
11. Newer trusted state can invalidate a proposal.
12. Relevant drift can trigger validity re-evaluation.
13. Proposal expiration exists.
14. Digital Twin is reused rather than replaced.
15. Twin/source compatibility is checked.
16. Network knowledge confidence and simulation confidence remain
    distinct.
17. Synthetic simulation is not represented as production RF certainty.
18. Risk is deterministic.
19. Benefit is evidence-based.
20. Ranking is deterministic/reproducible.
21. LLMs cannot override authoritative ranking/risk/confidence.
22. Strong evidence/provenance is preserved.
23. Generation and approval are separate authorities.
24. Approval requires governed authorization.
25. `APPROVED` does not mean executable.
26. Agents cannot approve their own proposals.
27. Agents cannot call vendor connector/transport.
28. MCP cannot execute an approved proposal.
29. Phase 4 cannot execute an approved proposal against a real vendor.
30. Phase 13 does not depend directly on vendor
    connector/transport/credentials.
31. No real vendor write capability is introduced.
32. Production ENM transport remains unconfigured/fail-closed.
33. Real vendor continuous synchronization remains separately
    unverified.
34. No secrets/raw vendor payloads enter proposal
    persistence/API/metrics/audit.
35. Default CI remains Azure-independent and vendor-independent.
36. No new distributed lock is introduced solely for proposal
    governance.
37. Historical migrations remain immutable.
38. Phase 1--12 regression behavior remains protected.
39. Closed-loop optimization remains unauthorized.
40. Phase 14 implementation does not start.
41. `SimulatableParameterRegistry` remains the authoritative supported-
    parameter/range source; no duplicate Phase 13 registry exists.
42. `DigitalTwinSimulationService` remains the simulation authority.
43. Assurance, network-knowledge, and simulation confidence remain
    distinct and are not collapsed.
44. `LOW` / `UNKNOWN` network knowledge confidence is a hard
    `RECOMMENDED` gate that scoring/LLMs cannot compensate for.
45. `OptimizationOpportunity` remains a logical concept; a dedicated
    persistent entity is not mandatory.
46. `NetworkChangeProposal` is distinct from Phase 4 `ProposedAction`;
    automatic conversion between them is prohibited.
47. Agents/`AgentProposalAdapter` may request analysis but cannot
    authoritatively create or approve Phase 13 proposals.
48. Phase 12 knowledge-confidence/drift authority is reused; algorithms
    are not duplicated.
49. Proposal governance uses distinct permissions; vendor-import
    authorization is not proposal approval.
50. **`V14`** is the expected next forward-only migration; exact table
    decomposition is deferred to the implementation specification.
51. `APPROVED` proposals do not modify canonical network state.
52. Canonical state changes only through trusted observation/
    reconciliation — not recommendation or approval state.
53. Architecture/isolation tests protect the boundaries above.

------------------------------------------------------------------------

## 83. Architecture Review Questions

Architectural review is complete. The following were considered; the
implementation specification may refine:

1.  Is `txPower` still the correct sole initial parameter given the
    frozen Phase 6 model?
2.  Which existing Phase 3 entities should represent optimization
    opportunities?
3.  Which Phase 6 simulation result fields can safely support
    deterministic benefit/risk scoring?
4.  Should proposal versions be separate rows or linked immutable
    revisions?
5.  What exact proposal expiration policy should the implementation
    specification define?
6.  Should `ASSURANCE_TRIGGERED` generation be implemented immediately
    or remain architecture-ready?
7.  Which existing authorization conventions should name proposal
    permissions?
8.  Which existing audit abstractions should be reused?
9.  Which existing persistence objects can prevent unnecessary new
    tables?
10. What exact Twin/source snapshot compatibility rule is implementable
    with current Phase 6 metadata?

These questions refine implementation; they do not weaken the safety
boundary.

------------------------------------------------------------------------

## 84. Implementation Specification Preconditions

This document is the **accepted** Phase 13 architecture
(architecturally accepted 2026-08-28).

A formal Phase 13 implementation specification may be written against
this architecture.

Cursor MUST NOT implement Phase 13 application code without an
authorized implementation specification.

Cursor or any implementation agent MUST NOT:

-   begin Phase 14;
-   implement real vendor write capability;
-   introduce closed-loop optimization;
-   configure production ENM transport;
-   weaken any §82 architectural acceptance gate.

If implementation specification authorization requires architectural
changes, this document must be updated before code implementation
begins.

------------------------------------------------------------------------

## 85. Required Repository Inspection Before Specification

Before writing implementation-level class/table decisions, inspect:

-   Phase 3 Assurance/Decision entities/services;
-   Phase 4 Action/MCP governance;
-   Phase 5 Agent permissions/orchestration;
-   Phase 6 Twin synchronization, simulation model, confidence,
    persistence;
-   Phase 12 knowledge status/drift/source-state APIs and entities;
-   authorization conventions;
-   audit/metrics abstractions;
-   current Flyway migration sequence;
-   current API namespace conventions;
-   test architecture/isolation conventions.

Do not create duplicate abstractions without repository evidence.

------------------------------------------------------------------------

## 86. Proposed Documentation Set

Phase 13 should eventually maintain:

``` text
docs/architecture/SNIP-PHASE-13-NETWORK-CHANGE-INTELLIGENCE-OPTIMIZATION-PROPOSALS-GOVERNED-RECOMMENDATIONS-ARCHITECTURE.md

docs/implementation/SNIP-PHASE-13-NETWORK-CHANGE-INTELLIGENCE-OPTIMIZATION-PROPOSALS-GOVERNED-RECOMMENDATIONS-SPECIFICATION.md

docs/implementation/SNIP-PHASE-13-NETWORK-CHANGE-INTELLIGENCE-OPTIMIZATION-PROPOSALS-GOVERNED-RECOMMENDATIONS-COMPLETION-REPORT.md
```

Status/context/README/Cursor architecture rules should be synchronized
only at the appropriate lifecycle stages.

------------------------------------------------------------------------

## 87. Proposed Phase Status

``` text
PHASE 13 ARCHITECTURE STATUS: ACCEPTED

PARENT IMMUTABLE BASELINE:
2f51bc1fd746633ec051a2ea933aa339c0ddc804

PHASE 13 IMPLEMENTATION STATUS: NOT STARTED

NETWORK CHANGE PROPOSAL STATUS:
ARCHITECTURE ACCEPTED — IMPLEMENTATION NOT STARTED

REAL VENDOR WRITE CAPABILITY:
NOT AUTHORIZED

CLOSED-LOOP OPTIMIZATION:
NOT AUTHORIZED

PRODUCTION ENM TRANSPORT:
NOT CONFIGURED

REAL VENDOR CONTINUOUS SYNCHRONIZATION:
NOT YET VERIFIED

PHASE 14 STATUS:
NOT STARTED
```

------------------------------------------------------------------------

## 88. Final Architectural Invariants

Phase 13 creates **recommendation authority**, not **execution
authority**.

The system may:

-   understand trusted current state;
-   identify an optimization opportunity;
-   generate bounded candidates;
-   simulate hypothetical changes;
-   evaluate risk and benefit;
-   rank candidates;
-   explain evidence;
-   create a recommendation;
-   allow an authorized human to approve or reject that recommendation.

The system may not:

-   translate that approval into a vendor command;
-   open a vendor mutation session;
-   execute a configuration change;
-   claim synthetic simulation as production certainty;
-   let an Agent/LLM bypass deterministic governance;
-   begin closed-loop optimization.

------------------------------------------------------------------------

## 89. Final Authorization Statement

``` text
PHASE 13 ARCHITECTURE STATUS: ACCEPTED
PHASE 13 IMPLEMENTATION STATUS: NOT STARTED
REAL VENDOR WRITE CAPABILITY: NOT AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT AUTHORIZED
PRODUCTION ENM TRANSPORT: NOT CONFIGURED
REAL VENDOR CONTINUOUS SYNCHRONIZATION: NOT YET VERIFIED
PHASE 14 STATUS: NOT STARTED
```

Phase 13 implementation is **NOT STARTED**. Phase 13 application code
MUST NOT be implemented without an authorized implementation
specification. Phase 14 is **NOT STARTED**.

PHASE 13 ARCHITECTURE STATUS: ACCEPTED
