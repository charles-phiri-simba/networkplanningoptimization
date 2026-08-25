# SNIP Phase 7 --- Multi-Vendor Network Integration Foundation Implementation Specification

## 1. Authority and Baseline

This is the bounded Cursor implementation contract for Phase 7.

Start exactly from:

``` text
Branch: main
Commit: 9c8d57b600f3bc8f9d251767211985a550502e5d
Commit message: feat: establish SNIP Phase 6 digital twin simulation foundation
Phase 6: ARCHITECTURALLY ACCEPTED
CI: success
Maven: 98 tests, 0 failures
Go: tests/build PASS
Working tree: clean
```

Read `SNIP-PHASE-7-MULTI-VENDOR-NETWORK-INTEGRATION-ARCHITECTURE.md`
completely before modifying code. The architecture is authoritative.

Implement Phase 7 only. Do not start Phase 8. Do not commit or push a
Phase 7 baseline until architectural acceptance and explicit
authorization.

## 2. Objective

Implement:

``` text
Ericsson Fixture ─┐
                  ├─> Vendor Adapters
Nokia Fixture ────┘
                         ↓
                Canonical Integration
                         ↓
              Validation / Normalization
                         ↓
              Deterministic Reconciliation
                         ↓
                 SNIP Operational State
```

Prove that vendor-specific representations stop at the adapter boundary.

## 3. Preserve Phases 1--6

Do not redesign or weaken existing Knowledge/RAG, network context,
telemetry/Kafka, Assurance, Decision Intelligence, Phase 4
governance/MCP, Phase 5 Agent boundaries, or Phase 6 Twin/simulation
semantics.

In particular:

``` text
Phase 4 remains authoritative for governed action.
Agents still cannot call MCP directly.
Phase 6 Twin synchronization remains manual/on-demand.
STALE Twin simulation remains blocked.
No live network write path exists.
```

## 4. Required Integration Components

Implement responsibilities equivalent to:

``` text
NetworkSourceAdapter
NetworkSourceAdapterRegistry
EricssonFixtureAdapter
NokiaFixtureAdapter
SourceSnapshot
CanonicalSite
CanonicalGnb
CanonicalCell
CanonicalCellConfiguration
CanonicalNeighbourRelation
CanonicalUnitNormalizer
CanonicalValidator
NetworkImportService
NetworkReconciliationService
NetworkImportBatch
SourceReference
ImportRejection
IntegrationConflict
ImportAuditEvent
```

Names/package structure may follow repository conventions without
weakening semantics.

## 5. Vendor Adapter Contract

`NetworkSourceAdapter` must expose enough information for:

``` text
vendor
sourceSystem
vendorSchemaVersion
readSnapshot
```

Adapters are read-only and must not persist JPA entities directly.

Registry:

``` text
ERICSSON -> EricssonFixtureAdapter
NOKIA    -> NokiaFixtureAdapter
```

Unsupported vendors fail deterministically.

## 6. Fixtures

Use synthetic classpath/resource JSON fixtures. The Ericsson and Nokia
schemas must be materially different.

Create normal fixtures, dedicated conflict fixtures, and a controlled
`CELL-001` staleness fixture.

No real vendor exports, credentials or endpoints.

## 7. Normal Dataset Isolation

Use separate normal canonical ranges equivalent to:

``` text
Ericsson: SITE-E001 / GNB-E001 / CELL-E001 / CELL-E002
Nokia:    SITE-N001 / GNB-N001 / CELL-N001 / CELL-N002
```

Normal imports must not mutate `CELL-001`.

## 8. SourceSnapshot

Minimum metadata:

``` text
sourceSnapshotId
sourceSystem
vendor
vendorSchemaVersion
capturedAt
completeSnapshot
```

Collections:

``` text
sites
gnbs
cells
configurations
neighbours
```

Missing-entity processing may only run for a complete snapshot.

## 9. Canonical Identity and SourceReference

SNIP owns canonical IDs. Vendor IDs remain source references.

Persist:

``` text
canonicalEntityType
canonicalEntityId
sourceSystem
vendor
sourceEntityType
sourceEntityId
sourceDn if available
firstSeenAt
lastSeenAt
sourceStatus
```

Avoid duplicates on replay.

## 10. Source Status

Support:

``` text
ACTIVE
MISSING
```

Rules:

``` text
seen in complete snapshot -> ACTIVE
absent from later complete authoritative snapshot -> MISSING
reappears -> ACTIVE
```

Never physically delete due to one missing snapshot. Do not add RETIRED
unless required by an existing domain contract.

## 11. Provenance

Persist entity/snapshot-level provenance sufficient to identify:

``` text
sourceSystem
vendor
sourceSnapshotId
vendorSchemaVersion
capturedAt/sourceObservedAt
importedAt
```

Field-level provenance is deferred.

## 12. NetworkImportBatch

Persist each attempt with:

``` text
importId
sourceSystem
vendor
sourceSnapshotId
vendorSchemaVersion
startedAt
completedAt
status
entitiesRead
entitiesCreated
entitiesUpdated
entitiesUnchanged
entitiesRejected
conflictsDetected
```

Use bounded statuses.

## 13. Import Orchestration

`NetworkImportService` flow:

``` text
select adapter
  ↓
create batch
  ↓
read snapshot
  ↓
normalize
  ↓
validate
  ↓
reconcile
  ↓
persist accepted canonical changes
  ↓
persist source references/conflicts/rejections
  ↓
process MISSING only if complete snapshot
  ↓
append audit
  ↓
complete/fail batch
```

No LLM, Agent or MCP in this path.

## 14. Unit Normalization

Canonical `txPower` must use the Phase 6 canonical unit: `dBm`.

Use materially different fixture representations, for example a scaled
Ericsson value and direct Nokia dBm.

Document and unit-test exact conversions.

Do not silently mix units.

## 15. Enumeration Normalization

Normalize vendor-specific values into existing SNIP vocabulary,
including fixture-used values for:

``` text
technology -> LTE / NR
duplex mode -> FDD / TDD
operational/source state -> bounded canonical value
```

No vendor-prefixed technology enums in the operational domain.

## 16. Canonical Validation

Validate at least:

``` text
missing source ID
missing canonical ID
duplicate source identity
unsupported technology
invalid unit/value
invalid txPower
missing parent Site/gNB
invalid neighbour
malformed required relationship
```

Non-catastrophic invalid records become persisted rejections.

## 17. ImportRejection

Persist:

``` text
rejectionId
importId
sourceEntityId
entityType
reasonCode
details
rejectedAt
```

Use bounded reason codes.

## 18. Reconciliation

Implement deterministic:

``` text
CREATE
UPDATE
UNCHANGED
CONFLICT
REJECT
```

No LLM or Agent chooses outcomes.

### Authority rule

One current authoritative source per canonical entity/import scope.

No global Ericsson/Nokia precedence. No last-writer-wins.

Same-authoritative-source changed snapshots may `UPDATE`.

A second conflicting source produces `CONFLICT`.

## 19. IntegrationConflict

Persist:

``` text
conflictId
importId
entityType
canonicalEntityId
field/conflictScope
currentValue
incomingValue
authoritativeSource
incomingSource
reasonCode
status
detectedAt
```

Initial status may be `OPEN`.

Do not automatically resolve conflicts.

## 20. Dedicated Conflict Proof

Use an isolated overlap such as:

``` text
CELL-CONFLICT-001

Ericsson normalized txPower = 46 dBm
Nokia normalized txPower    = 43 dBm
```

Verify:

1.  first source establishes authoritative state;
2.  second source conflicts;
3.  conflict is persisted;
4.  authoritative canonical state is not overwritten;
5.  no duplicate canonical entity is created.

## 21. Equivalent Normalization Proof

Create logically equivalent Ericsson and Nokia source records with
materially different source representations.

Normalize independently and assert equivalence for relevant canonical
fields before persistence/source-authority rules interfere.

## 22. Idempotency

Import the same normal snapshot twice.

Second import must produce no canonical duplication and:

``` text
created = 0
updated = 0
unchanged > 0
```

Do not duplicate SourceReferences or neighbour relationships.

Document whether replay creates a new idempotent batch or returns an
existing batch; either is acceptable if canonical state remains
idempotent.

## 23. Same-Source Update

A later snapshot from the same authoritative source may update canonical
state.

Test at least one safe configuration update, such as a `txPower` change.

Expected outcome: `UPDATE`, not cross-source conflict.

## 24. Missing Entity Proof

Complete snapshot 1 contains two source cells. Complete snapshot 2 omits
one.

Expected:

``` text
omitted entity SourceReference -> MISSING
canonical entity not physically deleted
```

A partial snapshot must not mark the omitted entity `MISSING`.

## 25. Flyway / PostgreSQL

Phase 6 used `V7__digital_twin_simulation.sql`; therefore use the next
migration, expected:

``` text
V8__multi_vendor_network_integration.sql
```

unless repository inspection proves otherwise.

Persist concepts equivalent to:

``` text
network_source
network_import_batch
network_source_reference
network_import_rejection
network_integration_conflict
network_import_audit_event
```

Reuse existing canonical operational tables. Do not create
Ericsson/Nokia operational tables.

## 26. Source Metadata

Represent fixture sources such as:

``` text
ERICSSON_FIXTURE
NOKIA_FIXTURE
```

with metadata equivalent to:

``` text
sourceSystem
vendor
mode = FIXTURE
readOnly = true
enabled
```

No credentials.

## 27. Import Audit

Append-only events:

``` text
IMPORT_STARTED
SNAPSHOT_READ
VALIDATION_COMPLETED
RECONCILIATION_COMPLETED
IMPORT_COMPLETED
IMPORT_FAILED
```

Do not update/delete old audit events.

## 28. APIs

Implement bounded APIs consistent with repository conventions:

``` text
POST /api/v1/integration/imports/ericsson
POST /api/v1/integration/imports/nokia

GET  /api/v1/integration/imports
GET  /api/v1/integration/imports/{importId}

GET  /api/v1/integration/conflicts
GET  /api/v1/integration/conflicts/{conflictId}

GET  /api/v1/integration/rejections
```

POST triggers configured local fixtures only.

Do not accept arbitrary filesystem paths, external URLs, credentials or
vendor write instructions.

## 29. Ericsson E2E

Assert successful import and canonical Site, gNB, Cells, configuration,
neighbours, SourceReferences, provenance, batch and audit.

## 30. Nokia E2E

Assert the same through Nokia using the same canonical operational
layer.

Downstream reads must not need vendor-specific branching.

## 31. Controlled CELL-001 Twin Staleness Proof

Required isolated flow:

``` text
ensure CELL-001 baseline
  ↓
synchronize Phase 6 Twin
  ↓
assert CURRENT
  ↓
controlled fixture import changes a Twin-relevant operational configuration
  ↓
assert operational state changed
  ↓
assert existing Twin = STALE
  ↓
assert no automatic new Twin version
```

The import must not invoke `TwinSynchronizationService`.

Clean up/isolate state to preserve deterministic tests.

## 32. Phase 6 Regression

Preserve:

``` text
STALE Twin -> simulation blocked
```

Do not redesign the Phase 6 freshness algorithm.

## 33. Phase 5 Agent Boundary

No Agent may directly depend on adapters, registry or vendor fixture
DTOs.

No Vendor Integration Agent.

No Agent-triggered vendor mutation.

## 34. Phase 4 MCP Boundary

Register no vendor mutation tools.

`APPLY_CELL_PARAMETER_CHANGE` remains HIGH / DENY.

Import APIs mutate SNIP integration/operational state only, never an
external network.

## 35. Telemetry and RAG Boundaries

Do not add Ericsson/Nokia telemetry adapters or route vendor fixture
KPIs into Phase 2 Kafka.

Do not vectorize imported operational records.

Vendor manuals/document ingestion is out of scope.

## 36. Catastrophic Failure

For unrecoverable structural snapshot failure:

-   mark batch FAILED;
-   append `IMPORT_FAILED`;
-   avoid partial canonical mutation for the failed structural
    operation;
-   preserve prior canonical state;
-   do not leave the batch permanently STARTED.

## 37. Concurrency

No distributed locking infrastructure.

Use the simplest repository-consistent guard against obvious duplicate
concurrent mutation of the same source/scope if required.

Document accepted concurrency limitations.

## 38. Observability

Add logs/counters equivalent to:

``` text
importsStarted
importsSucceeded
importsFailed
recordsRead
recordsCreated
recordsUpdated
recordsUnchanged
recordsRejected
conflictsDetected
missingEntitiesDetected
importLatencyMs
```

Correlate by import/source identifiers.

## 39. Security Review

Completion report must confirm:

``` text
no real credentials
no ENM endpoint
no NetAct endpoint
read-only adapters
no vendor write method
no vendor MCP write
no Agent vendor write path
no LLM reconciliation
no automatic conflict resolution
no physical deletion from missing source data
no automatic Twin synchronization
```

## 40. Required Tests

Add focused tests for at least:

``` text
adapter registry
Ericsson normalization
Nokia normalization
cross-vendor equivalent canonical normalization
unit normalization
enumeration normalization
canonical validation
CREATE
UPDATE
UNCHANGED
CONFLICT
REJECT
idempotency
SourceReference lifecycle
complete-snapshot MISSING
partial-snapshot safety if supported
conflict persistence
import audit
Ericsson API import
Nokia API import
CELL-001 Twin staleness integration
```

Use existing Testcontainers PostgreSQL conventions.

## 41. Regression / CI

All Phase 1--6 tests must remain passing.

Required verification:

``` text
mvn -B test
go test ./...
go build ./cmd/simulator
```

Default CI must require no Ollama, ENM, NetAct, vendor credentials,
external vendor network, external DB or SFTP.

## 42. Local E2E Evidence

Document four proofs:

### A. Ericsson normal import

Record import ID, snapshot ID, schema version, counts, canonical IDs and
source references.

### B. Nokia normal import

Record equivalent evidence.

### C. Conflict

Record authoritative source, incoming source, canonical entity, conflict
value, conflict ID and proof authoritative state was not overwritten.

### D. Digital Twin staleness

Record `CELL-001`, twin ID/version, CURRENT before import, imported
field change, STALE after import and proof no automatic Twin version was
created.

## 43. ADRs

Create sequential ADRs after Phase 6 ADR 042:

``` text
043 Vendor Adapter Boundary
044 Canonical Multi-Vendor Integration Model
045 SNIP-Owned Canonical Identity and SourceReference
046 Deterministic Reconciliation and No Vendor Precedence
047 Snapshot Import, Idempotency and Missing-Entity Semantics
048 Fixture-First Read-Only Ericsson/Nokia Integration
049 Vendor Telemetry Remains in Phase 2 Boundary
050 Vendor Import Makes Digital Twin Stale Without Auto-Sync
```

Use established repository ADR format.

## 44. Documentation

Update as appropriate:

``` text
README.md
docs/implementation/SNIP-IMPLEMENTATION-CONTEXT.md
docs/implementation/SNIP-IMPLEMENTATION-STATUS.md
.cursor/rules/snip-architecture.mdc
```

Copy architecture/spec into established repository locations following
prior phase conventions.

Update the Cursor rule from Phase 6-only to Phase 7-only scope while
preserving frozen Phase 1--6 decisions.

## 45. Explicitly Out of Scope

Do NOT implement:

``` text
real Ericsson ENM connectivity
real Nokia NetAct connectivity
production vendor credentials
vendor REST/database connectivity
SFTP
SNMP
NETCONF
gNMI
3GPP Bulk CM
vendor telemetry streaming
Kafka vendor telemetry adapter
continuous import scheduler
automatic polling
vendor writes
vendor configuration execution
automatic conflict resolution
AI reconciliation
field-level master-data governance
global vendor precedence
dynamic adapter plugins
remote vendor MCP
raw payload DB archive
automatic Twin synchronization
Vendor Integration Agent
Phase 8
```

## 46. Acceptance Criteria --- Baseline and Boundaries

-   [ ] Starts from `9c8d57b600f3bc8f9d251767211985a550502e5d`.
-   [ ] Phase 1--6 regressions pass.
-   [ ] Phase 6 remains frozen.
-   [ ] No Phase 8 implementation.
-   [ ] No live/vendor write path.
-   [ ] No vendor-specific model leaks into higher intelligence layers.

## 47. Acceptance Criteria --- Adapters and Canonical Model

-   [ ] Adapter abstraction exists.
-   [ ] Static registry exists.
-   [ ] Ericsson fixture adapter exists.
-   [ ] Nokia fixture adapter exists.
-   [ ] Source schemas are materially different.
-   [ ] Schema versions explicit.
-   [ ] Adapters do not persist JPA entities directly.
-   [ ] Canonical Site/gNB/Cell/configuration/neighbour model
    implemented/reused.
-   [ ] SNIP owns canonical IDs.
-   [ ] Vendor IDs remain SourceReferences.
-   [ ] Units/enums normalized.
-   [ ] Equivalent vendor inputs normalize equivalently.

## 48. Acceptance Criteria --- Import and Reconciliation

-   [ ] Import batches persisted.
-   [ ] SourceReferences/provenance persisted.
-   [ ] CREATE deterministic.
-   [ ] UPDATE deterministic.
-   [ ] UNCHANGED deterministic.
-   [ ] CONFLICT deterministic.
-   [ ] REJECT deterministic.
-   [ ] No LLM reconciliation.
-   [ ] No vendor precedence.
-   [ ] No last-writer-wins.
-   [ ] Conflict persisted/reported.
-   [ ] Authoritative state protected.

## 49. Acceptance Criteria --- Idempotency and Lifecycle

-   [ ] Identical reimport does not duplicate canonical state.
-   [ ] Identical reimport does not duplicate source references.
-   [ ] Same-source changed snapshot can UPDATE.
-   [ ] Complete snapshot can mark absent source entity MISSING.
-   [ ] Missing entity is not physically deleted.
-   [ ] Partial snapshot cannot incorrectly mark unrelated entities
    MISSING.
-   [ ] Reappearing entity can become ACTIVE.

## 50. Acceptance Criteria --- Dataset Isolation / Conflict

-   [ ] Normal Ericsson and Nokia ranges are separate.
-   [ ] Normal imports do not mutate `CELL-001`.
-   [ ] Dedicated conflict fixture intentionally overlaps.
-   [ ] Second conflicting source cannot silently overwrite canonical
    state.
-   [ ] Controlled `CELL-001` proof is isolated.

## 51. Acceptance Criteria --- Digital Twin

-   [ ] Controlled import changes Twin-relevant `CELL-001` state.
-   [ ] Existing Twin becomes STALE.
-   [ ] Import does not resynchronize Twin.
-   [ ] No new Twin version is created by import.
-   [ ] Phase 6 stale-simulation blocking remains intact.

## 52. Acceptance Criteria --- Safety / CI / Docs

-   [ ] No real credentials/endpoints.
-   [ ] No vendor write method.
-   [ ] No vendor MCP write.
-   [ ] No Agent vendor write.
-   [ ] No automatic conflict resolution.
-   [ ] No automatic Twin synchronization.
-   [ ] `mvn -B test` passes.
-   [ ] `go test ./...` passes.
-   [ ] `go build ./cmd/simulator` passes.
-   [ ] Default CI needs no vendor systems/Ollama.
-   [ ] ADRs 043--050 created.
-   [ ] Documentation/rule updated.
-   [ ] No secrets/generated binaries committed.
-   [ ] Phase 8 not started.

## 53. Required Completion Report

Create:

``` text
docs/implementation/SNIP-PHASE-7-COMPLETION-REPORT.md
```

Include:

1.  Executive Summary
2.  Phase 6 Baseline Verification
3.  Scope Delivered
4.  Integration Architecture
5.  Vendor Adapter Boundary
6.  Adapter Registry
7.  Ericsson Fixture Model
8.  Nokia Fixture Model
9.  SourceSnapshot
10. Canonical Integration Model
11. Canonical Identity
12. SourceReference / Provenance
13. Unit Normalization
14. Enumeration Normalization
15. Validation
16. NetworkImportBatch
17. NetworkImportService
18. Deterministic Reconciliation
19. Source Authority
20. Conflict Model
21. Rejection Model
22. Idempotency
23. Update Semantics
24. Missing-Entity Semantics
25. Import Audit
26. PostgreSQL / Flyway
27. APIs
28. Ericsson Normal Import Proof
29. Nokia Normal Import Proof
30. Equivalent Normalization Proof
31. Dedicated Conflict Proof
32. Controlled CELL-001 Twin Staleness Proof
33. Phase 6 Regression
34. Phase 5 Agent Boundary
35. Phase 4 MCP Boundary
36. Telemetry / RAG Boundaries
37. Failure Cases
38. Tests
39. Local E2E Evidence
40. Observability
41. Security / Zero-Vendor-Write Review
42. ADRs
43. Performance
44. Acceptance PASS/FAIL
45. Known Limitations
46. Technical Debt
47. Lessons Learned
48. Recommended Next Phase
49. Architectural Questions

End with exactly one:

``` text
PHASE 7 STATUS: ACCEPTANCE RECOMMENDED
```

or:

``` text
PHASE 7 STATUS: ACCEPTANCE NOT RECOMMENDED
```

Do not mark Phase 7 architecturally accepted yourself.

## 54. Architectural Questions for Review

Report recommendations, without broadening implementation, on:

1.  whether entity/snapshot-level provenance remains sufficient or
    field-level provenance should come later;
2.  whether one-authoritative-source-per-entity remains sufficient or a
    formal mastership policy should be designed next;
3.  whether fixture adapters should next evolve toward realistic
    protocol/source connectors or integration runtime hardening should
    come first;
4.  whether raw snapshot archival/replay should remain deferred;
5.  whether continuous/incremental import should remain deferred.

## 55. Final Instruction to Cursor

Treat this as authorization for **Phase 7 only**.

The objective is:

> **Prove that SNIP can ingest materially different Ericsson and Nokia
> read-only source representations through explicit vendor adapters,
> normalize them into one SNIP-owned canonical model, reconcile them
> deterministically with provenance and conflict safety, and make
> resulting operational changes visible to the existing architecture
> without introducing vendor write authority.**

Preserve Phase 1--6 architecture.

Do not connect to real ENM/NetAct. Do not add production credentials. Do
not add vendor telemetry. Do not add vendor writes. Do not automatically
resynchronize the Digital Twin. Do not start Phase 8.

When implementation and validation are complete:

1.  produce the Phase 7 completion report;
2.  leave Phase 7 work uncommitted unless separately authorized;
3.  do not push;
4.  STOP for architectural review.
