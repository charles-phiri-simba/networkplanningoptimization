# SNIP Phase 7 --- Multi-Vendor Network Integration Foundation Architecture

## 1. Purpose

Phase 7 establishes SNIP's first explicit multi-vendor network
integration boundary.

Starting baseline:

``` text
Branch: main
Commit: 9c8d57b600f3bc8f9d251767211985a550502e5d
Phase 6: ARCHITECTURALLY ACCEPTED
CI: PASS
Maven: 98 tests, 0 failures
Go: tests/build PASS
Working tree: clean
```

The architectural question is:

> **How can SNIP ingest read-only network inventory, topology and
> configuration from materially different vendor representations while
> preserving a canonical vendor-neutral operational model, explicit
> provenance, deterministic reconciliation and zero vendor write
> authority?**

The governing principle is:

> **Vendor specificity ends at the integration boundary.**

Phase 7 proves the architecture with local Ericsson and Nokia fixture
adapters. It does not connect to production Ericsson ENM or Nokia
NetAct.

## 2. Architectural Position

``` text
Ericsson Fixture ─┐
                  ├─> Vendor Adapter Layer
Nokia Fixture ────┘
                         ↓
                Canonical Integration Model
                         ↓
              Validation / Normalization
                         ↓
              Deterministic Reconciliation
                         ↓
                 SNIP Operational State
                         ↓
 Context → Assurance → Agents → Digital Twin
```

The following must remain distinct:

``` text
Vendor Model != Canonical Integration Model != SNIP Operational Domain
```

Vendor-specific DTOs/types must not propagate into Assurance, Decision
Intelligence, Agents, Digital Twin, RAG or governed action logic.

## 3. Connectivity and Safety

Phase 7 uses local synthetic fixtures only:

``` text
EricssonFixtureAdapter
NokiaFixtureAdapter
```

Adapters are read-only.

Allowed:

``` text
READ inventory
READ topology
READ configuration
```

Prohibited:

``` text
WRITE
DELETE
ACTIVATE
DEACTIVATE
CONFIGURE
EXECUTE
```

No production credentials, real vendor endpoints or external network
dependencies are introduced.

## 4. NetworkSourceAdapter

Introduce a vendor source abstraction equivalent to:

``` text
NetworkSourceAdapter
  vendor()
  sourceSystem()
  schemaVersion()
  readSnapshot()
```

Adapters know vendor semantics. Core SNIP services do not.

A static/in-code `NetworkSourceAdapterRegistry` initially maps:

``` text
ERICSSON -> EricssonFixtureAdapter
NOKIA    -> NokiaFixtureAdapter
```

No dynamic plugin system or database registry in Phase 7.

## 5. SourceSnapshot

Adapters return a source-neutral snapshot rather than persisting JPA
entities directly.

``` text
SourceSnapshot
  sourceSnapshotId
  sourceSystem
  vendor
  vendorSchemaVersion
  capturedAt
  completeSnapshot
  sites[]
  gnbs[]
  cells[]
  configurations[]
  neighbours[]
```

`completeSnapshot` or equivalent semantics are mandatory so
missing-entity detection is safe.

## 6. Canonical Integration Model

Introduce bounded vendor-neutral records such as:

``` text
CanonicalSite
CanonicalGnb
CanonicalCell
CanonicalCellConfiguration
CanonicalNeighbourRelation
```

The canonical model contains what SNIP needs, not every vendor
attribute.

SNIP owns canonical identifiers. Vendor IDs remain source
identity/provenance.

## 7. Fixture Identity Strategy

Normal fixture imports use separate canonical ranges so existing Phase
1--6 fixtures remain stable.

``` text
Ericsson:
  SITE-E001
  GNB-E001
  CELL-E001
  CELL-E002

Nokia:
  SITE-N001
  GNB-N001
  CELL-N001
  CELL-N002
```

Exact small fixture counts may vary, but normal Ericsson and Nokia
datasets must not accidentally claim the same canonical entity.

## 8. Dedicated Conflict Fixture

A dedicated fixture intentionally overlaps on one canonical entity,
e.g.:

``` text
CELL-CONFLICT-001

Ericsson normalized txPower = 46 dBm
Nokia normalized txPower    = 43 dBm
```

Expected result:

``` text
IntegrationConflict
```

not last-writer-wins.

The main demo datasets remain separate.

## 9. Controlled CELL-001 Proof

One isolated integration test may target existing `CELL-001` only to
prove:

``` text
Vendor import changes Twin-relevant operational state
        ↓
Existing Phase 6 Twin fingerprint no longer matches
        ↓
Twin becomes STALE
```

The import must not automatically synchronize the Twin or create a new
Twin version.

The test must not pollute shared demo/test state.

## 10. SourceReference and Provenance

Persist source-to-canonical mapping:

``` text
SourceReference
  canonicalEntityType
  canonicalEntityId
  sourceSystem
  vendor
  sourceEntityType
  sourceEntityId
  sourceDn
  firstSeenAt
  lastSeenAt
  sourceStatus
```

Entity/snapshot-level provenance is sufficient for Phase 7:

``` text
sourceSystem
vendor
sourceSnapshotId
vendorSchemaVersion
sourceObservedAt/capturedAt
importedAt
```

Field-level provenance is deferred.

## 11. NetworkImportBatch

Every import execution is persisted:

``` text
NetworkImportBatch
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

## 12. Import Flow

``` text
Configured Fixture
      ↓
NetworkSourceAdapter
      ↓
SourceSnapshot
      ↓
Canonical Normalization
      ↓
Canonical Validation
      ↓
Deterministic Reconciliation
      ↓
Persistence
      ↓
Import Result / Audit
```

`NetworkImportService` orchestrates this path. No LLM, Agent or MCP
participates.

## 13. Reconciliation

Introduce deterministic reconciliation with outcomes:

``` text
CREATE
UPDATE
UNCHANGED
CONFLICT
REJECT
```

No LLM or Agent may choose reconciliation outcomes.

### Source authority

Phase 7 uses:

> **One current authoritative source per canonical entity/import
> scope.**

There is no global vendor precedence.

Prohibited:

``` text
ERICSSON > NOKIA
NOKIA > ERICSSON
latest import wins
last writer wins
```

A conflicting second source produces a persisted conflict instead of
silently overwriting canonical state.

## 14. IntegrationConflict

Persist conflicts with data equivalent to:

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

Phase 7 reports conflicts. It does not automatically resolve them.

AI conflict resolution is prohibited.

## 15. Validation and Rejections

Canonical validation occurs before reconciliation.

Validate at least:

``` text
required source/canonical IDs
supported technology
supported duplex mode where present
valid units
valid txPower
valid parent references
valid neighbour references
unique source identity
```

Persist non-catastrophic rejected records as `ImportRejection` with
bounded reason codes.

Catastrophic structural failures may fail the import without canonical
mutation.

## 16. Unit and Enumeration Normalization

Canonical units and vocabulary belong to SNIP.

At minimum:

``` text
txPower   -> dBm
technology -> LTE / NR
duplexMode -> FDD / TDD
```

Example:

``` text
Ericsson configuredMaxTxPower = 460 tenths dBm
                    ↓
Canonical txPower = 46.0 dBm
```

Different vendor representations must normalize to equivalent canonical
values.

## 17. Imported Scope

Phase 7 imports exactly:

``` text
Site
gNB
Cell
Cell Configuration
Neighbour Relations
```

Reuse/extend the existing canonical operational model rather than
creating a parallel vendor model.

## 18. Telemetry Boundary

Phase 7 does not ingest vendor streaming telemetry.

``` text
Phase 7 -> inventory / topology / configuration
Phase 2 -> telemetry / KPI events / temporal intelligence
```

No Ericsson/Nokia telemetry adapter in Phase 7.

## 19. Idempotency

Snapshot imports are idempotent.

Importing the same snapshot twice must not duplicate canonical entities,
source references or neighbours.

Expected second import:

``` text
created = 0
updated = 0
unchanged > 0
```

## 20. Missing Entity Semantics

Physical deletion is prohibited.

For an authoritative **complete** snapshot:

``` text
previously seen + absent now -> MISSING
reappears later              -> ACTIVE
```

Do not automatically retire the entity.

Partial snapshots must not mark unrelated entities `MISSING`.

## 21. Audit

Persist append-only events such as:

``` text
IMPORT_STARTED
SNAPSHOT_READ
VALIDATION_COMPLETED
RECONCILIATION_COMPLETED
IMPORT_COMPLETED
IMPORT_FAILED
```

## 22. Schema Evolution

Every adapter/snapshot exposes a schema version, initially:

``` text
ERICSSON_FIXTURE_V1
NOKIA_FIXTURE_V1
```

This is the future vendor schema evolution boundary.

## 23. Raw Payload Retention

Do not create a database archive of complete vendor raw payloads in
Phase 7.

Retain fixture resources plus source identity, mappings, provenance,
conflicts, rejections and audit.

## 24. Digital Twin Relationship

A successful import may change operational state and therefore make an
existing Phase 6 Twin `STALE`.

It must not call `TwinSynchronizationService`.

Phase 6 remains authoritative:

``` text
STALE -> explicit resynchronization required
```

## 25. Agent Relationship

Agents continue consuming vendor-neutral canonical services.

They must not depend on adapters, vendor fixture DTOs or adapter
registry.

No Vendor Integration Agent is added.

## 26. RAG and Decision Intelligence

Imported topology/configuration remains structured operational state and
is not vectorized.

Decision Intelligence consumes canonical state, not vendor DTOs.

Vendor manuals/document ingestion is not Phase 7 scope.

## 27. MCP Relationship

Phase 7 adds no vendor write capability and no vendor MCP tools.

Do not register:

``` text
vendor.write
vendor.configure
vendor.activate
vendor.deactivate
vendor.execute
```

## 28. Persistence

Continue PostgreSQL + Flyway.

Conceptual new persistence:

``` text
network_source
network_import_batch
network_source_reference
network_import_rejection
network_integration_conflict
network_import_audit_event
```

Existing canonical Site/gNB/Cell/configuration/neighbour state remains
authoritative.

Avoid vendor-specific operational tables.

## 29. Conceptual APIs

``` text
POST /api/v1/integration/imports/ericsson
POST /api/v1/integration/imports/nokia

GET  /api/v1/integration/imports
GET  /api/v1/integration/imports/{importId}

GET  /api/v1/integration/conflicts
GET  /api/v1/integration/conflicts/{conflictId}

GET  /api/v1/integration/rejections
```

POST endpoints trigger configured local fixture imports only. They do
not accept credentials, arbitrary external URLs or vendor write
instructions.

## 30. Required Proofs

### A --- Ericsson normal import

Prove Site, gNB, Cell, Configuration, Neighbours, SourceReference,
provenance, import batch and audit.

### B --- Nokia normal import

Prove the same canonical pipeline and downstream model.

### C --- Equivalent normalization

Normalize logically equivalent but materially different Ericsson/Nokia
source records and assert canonical equivalence.

### D --- Conflict

A second conflicting source produces `IntegrationConflict`;
authoritative canonical state is not overwritten.

### E --- Idempotency

Reimport identical snapshot with no duplication.

### F --- Missing entity

A complete later snapshot marks an absent previously-seen entity
`MISSING`, not deleted.

### G --- Digital Twin staleness

A controlled `CELL-001` import changes relevant operational state, makes
an existing Twin `STALE`, and creates no automatic Twin version.

## 31. Observability

Track useful logs/counters:

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

Correlate by `importId`, `vendor`, `sourceSystem`, and
`sourceSnapshotId`.

## 32. Explicitly Out of Scope

Do not implement:

``` text
real Ericsson ENM connectivity
real Nokia NetAct connectivity
production vendor credentials
vendor REST/database clients
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
field-level master-data policy
global vendor precedence
dynamic adapter plugins
remote vendor MCP servers
raw payload database archive
automatic Digital Twin synchronization
Vendor Integration Agent
Phase 8 functionality
```

## 33. Locked Phase 7 Decisions

-   Phase: **Multi-Vendor Network Integration Foundation**
-   Baseline: `9c8d57b600f3bc8f9d251767211985a550502e5d`
-   Vendors: Ericsson + Nokia
-   Connectivity: local synthetic fixtures only
-   Mode: read-only
-   Imported scope: Site, gNB, Cell, Configuration, Neighbours
-   Vendor telemetry: excluded
-   Adapter abstraction: required
-   Adapter registry: static/in-code
-   Canonical model: required
-   Canonical IDs: SNIP-owned
-   Normal fixture ranges: separate
-   Dedicated conflict fixture: intentional overlap
-   Controlled `CELL-001` proof: Twin staleness only
-   SourceReference: persisted
-   Provenance: mandatory
-   Import batches: persisted
-   Reconciliation: deterministic
-   Outcomes: CREATE / UPDATE / UNCHANGED / CONFLICT / REJECT
-   Vendor precedence: none
-   Source authority: one authoritative source per entity/scope
-   Conflicts: persist/report; no silent overwrite
-   AI reconciliation: prohibited
-   Idempotency: required
-   Missing entity: MISSING, not delete
-   Missing detection: complete snapshots only
-   Unit/enumeration normalization: required
-   Raw payload DB archive: excluded
-   Vendor import may make Twin STALE
-   Automatic Twin synchronization: prohibited
-   Agents: vendor-neutral only
-   Vendor MCP writes: prohibited
-   Real ENM/NetAct: deferred
-   Live network writes: prohibited

## 34. Architectural Outcome

At completion:

``` text
Ericsson Representation ─┐
                         ├─> Vendor Adapters
Nokia Representation ────┘
                              ↓
                    Canonical Integration
                              ↓
                  Validation / Normalization
                              ↓
                 Deterministic Reconciliation
                              ↓
                    SNIP Operational State
                              ↓
          Context / Assurance / Agents / Twin
```

while preserving vendor neutrality, provenance, conflict safety,
idempotency, Phase 6 manual Twin synchronization and zero vendor write
authority.

SNIP progression:

``` text
KNOW
  ↓
UNDERSTAND
  ↓
OBSERVE CHANGE
  ↓
ASSESS
  ↓
ACT SAFELY
  ↓
COORDINATE INTELLIGENTLY
  ↓
SIMULATE BEFORE CHANGE
  ↓
INTEGRATE ACROSS VENDORS
```
