# SNIP Phase 17 — Certified Vendor Write Transport Integration, Target Onboarding & Production Operational Readiness — Implementation Specification

## Document control

| Field | Value |
|---|---|
| Document type | Implementation specification (coding contract) |
| Status | CORRECTED CANDIDATE — READY FOR IMPLEMENTATION-SPECIFICATION RE-REVIEW |
| Date | 2026-09-02 |
| Phase | 17 |
| Implementation | NOT STARTED |
| Implementation authorization | NOT GRANTED by this draft |
| V18 | SPECIFIED ONLY — MUST NOT be created by this authoring task |
| Phase 18 | NOT STARTED |
| Real production execution | NOT AUTHORIZED |

**Phase 16 immutable implementation baseline:** `f4e09b42f7b8f56c3794fae3c91a50a7af490c82`  
**Phase 17 immutable architecture baseline:** `77fd24c0fd32c920c97ff5169f4bc8a93a77b208`  
**Frozen Phase 17 architecture SHA-256:** `ea92c6e9183234485da83798ab4fc91c224cfbd1dad80bc464d41009fce576a0`  
**Architecture gates:** 158 (`G17-001`–`G17-158`)  
**Threats:** 33 (`T17-01`–`T17-33`)  
**Architecture exact-SHA CI:** workflow `ci`, run `33595626422`, SUCCESS  

Canonical architecture:

```text
docs/architecture/SNIP-PHASE-17-CERTIFIED-VENDOR-WRITE-TRANSPORT-INTEGRATION-TARGET-ONBOARDING-PRODUCTION-OPERATIONAL-READINESS-ARCHITECTURE.md
```

Root copy MUST remain byte-identical to the canonical architecture. This specification MUST NOT amend either file.

Cursor MUST start implementation only after this specification is independently accepted and authorized. If HEAD is not `77fd24c0fd32c920c97ff5169f4bc8a93a77b208` at implementation start, STOP.

---

## Status at specification freeze

```text
ERICSSON PRODUCTION WRITE PROTOCOL: UNRESOLVED
ERICSSON PRODUCTION WRITE TRANSPORT: NOT IMPLEMENTED
REAL ENDPOINT: NONE
REAL CREDENTIAL: NONE
LEVEL 0: SATISFIED THROUGH EXISTING LOCAL/CONTROLLED-TRANSPORT FOUNDATION
LEVEL 1: NOT EXECUTED
LEVEL 2: NOT EXECUTED
LEVEL 3: NOT SATISFIED
LEVEL 4: NOT SATISFIED
REAL PRODUCTION EXECUTION: NOT AUTHORIZED
NOKIA: DEFERRED
AGENT / MCP / SCHEDULED / EVENT EXECUTION: NOT AUTHORIZED
AUTOMATIC ROLLBACK: NOT AUTHORIZED
CLOSED LOOP: NOT AUTHORIZED
V18: NOT CREATED
PHASE 17 IMPLEMENTATION: NOT STARTED
PHASE 18: NOT STARTED
```

---

## 1. Implementation principle

> Phase 17 implements the certification/onboarding control plane plus production-gateway runtime safety enforcement required for a future vendor-supported write transport. It must not invent that transport. A production vendor transport becomes implementable only after authoritative vendor interface evidence exists and after the separately defined external certification stages are performed. UnconfiguredProductionEricssonWriteTransport remains the production binding for this implementation.

Normative corollaries:

1. No Ericsson production write protocol may be invented, inferred, or guessed.
2. `EricssonWriteTransport` remains an unresolved abstraction. Production continues to bind `UnconfiguredProductionEricssonWriteTransport`.
3. Certification, onboarding, and Level 3 registration never authorize production mutation.
4. Level 4 is **not** a persisted target flag. It is `Level4Satisfied(executionId)` at the send boundary.
5. Existing Phase 16 enum `CertificationLevel.L4` MUST NOT be written onto `production_network_target.certification_level` as a reusable entitlement. `CertificationLevel.meets(L4)` MUST NOT be used as send-boundary Level 4 proof.
6. The production-write-gateway remains the only mutation plane. Certification services MUST NOT invoke vendor mutation.
7. Durable PostgreSQL state is certification authority. Cache and replica memory are not.
8. Phase 16 grant consume, lease/fencing, kill switch, rate/blast, authorization, verification, and rollback governance remain controlling.
9. Agents, MCP, schedulers, and event consumers have no certification, onboarding, or mutation authority.
10. Default CI is Level 0 only. External L1/L2/L3 evidence is NOT EXECUTED / NOT SATISFIED at this freeze.

---

## 2. Scope

### 2.1 Frozen initial scope

| Constraint | Value |
|---|---|
| Vendor | ERICSSON |
| Platform | ENM |
| Object | CELL |
| Parameter | txPower |
| Mutation cardinality | one target, one cell, one parameter, one mutation |
| Rollback | exact persisted Phase 14 rollback mutation only |
| Nokia | DEFERRED |
| Bulk / multi-cell | FORBIDDEN |
| Generic command execution | FORBIDDEN |

### 2.2 What this specification authorizes Cursor to implement later

After independent acceptance of this specification only:

- V18 additive schema
- certification / onboarding / evidence / bundle domain
- invalidation cascade into existing Phase 16 stale/revoke services
- gateway durable currentness + artifact identity + destination-trust hooks
- control-plane APIs
- audit / metrics / alerts
- local/default-CI tests and failure injection
- external-certification scaffolding (records and checklists, not execution)

### 2.3 Explicit non-goals

Phase 17 implementation MUST NOT:

- invent Ericsson REST/SOAP/Bulk CM/CLI/SSH/SDK/NETCONF/CORBA routes, payloads, or auth flows
- implement a real Ericsson write transport
- add a real production or lab hostname, IP, or credential value
- enable production writes by default
- create V18 during this authoring task
- start Phase 18
- add Nokia write support
- expand beyond CELL/txPower
- add Agent/MCP mutation or certification tools
- authorize automatic rollback or closed-loop optimization
- persist a standing Level 4 entitlement
- write canonical `radio_configuration`
- add write methods to Phase 11 `EnmTransport`
- collapse `production-write-gateway` into `snip-npo-app`
- amend V1–V17 SQL content
- amend frozen Phase 16 architecture, specification, or implementation baseline

---

## 3. Frozen invariants (implementation MUST preserve)

1. Interface revocation cascade (§7.4 / G17-134)
2. Documentation revocation cascade (G17-135)
3. Durable certification authority (G17-004 / G17-137)
4. Send-boundary currentness (G17-138)
5. Cache fail-closed (G17-139)
6. Multi-replica fail-closed / currentness (G17-140)
7. Runtime artifact binding (G17-090 / G17-141 / G17-142)
8. Certification lifecycle transition table (G17-143)
9. Certification / onboarding SoD (G17-144 / G17-145)
10. Endpoint identity tuple (G17-146)
11. DNS/TLS destination trust (G17-147)
12. Target-bound credential resolution (G17-148)
13. `POSITIVE_NOT_SENT` requires certified positive proof (G17-149)
14. Health AND composition (G17-150)
15. Evidence trust authority (G17-151)
16. Evidence supersession (G17-152)
17. Immutable / versioned certification snapshots (G17-153)
18. Change-invalidation matrix (G17-154)
19. Level 4 exact identity / currentness (G17-028 / G17-155)
20. Phase 16 governance preserved (G17-001 / G17-100)
21. Level 3 ≠ Level 4 (G17-021 / G17-029)
22. No blind retry (G17-070 / G17-156)
23. Independent readback (G17-074 / G17-075)
24. Separate rollback governance (G17-077 / G17-078)
25. Ambiguous-outcome recovery preserved (G17-081 / G17-082)

---

## 4. Module and package design

Respect the existing three Maven modules. Do not add a fourth runtime process.

| Module | Phase 17 ownership | MUST NOT |
|---|---|---|
| `snip-npo-app` | Certification, onboarding, evidence, invalidation, control-plane APIs, SoD, audit of certification events | Resolve write credentials; call vendor mutation; load transport plugins |
| `production-change-protocol` | Shared Phase 17 enums, denial codes, currentness snapshot DTO, artifact identity DTO | Azure/Key Vault/vendor deps; secret values |
| `production-write-gateway` | Durable authority reader, send-boundary preflight, artifact identity provider, destination-trust hooks, health composition | Own mutable certification write path that bypasses app SoD; invent protocol |

### 4.1 Application packages

```text
com.simba.snip.npo.vendorcertification
  api
  domain
  entity
  repository
  service
  policy
  audit
  metrics
  config
  exception

com.simba.snip.npo.targetonboarding
  api
  domain
  entity
  repository
  service
  policy
```

Do not place Phase 17 certification logic in `changeexecution`, `changeplanning`, `integration`, `agent`, or `mcp`.

Phase 16 `com.simba.snip.npo.productionchange` remains owner of grants, authorization, fingerprints, lease, kill switch, and production-change APIs. Phase 17 calls into those services; it does not fork them.

### 4.2 Gateway packages

```text
com.simba.snip.npo.productionwritegateway.vendortransport
  ProductionCertificationAuthority
  RuntimeTransportArtifactIdentityProvider
  CertifiedTransportResolver
  CertificationSendBoundaryPreflight
  DestinationTrustValidator
  TransportHealthComposer
```

Existing packages remain:

```text
adapter    VendorNetworkWriteAdapter / EricssonEnmWriteAdapter
transport  EricssonWriteTransport / UnconfiguredProductionEricssonWriteTransport / ControlledTestEricssonWriteTransport
```

### 4.3 Protocol additions

Place under `com.simba.snip.npo.productionchange.protocol` (existing module; new types only):

```text
VendorInterfaceStatus
TransportCertificationState
TransportHealthState
CertificationEvidenceType
CertificationEvidenceStatus
AttemptSendClass          // POSITIVE_NOT_SENT | MAY_HAVE_SENT
// Reuse frozen Phase 16 ExpectedStateGuardStrength. Do NOT add ExpectedStateStrength.
Phase17DenialCode
CertificationCurrentnessSnapshot
RuntimeArtifactIdentity
```

### 4.4 Dependency direction

```text
Phase13 recommendation
  → Phase14 planning
    → Phase15 execution readiness
      → Phase16 production governance (snip-npo-app.productionchange)
        → Phase17 certification/onboarding write services (app)
          → production-change-protocol
            → production-write-gateway
              → Phase17 certified transport resolver/control
                → VendorNetworkWriteAdapter
                  → EricssonEnmWriteAdapter
                    → EricssonWriteTransport
                      → approved vendor interface   // UNRESOLVED; production = UnconfiguredProductionEricssonWriteTransport
```

Forbidden:

| Source | Prohibition |
|---|---|
| Agent / MCP | No certify, onboard, approve, reactivate, execute, rollback |
| Scheduler / Kafka consumers | No production mutation; expiry/health maintenance only |
| `EnmTransport` | No write methods |
| App process | No write-credential resolve; no vendor mutation |
| Gateway | No inventing protocol; no bypass of consume/lease/kill/rate |
| HTTP caller | No endpoint, credential, protocol, or L4 flag in request body |

---

## 5. Existing Phase 16 types — reuse and constraints

| Existing type | Phase 17 rule |
|---|---|
| `ProductionNetworkTarget` | Remains execution-authoritative. Onboarding enriches it; does not replace it. |
| `ProductionAuthorizationService.markStale` | Called by `CertificationInvalidationService`. Do not invent a second stale writer. |
| Grant consume SQL | Unchanged ISSUED→CONSUMED durable predicate in the gateway. Phase 17 MUST NOT own grant rows, MUST NOT create a grant table, and MUST NOT create a parallel grant state machine. |
| `ProductionChangePermission` | Retained. Add Phase 17 permissions beside it; do not overload `ADMINISTER_PRODUCTION_TARGET` as onboarding SoD. |
| `CertificationLevel` | L0–L3 may describe registration class. **Never persist L4 on a target as entitlement.** Send-boundary Level 4 is a function, not `meets(L4)`. |
| `EricssonWriteTransport` | Interface unchanged. No new protocol methods. |
| `UnconfiguredProductionEricssonWriteTransport` | Remains production binding. |
| `ControlledTestEricssonWriteTransport` | Test-only; refuse production-runtime mode. |
| `ExpectedStateGuardStrength` | **Reuse the frozen Phase 16 enum only.** Do **not** introduce `ExpectedStateStrength`. Phase 17 may certify which existing value is permitted. Default `READ_THEN_WRITE`. `ATOMIC` only after certified interface proof. Unknown/unproven: `READ_THEN_WRITE` or DENY per frozen Phase 16 policy. |
| Phase 16 audit chain | Unchanged scope (`productionChangeId`). Phase 17 certification events use a **separate** certification audit chain (below). |
| Phase 16 fingerprint algorithm | Unchanged. Phase 17 currentness is validated independently at send. Invalidation marks existing authorizations STALE. |

---

## 6. V18 schema specification (DO NOT CREATE NOW)

When implementation is authorized, create **exactly one** additive Flyway file:

```text
snip-npo-app/src/main/resources/db/migration/V18__phase17_certified_vendor_transport.sql
```

Rules:

- Additive only. V1–V17 files MUST NOT be edited.
- No secret, private-key, password, token, certificate-PEM, or vendor-payload columns.
- No production hostname seed data.
- Existing `production_network_target` rows default to **not Phase 17 certified** and **not PRODUCTION_REGISTERED**. Gateway DENY until explicit onboarding.
- Flyway location remains the app module (same as V17). Gateway reads the same database.

SHA-256 **content-digest** columns use `CHAR(64)` and `CHECK (column ~ '^[0-9a-f]{64}$')`.

Git **source baseline** columns use `CHAR(40)` and `CHECK (column ~ '^[0-9a-f]{40}$')`.

Inherited Phase 14/15/16 fingerprint columns MUST NOT receive new Phase 17 CHECKs.

Opaque UUID/VARCHAR ids MUST NOT be constrained as hashes.

Logical identity + version identity: certified content is immutable. Material change inserts a new version row and invalidates dependents. No `UPDATE` of security-significant certified columns after the row leaves `DRAFT`.

### 6.1 Common versioning pattern

Every certified snapshot table uses:

| Concept | Columns |
|---|---|
| Logical identity | `*_id UUID` (stable) |
| Version identity | `version_no INTEGER` plus `content_digest CHAR(64)` |
| Status | table-specific CHECK |
| Supersession | `supersedes_version_id UUID NULL` |
| Effective interval | `effective_from TIMESTAMPTZ NOT NULL`, `effective_until TIMESTAMPTZ NULL` |
| Immutability | after non-DRAFT, only `status` / `updated_at` / invalidation timestamps may change, and only through the invalidation/lifecycle service |
| Currentness | partial unique index: at most one `CURRENT` / `ACTIVE` version per logical id where the table uses that flag |

`effective_until` NULL means open-ended. CHECK: `effective_until IS NULL OR effective_until > effective_from`.

Where a table has `vendor` / `platform` columns on the Phase 17 write path, CHECK `vendor IN ('ERICSSON')` AND `platform IN ('ENM')`. Do not invent additional vendors.

Retention: rows are never physically deleted. Revocation/expiry is status change. Physical delete is FORBIDDEN in application code.

### 6.2 `vendor_interface_definition`

Authoritative interface record. No proprietary documentation blobs.

| Column | Type | Notes |
|---|---|---|
| `interface_definition_version_id` | UUID PK | Version identity |
| `interface_definition_id` | UUID NOT NULL | Logical identity |
| `version_no` | INTEGER NOT NULL | Monotonic per logical id |
| `content_digest` | CHAR(64) NOT NULL | SHA-256 of canonical certified fields |
| `vendor` | VARCHAR(32) NOT NULL | CHECK `ERICSSON` for Phase 17 writes |
| `platform` | VARCHAR(32) NOT NULL | CHECK `ENM` for Phase 17 writes |
| `vendor_product_version_predicate` | VARCHAR(512) NOT NULL | Explicit predicate text; not inferred SemVer |
| `interface_type_category` | VARCHAR(64) NOT NULL | CHECK `IN ('UNRESOLVED','ABSTRACT_ALTERNATIVE')`. No protocol fields. |
| `documentation_reference` | VARCHAR(512) NOT NULL | URI/handle metadata, not blob |
| `documentation_version` | VARCHAR(128) NOT NULL | |
| `documentation_status` | VARCHAR(32) NOT NULL | CHECK `IN ('ACTIVE','WITHDRAWN','SUPERSEDED')` |
| `abstract_protocol_placeholder_version_id` | UUID NULL FK | FK `vendor_abstract_protocol_placeholder.placeholder_version_id`. NULL allowed. No protocol mechanics. |
| `security_cert_version_id` | UUID NULL FK | FK `vendor_security_certification.security_cert_version_id` |
| `capability_cert_version_id` | UUID NULL FK | FK `vendor_capability_certification.capability_cert_version_id` |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','INTERFACE_VERIFIED','SUPERSEDED','REVOKED','EXPIRED')` |
| `effective_from` / `effective_until` | TIMESTAMPTZ | |
| `created_at` / `created_by` | TIMESTAMPTZ / VARCHAR(128) | |
| `supersedes_version_id` | UUID NULL | FK self |
| `updated_at` | TIMESTAMPTZ NOT NULL | |

Unique: `(interface_definition_id, version_no)`, `(interface_definition_id, content_digest)`.  
Indexes: `(interface_definition_id, status)`, `(documentation_status)`.  
**No** `approval_id` column. Current approval is derived: the unique ACTIVE/APPROVED `vendor_interface_approval` row for this `interface_definition_version_id`.  
Insert order: (1) interface version in `DRAFT` or `INTERFACE_VERIFIED` pending approval, (2) `vendor_interface_approval` row referencing that version.  
Do **not** store REST paths, SOAP operations, NETCONF RPC names, or auth flows.

### 6.2A `vendor_abstract_protocol_placeholder`

Exists only so interface rows can optionally reference an abstract placeholder **without encoding protocol**. Columns: `placeholder_version_id` UUID PK; `placeholder_id` UUID NOT NULL; `version_no` INTEGER NOT NULL; `content_digest` CHAR(64) NOT NULL; `interface_type_category` VARCHAR(64) NOT NULL CHECK `IN ('UNRESOLVED','ABSTRACT_ALTERNATIVE')`; `status` VARCHAR(32) NOT NULL CHECK `IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED')`; `effective_from` TIMESTAMPTZ NOT NULL; `effective_until` TIMESTAMPTZ NULL; `created_at` TIMESTAMPTZ NOT NULL; `created_by` VARCHAR(128) NOT NULL. Unique `(placeholder_id, version_no)`. Partial unique one `ACTIVE` per `placeholder_id`. **Forbidden columns:** URL, RPC name, payload schema, auth flow.

### 6.3 `vendor_interface_approval`

| Column | Type | Notes |
|---|---|---|
| `approval_id` | UUID PK | |
| `interface_definition_version_id` | UUID NOT NULL FK | |
| `approver_principal_id` | VARCHAR(128) NOT NULL | Requires `VENDOR_INTERFACE_REVIEW` |
| `approval_status` | VARCHAR(32) NOT NULL | CHECK `IN ('APPROVED','REVOKED','WITHDRAWN')` |
| `approved_at` | TIMESTAMPTZ NOT NULL | |
| `revoked_at` | TIMESTAMPTZ NULL | |
| `revoked_by` | VARCHAR(128) NULL | |
| `reason_code` | VARCHAR(128) NULL | |
| `content_digest` | CHAR(64) NOT NULL | |

Partial unique: at most one row with `approval_status = 'APPROVED'` per `interface_definition_version_id`.

Revoking approval MUST run the interface revocation cascade even if `interface_definition_id` is unchanged.

### 6.4 `production_endpoint_profile`

| Column | Type | Notes |
|---|---|---|
| `endpoint_profile_version_id` | UUID PK | |
| `endpoint_profile_id` | UUID NOT NULL | Logical |
| `version_no` | INTEGER NOT NULL | |
| `content_digest` | CHAR(64) NOT NULL | |
| `production_target_id` | VARCHAR(128) NULL FK `production_network_target(target_id)` | NULL allowed only when `status = 'DRAFT'`. CHECK: if `status = 'ACTIVE' AND environment IN ('PREPROD','PROD')` then `production_target_id IS NOT NULL`. |
| `environment` | VARCHAR(32) NOT NULL | CHECK `IN ('LAB','PREPROD','PROD')` |
| `network_domain` | VARCHAR(64) NOT NULL | |
| `approved_fqdn` | VARCHAR(255) NOT NULL | Synthetic test FQDN only; no real production seed |
| `approved_port` | INTEGER NOT NULL CHECK (1–65535) | |
| `tls_server_identity` | VARCHAR(255) NOT NULL | Expected server identity |
| `route_zone_id` | VARCHAR(128) NOT NULL | Egress / private-route class |
| `vendor` / `platform` | VARCHAR(32) NOT NULL | CHECK `vendor IN ('ERICSSON')` and `platform IN ('ENM')` for Phase 17 write profiles |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED')` |
| `effective_from` / `effective_until` | TIMESTAMPTZ | |
| `created_at` / `created_by` | | |

Unique current ACTIVE version per `(production_target_id)` via partial unique index `WHERE status = 'ACTIVE'`.  
CHECK: `status IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED')`.  
CHECK: if `status = 'ACTIVE' AND environment IN ('PREPROD','PROD')` then `production_target_id IS NOT NULL`.  
Material change of FQDN/port/TLS identity/route ⇒ new version + invalidation cascade.  
No IP column required. No request-time override column.

### 6.5 `production_tls_profile`

| Column | Type | Notes |
|---|---|---|
| `tls_profile_version_id` | UUID PK | |
| `tls_profile_id` | UUID NOT NULL | |
| `version_no` | INTEGER NOT NULL | |
| `content_digest` | CHAR(64) NOT NULL | |
| `production_target_id` | VARCHAR(128) NULL FK `production_network_target(target_id)` | NULL only when `status = 'DRAFT'`. CHECK: if `status IN ('ACTIVE')` and profile is PREPROD/PRODUCTION-eligible then `production_target_id IS NOT NULL`. |
| `hostname_verification_required` | BOOLEAN NOT NULL DEFAULT TRUE | CHECK: if `status <> 'DRAFT' AND` this profile is bound into any PREPROD or PRODUCTION bundle, value MUST be TRUE. Default TRUE in every environment. No eligible PREPROD/PRODUCTION profile may set this FALSE. |
| `trust_store_profile_ref` | VARCHAR(256) NOT NULL | Reference only |
| `minimum_tls_policy` | VARCHAR(32) NOT NULL | CHECK `IN ('TLS_1_2','TLS_1_3')` |
| `cipher_policy_ref` | VARCHAR(128) NULL | Central policy reference |
| `mtls_required` | BOOLEAN NOT NULL DEFAULT FALSE | |
| `client_certificate_profile_ref` | VARCHAR(256) NULL | Reference only; no PEM |
| `server_identity_expectation` | VARCHAR(255) NOT NULL | |
| `rotation_policy` | VARCHAR(128) NULL | |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')` |
| `effective_from` / `effective_until` | | |

Forbidden rows: trust-all, hostname-verify false on any PREPROD/PRODUCTION-eligible profile, plaintext fallback.  
Partial unique: one `ACTIVE` per `tls_profile_id`.  
CHECK: if `status = 'ACTIVE'` and profile is referenced by a PREPROD/PROD bundle, `hostname_verification_required = TRUE` AND `production` eligibility implies no plaintext.  
A LAB profile with relaxed rules MUST NOT be referenced by a PREPROD/PRODUCTION bundle. Promotion requires a **new** compliant TLS profile version. Default hostname verification TRUE in LAB as well.

### 6.6 `production_network_policy_profile`

| Column | Type | Notes |
|---|---|---|
| `network_policy_profile_version_id` | UUID PK | |
| `network_policy_profile_id` | UUID NOT NULL | |
| `version_no` / `content_digest` | | |
| `gateway_workload_id` | VARCHAR(128) NOT NULL | Write-gateway identity class |
| `production_target_id` | VARCHAR(128) NULL FK `production_network_target(target_id)` | NULL only when `status = 'DRAFT'` |
| `destination_identity` | VARCHAR(255) NOT NULL | Approved FQDN class |
| `destination_port` | INTEGER NOT NULL CHECK (1–65535) | |
| `dns_requirement` | VARCHAR(128) NOT NULL | |
| `private_route_class` | VARCHAR(128) NOT NULL | |
| `allowed_egress_scope` | VARCHAR(256) NOT NULL | CHECK NOT `0.0.0.0/0` when `status = 'ACTIVE'` AND environment class PREPROD/PROD |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED')` |
| `effective_from` / `effective_until` | | |

Partial unique: one `ACTIVE` per `(network_policy_profile_id)` WHERE `status = 'ACTIVE'`.
CHECK: if `status = 'ACTIVE'` and the profile is PREPROD/PRODUCTION-eligible then `production_target_id IS NOT NULL`.

### 6.7 `vendor_write_transport_profile`

| Column | Type | Notes |
|---|---|---|
| `transport_profile_version_id` | UUID PK | |
| `transport_profile_id` | UUID NOT NULL | |
| `version_no` / `content_digest` | | |
| `vendor` / `platform` | VARCHAR(32) NOT NULL | CHECK `vendor IN ('ERICSSON')` AND `platform IN ('ENM')` |
| `interface_definition_version_id` | UUID NOT NULL FK `vendor_interface_definition(interface_definition_version_id)` | |
| `vendor_version_predicate` | VARCHAR(512) NOT NULL | |
| `transport_implementation_version` | VARCHAR(64) NOT NULL | |
| `artifact_digest` | CHAR(64) NOT NULL | Authoritative packaged digest |
| `security_cert_version_id` | UUID NOT NULL FK `vendor_security_certification(security_cert_version_id)` | Replaces conceptual `security_profile_version_id`. |
| `credential_profile_version_id` | UUID NOT NULL FK `production_credential_profile(credential_profile_version_id)` | Metadata only |
| `capability_cert_version_id` | UUID NULL FK `vendor_capability_certification(capability_cert_version_id)` | NULL while `status='DRAFT'` until capability cert exists (breaks insert cycle). NOT NULL once `certification_state` leaves `DRAFT`. Replaces conceptual `capability_profile_version_id`. |
| `tls_profile_version_id` | UUID NOT NULL FK `production_tls_profile(tls_profile_version_id)` | |
| `network_policy_profile_version_id` | UUID NOT NULL FK `production_network_policy_profile(network_policy_profile_version_id)` | |
| `expected_state_strategy` | VARCHAR(32) NOT NULL | CHECK `IN ('READ_THEN_WRITE','ATOMIC')`. Stores `ExpectedStateGuardStrength` **name**. Do not add a parallel type. |
| `atomic_certified` | BOOLEAN NOT NULL DEFAULT FALSE | TRUE only when ACTIVE `EXPECTED_STATE` evidence proves vendor CAS. Default FALSE. |
| `mutation_strategy` | VARCHAR(64) NOT NULL | Abstract; no protocol fields |
| `readback_strategy` | VARCHAR(64) NOT NULL | Abstract |
| `rollback_strategy` | VARCHAR(64) NOT NULL | Abstract |
| `timeout_policy` | VARCHAR(256) NOT NULL | JSON policy, no secrets |
| `retry_policy` | VARCHAR(256) NOT NULL | PRE-SEND / POSITIVE_NOT_SENT only. Mutation-path retry MUST be `NONE`. |
| `supported_object_types` | VARCHAR(64) NOT NULL | CHECK `CELL` |
| `supported_parameters` | VARCHAR(64) NOT NULL | CHECK `txPower` |
| `certification_state` | VARCHAR(32) NOT NULL | CHECK same set as `transport_certification.state` |
| `certification_expiry` | TIMESTAMPTZ NULL | |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')` |
| `effective_from` / `effective_until` | | |

Do **not** create `protocol_profile_id` or Ericsson protocol columns. Abstract interface/security/transport-profile FKs above are sufficient.

CHECK: `expected_state_strategy = 'ATOMIC'` is allowed only if `atomic_certified = TRUE`.  
Once `certification_state` ∈ {`LAB_CERTIFIED`,`PREPROD_CERTIFIED`,`PRODUCTION_REGISTERED`}, certified columns are immutable. Partial unique: one `ACTIVE` per `transport_profile_id`.

### 6.8 `vendor_transport_artifact`

| Column | Type | Authority |
|---|---|---|
| `artifact_id` | UUID PK | |
| `artifact_digest` | CHAR(64) NOT NULL UNIQUE | **Authoritative** |
| `transport_implementation_version` | VARCHAR(64) NOT NULL | **Authoritative** |
| `source_baseline_sha` | CHAR(40) NOT NULL | **Authoritative** Git object SHA-1. CHECK `source_baseline_sha ~ '^[0-9a-f]{40}$'`. NOT NULL. Future Git object formats require a new versioned column, not silent acceptance. |
| `certification_bundle_version` | VARCHAR(64) NULL | Filled when bound |
| `container_image_digest` | VARCHAR(128) NULL | Informational |
| `jar_digest` | CHAR(64) NULL | Informational |
| `sbom_reference` | VARCHAR(256) NULL | Informational |
| `ci_run_id` | VARCHAR(64) NULL | Informational |
| `build_provenance_reference` | VARCHAR(256) NULL | Informational |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('REGISTERED','CERTIFIED','SUPERSEDED','REVOKED')` |

Do not claim signed provenance unless a later authorized change actually stores signatures.

### 6.9 `vendor_capability_certification`

| Column | Type | Notes |
|---|---|---|
| `capability_cert_version_id` | UUID PK | |
| `capability_cert_id` | UUID NOT NULL | |
| `version_no` / `content_digest` | | |
| `object_type` | VARCHAR(32) NOT NULL | CHECK `CELL` |
| `parameter` | VARCHAR(32) NOT NULL | CHECK `txPower` |
| `addressing_semantics` | VARCHAR(256) NOT NULL | Metadata |
| `parameter_type` | VARCHAR(64) NOT NULL | |
| `unit` | VARCHAR(32) NOT NULL | |
| `valid_range` | VARCHAR(128) NOT NULL | |
| `precision` | VARCHAR(64) NOT NULL | |
| `read_semantics` / `write_semantics` / `rollback_semantics` / `verification_semantics` | VARCHAR(256) | |
| `propagation_delay` | VARCHAR(64) NOT NULL | |
| `eventual_consistency` | VARCHAR(128) NOT NULL | |
| `conditional_write_supported` | BOOLEAN NOT NULL DEFAULT FALSE | |
| `vendor` / `platform` / `version_predicate` | | |
| `transport_profile_version_id` | UUID NOT NULL FK | |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')` |
| `certified_at` / `expires_at` | | |

Partial unique: one `ACTIVE` per `capability_cert_id`.

### 6.10 `vendor_security_certification`

Binds TLS + credential-profile metadata + network-policy versions. No secrets.

| Column | Type | Notes |
|---|---|---|
| `security_cert_version_id` | UUID PK | |
| `security_cert_id` | UUID NOT NULL | |
| `version_no` / `content_digest` | | |
| `tls_profile_version_id` | UUID NOT NULL FK | |
| `network_policy_profile_version_id` | UUID NOT NULL FK | |
| `credential_profile_version_id` | UUID NOT NULL FK | |
| `mtls_required` | BOOLEAN NOT NULL | Must match TLS profile |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')` |
| `certified_at` / `expires_at` | | |

Partial unique: one `ACTIVE` per `security_cert_id`.

### 6.11 `transport_certification`

Lifecycle row for a transport/target-class certification path.

| Column | Type | Notes |
|---|---|---|
| `transport_certification_id` | UUID PK | Logical |
| `current_version_id` | UUID NULL FK `transport_certification_version(transport_certification_version_id)` | NULL until the first version row is inserted in the same transaction; then set. Breaks insert cycle. |
| `vendor` / `platform` | | |
| `transport_profile_version_id` | UUID NOT NULL FK | |
| `state` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','INTERFACE_VERIFIED','LAB_CERTIFICATION_PENDING','LAB_CERTIFIED','PREPROD_CERTIFICATION_PENDING','PREPROD_CERTIFIED','PRODUCTION_REGISTRATION_PENDING','PRODUCTION_REGISTERED','SUSPENDED','EXPIRED','REVOKED')` |
| `environment_class` | VARCHAR(32) NOT NULL | CHECK `IN ('LOCAL','LAB','PREPROD','PROD')` |
| `created_by` | VARCHAR(128) NOT NULL | |
| `updated_at` | TIMESTAMPTZ NOT NULL | |

### 6.12 `transport_certification_version`

Immutable snapshot once state leaves `DRAFT`.

| Column | Type | Notes |
|---|---|---|
| `transport_certification_version_id` | UUID PK | |
| `transport_certification_id` | UUID NOT NULL FK | |
| `version_no` | INTEGER NOT NULL | |
| `content_digest` | CHAR(64) NOT NULL | |
| `state` | VARCHAR(32) NOT NULL | CHECK same set as `transport_certification.state` |
| `interface_definition_version_id` | UUID NOT NULL FK | |
| `bundle_version_id` | UUID NULL FK | Set when bundle issued |
| `artifact_digest` | CHAR(64) NOT NULL | |
| `source_baseline_sha` | CHAR(40) NOT NULL | CHECK `~ '^[0-9a-f]{40}$'` |
| `actor_principal_id` | VARCHAR(128) NOT NULL | Transition actor |
| `created_at` | TIMESTAMPTZ NOT NULL | |

### 6.13 `transport_certification_evidence`

| Column | Type | Notes |
|---|---|---|
| `evidence_id` | UUID PK | |
| `evidence_version` | INTEGER NOT NULL | |
| `certification_subject_type` | VARCHAR(64) NOT NULL | |
| `certification_subject_id` | UUID NOT NULL | |
| `certification_subject_version_id` | UUID NOT NULL | |
| `evidence_type` | VARCHAR(64) NOT NULL | See §11 |
| `environment_level` | VARCHAR(8) NOT NULL | `L0`/`L1`/`L2`/`L3` |
| `issuer_principal_id` | VARCHAR(128) NOT NULL | Authenticated certifier |
| `certifier_permission` | VARCHAR(64) NOT NULL | |
| `result` | VARCHAR(16) NOT NULL | CHECK `IN ('PASS','FAIL','NOT_EXECUTED','NOT_SATISFIED')` |
| `reference` | VARCHAR(512) NOT NULL | External ticket/doc handle |
| `evidence_hash` | CHAR(64) NOT NULL | Hash of referenced artifact, not a lone authority |
| `artifact_binding` | VARCHAR(128) NULL | |
| `created_at` / `effective_at` | TIMESTAMPTZ | |
| `superseded_by` | UUID NULL FK self | |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('ACTIVE','SUPERSEDED','WITHDRAWN','REVOKED')` |

Unique: `(certification_subject_version_id, evidence_type, evidence_version)`.  
At most one `ACTIVE` PASS per `(subject_version, evidence_type)` (partial unique).  
PASS without `issuer_principal_id` + `certifier_permission` is CHECK-forbidden.

### 6.14 `transport_certification_bundle`

Immutable versioned snapshot. No mutable pointers to live mutable rows.

| Column | Type | Notes |
|---|---|---|
| `bundle_version_id` | UUID PK | |
| `bundle_id` | UUID NOT NULL | Logical |
| `version_no` / `content_digest` | | Digest of **all** bound version ids |
| `vendor` / `platform` | | |
| `interface_definition_version_id` | UUID NOT NULL | |
| `interface_approval_id` | UUID NOT NULL | |
| `transport_profile_version_id` | UUID NOT NULL | |
| `artifact_digest` | CHAR(64) NOT NULL | |
| `transport_implementation_version` | VARCHAR(64) NOT NULL | |
| `source_baseline_sha` | CHAR(40) NOT NULL | CHECK `~ '^[0-9a-f]{40}$'` |
| `vendor_version_predicate` | VARCHAR(512) NOT NULL | |
| `capability_cert_version_id` | UUID NOT NULL | |
| `security_cert_version_id` | UUID NOT NULL | |
| `credential_profile_version_id` | UUID NOT NULL | |
| `tls_profile_version_id` | UUID NOT NULL | |
| `network_policy_profile_version_id` | UUID NOT NULL | |
| `endpoint_profile_version_id` | UUID NULL | Required for L3 |
| `target_class` | VARCHAR(32) NOT NULL | CHECK `IN ('LAB','PREPROD','PROD')` |
| `active_evidence_set_digest` | CHAR(64) NOT NULL | Canonical hash of active evidence ids |
| `certifier_principal_id` | VARCHAR(128) NOT NULL | |
| `certified_at` / `expires_at` | TIMESTAMPTZ | |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('ACTIVE','INVALID','EXPIRED','REVOKED')` |

Material component change MUST insert a new bundle version and mark the old `INVALID`.

### 6.15 `production_target_onboarding`

| Column | Type | Notes |
|---|---|---|
| `onboarding_id` | UUID PK | Logical |
| `onboarding_version_id` | UUID NOT NULL | Current version |
| `production_target_id` | VARCHAR(128) NOT NULL FK | Phase 16 target |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','IN_REVIEW','APPROVED','INVALID','SUSPENDED','REVOKED')` |
| `certification_level` | VARCHAR(8) NOT NULL | CHECK `IN ('L0','L1','L2','L3')` |
| `created_by` / `reviewed_by` / `approved_by` | VARCHAR(128) | Distinct principals when set |
| `created_at` / `updated_at` | | |

CHECK: `created_by` ≠ `reviewed_by` ≠ `approved_by` when APPROVED.

### 6.16 `production_target_onboarding_version`

Immutable snapshot of bindings.

| Column | Type | Notes |
|---|---|---|
| `onboarding_version_id` | UUID PK | |
| `onboarding_id` | UUID NOT NULL FK | |
| `version_no` / `content_digest` | | |
| `production_target_id` | VARCHAR(128) NOT NULL | |
| `vendor` / `platform` / `vendor_software_version` | | Observed/declared version string; unknown ⇒ deny at send |
| `interface_definition_version_id` | UUID NOT NULL | |
| `transport_profile_version_id` | UUID NOT NULL | |
| `artifact_digest` | CHAR(64) NOT NULL | |
| `capability_cert_version_id` | UUID NOT NULL | |
| `security_cert_version_id` | UUID NOT NULL | |
| `credential_profile_version_id` | UUID NOT NULL | |
| `tls_profile_version_id` | UUID NOT NULL | |
| `network_policy_profile_version_id` | UUID NOT NULL | |
| `endpoint_profile_version_id` | UUID NOT NULL | |
| `bundle_version_id` | UUID NOT NULL | |
| `change_control_policy` | VARCHAR(256) NOT NULL | Abstract; MANUAL acceptable |
| `verification_policy` / `rollback_policy` / `monitoring_profile` | VARCHAR(256) | |
| `support_owner` | VARCHAR(128) NOT NULL | |
| `environment` / `region` / `network_domain` | | |
| `expires_at` | TIMESTAMPTZ NOT NULL | |
| `created_at` / `created_by` | | |

### 6.17 `production_target_certification`

| Column | Type | Notes |
|---|---|---|
| `target_certification_id` | UUID PK | |
| `production_target_id` | VARCHAR(128) NOT NULL FK | |
| `onboarding_version_id` | UUID NOT NULL FK | |
| `bundle_version_id` | UUID NOT NULL FK | |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('CURRENT','STALE','INVALID','SUSPENDED','EXPIRED','REVOKED')` |
| `certified_at` / `expires_at` | | |
| `content_digest` | CHAR(64) NOT NULL | |

Partial unique: one `CURRENT` row per `production_target_id`.

### 6.18 `vendor_version_compatibility`

| Column | Type | Notes |
|---|---|---|
| `compatibility_id` | UUID PK | |
| `vendor` / `platform` | | |
| `transport_profile_version_id` | UUID NOT NULL FK | |
| `version_predicate` | VARCHAR(512) NOT NULL | Explicit; no implicit SemVer expand |
| `evidence_id` | UUID NOT NULL FK | |
| `certified_at` / `expires_at` | | |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('ACTIVE','SUSPENDED','EXPIRED','REVOKED')` |

Range expansion = new row + recertification. UPDATE of predicate on ACTIVE row is FORBIDDEN.

### 6.19 `vendor_transport_health`

| Column | Type | Notes |
|---|---|---|
| `health_id` | UUID PK | |
| `production_target_id` | VARCHAR(128) NOT NULL FK | |
| `transport_profile_version_id` | UUID NOT NULL | |
| `health_state` | VARCHAR(32) NOT NULL | CHECK `IN ('HEALTHY','DEGRADED','UNAVAILABLE','SECURITY_FAILURE','CAPABILITY_MISMATCH','VERSION_MISMATCH','SUSPENDED')` |
| `source` | VARCHAR(32) NOT NULL | CHECK `IN ('OBSERVATION','POLICY','HUMAN')` |
| `detail_code` | VARCHAR(128) NOT NULL | No secrets |
| `observed_at` | TIMESTAMPTZ NOT NULL | |
| `requires_human_reactivation` | BOOLEAN NOT NULL | |

One current row per target+profile (upsert of current observation). History MAY be appended to `vendor_transport_health_event` (optional; recommended).

### 6.20 `production_credential_profile`

Metadata only. Phase 10 remains secret-value authority.

| Column | Type | Notes |
|---|---|---|
| `credential_profile_version_id` | UUID PK | |
| `credential_profile_id` | UUID NOT NULL | |
| `version_no` / `content_digest` | | |
| `production_target_id` | VARCHAR(128) NULL FK `production_network_target(target_id)` | NULL only when `status = 'DRAFT'`. CHECK: if `status = 'ACTIVE'` then `production_target_id IS NOT NULL`. |
| `vendor` / `platform` | VARCHAR(32) NOT NULL | CHECK `vendor IN ('ERICSSON')` AND `platform IN ('ENM')` |
| `secret_reference` | VARCHAR(256) NOT NULL | Name/URI metadata only |
| `workload_identity_profile` | VARCHAR(128) NOT NULL | Write-gateway WI class |
| `status` | VARCHAR(32) NOT NULL | CHECK `IN ('DRAFT','ACTIVE','SUPERSEDED','REVOKED','EXPIRED')` |
| `effective_from` / `effective_until` | | |

No generic vendor-wide write credential row is eligible for PREPROD/PROD unless `production_target_id` is bound **and** the bundle certifies that exact binding. Partial unique: one `ACTIVE` per `(credential_profile_id)` WHERE `status = 'ACTIVE'`. Partial unique: one `ACTIVE` credential profile per `production_target_id` WHERE `status = 'ACTIVE'`.

### 6.21 Certification audit tables

Separate from Phase 16 `production_change_audit_event`.

```text
phase17_certification_audit_event
  event_id UUID PK
  subject_type / subject_id / subject_version_id
  event_type VARCHAR(64)
  actor_principal_id
  sequence_number BIGINT
  previous_event_hash CHAR(64)
  event_hash CHAR(64)
  payload_canonical TEXT   -- no secrets
  created_at
```

Chain scope: one chain per `subject_type + subject_id` (logical). Genesis:

```text
SHA-256("SNIP-PHASE17-CERTIFICATION-AUDIT-GENESIS-v1")
```

Canonical JSON rules match Phase 16 (UTF-8, sorted keys, no insignificant whitespace, omit nulls).

### 6.22 Relationship to Phase 16 records

| Phase 17 | Phase 16 |
|---|---|
| `production_target_id` | FK `production_network_target.target_id` |
| Invalidation | Existing Phase 16 `ProductionAuthorizationService.markStale` + existing Phase 16 grant authority `revokeIssued` / ISSUED-predicate revoke. See §18.1. |
| Send-boundary | After grant consume, before `EricssonWriteTransport.transmitMutation` |
| Fingerprint | Not redesigned; STALE via existing authorization generation |
| Grants | Existing Phase 16 grant authority only. See §18.1. |
| Kill switch / lease / rate | Unchanged tables |

Backfill: every existing target is treated as **no CURRENT `production_target_certification`**. Gateway DENY on Phase 17 checks.


### 6.23 Foreign-key catalogue (safety-significant)

All of the following use `ON DELETE RESTRICT` / `ON UPDATE RESTRICT` (NO ACTION equivalent). Do not CASCADE-delete certified history.

| FK field | Source table | Target table | Target column | ON DELETE |
|---|---|---|---|---|
| `abstract_protocol_placeholder_version_id` | `vendor_interface_definition` | `vendor_abstract_protocol_placeholder` | `placeholder_version_id` | RESTRICT |
| `security_cert_version_id` | `vendor_interface_definition` | `vendor_security_certification` | `security_cert_version_id` | RESTRICT |
| `capability_cert_version_id` | `vendor_interface_definition` | `vendor_capability_certification` | `capability_cert_version_id` | RESTRICT |
| `supersedes_version_id` | `vendor_interface_definition` | `vendor_interface_definition` | `interface_definition_version_id` | RESTRICT |
| `interface_definition_version_id` | `vendor_interface_approval` | `vendor_interface_definition` | `interface_definition_version_id` | RESTRICT |
| `production_target_id` | `production_endpoint_profile` | `production_network_target` | `target_id` | RESTRICT |
| `production_target_id` | `production_tls_profile` | `production_network_target` | `target_id` | RESTRICT |
| `production_target_id` | `production_network_policy_profile` | `production_network_target` | `target_id` | RESTRICT |
| `interface_definition_version_id` | `vendor_write_transport_profile` | `vendor_interface_definition` | `interface_definition_version_id` | RESTRICT |
| `security_cert_version_id` | `vendor_write_transport_profile` | `vendor_security_certification` | `security_cert_version_id` | RESTRICT |
| `credential_profile_version_id` | `vendor_write_transport_profile` | `production_credential_profile` | `credential_profile_version_id` | RESTRICT |
| `capability_cert_version_id` | `vendor_write_transport_profile` | `vendor_capability_certification` | `capability_cert_version_id` | RESTRICT |
| `tls_profile_version_id` | `vendor_write_transport_profile` | `production_tls_profile` | `tls_profile_version_id` | RESTRICT |
| `network_policy_profile_version_id` | `vendor_write_transport_profile` | `production_network_policy_profile` | `network_policy_profile_version_id` | RESTRICT |
| `tls_profile_version_id` | `vendor_security_certification` | `production_tls_profile` | `tls_profile_version_id` | RESTRICT |
| `network_policy_profile_version_id` | `vendor_security_certification` | `production_network_policy_profile` | `network_policy_profile_version_id` | RESTRICT |
| `credential_profile_version_id` | `vendor_security_certification` | `production_credential_profile` | `credential_profile_version_id` | RESTRICT |
| `transport_profile_version_id` | `vendor_capability_certification` | `vendor_write_transport_profile` | `transport_profile_version_id` | RESTRICT |
| `transport_profile_version_id` | `transport_certification` | `vendor_write_transport_profile` | `transport_profile_version_id` | RESTRICT |
| `current_version_id` | `transport_certification` | `transport_certification_version` | `transport_certification_version_id` | RESTRICT |
| `transport_certification_id` | `transport_certification_version` | `transport_certification` | `transport_certification_id` | RESTRICT |
| `interface_definition_version_id` | `transport_certification_version` | `vendor_interface_definition` | `interface_definition_version_id` | RESTRICT |
| `bundle_version_id` | `transport_certification_version` | `transport_certification_bundle` | `bundle_version_id` | RESTRICT |
| `interface_definition_version_id` | `transport_certification_bundle` | `vendor_interface_definition` | `interface_definition_version_id` | RESTRICT |
| `interface_approval_id` | `transport_certification_bundle` | `vendor_interface_approval` | `approval_id` | RESTRICT |
| `transport_profile_version_id` | `transport_certification_bundle` | `vendor_write_transport_profile` | `transport_profile_version_id` | RESTRICT |
| `capability_cert_version_id` | `transport_certification_bundle` | `vendor_capability_certification` | `capability_cert_version_id` | RESTRICT |
| `security_cert_version_id` | `transport_certification_bundle` | `vendor_security_certification` | `security_cert_version_id` | RESTRICT |
| `credential_profile_version_id` | `transport_certification_bundle` | `production_credential_profile` | `credential_profile_version_id` | RESTRICT |
| `tls_profile_version_id` | `transport_certification_bundle` | `production_tls_profile` | `tls_profile_version_id` | RESTRICT |
| `network_policy_profile_version_id` | `transport_certification_bundle` | `production_network_policy_profile` | `network_policy_profile_version_id` | RESTRICT |
| `endpoint_profile_version_id` | `transport_certification_bundle` | `production_endpoint_profile` | `endpoint_profile_version_id` | RESTRICT |
| `production_target_id` | `production_target_onboarding` | `production_network_target` | `target_id` | RESTRICT |
| `onboarding_id` | `production_target_onboarding_version` | `production_target_onboarding` | `onboarding_id` | RESTRICT |
| `bundle_version_id` | `production_target_onboarding_version` | `transport_certification_bundle` | `bundle_version_id` | RESTRICT |
| `production_target_id` | `production_target_certification` | `production_network_target` | `target_id` | RESTRICT |
| `onboarding_version_id` | `production_target_certification` | `production_target_onboarding_version` | `onboarding_version_id` | RESTRICT |
| `bundle_version_id` | `production_target_certification` | `transport_certification_bundle` | `bundle_version_id` | RESTRICT |
| `transport_profile_version_id` | `vendor_version_compatibility` | `vendor_write_transport_profile` | `transport_profile_version_id` | RESTRICT |
| `evidence_id` | `vendor_version_compatibility` | `transport_certification_evidence` | `evidence_id` | RESTRICT |
| `production_target_id` | `vendor_transport_health` | `production_network_target` | `target_id` | RESTRICT |
| `transport_profile_version_id` | `vendor_transport_health` | `vendor_write_transport_profile` | `transport_profile_version_id` | RESTRICT |
| `production_target_id` | `production_credential_profile` | `production_network_target` | `target_id` | RESTRICT |

Insert order (no cycle): abstract protocol placeholder → TLS / network / credential / endpoint profiles → security certification → interface definition version (`DRAFT` or `INTERFACE_VERIFIED`, capability FK NULL) → interface approval (FK to that interface version) → transport profile `DRAFT` with `capability_cert_version_id` NULL → capability certification (FK to transport profile) → update transport profile `capability_cert_version_id` while still `DRAFT` → transport certification row (`current_version_id` NULL) → transport certification version → set `current_version_id` → evidence → bundle → onboarding version → target certification. Do not insert a reverse `approval_id` on the interface row.

### 6.24 `phase17_invalidation_event` (idempotency)

| Column | Type | Notes |
|---|---|---|
| `invalidation_event_id` | UUID PK | |
| `idempotency_key` | CHAR(64) NOT NULL UNIQUE | SHA-256 of canonical trigger identity |
| `trigger_type` | VARCHAR(64) NOT NULL | CHECK enum of §18.6 trigger names |
| `source_table` | VARCHAR(128) NOT NULL | |
| `source_logical_id` | VARCHAR(128) NOT NULL | |
| `source_version_id` | UUID NULL | |
| `new_status` | VARCHAR(32) NOT NULL | |
| `effective_at` | TIMESTAMPTZ NOT NULL | |
| `processed_at` | TIMESTAMPTZ NOT NULL | |
| `actor_principal_id` | VARCHAR(128) NOT NULL | |

Idempotency key canonical bytes (UTF-8, `|` delimiter, empty string for null version): `triggerType|sourceLogicalId|sourceVersionId|newStatus|effectiveAt` (UTC ISO-8601). Repeat insert of the same key MUST be a no-op success (same denied durable state). MUST NOT resurrect rows, create grants, or emit a second active audit authority.

Optional outbox `phase17_invalidation_outbox` MAY store cache/alert payloads **in the same transaction** and be published **after commit**. Outbox/events are never execution permission.

### 6.25 Hash / digest classes

| Class | Storage | CHECK | Applies to |
|---|---|---|---|
| New Phase 17 content digest | `CHAR(64)` | `~ '^[0-9a-f]{64}$'` | `content_digest`, `artifact_digest`, `evidence_hash`, `active_evidence_set_digest`, `event_hash`, `idempotency_key` |
| New Phase 17 Git source baseline | `CHAR(40)` | `~ '^[0-9a-f]{40}$'` | `source_baseline_sha` |
| Inherited Phase 14/15/16 fingerprints | existing columns | **Do not add Phase 17 CHECKs** unless frozen format already matches | Phase 16 fingerprint / grant / authorization tables |
| Opaque IDs | UUID / VARCHAR | not a hash | `*_id`, principal ids, secret references |

Uppercase hex in **new** Phase 17 canonical digest fields MUST be rejected.

### 6.26 Bundle content-digest canonicalization

`transport_certification_bundle.content_digest` = SHA-256 (lowercase 64 hex) over canonical bytes of **immutable identifiers only**, not display text.

Canonical serialization:

1. UTF-8
2. Fixed field order below
3. Each field encoded as `name` UTF-8, then unsigned 32-bit big-endian length, then value bytes
4. NULL: length `0xFFFFFFFF` and zero value bytes (distinct from empty string length `0`)
5. Timestamps if ever added: UTC `YYYY-MM-DDTHH:MM:SS.sssZ` only; this digest currently **excludes** timestamps and certifier display names
6. No locale, no JSON key reordering, no insignificant whitespace

Exact fields in order:

```text
bundle_id
version_no
vendor
platform
interface_definition_version_id
interface_approval_id
transport_profile_version_id
artifact_digest
transport_implementation_version
source_baseline_sha
vendor_version_predicate
capability_cert_version_id
security_cert_version_id
credential_profile_version_id
tls_profile_version_id
network_policy_profile_version_id
endpoint_profile_version_id
target_class
active_evidence_set_digest
```

`active_evidence_set_digest` is SHA-256 of sorted unique ACTIVE evidence UUIDs (canonical hex lowercase, comma-separated, UTF-8) computed first.

Tests (T17-DB-030, T17-IMPL-069/070): same bundle → same digest; source-object field-order variation → same digest; one bound version id changes → different digest; null vs empty `endpoint_profile_version_id` → distinct; uppercase digest input → reject.

Partial unique: one `ACTIVE` bundle version per `bundle_id` WHERE `status = 'ACTIVE'`.

---

## 7. Domain types and repositories

### 7.1 Core records

`VendorInterfaceDefinition`, `VendorInterfaceApproval`, `VendorWriteTransportProfile`, `VendorTransportArtifact`, `CertifiedVendorCapability`, `VendorSecurityCertification`, `TransportCertification`, `TransportCertificationEvidence`, `TransportCertificationBundle`, `ProductionTargetOnboardingRecord`, `ProductionTargetCertification`, `VendorVersionCompatibility`, `VendorTransportHealth`, `ProductionEndpointProfile`, `ProductionTlsProfile`, `ProductionNetworkPolicyProfile`, `ProductionCredentialProfile`.

### 7.2 Repositories (app write / gateway read)

App: Spring Data JPA repositories under the packages in §4.1.  
Gateway: JDBC readers inside `ProductionCertificationAuthority` (same pattern as `ProductionGrantConsumeService`). Gateway MUST NOT use JPA write repositories for certification rows.

### 7.3 Required app services

| Service | Responsibility |
|---|---|
| `VendorInterfaceDefinitionService` | Create versions; no protocol fields |
| `VendorInterfaceApprovalService` | Approve/revoke; cascade |
| `TransportCertificationLifecycleService` | Legal transitions only |
| `TransportCertificationEvidenceService` | Add/supersede/withdraw evidence |
| `TransportCertificationBundleService` | Issue immutable bundles |
| `VendorCapabilityCertificationService` | CELL/txPower only |
| `VendorSecurityCertificationService` | TLS/network/credential metadata |
| `VendorVersionCompatibilityService` | Predicate bind; no auto-expand |
| `CertificationInvalidationService` | §18 owner of the **single** invalidation database transaction. Calls Phase 16 authorization/grant services in the **same** transaction. |
| `ProductionTargetOnboardingService` | Create/review/approve/suspend/reactivate |
| `ProductionEndpointProfileService` | Versioned endpoint identity |
| `Phase17CertificationAuditService` | Tamper-evident Phase 17 chain |
| `Phase17CertificationMetrics` | Low-cardinality meters |

### 7.4 Required gateway services

| Service | Responsibility |
|---|---|
| `ProductionCertificationAuthority` | Atomic/current durable reads |
| `RuntimeTransportArtifactIdentityProvider` | Deployed artifact identity |
| `CertifiedTransportResolver` | Resolve only CURRENT certified profile |
| `CertificationSendBoundaryPreflight` | §15 algorithm |
| `DestinationTrustValidator` | Hostname/port/TLS/egress hooks. Production uses real TLS/hostname verification. Tests use `TestDestinationIdentityDouble` only. |
| `TransportHealthComposer` | AND composition |
| `Phase17CredentialResolutionBinder` | Target + profile + WI bind before Phase 10 resolve |

---

## 8. Certification lifecycle

Enum `TransportCertificationState`:

```text
DRAFT
INTERFACE_VERIFIED
LAB_CERTIFICATION_PENDING
LAB_CERTIFIED
PREPROD_CERTIFICATION_PENDING
PREPROD_CERTIFIED
PRODUCTION_REGISTRATION_PENDING
PRODUCTION_REGISTERED
SUSPENDED
EXPIRED
REVOKED
```

`TransportCertificationLifecycleService.transition(from, to, actor, evidenceSet)` MUST reject unknown pairs.

| From | To | Required permission(s) | Independent actor predicate | Evidence prerequisites | SoD | Auto | Reversible | Recertify | Execution |
|---|---|---|---|---|---|---|---|---|---|
| none | DRAFT | `TRANSPORT_CERTIFY` | creator principal non-null/non-blank | draft record | N/A | No | N/A | N/A | NO |
| DRAFT | INTERFACE_VERIFIED | `VENDOR_INTERFACE_REVIEW` | reviewer ≠ draft creator | approved interface **version** row | CREATE ≠ REVIEW | No | No | N/A | NO |
| INTERFACE_VERIFIED | LAB_CERTIFICATION_PENDING | `TRANSPORT_CERTIFY` | actor ≠ INTERFACE reviewer optional | L1 plan bound to that interface version | N/A | No | Withdraw→DRAFT only | N/A | NO |
| LAB_CERTIFICATION_PENDING | LAB_CERTIFIED | `TRANSPORT_CERTIFY` **and** `CAPABILITY_CERTIFY` **and** `SECURITY_CERTIFY` (all three mandatory; no optional security certification) | `TRANSPORT_CERTIFY` and `CAPABILITY_CERTIFY` MAY be the same principal for this lab campaign. `SECURITY_CERTIFY` MAY be the same principal as `TRANSPORT_CERTIFY` **only** at LAB (architecture requires `SECURITY_CERTIFY` ≠ `TRANSPORT_CERTIFY` at `PRODUCTION_REGISTERED`). | L1 evidence set: lab mutation, readback, rollback, failures (`INTERFACE_DOCUMENTATION`,`SECURITY`,`CONNECTIVITY`,`CAPABILITY`,`MUTATION`,`EXPECTED_STATE`,`VERIFICATION`,`ROLLBACK`,`FAILURE_INJECTION`) | complete-stack self-certify still forbidden for later production registration | No | No | New L1 if material | NO (lab only) |
| LAB_CERTIFIED | PREPROD_CERTIFICATION_PENDING | `TRANSPORT_CERTIFY` | N/A | L2 plan | N/A | No | No | N/A | NO |
| PREPROD_CERTIFICATION_PENDING | PREPROD_CERTIFIED | `TRANSPORT_CERTIFY` **and** `CAPABILITY_CERTIFY` **and** `SECURITY_CERTIFY` | `SECURITY_CERTIFY` principal ≠ `TRANSPORT_CERTIFY` principal. `CAPABILITY_CERTIFY` MAY equal `TRANSPORT_CERTIFY`. | L2 production-equivalence evidence set | independent certifiers | No | No | New L2 if material | NO |
| PREPROD_CERTIFIED | PRODUCTION_REGISTRATION_PENDING | `TARGET_ONBOARD_CREATE` | creator ≠ production executor for the target if any grant exists | onboarding draft bound to certified versions | CREATE starts onboarding SoD | No | Withdraw pending | N/A | NO |
| PRODUCTION_REGISTRATION_PENDING | PRODUCTION_REGISTERED | `TARGET_ONBOARD_REVIEW` then `TARGET_ONBOARD_APPROVE` | REVIEW ≠ APPROVE ≠ CREATE; APPROVE ≠ production authorizer; APPROVE ≠ production executor; `SECURITY_CERTIFY` ≠ `TRANSPORT_CERTIFY` on the bound stack; no principal holds the full cert+approve set | L3 registration evidence; **zero** production mutation | CREATE ≠ REVIEW ≠ APPROVE | No | No | New L3 if material | NO |
| any certified | SUSPENDED | policy auto **or** `TARGET_SUSPEND` | human suspender ≠ executor if human | trigger record | N/A | Yes or human | Only `TARGET_REACTIVATE` | If cause requires | NO |
| any certified | EXPIRED | clock / `expiresAt` (system) | N/A | expiry | N/A | Yes | No auto return | Required | NO |
| any except none | REVOKED | policy auto **or** `TRANSPORT_CERTIFY` / `SECURITY_CERTIFY` / `TARGET_ONBOARD_APPROVE` as owner of the revoked subject | N/A | revoke cause | N/A | Yes or human | NEVER auto | New DRAFT path | NO |
| SUSPENDED | PRODUCTION_REGISTERED | `TARGET_REACTIVATE` | reactivator ≠ executor; ≠ CREATE of the suspended onboarding if that would collapse SoD | cause resolved + current certs | human only | **Human only** | N/A | If security/capability/version | NO until L4 |
| EXPIRED | PRODUCTION_REGISTERED | `TRANSPORT_CERTIFY` + `CAPABILITY_CERTIFY` + `SECURITY_CERTIFY` + `TARGET_ONBOARD_APPROVE` | `SECURITY_CERTIFY` ≠ `TRANSPORT_CERTIFY`; APPROVE ≠ CREATE; APPROVE ≠ executor/authorizer | renewal bundle | same as production registration | Human only | N/A | Required | NO until L4 |
| REVOKED | any prior certified | none — transition forbidden | — | — | — | Forbidden | Forbidden | New path only | NO |

Audit every transition. Automatic production re-enable is forbidden.

`PRODUCTION_REGISTERED` MUST NOT be treated as execution eligibility (G17-021).

---

## 9. Level model

| Level | Persistence | Meaning |
|---|---|---|
| L0 | Local tests / default CI | Protocol-independent safety |
| L1 | External evidence rows, result `NOT_EXECUTED` until later | Vendor lab |
| L2 | External evidence rows, `NOT_EXECUTED` | Operator pre-prod |
| L3 | Onboarding `certification_level='L3'` + CURRENT target cert | Registration only |
| L4 | **Not persisted on target** | `Level4Satisfied(executionId)` |

```text
Level4Satisfied(executionId) iff
  current Phase16 production authorization exists for that execution
  AND fingerprint + authorizationGeneration still match
  AND authorization not expired/revoked/stale
  AND grant consumed for this attempt is the bound grant
  AND all Phase17 certification prerequisites are current
```

No `level4=true` column. No reusable L4 token beyond the existing Phase 16 grant.

---

## 10. Target onboarding

`ProductionTargetOnboardingService` binds the immutable versions listed in §6.16. Callers MUST NOT supply endpoint or credential values. APIs accept only SNIP identifiers of already-registered profiles.

SoD: CREATE ≠ REVIEW ≠ APPROVE. Executor MUST NOT create/review/approve/reactivate its own target onboarding. Compare executor principal of any ISSUED/CONSUMED grant for that `productionTargetId` against onboarding actors.

`ADMINISTER_PRODUCTION_TARGET` MUST NOT satisfy onboarding approve. Phase 16 target admin may still suspend/disable the Phase 16 target; Phase 17 `TARGET_SUSPEND` is the certification-plane suspend and MUST also run invalidation.

---

## 11. Evidence and bundle model

Evidence types (minimum):

```text
INTERFACE_DOCUMENTATION
SECURITY
CONNECTIVITY
CAPABILITY
MUTATION
EXPECTED_STATE
VERIFICATION
ROLLBACK
AMBIGUOUS_OUTCOME
FAILURE_INJECTION
PERFORMANCE
OPERATIONS
AUDIT
MONITORING
ARTIFACT_INTEGRITY
TARGET_ONBOARDING
VERSION_COMPATIBILITY
```

Trust rule: hash alone is not PASS. Required: authenticated certifier, permission, durable row, subject/version/environment bind, audit event, result, timestamp, reference/hash.

Supersession: new ACTIVE evidence supersedes previous ACTIVE of same type; old PASS is historical only. Failed recertification MUST revoke/supersede old PASS and invalidate the bundle.

Bundle MUST bind exact versions including TLS and network-policy profile versions (G17-157).

---

## 12. Artifact identity

Authoritative certified identity (durable bundle/profile): `artifactDigest`, `transportImplementationVersion`, `sourceBaselineSha`, `certificationBundleVersion`.

**Authoritative runtime identity** MUST be derived from an **artifact-bound** source, not from arbitrary application configuration.

Required implementation:

1. Build generates an immutable manifest packaged inside the gateway artifact/image at `/META-INF/snip-transport-artifact.json` (or equivalent classpath resource hashed into the image). Minimum fields: `transportImplementationVersion`, `sourceBaselineSha`, `artifactDigest` or build identity, `buildProvenanceReference` if available.
2. The gateway computes/verifies runtime identity from that packaged manifest (hash the packaged bytes with SHA-256; compare to the certified `artifactDigest`).
3. The deployment MAY **additionally** expose an immutable container image digest from the orchestrator/runtime identity plane. If present, it is a second independent check; mismatch ⇒ DENY.
4. Free-form environment/configuration variables MAY identify deployment **context** (cluster, namespace, replica). They MUST NOT independently establish artifact authenticity. A config claim of digest A while packaged identity is B ⇒ DENY (`P17_ARTIFACT_MISMATCH`).

Forbidden as sole proof: `SNIP_WRITE_GATEWAY_ARTIFACT_DIGEST=<expected>` or any operator-supplied expected-digest environment variable.

Informational only (not sufficient): SBOM handle, CI run id, unsigned provenance reference, hostname, clock.

Local CI test double: `PackagedRuntimeArtifactIdentityProvider` reads a test-classpath manifest fixture. It MUST NOT be the production provider. Production uses the packaged-in-image provider.

Tests:

- certified A + runtime A → eligible subject to all other controls (T17-INT-037)
- certified A + runtime B → DENY (CS17-X, FI17-011)
- missing runtime identity → DENY (T17-IMPL-064)
- malformed runtime identity → DENY (T17-IMPL-065)
- config claims A but packaged identity is B → DENY (T17-INT-036, T17-SEC-029)

Do not derive identity from hostname or clock. Startup mismatch: mark `CAPABILITY_MISMATCH`/`SECURITY_FAILURE` and remain fail-closed. Before mutation: compare runtime identity to current bundle; mismatch ⇒ `P17_ARTIFACT_MISMATCH`, health update, alert, audit, DENY, mutation count 0.

---

## 13. SoD and permissions

Add to a new `Phase17CertificationPermission` (do not overload Phase 16 execute permissions):

```text
VENDOR_INTERFACE_REVIEW
TRANSPORT_CERTIFY
CAPABILITY_CERTIFY
SECURITY_CERTIFY
TARGET_ONBOARD_CREATE
TARGET_ONBOARD_REVIEW
TARGET_ONBOARD_APPROVE
TARGET_SUSPEND
TARGET_REACTIVATE
VIEW_CERTIFICATION_STATUS
```

Combinability:

- `TRANSPORT_CERTIFY` + `CAPABILITY_CERTIFY` MAY be same principal for one lab campaign.
- `SECURITY_CERTIFY` MUST ≠ `TRANSPORT_CERTIFY` for `PRODUCTION_REGISTERED`.
- CREATE ≠ REVIEW ≠ APPROVE for the same onboarding.
- Final APPROVE MUST ≠ CREATE, and ≠ production authorizer/executor for that target.
- No principal MAY hold all of `VENDOR_INTERFACE_REVIEW` + `TRANSPORT_CERTIFY` + `CAPABILITY_CERTIFY` + `SECURITY_CERTIFY` + `TARGET_ONBOARD_APPROVE` for the same production registration.
- Agents/MCP: none of the above except optional later `VIEW_CERTIFICATION_STATUS` if explicitly granted; default deny.

Reuse `ActorPrincipal` + permission guard style from Phase 16. Extend `ProductionSeparationOfDutiesPolicy` or add `Phase17SeparationOfDutiesPolicy` that calls the same distinct-principal primitive.

Optional Agent `VIEW_CERTIFICATION_STATUS` remains **default-deny** and does not authorize mutation, certification, onboarding, or grant operations. Do not expand Agent permissions.

### 13.1 Fail-closed principal validation (policy/service layer)

For every security-sensitive certification/onboarding operation, `Phase17SeparationOfDutiesPolicy` / the owning service MUST DENY **before** any durable write when:

- `principal == null`
- principal is blank (`""` or whitespace-only)
- principal is malformed/invalid (does not satisfy `ActorPrincipal` construction)
- required role/permission is missing
- required independent principal is the same as a prohibited actor

Controllers are not sufficient. Direct unit tests MUST call the policy/service layer **without HTTP** (T17-SEC-021–028):

- null requester / blank requester
- null reviewer / blank reviewer
- null certifier / blank certifier
- invalid executor identity
- same principal across forbidden SoD roles

No normalization may turn blank into an acceptable identity.

---

## 14. Durable authority, cache, replicas

`ProductionCertificationAuthority.readCurrent(productionTargetId)` MUST return a version-bound snapshot or empty.

Snapshot includes: onboarding version, bundle version, interface version+status+approval, transport profile version, artifact binding, capability/security/credential/TLS/network/endpoint versions, vendor version compatibility, health, expiry/revocation, `authorityReadAt`, `authorityRowVersions`.

Unknown / unavailable / timeout / stale ⇒ DENY `P17_AUTHORITY_UNAVAILABLE` or `P17_CERTIFICATION_UNKNOWN`.

Cache:

- Optional read-through keyed by `(targetId, bundleVersion, onboardingVersion)`.
- Negative/deny MAY be cached conservatively with short TTL.
- Positive eligibility MUST be re-validated against durable versions immediately before mutation (compare version tokens).
- No stale-while-revalidate for positive permission.
- No unbounded map of “target is certified”.

Multi-replica: no local-memory positive authority. Events MAY invalidate caches only. Partition from DB ⇒ DENY.

---

## 15. Send-boundary preflight algorithm

Runs **after** Phase 16 grant consume succeeds and **before** `transmitMutation`. A prior ISSUED grant does not waive these checks.

```text
1. Read Phase16 current:
   authorization, fingerprint, generation, grant binding, lease/fence,
   kill switch, target state, window/change-control, rate/blast
   If any false/unknown/stale/unavailable → DENY (existing Phase16 codes)

2. authority = ProductionCertificationAuthority.readCurrent(targetId)
   If missing/unavailable → P17_AUTHORITY_UNAVAILABLE

3. Verify Phase17 current and matching consumed grant bindings:
   interface definition + approval
   certification bundle ACTIVE and not expired
   transport certification current
   target onboarding APPROVED and target cert CURRENT
   transport profile version
   runtime artifact identity == bundle artifact identity
   endpoint profile
   vendor version compatibility (unknown version → P17_VENDOR_VERSION_UNKNOWN)
   capability CELL/txPower certified
   security / credential / TLS / network profiles ACTIVE
   transport health eligible
   Level4Satisfied(this execution)

4. DestinationTrustValidator.validate(resolved destination vs endpoint+TLS+network profiles)
   DNS alone is not proof. TLS server identity and egress must match.
   Mismatch → P17_ENDPOINT_IDENTITY_MISMATCH or P17_TLS_IDENTITY_MISMATCH

5. Credential binder: resolve(targetId, credentialProfileVersionId, transport/security bind, write WI)
   Generic/unbound → P17_CREDENTIAL_PROFILE_DENIED
   Phase10 still fetches the secret value. No value cache.

6. HealthComposer.eligible(phase16TargetHealth, phase17TransportHealth, certs)
   HEALTHY transport never overrides Phase16 deny.

7. Only if all affirmative: allow adapter to call transport.
   Production transport remains UnconfiguredProductionEricssonWriteTransport
   until a later authorized certified replacement exists.
```

Deterministic denial codes (protocol enum `Phase17DenialCode`):

```text
P17_AUTHORITY_UNAVAILABLE
P17_CERTIFICATION_UNKNOWN
P17_CERTIFICATION_EXPIRED
P17_CERTIFICATION_REVOKED
P17_CERTIFICATION_STALE
P17_INTERFACE_UNRESOLVED
P17_INTERFACE_REVOKED
P17_INTERFACE_SUPERSEDED
P17_DOCUMENTATION_WITHDRAWN
P17_APPROVAL_REVOKED
P17_BUNDLE_INVALID
P17_PROFILE_MISMATCH
P17_ARTIFACT_MISMATCH
P17_ENDPOINT_IDENTITY_MISMATCH
P17_TLS_IDENTITY_MISMATCH
P17_NETWORK_POLICY_INACTIVE
P17_CREDENTIAL_PROFILE_DENIED
P17_VENDOR_VERSION_UNKNOWN
P17_VENDOR_VERSION_MISMATCH
P17_CAPABILITY_NOT_CERTIFIED
P17_ATOMIC_NOT_CERTIFIED
P17_HEALTH_BLOCKING
P17_TARGET_NOT_ONBOARDED
P17_TARGET_SUSPENDED
P17_LEVEL3_NOT_LEVEL4
P17_LEVEL4_NOT_CURRENT
P17_SOD_VIOLATION
P17_AGENT_DENIED
P17_MCP_DENIED
```

---

## 16. Health composition

Enum `TransportHealthState`: `HEALTHY`, `DEGRADED`, `UNAVAILABLE`, `SECURITY_FAILURE`, `CAPABILITY_MISMATCH`, `VERSION_MISMATCH`, `SUSPENDED`.

| State | Production mutation |
|---|---|
| HEALTHY | Eligible only if all other controls pass |
| DEGRADED | DENY by default |
| UNAVAILABLE | DENY |
| SECURITY_FAILURE | DENY + human reactivation |
| CAPABILITY_MISMATCH | DENY + recertify |
| VERSION_MISMATCH | DENY + recertify |
| SUSPENDED | DENY + `TARGET_REACTIVATE` |
| Unknown | DENY |

```text
eligible =
  Phase16 target health eligible
  AND Phase17 transport health eligible
  AND Phase17 certification current
  AND Phase17 target certification current
  AND remaining Phase16 controls
```

---

## 17. Expected-state, retry, readback, rollback, ambiguous outcome

### 17.1 Expected state

Reuse frozen Phase 16 `ExpectedStateGuardStrength`. Values: `ATOMIC`, `READ_THEN_WRITE`. Do **not** introduce `ExpectedStateStrength`.

Phase 17 certification binds which existing enum value is permitted on the certified transport profile (`expected_state_strategy` stores the Phase 16 enum name).

Default `READ_THEN_WRITE` with explicit residual TOCTOU.  
`ATOMIC` remains eligible only with authoritative certified evidence: interface evidence + capability `conditional_write_supported=true` + `atomic_certified=TRUE` + ACTIVE `EXPECTED_STATE` PASS that states vendor CAS.  
Unknown/unproven: `READ_THEN_WRITE` or DENY according to frozen Phase 16 policy. Lab timing MUST NOT upgrade to ATOMIC (`P17_ATOMIC_NOT_CERTIFIED`).

Direct vendor observation remains mandatory immediately before mutation (Phase 16). Canonical Phase 12 state is not verification.

### 17.6 Mutation-path retry prohibition

**No automatic retry may wrap vendor mutation dispatch.** This is an implementation prohibition, not a service-level comment.

Forbidden around the mutation invocation, including: Spring Retry, `@Retryable`, Resilience4j Retry, WebClient `retry`/`retryWhen`, HTTP client automatic request retry, Feign retry, RestClient retry, transport SDK retry, session-level write retry, adapter wrapper retry, generic resilience interceptor.

If a future authoritative vendor SDK performs internal retries, its semantics MUST be explicitly certified before use. Default: disable mutation request retries.

PRE-SEND operations MAY use bounded retries **only** where positive proof exists that vendor mutation has not been dispatched (durable authority read, credential metadata resolution before session, DNS lookup before mutation, non-mutating health check).

Once the mutation dispatch boundary is crossed: **no retry**. Tests MUST inspect/configure the actual client/resilience stack (T17-IMPL-066, T17-SEC-030), not merely assert a service flag.

### 17.2 Send classification

Reuse Phase 16 attempt states. Classify every failure as:

| Class | Rule |
|---|---|
| `POSITIVE_NOT_SENT` | Certified positive proof that invocation did not reach vendor accept/apply |
| `MAY_HAVE_SENT` | Default after invocation boundary, timeout, connection/response loss, unknown post-dispatch exception |

Candidate NOT_SENT (only if later certified for the real interface; locally testable on controlled transport): credential resolution failure before session; TLS/session failure before dispatch; local validation/serialization before dispatch.

Do not encode Ericsson mechanics. HTTP PUT, same desired value, request/operation/job IDs MUST NOT infer idempotency or NOT_SENT (G17-156).

Vendor idempotency: UNKNOWN. Vendor operation IDs: sanitized correlation only.

### 17.3 Readback

Certify metadata: source, freshness basis, timestamp semantics, staleness threshold, read-after-write, eventual-consistency window, timeout. Only fresh vendor desired-state observation ⇒ VERIFIED.

### 17.4 Rollback

Capability evidence that exact Phase 14 rollback value can be written and read back. Governance remains Phase 16 request/review/authorize/grant/consume/write/verify. No automatic rollback.

### 17.5 Ambiguous outcome (exact Phase 16)

| Observation after OUTCOME_UNKNOWN | Result |
|---|---|
| desired | VERIFIED |
| expected | stop; no auto resend; new governed execution if needed |
| third | MANUAL_INTERVENTION_REQUIRED |
| unavailable | PRODUCTION_OUTCOME_UNRESOLVED |

Local tests use controlled transport. External certification later proves real transport.

---

## 18. Invalidation cascade

`CertificationInvalidationService` is the **single writer** for safety-significant Phase 17 status flips that affect execution eligibility. It **owns the transaction boundary**.

For every execution-significant invalidation, **one durable database transaction is MANDATORY**. There is **no** two-phase alternative. Events/outbox are not the authoritative revocation path.

If any required durable write fails: **ROLLBACK THE ENTIRE TRANSACTION**. Do not commit partial state such as certification `REVOKED` with an ISSUED grant remaining, or grant `REVOKED` with the authoritative certification still `ACTIVE`.

### 18.1 Phase 16 grant authority (normative)

Phase 17 MUST NOT create a second grant writer, grant repository, grant table, or grant state machine.

Use the existing Phase 16 grant authority:

- `ProductionExecutionGrantService.revokeIssued(productionChangeId)`
- `ProductionExecutionGrantRepository.findByTargetIdAndStatus(targetId, ISSUED)`

Target-scoped invalidation MUST locate affected grants through that authority: find affected Phase 16 production changes/executions and/or `findByTargetIdAndStatus(targetId, ISSUED)`, then invoke existing `revokeIssued(productionChangeId)`.

Existing `revokeIssued` is `@Transactional(REQUIRES_NEW)`. That MUST NOT be invoked from `CertificationInvalidationService` because it would open an independent transaction.

Minimal safe adaptation (frozen ISSUED→REVOKED semantics unchanged):

1. Keep existing `revokeIssued` for current Phase 16 API callers.
2. On the **existing** Phase 16 grant repository/service only, add a same-transaction operation used exclusively by `CertificationInvalidationService`, for example `revokeIssuedInCurrentTransaction(productionChangeId)` and optionally `revokeIssuedByTargetId(targetId)`, with `@Transactional(MANDATORY)` so it **joins** the invalidation transaction.
3. The durable mutation itself MUST include an ISSUED predicate:

```text
UPDATE production_execution_grant
   SET status = 'REVOKED', version = version + 1
 WHERE ...
   AND status = 'ISSUED'
```

CONSUMED rows MUST be excluded by that durable predicate. Not: load state, check in Java, then unconstrained UPDATE.

`ProductionAuthorizationService.markStale` is `@Transactional` (REQUIRED) and MUST participate in the same invalidation transaction. Do not wrap it in a new transaction.

Forbidden grant transitions (never):

- CONSUMED → REVOKED
- CONSUMED → ISSUED
- REVOKED → ISSUED
- EXPIRED → ISSUED

Phase 17 MUST NOT issue replacement grants, reset grants, or resurrect grants.

Required tests mapped to **G17-134**, **G17-136**, **G17-154** and threats T17-01 / T17-09 / T17-17 / T17-33:

| Test | Expected |
|---|---|
| T17-IMPL-051 | ISSUED affected grant → REVOKED |
| T17-IMPL-052 | CONSUMED affected grant → unchanged CONSUMED |
| T17-IMPL-053 | EXPIRED → unchanged |
| T17-IMPL-054 | REVOKED → unchanged |
| T17-IMPL-055 | unrelated target ISSUED grant → unchanged |
| T17-INT-031/032 | concurrent consume vs revoke → only a legal Phase 16 terminal result; never resurrection; never a second mutation authority |

### 18.2 Deterministic transaction order

`CertificationInvalidationService.invalidate(...)` MUST execute, in this order, inside one transaction:

1. Obtain/validate trigger identity and idempotency key. If `phase17_invalidation_event.idempotency_key` already exists, return success with the already-denied durable state (no resurrection, no duplicate grants, no second active audit authority).
2. Acquire required durable locks in the §18.3 class order.
3. Mark the authoritative source record revoked/superseded/expired/withdrawn/suspended as required by §18.6 (or record NO CHANGE).
4. Invalidate dependent certification records.
5. Invalidate certification bundle(s).
6. Invalidate/suspend target certification/onboarding as required.
7. Mark affected Phase 16 production authorization STALE through existing `ProductionAuthorizationService.markStale`.
8. Revoke affected Phase 16 ISSUED grants through the existing Phase 16 grant authority (§18.1).
9. Update target/transport health where the matrix requires it (else NO CHANGE).
10. Persist required Phase 17 (and Phase 16, where applicable) audit event(s).
11. Persist invalidation event and optional outbox row for post-commit cache/alerts.
12. Commit.

After commit, events MAY invalidate caches, raise alerts, and emit observability. Execution eligibility MUST derive from durable state. A missing or delayed event MUST NEVER leave stale positive eligibility if the gateway can reach the authoritative database. A replica that cannot establish durable currentness: DENY.

### 18.3 Lock order

Use repository-consistent locking (`SELECT … FOR UPDATE` or equivalent JPA lock) on the rows that will be mutated.

Class order (deadlock avoidance):

1. Authoritative interface / approval
2. Certification / bundle
3. Onboarding / target certification
4. Phase 16 production change / authorization
5. Phase 16 ISSUED grants
6. Health
7. Audit / invalidation event / outbox

Within a class, lock by stable primary/business key ascending (UUID/text).

### 18.4 Invalidation vs grant consume

Both contend on the same durable grant row. Allowed results follow the frozen Phase 16 state machine.

**Case A** — invalidation obtains effective ISSUED state first:

- ISSUED → REVOKED
- subsequent consume updates 0 rows
- gateway MUST NOT dispatch
- vendor mutation count = **exactly 0**
- evidence: T17-INT-031

**Case B** — consume atomically obtains the grant first:

- ISSUED → CONSUMED
- Phase 17 invalidation MUST NOT rewrite CONSUMED
- If revocation of certification became durable **before** mutation dispatch, gateway final currentness/preflight MUST observe it and DENY; mutation count = **exactly 0**
- If mutation already crossed the dispatch boundary: Phase 16 `MAY_HAVE_SENT` / verification/recovery apply; mutation count = **exactly 1**; no resend; no replacement grant
- evidence: T17-INT-032

Never CONSUMED → REVOKED, CONSUMED → ISSUED, new replacement grant, or automatic resend.

### 18.5 Invalidation vs send

Database transaction atomicity **cannot** physically cancel an external vendor write already in flight. Do not claim fencing/revocation retracts an in-flight vendor mutation.

- Before mutation dispatch: durable currentness failure → DENY, mutation count **exactly 0** (T17-INT-033).
- Concurrent invalidation before final preflight completes: gateway MUST observe it through authoritative locking/version/currentness and DENY, mutation count **exactly 0**.
- Invalidation after dispatch: cannot be assumed to cancel the vendor action. Outcome proceeds through Phase 16 verification/recovery; mutation count **exactly 1**; no automatic second mutation (T17-INT-034, FI17-017–019).

### 18.6 Per-trigger invalidation matrix

Every cell is filled. `NO CHANGE` means no durable Phase 17 rewrite is required. Grant effect `ISSUED→REVOKED` means only ISSUED rows; CONSUMED/EXPIRED/REVOKED are unchanged. Gateway DENY is the eligibility result if a send is attempted after the trigger is durable.

| Trigger | Source table/record | Source status write | Dependent certification write | Bundle write | Target/onboarding write | Phase16 authorization | Phase16 ISSUED grant | Health | Audit event | Cache/event after commit | Gateway eligibility |
|---|---|---|---|---|---|---|---|---|---|---|---|
| interface revoked | `vendor_interface_definition` | `status=REVOKED` | `transport_certification.state=REVOKED` | `status=REVOKED` | `production_target_certification.status=REVOKED`; `production_target_onboarding.status=REVOKED` | `markStale` | ISSUED→REVOKED | `SECURITY_FAILURE` | `INTERFACE_REVOKED` | cache invalidate; alert | DENY `P17_INTERFACE_REVOKED` |
| interface superseded | `vendor_interface_definition` | old `status=SUPERSEDED` | bound cert `state=REVOKED` | bound `status=INVALID` | bound target cert `status=INVALID`; onboarding `status=INVALID` | `markStale` | ISSUED→REVOKED | `CAPABILITY_MISMATCH` | `INTERFACE_SUPERSEDED` | cache invalidate; alert | DENY `P17_INTERFACE_SUPERSEDED` |
| documentation withdrawn | `vendor_interface_definition.documentation_status` | `WITHDRAWN` | bound cert `state=REVOKED` | `status=REVOKED` | target cert `REVOKED`; onboarding `REVOKED` | `markStale` | ISSUED→REVOKED | `UNAVAILABLE` | `DOCUMENTATION_APPROVAL_REVOKED` | cache invalidate; alert | DENY `P17_DOCUMENTATION_WITHDRAWN` |
| documentation superseded | `vendor_interface_definition.documentation_status` | `SUPERSEDED` | bound cert `state=REVOKED` | `status=INVALID` | target cert `INVALID`; onboarding `INVALID` | `markStale` | ISSUED→REVOKED | `UNAVAILABLE` | `INTERFACE_SUPERSEDED` | cache invalidate; alert | DENY `P17_DOCUMENTATION_WITHDRAWN` |
| approval revoked | `vendor_interface_approval` | `approval_status=REVOKED` | bound cert `state=REVOKED` | `status=REVOKED` | target cert `REVOKED`; onboarding `REVOKED` | `markStale` | ISSUED→REVOKED | `SECURITY_FAILURE` | `INTERFACE_REVOKED` | cache invalidate; alert | DENY `P17_APPROVAL_REVOKED` |
| transport implementation changed | `vendor_write_transport_profile` | new version; old `status=SUPERSEDED` | bound cert `state=REVOKED` | bound `status=INVALID` | bound target cert `INVALID` | `markStale` | ISSUED→REVOKED | `CAPABILITY_MISMATCH` | `PROFILE_VERSIONED` | cache invalidate; alert | DENY `P17_PROFILE_MISMATCH` |
| artifact digest changed | `vendor_transport_artifact` / profile `artifact_digest` | new artifact version; old `status=SUPERSEDED` | bound cert `state=REVOKED` | bound `status=INVALID` | bound target cert `INVALID` | `markStale` | ISSUED→REVOKED | `CAPABILITY_MISMATCH` | `ARTIFACT_MISMATCH` | cache invalidate; alert | DENY `P17_ARTIFACT_MISMATCH` |
| endpoint profile changed | `production_endpoint_profile` | new version; old `status=SUPERSEDED` | cert bound to endpoint `state=REVOKED` | bound `status=INVALID` | target cert `INVALID` | `markStale` | ISSUED→REVOKED | `UNAVAILABLE` | `ENDPOINT_CHANGED` | cache invalidate; alert | DENY `P17_ENDPOINT_IDENTITY_MISMATCH` |
| network profile changed | `production_network_policy_profile` | new version; old `status=SUPERSEDED` | bound cert `state=REVOKED` | bound `status=INVALID` | target cert `INVALID` | `markStale` | ISSUED→REVOKED | `UNAVAILABLE` | `NETWORK_PROFILE_CHANGED` | cache invalidate; alert | DENY `P17_NETWORK_POLICY_INACTIVE` |
| TLS profile changed | `production_tls_profile` | new version; old `status=SUPERSEDED` | bound cert `state=REVOKED` | bound `status=INVALID` | target cert `INVALID` | `markStale` | ISSUED→REVOKED | `SECURITY_FAILURE` | `TLS_PROFILE_CHANGED` | cache invalidate; alert | DENY `P17_TLS_IDENTITY_MISMATCH` |
| security profile changed | `vendor_security_certification` | new version; old `status=SUPERSEDED` | bound cert `state=REVOKED` | bound `status=INVALID` | target cert `INVALID` | `markStale` | ISSUED→REVOKED | `SECURITY_FAILURE` | `CERT_REVOKED` | cache invalidate; alert | DENY `P17_PROFILE_MISMATCH` |
| credential profile changed | `production_credential_profile` | new version; old `status=SUPERSEDED` | bound cert `state=REVOKED` | bound `status=INVALID` | target cert `INVALID` | `markStale` | ISSUED→REVOKED | `SECURITY_FAILURE` | `CREDENTIAL_PROFILE_CHANGED` | cache invalidate; alert | DENY `P17_CREDENTIAL_PROFILE_DENIED` |
| capability profile changed | `vendor_capability_certification` | new version; old `status=SUPERSEDED` | bound cert `state=REVOKED` | bound `status=INVALID` | target cert `INVALID` | `markStale` | ISSUED→REVOKED | `CAPABILITY_MISMATCH` | `CERT_REVOKED` | cache invalidate; alert | DENY `P17_CAPABILITY_NOT_CERTIFIED` |
| vendor version mismatch/change | `vendor_version_compatibility` | `status=SUSPENDED` or `REVOKED` | bound cert `state=SUSPENDED` | bound `status=INVALID` | target cert `SUSPENDED` | `markStale` | ISSUED→REVOKED | `VERSION_MISMATCH` | `VENDOR_VERSION_MISMATCH` | cache invalidate; alert | DENY `P17_VENDOR_VERSION_MISMATCH` |
| target onboarding changed | `production_target_onboarding` / `_version` | new version; previous `status=INVALID` | NO CHANGE unless bound cert lists that onboarding | bound bundle `status=INVALID` if that onboarding version was bound | previous target cert `INVALID`; new version not CURRENT until APPROVED | `markStale` | ISSUED→REVOKED | NO CHANGE | `ONBOARD_CREATED` | cache invalidate | DENY `P17_CERTIFICATION_STALE` |
| target suspended | `production_target_onboarding` + `production_target_certification` | onboarding `SUSPENDED`; target cert `SUSPENDED` | NO CHANGE | NO CHANGE | as source | `markStale` | ISSUED→REVOKED | `SUSPENDED` | `TARGET_SUSPENDED` | cache invalidate; alert | DENY `P17_TARGET_SUSPENDED` |
| certification expired | `transport_certification` / bundle / target cert | `state`/`status=EXPIRED` | `state=EXPIRED` | `status=EXPIRED` | target cert `EXPIRED` | `markStale` | ISSUED→REVOKED | `UNAVAILABLE` | `CERT_EXPIRED` | cache invalidate; alert | DENY `P17_CERTIFICATION_EXPIRED` |
| certification revoked | `transport_certification` | `state=REVOKED` | as source | `status=REVOKED` | target cert `REVOKED`; onboarding `REVOKED` | `markStale` | ISSUED→REVOKED | `SECURITY_FAILURE` | `CERT_REVOKED` | cache invalidate; alert | DENY `P17_CERTIFICATION_REVOKED` |
| Phase16 L4 authorization revoked | existing Phase 16 authorization row | Phase 16 revoked/stale write (existing authority) | NO CHANGE | NO CHANGE | NO CHANGE | existing Phase 16 revoke/stale (source) | ISSUED→REVOKED via existing Phase 16 grant authority | NO CHANGE | Phase 16 audit | optional cache invalidate | DENY `P17_LEVEL4_NOT_CURRENT` |
| kill switch disabled | existing Phase 16 kill-switch / writes-enabled state | NO CHANGE (Phase 16 already authoritative) | NO CHANGE | NO CHANGE | NO CHANGE | NO CHANGE | NO CHANGE | NO CHANGE | Phase 16 kill-switch audit | optional cache invalidate | DENY via Phase 16 preflight step 1 |

Race: if send-boundary sees a newer `updated_at`/version than the snapshot captured at consume, DENY, mutation count 0.

---

## 19. APIs

Base: `/api/v1/vendor-certifications` and `/api/v1/target-onboardings`. Existing `/api/v1/production-changes` unchanged and MUST NOT grow mutation-bypass fields.

```text
POST /api/v1/vendor-interfaces
POST /api/v1/vendor-interfaces/{id}/versions
POST /api/v1/vendor-interfaces/{versionId}/review
POST /api/v1/vendor-interfaces/{versionId}/revoke

POST /api/v1/transport-profiles
GET  /api/v1/transport-profiles/{versionId}

POST /api/v1/transport-certifications
POST /api/v1/transport-certifications/{id}/transition
POST /api/v1/transport-certifications/{id}/evidence
GET  /api/v1/transport-certifications/{id}

POST /api/v1/target-onboardings
POST /api/v1/target-onboardings/{id}/review
POST /api/v1/target-onboardings/{id}/approve
POST /api/v1/target-onboardings/{id}/suspend
POST /api/v1/target-onboardings/{id}/reactivate
GET  /api/v1/target-onboardings/{id}
GET  /api/v1/production-targets/{targetId}/readiness
```

No API may: execute production mutation; accept raw endpoint/credential/protocol; set L4 standing flag; invoke vendor command.

DTO rules: identifiers and versions only; SHA-256 hex; no secret fields; reason codes from `Phase17DenialCode` + existing Phase 16 codes.

Permissions: as §13. `VIEW_CERTIFICATION_STATUS` for GET.

---

## 20. Agents, MCP, schedulers, events

| Actor | Allowed | Forbidden |
|---|---|---|
| Agents | Optional non-sensitive readiness GET if granted | Register, certify, onboard, approve, L4, execute, rollback, reactivate |
| MCP | None of the mutation/cert tools | Any production mutation tool |
| Scheduler | Mark EXPIRED; health degrade; alert | Initiate mutation or rollback |
| Events | Cache invalidate; alert | Authorization |

No closed loop.

---

## 21. Audit, metrics, alerts

### 21.1 Phase 17 audit events

`INTERFACE_CREATED`, `INTERFACE_REVIEWED`, `INTERFACE_APPROVED`, `INTERFACE_SUPERSEDED`, `INTERFACE_REVOKED`, `DOCUMENTATION_APPROVAL_REVOKED`, `PROFILE_CREATED`, `PROFILE_VERSIONED`, `CERT_TRANSITION`, `CERT_REVOKED`, `CERT_EXPIRED`, `EVIDENCE_ADDED`, `EVIDENCE_SUPERSEDED`, `ARTIFACT_BOUND`, `ARTIFACT_MISMATCH`, `ONBOARD_CREATED`, `ONBOARD_REVIEWED`, `ONBOARD_APPROVED`, `TARGET_SUSPENDED`, `TARGET_REACTIVATED`, `ENDPOINT_CHANGED`, `CREDENTIAL_PROFILE_CHANGED`, `TLS_PROFILE_CHANGED`, `NETWORK_PROFILE_CHANGED`, `VENDOR_VERSION_MISMATCH`, `HEALTH_TRANSITION`, `LEVEL3_REGISTERED`.

No secrets or proprietary payloads.

### 21.2 Metrics (low cardinality)

`snip.p17.certification.state` (state)  
`snip.p17.certification.expiry`  
`snip.p17.certification.revocation`  
`snip.p17.onboarding.status`  
`snip.p17.transport.health`  
`snip.p17.artifact.mismatch`  
`snip.p17.vendor.version.mismatch`  
`snip.p17.credential.profile.failure`  
`snip.p17.tls.failure`  
`snip.p17.endpoint.mismatch`  
`snip.p17.authority.unavailable`  
`snip.p17.gateway.cert.deny` (code)  
`snip.p17.outcome.unknown`  
`snip.p17.verification.mismatch`  
`snip.p17.rollback.failure`

No cell id, parameter value, or secret labels.

### 21.3 High-priority alerts

Certification revoked; interface authority revoked; artifact mismatch; target suspension; version mismatch; security failure; TLS identity mismatch; credential resolution failure; authority unavailable; unknown production outcome; verification failure; rollback failure; audit invalidity; kill-switch activation.

---

## 22. DNS / TLS / credential / version / capability

Destination trust order: approved FQDN → egress constraint → TLS hostname/server identity → optional mTLS peer. DNS response alone is not vendor identity.

Credential resolve arguments MUST include `productionTargetId`, `credentialProfileId/version`, transport/security profile binding, write WI. Phase 10 fetches values. No older-version secret fallback. No vendor-secret value cache.

Unknown target vendor version ⇒ DENY. Out of predicate ⇒ SUSPEND + DENY. Predicate expansion ⇒ new certification.

Capability locked to CELL/txPower. No generic commands.

---

## 23. Controlled / unconfigured transport

Preserve `EricssonWriteTransport` with no protocol methods added.

Production: `UnconfiguredProductionEricssonWriteTransport`.

Tests MAY extend `ControlledTestEricssonWriteTransport` for Phase 17 denial/classification. Test transport MUST:

- require explicit test profile
- refuse production-runtime mode
- contain no real vendor protocol, endpoint, or credential
- never become production transport

### 23.1 Destination identity test harness

Specify test-only `TestDestinationIdentityDouble` used by `DestinationTrustValidator` in default CI.

It MUST simulate, without a real vendor network:

- approved FQDN / wrong FQDN
- approved port / wrong port
- expected TLS identity / wrong TLS identity
- trusted chain / untrusted chain
- hostname mismatch
- network-profile mismatch

The double MUST NEVER be wired as production transport.

Hostname verification is **required** for every transport/TLS profile eligible for `PREPROD` or `PRODUCTION`. No eligible PREPROD/PRODUCTION profile may set hostname verification false.

LAB may disable/relax hostname or trust rules **only** if the profile is isolated from any PREPROD/PRODUCTION certification bundle and is never promotable without a **new** compliant profile version. Prefer secure defaults in every environment, including LAB.

---

## 24. Implementation order

1. Durable schema / domain records (V18 when authorized)
2. Certification lifecycle
3. Evidence / bundle model
4. Invalidation cascade
5. SoD / RBAC
6. Target onboarding
7. Endpoint / TLS / network profiles
8. Version / capability certification
9. Health
10. Gateway durable currentness resolver
11. Runtime artifact binding
12. Gateway final certification preflight
13. Audit / metrics
14. Local / failure tests
15. External-certification scaffolding

No real transport implementation in this sequence.

---

## 25. Migration safety and defaults

V18 additive. No destructive migration. No reinterpretation of Phase 16 rows.

Defaults:

```text
production writes disabled
no certified transport
no L1/L2/L3 satisfaction
no endpoint profile
no production-write-eligible credential profile
no capability certification
no target onboarding
no standing L4
UnconfiguredProductionEricssonWriteTransport
```

Existing targets: NOT PHASE17 CERTIFIED; NOT PRODUCTION REGISTERED; DENY.

---

## 26. CI plan

Default CI remains `go test ./...` (simulator) + `mvn -B test`. Azure-independent, vendor-independent, credential-independent, write-free.

CI MUST prove Level 0 items in §32. CI MUST NOT require L1/L2/L3 systems. Passing CI MUST NOT be recorded as vendor certification.

---

## 27. External certification plan (scaffolding only)

### 27.1 L1 — NOT EXECUTED (22 items)

`EXT17-L1-001` authoritative interface provenance  
`EXT17-L1-002` protocol/security profile  
`EXT17-L1-003` lab endpoint identity  
`EXT17-L1-004` non-production credential path  
`EXT17-L1-005` TLS/mTLS  
`EXT17-L1-006` session establishment  
`EXT17-L1-007` CELL/txPower read  
`EXT17-L1-008` expected-state observation  
`EXT17-L1-009` one approved lab mutation  
`EXT17-L1-010` vendor acknowledgement meaning  
`EXT17-L1-011` fresh readback  
`EXT17-L1-012` rollback  
`EXT17-L1-013` timeout  
`EXT17-L1-014` response loss  
`EXT17-L1-015` connection loss  
`EXT17-L1-016` unknown outcome  
`EXT17-L1-017` credential expiry/revoke  
`EXT17-L1-018` certificate failure  
`EXT17-L1-019` vendor restart/disconnect  
`EXT17-L1-020` version compatibility  
`EXT17-L1-021` operation-id semantics if available  
`EXT17-L1-022` idempotency semantics if claimed  

### 27.2 L2 — NOT EXECUTED (15 items)

`EXT17-L2-001` production-equivalent network path  
`EXT17-L2-002` write workload identity  
`EXT17-L2-003` secret resolution  
`EXT17-L2-004` firewall/egress  
`EXT17-L2-005` TLS  
`EXT17-L2-006` target registration (pre-prod class)  
`EXT17-L2-007` change control  
`EXT17-L2-008` monitoring  
`EXT17-L2-009` audit  
`EXT17-L2-010` one controlled pre-prod mutation  
`EXT17-L2-011` verification  
`EXT17-L2-012` rollback  
`EXT17-L2-013` unknown outcome  
`EXT17-L2-014` operational support/runbook  
`EXT17-L2-015` kill switch + suspend/reactivate exercise  

### 27.3 L3 — NOT SATISFIED (16 items)

`EXT17-L3-001` approved endpoint identity  
`EXT17-L3-002` approved interface definition  
`EXT17-L3-003` certified transport artifact/profile  
`EXT17-L3-004` vendor version compatible  
`EXT17-L3-005` capability certified  
`EXT17-L3-006` security certified  
`EXT17-L3-007` credential profile approved  
`EXT17-L3-008` TLS profile approved  
`EXT17-L3-009` network profile approved  
`EXT17-L3-010` monitoring ready  
`EXT17-L3-011` support owner  
`EXT17-L3-012` change-control policy  
`EXT17-L3-013` verification policy  
`EXT17-L3-014` rollback policy  
`EXT17-L3-015` target onboarding approved  
`EXT17-L3-016` certifications current/not expired  

L3 requires **no** production mutation.

### 27.4 L4

No L4 external evidence that silently authorizes execution. Local tests prove gateway checks `Level4Satisfied`. At freeze: NOT SATISFIED.

---

## 28. Failure-injection catalogue (FI17)

Every FI17 row is a default-CI integration test. Pre-send DENY ⇒ vendor mutation count **exactly 0**. Dispatch-then-loss ⇒ mutation count **exactly 1** and no resend. Do not use `<= 1` except the named consume/revoke concurrency rows, which explain why the winner is not predetermined; each legal winner still has an exact count.

| ID | Precondition | Action/fault | Durable end-state | Vendor mutation count | Verification/recovery | Gates | Evidence IDs |
|---|---|---|---|---|---|---|---|
| FI17-001 | CURRENT target cert; ISSUED grant; gateway at preflight | Authority DB unavailable | Attempt DENY `P17_AUTHORITY_UNAVAILABLE`; cert/grant unchanged (grant remains ISSUED if consume not yet attempted, CONSUMED if already consumed); no dispatch | exactly 0 | verification not started | G17-099 G17-137 | FI17-001 T17-IMPL-038 CS17-H |
| FI17-002 | Same as FI17-001 | Authority DB timeout | DENY `P17_AUTHORITY_UNAVAILABLE`; no dispatch | exactly 0 | not started | G17-099 G17-137 | FI17-002 T17-IMPL-038 |
| FI17-003 | Cache replica holds positive eligibility; DB cert `REVOKED` | Send-boundary currentness read | Durable `REVOKED` wins; cache ignored; DENY `P17_CERTIFICATION_REVOKED` | exactly 0 | not started | G17-139 G17-137 | FI17-003 T17-IMPL-037 CS17-I |
| FI17-004 | ISSUED grant; cert CURRENT | Revoke certification after issue, before dispatch | Cert `REVOKED`; ISSUED grant `REVOKED` via Phase 16 authority if invalidation precedes consume; else CONSUMED preserved and preflight DENY | exactly 0 if before dispatch | not started | G17-134 G17-136 G17-154 | FI17-004 T17-IMPL-051 T17-INT-031/032 |
| FI17-005 | ISSUED grant; interface INTERFACE_VERIFIED | Interface revoke before dispatch | Interface `REVOKED`; bundle `REVOKED`; ISSUED grants `REVOKED` or CONSUMED preserved; DENY `P17_INTERFACE_REVOKED` | exactly 0 | not started | G17-134 G17-154 | FI17-005 T17-IMPL-007 CS17-C |
| FI17-006 | ISSUED grant; target CURRENT | Target suspend before dispatch | Target cert `SUSPENDED`; ISSUED→REVOKED or CONSUMED preserved; DENY `P17_TARGET_SUSPENDED` | exactly 0 | not started | G17-154 G17-138 | FI17-006 CS17-G |
| FI17-007 | ISSUED grant | Credential-profile change before dispatch | Old profile `SUPERSEDED`; bundle `INVALID`; DENY `P17_CREDENTIAL_PROFILE_DENIED` | exactly 0 | not started | G17-044 G17-148 | FI17-007 CS17-F |
| FI17-008 | ISSUED grant | TLS-profile change before dispatch | Old TLS `SUPERSEDED`; bundle `INVALID`; DENY `P17_TLS_IDENTITY_MISMATCH` | exactly 0 | not started | G17-051 G17-047 | FI17-008 |
| FI17-009 | ISSUED grant | Endpoint change before dispatch | Old endpoint `SUPERSEDED`; bundle `INVALID`; DENY `P17_ENDPOINT_IDENTITY_MISMATCH` | exactly 0 | not started | G17-039 G17-146 | FI17-009 CS17-D |
| FI17-010 | ISSUED grant | Vendor-version change/out-of-range before dispatch | Compatibility `SUSPENDED`; health `VERSION_MISMATCH`; DENY `P17_VENDOR_VERSION_MISMATCH` | exactly 0 | not started | G17-054 G17-056 | FI17-010 CS17-K |
| FI17-011 | Certified artifact A; runtime B | Send-boundary artifact compare | DENY `P17_ARTIFACT_MISMATCH`; health `CAPABILITY_MISMATCH`; no dispatch | exactly 0 | not started | G17-090 G17-142 | FI17-011 CS17-X T17-INT-036 |
| FI17-012 | Replica cannot reach authority DB | Partition | DENY `P17_AUTHORITY_UNAVAILABLE`; local cache not permission | exactly 0 | not started | G17-140 | FI17-012 CS17-J |
| FI17-013 | ACTIVE PASS evidence | Evidence revoke/withdraw before dispatch | Evidence `REVOKED`; bundle `INVALID`; DENY `P17_BUNDLE_INVALID` | exactly 0 | not started | G17-151 G17-152 | FI17-013 T17-IMPL-016 |
| FI17-014 | Transport health HEALTHY | Health downgrade to DEGRADED/SECURITY_FAILURE | Health blocking; DENY `P17_HEALTH_BLOCKING` | exactly 0 | not started | G17-084 G17-150 | FI17-014 CS17-Y |
| FI17-015 | Level4Satisfied true | Phase 16 L4 authorization revoked before send | Auth STALE/revoked; ISSUED grants REVOKED; DENY `P17_LEVEL4_NOT_CURRENT` | exactly 0 | not started | G17-155 G17-028 | FI17-015 CS17-B |
| FI17-016 | Writes enabled | Kill switch disable before send | Phase 16 kill-switch DENY; Phase 17 cert tables NO CHANGE | exactly 0 | not started | G17-100 G17-001 | FI17-016 T17-INT-016 |
| FI17-017 | Mutation dispatched on controlled transport | Response loss | Attempt `MAY_HAVE_SENT`; grant CONSUMED; no automatic resend; no replacement grant | exactly 1 | Phase 16 recovery; no second mutation | G17-070 G17-149 | FI17-017 CS17-N T17-IMPL-066 |
| FI17-018 | Mutation dispatched | Timeout after dispatch | `MAY_HAVE_SENT`; no resend | exactly 1 | recovery | G17-068 G17-070 | FI17-018 T17-IMPL-049 |
| FI17-019 | Mutation dispatched | Connection loss after dispatch | `MAY_HAVE_SENT`; no resend | exactly 1 | recovery | G17-070 | FI17-019 |
| FI17-020 | Grant already CONSUMED | Duplicate consume / second send | Second consume fails; DENY; first mutation not retried | first attempt: exactly 1 if already dispatched; second attempt: exactly 0 | no second mutation | G17-070 G17-136 | FI17-020 T17-IMPL-010 |

Concurrency note for FI17-004: the consume-vs-revoke winner is not predetermined. Legal terminals are Case A (ISSUED→REVOKED, mutation 0) or Case B (ISSUED→CONSUMED, then CONSUMED unchanged; mutation 0 if preflight still runs, mutation 1 only if dispatch already occurred). Never `<= 1` as a standalone invariant without that explanation (T17-INT-031/032).

---

## 29.Critical scenarios (CS17-A–Z)

| ID | Precondition | Action/fault | Durable end-state | Vendor mutation count | Verification/recovery | Gates | Evidence IDs |
|---|---|---|---|---|---|---|---|
| CS17-A | L3 CURRENT target; no current Phase 16 authorization | Send attempt | DENY `P17_LEVEL3_NOT_LEVEL4`; cert unchanged | exactly 0 | not started | G17-021 G17-029 G17-155 | CS17-A T17-IMPL-035 |
| CS17-B | L4 valid; then certification revoked | Send attempt | Cert `REVOKED`; DENY `P17_CERTIFICATION_REVOKED` | exactly 0 | not started | G17-028 G17-096 | CS17-B FI17-004 |
| CS17-C | Grant ISSUED; interface revoked | Send / invalidation | Interface `REVOKED`; ISSUED→REVOKED or CONSUMED preserved; DENY `P17_INTERFACE_REVOKED` | exactly 0 before dispatch | not started | G17-134 | CS17-C FI17-005 T17-IMPL-051 |
| CS17-D | Grant ISSUED; endpoint changed | Invalidation + send | Endpoint old `SUPERSEDED`; bundle `INVALID`; DENY | exactly 0 | not started | G17-039 G17-146 | CS17-D FI17-009 |
| CS17-E | Grant ISSUED; artifact changed | Invalidation + send | Bundle `INVALID`; DENY `P17_ARTIFACT_MISMATCH` | exactly 0 | not started | G17-090 G17-142 | CS17-E FI17-011 |
| CS17-F | Grant ISSUED; credential profile changed | Invalidation + send | Bundle `INVALID`; DENY `P17_CREDENTIAL_PROFILE_DENIED` | exactly 0 | not started | G17-044 G17-148 | CS17-F FI17-007 |
| CS17-G | Grant ISSUED; target suspended | Invalidation + send | Target `SUSPENDED`; DENY `P17_TARGET_SUSPENDED` | exactly 0 | not started | G17-154 | CS17-G FI17-006 |
| CS17-H | Authority unavailable | Send | DENY `P17_AUTHORITY_UNAVAILABLE` | exactly 0 | not started | G17-099 G17-137 | CS17-H FI17-001 |
| CS17-I | Stale cache valid; DB revoked | Send | Durable revoke wins; DENY | exactly 0 | not started | G17-139 | CS17-I FI17-003 |
| CS17-J | Replica partition from DB | Send | DENY `P17_AUTHORITY_UNAVAILABLE` | exactly 0 | not started | G17-140 | CS17-J FI17-012 |
| CS17-K | Vendor version mismatch | Send | Compatibility `SUSPENDED`; DENY `P17_VENDOR_VERSION_MISMATCH` | exactly 0 | not started | G17-054 | CS17-K FI17-010 |
| CS17-L | Capability not certified | Send | DENY `P17_CAPABILITY_NOT_CERTIFIED` | exactly 0 | not started | G17-060 | CS17-L T17-IMPL-024 |
| CS17-M | ATOMIC requested; unproven | Profile/preflight | `atomic_certified=FALSE`; strategy remains `READ_THEN_WRITE` or DENY `P17_ATOMIC_NOT_CERTIFIED`; no ATOMIC dispatch | exactly 0 if ATOMIC required and denied | not started | G17-062 G17-064 | CS17-M T17-IMPL-026 T17-IMPL-060 |
| CS17-N | Controlled transport; mutation dispatched; response loss | Loss | CONSUMED unchanged; `MAY_HAVE_SENT`; no retry; no replacement grant | exactly 1 | Phase 16 recovery; never automatic second mutation | G17-070 G17-149 | CS17-N FI17-017 T17-IMPL-066 |
| CS17-O | Controlled transport; unknown + desired observed | Ambiguous-outcome table | Attempt VERIFIED | exactly 1 | VERIFIED; no resend | G17-081 G17-082 | CS17-O T17-INT-003 |
| CS17-P | Controlled transport; unknown + expected observed | Ambiguous-outcome table | STOP; no replacement grant | exactly 1 | STOP; no resend | G17-081 G17-082 | CS17-P T17-INT-004 |
| CS17-Q | Controlled transport; unknown + third value | Ambiguous-outcome table | `MANUAL_INTERVENTION_REQUIRED` | exactly 1 | manual; no resend | G17-081 G17-082 | CS17-Q T17-INT-005 |
| CS17-R | Controlled transport; unknown + readback unavailable | Ambiguous-outcome table | `PRODUCTION_OUTCOME_UNRESOLVED` | exactly 1 | unresolved; no resend | G17-081 G17-082 | CS17-R T17-INT-006 |
| CS17-S | Executor principal = onboarding creator | Approve/reactivate | DENY `P17_SOD_VIOLATION`; no onboarding write | exactly 0 | N/A | G17-145 G17-093 | CS17-S T17-SEC-005 T17-SEC-028 |
| CS17-T | Agent principal | Certify/onboard/execute | DENY `P17_AGENT_DENIED` | exactly 0 | N/A | G17-095 G17-115 | CS17-T T17-SEC-003 |
| CS17-U | MCP principal | Certify/onboard/execute | DENY `P17_MCP_DENIED` | exactly 0 | N/A | G17-095 G17-116 | CS17-U T17-SEC-004 |
| CS17-V | Expired evidence/certification | Send | DENY `P17_CERTIFICATION_EXPIRED` | exactly 0 | not started | G17-096 | CS17-V T17-IMPL-004 |
| CS17-W | Superseded interface | Send | DENY `P17_INTERFACE_SUPERSEDED` | exactly 0 | not started | G17-134 | CS17-W T17-IMPL-007 |
| CS17-X | Artifact A certified; runtime B deployed | Send | DENY `P17_ARTIFACT_MISMATCH` | exactly 0 | not started | G17-090 G17-142 | CS17-X T17-INT-036 T17-IMPL-063 |
| CS17-Y | Transport HEALTHY; Phase 16 target suspended | Send | DENY (Phase 16 wins); health AND composition | exactly 0 | not started | G17-150 G17-084 | CS17-Y T17-IMPL-031 |
| CS17-Z | Documentation withdrawn after grant ISSUED | Invalidation + send | Documentation `WITHDRAWN`; cascade; DENY `P17_DOCUMENTATION_WITHDRAWN` | exactly 0 before dispatch | not started | G17-135 G17-154 | CS17-Z T17-IMPL-008 |

---

## 30. Local test requirements

Default CI MUST prove, without vendor/Azure:

state transitions; SoD; versioned immutability; revocation cascade; grant revocation; consumed grant non-reset; durable currentness fail-closed; cache fail-closed; multi-replica logical semantics; artifact mismatch deny; endpoint mismatch deny; target-bound credential selection; version mismatch deny; capability mismatch deny; ATOMIC not enabled without evidence; READ_THEN_WRITE default; POSITIVE_NOT_SENT proof on controlled transport; MAY_HAVE_SENT defaults; no-blind-retry; health AND composition; evidence trust/supersession; L3≠L4; Level4 current authorization check; unknown certification deny; Agent/MCP exclusion; scheduler/event no mutation; no auto rollback; no closed loop.

Database tests: FK; unique/version; immutable certified rows; status CHECK; digest format; effective interval; single current version; evidence supersession; bundle snapshot integrity; target/profile binding; credential target binding; expiry/revocation queries; concurrent transition safety.

Concurrency: two approvals; suspend vs preflight; revoke vs preflight; endpoint change vs preflight; artifact mismatch vs execute; reactivation races; evidence supersession races; duplicate onboarding approve; multi-replica stale cache. Safety: at most one legitimate transition; no stale positive permission.

Security tests: caller endpoint injection; credential override; Agent/MCP certify; executor self-onboard; invalid SoD combo; generic vendor credential; target-mismatch credential; unapproved artifact; expired/revoked/superseded; withdrawn approval; inactive TLS/network; unknown version; capability not certified.

---

## 31. Test / evidence ID catalogue

| Prefix | Count | Type | CI |
|---|---|---|---|
| T17-STR-001–030 | 30 | STRUCTURAL | default CI |
| T17-DB-001–031 | 31 | DATABASE | default CI |
| T17-IMPL-001–070 | 70 | BEHAVIORAL | default CI |
| T17-INT-001–038 | 38 | INTEGRATION | default CI |
| T17-SEC-001–030 | 30 | SECURITY | default CI |
| T17-INF-001–008 | 8 | INFRASTRUCTURE | structural/review |
| FI17-001–020 | 20 | INTEGRATION | default CI |
| CS17-A–Z | 26 | INTEGRATION | default CI |
| EXT17-L1-001–022 | 22 | EXTERNAL_CERTIFICATION | NOT EXECUTED |
| EXT17-L2-001–015 | 15 | EXTERNAL_CERTIFICATION | NOT EXECUTED |
| EXT17-L3-001–016 | 16 | EXTERNAL_CERTIFICATION | NOT SATISFIED |

**Evidence catalog total: 306**

Catalogue IDs not attached to a gate remain legitimate supporting tests (A17-S-05). Do not delete them solely to force every ID onto a gate.

Broken evidence references MUST be 0. Machine-readable map:

```text
docs/implementation/phase17-gate-evidence-map.json
```

must be semantically equivalent to §34.

### 31.1 Structural (T17-STR)

T17-STR-001 Phase16 packages unchanged  
T17-STR-002 Gateway remains separate module  
T17-STR-003 App has no Ericsson write protocol client  
T17-STR-004 `EnmTransport` has no write methods  
T17-STR-005 `UnconfiguredProductionEricssonWriteTransport` remains production bean  
T17-STR-006 No V1–V17 file edits  
T17-STR-007 No real hostname/secret seed  
T17-STR-008 Certification packages not in agent/mcp  
T17-STR-009 Gateway not a bean in app  
T17-STR-010 No plugin/classloader transport load  
T17-STR-011 No Nokia write types  
T17-STR-012 No Phase18 artifacts  
T17-STR-013 Protocol module has no Azure SDK  
T17-STR-014 No standing L4 column  
T17-STR-015 Onboarding CHECK forbids L4  
T17-STR-016 SPI layering preserved  
T17-STR-017 No closed-loop scheduler to gateway execute  
T17-STR-018 No MCP production mutation tool  
T17-STR-019 No Agent certify permission  
T17-STR-020 Change-control remains abstract/MANUAL  
T17-STR-021 Architecture SHA pin in completion report later  
T17-STR-022 Parent baselines pinned  
T17-STR-023 No secret columns in V18  
T17-STR-024 No protocol URL constants  
T17-STR-025 Gateway credential isolation remains  
T17-STR-026 Read vs write WI remain separate  
T17-STR-027 App cannot resolve write secrets  
T17-STR-028 Certification APIs cannot execute  
T17-STR-029 Default production writes disabled  
T17-STR-030 Fresh empty cert state cannot mutate  

### 31.2 Database (T17-DB)

T17-DB-001 FK to `production_network_target`  
T17-DB-002 Unique `(logical_id, version_no)`  
T17-DB-003 Unique content digest per logical id  
T17-DB-004 Immutable certified columns reject UPDATE  
T17-DB-005 Status CHECK enums  
T17-DB-006 SHA-256 format CHECK  
T17-DB-007 Effective interval CHECK  
T17-DB-008 Single CURRENT target certification  
T17-DB-009 Evidence supersession unique ACTIVE  
T17-DB-010 Bundle snapshot binds TLS+network versions  
T17-DB-011 Credential profile target NOT NULL for ACTIVE PROD  
T17-DB-012 Endpoint unique ACTIVE per target  
T17-DB-013 Expiry query returns due rows  
T17-DB-014 Revocation query  
T17-DB-015 Concurrent transition unique violation handled  
T17-DB-016 No delete API/repository delete of cert rows  
T17-DB-017 Approval FK  
T17-DB-018 Bundle evidence set digest  
T17-DB-019 Onboarding SoD CHECK  
T17-DB-020 ATOMIC default false  
T17-DB-021 Egress NOT 0.0.0.0/0 for PROD  
T17-DB-022 Hostname verification required for non-DRAFT PROD TLS  
T17-DB-023 `atomic_certified` column + ATOMIC CHECK  
T17-DB-024 `source_baseline_sha` CHAR(40) lowercase CHECK  
T17-DB-025 no reverse `approval_id` FK; insert order interface then approval  
T17-DB-026 FK catalogue RESTRICT/NO ACTION  
T17-DB-027 partial unique current profiles/bundles/target certs  
T17-DB-028 PREPROD/PROD-active profiles require `production_target_id`  
T17-DB-029 invalidation idempotency unique key  
T17-DB-030 bundle digest canonical bytes  
T17-DB-031 ISSUED-only grant UPDATE predicate on existing Phase 16 grant table  

### 31.3 Behavioral (T17-IMPL)

T17-IMPL-001 Legal transitions accept  
T17-IMPL-002 Unknown transition reject  
T17-IMPL-003 REVOKED cannot reactivate  
T17-IMPL-004 EXPIRED requires recertify  
T17-IMPL-005 SUSPENDED no auto resume  
T17-IMPL-006 PRODUCTION_REGISTERED ≠ execute  
T17-IMPL-007 Interface revoke cascade  
T17-IMPL-008 Documentation withdraw cascade  
T17-IMPL-009 Approval revoke cascade  
T17-IMPL-010 Consumed grant not reset  
T17-IMPL-011 Unconsumed grant revoked  
T17-IMPL-012 Phase16 auth marked STALE  
T17-IMPL-013 Bundle invalid on component change  
T17-IMPL-014 New version on material change  
T17-IMPL-015 Evidence hash-only rejected  
T17-IMPL-016 Evidence supersession hides old PASS  
T17-IMPL-017 Failed recertify does not fallback  
T17-IMPL-018 SoD CREATE=REVIEW rejected  
T17-IMPL-019 SECURITY_CERTIFY=TRANSPORT_CERTIFY at L3 rejected  
T17-IMPL-020 Executor self-onboard rejected  
T17-IMPL-021 Requester complete-stack certify rejected  
T17-IMPL-022 Authorizer self-certify rejected  
T17-IMPL-023 Full-stack permission set rejected  
T17-IMPL-024 Capability only CELL/txPower  
T17-IMPL-025 Cardinality one cell/parameter  
T17-IMPL-026 ATOMIC without evidence denied  
T17-IMPL-027 READ_THEN_WRITE default  
T17-IMPL-028 Unknown vendor version deny  
T17-IMPL-029 Predicate auto-expand rejected  
T17-IMPL-030 Out-of-range suspend+deny  
T17-IMPL-031 HEALTHY does not override P16 suspend  
T17-IMPL-032 DEGRADED denies  
T17-IMPL-033 Unknown health denies  
T17-IMPL-034 Level4 function not flag  
T17-IMPL-035 L3 target without P16 auth denies  
T17-IMPL-036 Current P16 auth + stale cert denies  
T17-IMPL-037 Cache positive not sufficient  
T17-IMPL-038 Authority timeout denies  
T17-IMPL-039 Artifact mismatch denies  
T17-IMPL-040 Endpoint mismatch denies  
T17-IMPL-041 Target-bound credential required  
T17-IMPL-042 Generic credential denies  
T17-IMPL-043 TLS hostname disable rejected  
T17-IMPL-044 Trust-all rejected  
T17-IMPL-045 Scheduler does not execute  
T17-IMPL-046 Event consumer does not execute  
T17-IMPL-047 No auto rollback  
T17-IMPL-048 POSITIVE_NOT_SENT requires proof  
T17-IMPL-049 Timeout → MAY_HAVE_SENT  
T17-IMPL-050 PUT/same-value ≠ idempotent retry  
T17-IMPL-051 ISSUED affected grant → REVOKED via Phase 16 authority  
T17-IMPL-052 CONSUMED affected grant unchanged  
T17-IMPL-053 EXPIRED grant unchanged  
T17-IMPL-054 REVOKED grant unchanged  
T17-IMPL-055 unrelated target ISSUED grant unchanged  
T17-IMPL-056 no replacement grant / no grant reset  
T17-IMPL-057 invalidation single transaction rollback (no cert-revoked+grant-ISSUED commit)  
T17-IMPL-058 deterministic lock order  
T17-IMPL-059 repeat invalidation idempotent; no resurrection  
T17-IMPL-060 no `ExpectedStateStrength` type; reuse `ExpectedStateGuardStrength`  
T17-IMPL-061 LAB_CERTIFIED requires TRANSPORT+CAPABILITY+SECURITY_CERTIFY  
T17-IMPL-062 hostname verification required for PREPROD/PRODUCTION-eligible profiles  
T17-IMPL-063 packaged artifact identity vs config claim  
T17-IMPL-064 missing runtime identity DENY  
T17-IMPL-065 malformed runtime identity DENY  
T17-IMPL-066 mutation client/resilience stack has retries disabled  
T17-IMPL-067 PRE-SEND retry only before dispatch with positive not-sent proof  
T17-IMPL-068 uppercase digest input rejected for new Phase 17 fields  
T17-IMPL-069 bundle digest field-order canonicalization  
T17-IMPL-070 null vs empty bundle field distinct digests  

### 31.4 Integration (T17-INT)

T17-INT-001 Gateway preflight after consume  
T17-INT-002 Unconfigured transport still denies production send  
T17-INT-003 Controlled transport unknown+desired VERIFIED  
T17-INT-004 Controlled transport unknown+expected stop  
T17-INT-005 Controlled transport unknown+third MANUAL  
T17-INT-006 Controlled transport unknown+unavailable UNRESOLVED  
T17-INT-007 Independent readback required  
T17-INT-008 Rollback still separately governed  
T17-INT-009 App API cannot pass endpoint  
T17-INT-010 App API cannot pass credential  
T17-INT-011 Grant consume then cert revoke denies  
T17-INT-012 Multi-replica stale cache simulation  
T17-INT-013 Startup artifact validate  
T17-INT-014 Destination trust hook deny  
T17-INT-015 Credential binder arguments  
T17-INT-016 Kill switch still wins  
T17-INT-017 Lease/fence still wins  
T17-INT-018 Rate/blast still wins  
T17-INT-019 Window/change-control still wins  
T17-INT-020 Existing Phase16 execute path unchanged when uncertified  
T17-INT-021 Onboarding cannot bypass consume  
T17-INT-022 Certification API 403 for agent principal  
T17-INT-023 Certification API 403 for MCP  
T17-INT-024 Concurrent revoke vs preflight  
T17-INT-025 Concurrent suspend vs preflight  
T17-INT-026 Concurrent endpoint change vs preflight  
T17-INT-027 Duplicate onboard approve  
T17-INT-028 Audit events contain no secrets  
T17-INT-029 Metrics exclude cell labels  
T17-INT-030 Fresh deploy empty cert DENY  
T17-INT-031 consume vs revoke Case A: ISSUED→REVOKED; consume fails; mutation 0  
T17-INT-032 consume vs revoke Case B: ISSUED→CONSUMED; invalidation does not rewrite CONSUMED  
T17-INT-033 invalidation before dispatch DENY; mutation 0  
T17-INT-034 invalidation after dispatch; mutation 1; MAY_HAVE_SENT; no resend  
T17-INT-035 destination identity harness FQDN/port/TLS/trust/hostname/network mismatch  
T17-INT-036 config claims A packaged identity B DENY  
T17-INT-037 certified A + runtime A eligible subject to all other controls  
T17-INT-038 delayed/missing outbox must not grant eligibility if DB reachable  

### 31.5 Security (T17-SEC)

T17-SEC-001 Caller endpoint injection  
T17-SEC-002 Caller credential override  
T17-SEC-003 Agent certification  
T17-SEC-004 MCP certification  
T17-SEC-005 Executor self-onboarding  
T17-SEC-006 Invalid SoD combination  
T17-SEC-007 Generic vendor credential  
T17-SEC-008 Target mismatch credential  
T17-SEC-009 Unapproved artifact  
T17-SEC-010 Expired cert  
T17-SEC-011 Revoked cert  
T17-SEC-012 Superseded interface  
T17-SEC-013 Withdrawn approval  
T17-SEC-014 TLS profile inactive  
T17-SEC-015 Network profile inactive  
T17-SEC-016 Unknown vendor version  
T17-SEC-017 Capability not certified  
T17-SEC-018 Secret not in API/audit/exception  
T17-SEC-019 No older secret fallback  
T17-SEC-020 No write secret in app process  
T17-SEC-021 null requester DENY at policy/service (no HTTP)  
T17-SEC-022 blank requester DENY at policy/service  
T17-SEC-023 null reviewer DENY at policy/service  
T17-SEC-024 blank reviewer DENY at policy/service  
T17-SEC-025 null certifier DENY at policy/service  
T17-SEC-026 blank certifier DENY at policy/service  
T17-SEC-027 invalid executor identity DENY  
T17-SEC-028 same principal forbidden SoD DENY  
T17-SEC-029 environment-variable digest is not artifact authenticity  
T17-SEC-030 mutation-path retry interceptors absent/disabled on client stack  

### 31.6 Infrastructure (T17-INF)

T17-INF-001 Separate gateway deploy remains  
T17-INF-002 Write WI distinct  
T17-INF-003 No 0.0.0.0/0 write egress described  
T17-INF-004 Packaged build manifest + optional runtime image digest; env var not authenticity  
T17-INF-005 NetworkPolicy additional, not replacement  
T17-INF-006 KV write secret scoped to gateway  
T17-INF-007 Rotation does not cache values  
T17-INF-008 Observability labels bounded  

These are specification/review evidence for later infra change; default CI MUST NOT require Azure/ENM.

---

## 32. High-risk proof index

Every row MUST have BEHAVIORAL and/or INTEGRATION (plus SECURITY where listed). Structural-only is forbidden.

| Invariant | Gates | Required proof IDs |
|---|---|---|
| Revocation cascade | G17-134 G17-135 G17-154 | T17-IMPL-007/008/009, T17-INT-011, FI17-004/005, CS17-C/Z |
| Phase16 grant revoke ISSUED predicate | G17-134 G17-136 G17-154 | T17-IMPL-051–055, T17-DB-031, T17-INT-031/032 |
| CONSUMED grant preservation | G17-136 | T17-IMPL-010/052, T17-INT-032, FI17-020 |
| Invalidation single transaction | G17-154 | T17-IMPL-057, T17-DB-029 |
| Invalidation lock order | G17-154 | T17-IMPL-058 |
| Invalidation idempotency | G17-154 | T17-IMPL-059, T17-DB-029 |
| Invalidation vs consume race | G17-136 G17-154 | T17-INT-031/032 |
| Invalidation vs send race | G17-138 G17-070 | T17-INT-033/034, FI17-017–019 |
| Outbox not execution authority | G17-137 G17-139 | T17-INT-038, FI17-003/012 |
| Send-boundary currentness | G17-137 G17-138 | T17-INT-001, FI17-004–016, CS17-B–H |
| Cache fail-closed | G17-139 | T17-IMPL-037, FI17-003, CS17-I |
| Multi-replica | G17-140 | T17-INT-012, FI17-012, CS17-J |
| Runtime artifact trust source | G17-090 G17-141 G17-142 | T17-IMPL-063–065, T17-INT-013/036/037, T17-SEC-029, CS17-X |
| Target-bound credential | G17-044 G17-148 | T17-IMPL-041/042, T17-SEC-007/008, FI17-007, CS17-F |
| Endpoint / DNS / TLS | G17-039 G17-146 G17-147 | T17-IMPL-040, T17-INT-014, T17-SEC-001/014, FI17-009, CS17-D |
| Destination identity harness | G17-047 G17-146 G17-147 | T17-INT-035, T17-IMPL-062 |
| Hostname verification PREPROD/PROD | G17-047 G17-049 | T17-DB-022, T17-IMPL-043/062 |
| Exact transition permissions | G17-094 G17-143 | T17-IMPL-061, T17-IMPL-018/019 |
| Null/blank principal fail-closed | G17-093 G17-144 G17-145 | T17-SEC-021–028, CS17-S |
| Level 4 currentness | G17-028 G17-029 G17-155 | T17-IMPL-034–036, CS17-A/B, FI17-015 |
| Mutation-path retries disabled | G17-069 G17-070 G17-149 G17-156 | T17-IMPL-066/067, T17-SEC-030, FI17-017–020, CS17-N |
| Exact mutation counts | G17-070 G17-081 G17-082 | FI17-001–020, CS17-A–Z, T17-INT-003–006 |
| Ambiguous outcome | G17-081 G17-082 | T17-INT-003–006, CS17-O–R |
| Health composition | G17-083 G17-084 G17-150 | T17-IMPL-031–033, FI17-014, CS17-Y |
| Evidence trust / supersession | G17-151 G17-152 | T17-IMPL-015–017, FI17-013 |
| Bundle digest canonicalization | G17-088 G17-157 | T17-DB-030, T17-IMPL-069/070 |
| Hash/digest constraints | G17-008 G17-090 | T17-DB-006/024, T17-IMPL-068 |
| ExpectedStateGuardStrength reuse | G17-062 G17-063 | T17-IMPL-026/027/060, CS17-M |
| Lifecycle transitions | G17-020 G17-143 | T17-IMPL-001–005 |
| Unknown cert deny | G17-099 | T17-IMPL-038, T17-INT-030, CS17-H |
| No protocol guessing / unconfigured transport | G17-005 G17-006 G17-017 | T17-STR-003/005/024, T17-INT-002 |
| Agent/MCP no certify or execute | G17-095 G17-115 G17-116 | T17-SEC-003/004, CS17-T/U |

High-risk item count: **32**. High-risk structural-only count: **0**. Protocol-guessing row includes STRUCTURAL plus INTEGRATION (`T17-INT-002`); it is not structural-only.

---

## 33. Threat → implementation / test mapping (T17-01–T17-33)

| Threat | Mitigating components | Gates | Implementation requirements | Evidence | CI vs external |
|---|---|---|---|---|---|
| T17-01 Fake interface | `VendorInterfaceDefinitionService`, `VENDOR_INTERFACE_REVIEW` | G17-007 G17-134 | Authoritative-source only; no protocol fields | T17-IMPL-007, T17-STR-024, EXT17-L1-001 | CI + EXT NOT EXECUTED |
| T17-02 Unapproved protocol | Unconfigured transport; no protocol client | G17-005 G17-010 | No guessed REST/NETCONF/CLI | T17-STR-003/005/024, T17-INT-002 | CI |
| T17-03 Endpoint substitution | Endpoint profile + invalidation | G17-039 G17-146 | Change ⇒ cascade | T17-IMPL-040, FI17-009, CS17-D | CI |
| T17-04 Credential substitution | Target-bound credential profile | G17-044 G17-148 | Bind target+profile+WI | T17-IMPL-041, FI17-007, CS17-F | CI |
| T17-05 Certificate spoofing | TLS profile; hostname verify | G17-047 G17-147 | No trust-all | T17-DB-022, T17-SEC-014, EXT17-L1-005 | CI + EXT |
| T17-06 Transport downgrade | Immutable certified profile | G17-014 G17-157 | Recertify on TLS/network change | T17-IMPL-013, T17-DB-010 | CI |
| T17-07 Version drift | `VendorVersionCompatibility` | G17-053 G17-054 | Unknown/out-of-range DENY | T17-IMPL-028–030, CS17-K | CI |
| T17-08 Capability drift | Capability cert version bind | G17-060 | Recertify per release | T17-IMPL-024, CS17-L | CI |
| T17-09 Stale certification | Expiry at send boundary | G17-096 G17-137 | Expired DENY | T17-IMPL-004, CS17-V | CI |
| T17-10 Forged evidence | Evidence trust authority | G17-151 | Hash-only reject | T17-IMPL-015 | CI |
| T17-11 Privilege escalation | SoD policy | G17-144 G17-145 | Certifier ≠ executor | T17-IMPL-020–022, CS17-S | CI |
| T17-12 Onboarding bypass | Gateway requires CURRENT target cert | G17-098 G17-145 | Missing onboarding DENY | T17-INT-020/030 | CI |
| T17-13 Implementation substitution | Artifact bind | G17-141 G17-142 | Digest compare | T17-IMPL-039, CS17-X | CI |
| T17-14 Artifact mismatch | Runtime identity provider | G17-090 G17-142 | A≠B DENY | FI17-011, CS17-E | CI |
| T17-15 Response spoofing | Independent readback | G17-067 | Ack ≠ applied | T17-INT-007, EXT17-L1-010 | CI + EXT |
| T17-16 Readback spoofing | Freshness rules | G17-075 | Only fresh desired | T17-INT-007, EXT17-L1-011 | CI + EXT |
| T17-17 Duplicate write | MAY_HAVE_SENT no retry | G17-070 G17-149 | No second send | T17-IMPL-049, FI17-017–020, CS17-N | CI |
| T17-18 Cross-target credential | Target-bound profile | G17-148 | Generic deny | T17-SEC-007/008 | CI |
| T17-19 Lab/prod confusion | Environment/target class bind | G17-146 | Profile class match | T17-INT-014, T17-SEC-001 | CI |
| T17-20 Unsafe failover | No uncertified failover | G17-140 | Partition DENY | FI17-012, CS17-J | CI |
| T17-21 Network policy bypass | Network profile; no 0.0.0.0/0 | G17-147 | Egress CHECK | T17-DB-021, T17-INF-003 | CI |
| T17-22 Agent/MCP bypass | Permission deny | G17-095 G17-115 | Agent/MCP 403 | T17-SEC-003/004, CS17-T/U | CI |
| T17-23 L3 as L4 | Level4Satisfied function | G17-021 G17-028 G17-155 | L3≠L4 | T17-IMPL-034/035, CS17-A | CI |
| T17-24 Endpoint injection | Reject request endpoint | G17-035 | API validation | T17-SEC-001, T17-INT-009 | CI |
| T17-25 Secret persistence | Schema forbids values | G17-043 | No secret columns | T17-STR-023, T17-SEC-018 | CI |
| T17-26 Auto re-enable | TARGET_REACTIVATE only | G17-085 G17-143 | No auto resume | T17-IMPL-003/005 | CI |
| T17-27 Closed loop | Structural absence | G17-120 | No optimizer execute | T17-STR-017, T17-IMPL-045/046 | CI |
| T17-28 ATOMIC false claim | ATOMIC certified flag | G17-062 G17-064 | Lab timing ≠ CAS | T17-IMPL-026, CS17-M | CI |
| T17-29 DNS substitution | Identity tuple + TLS | G17-146 G17-147 | DNS not sufficient | T17-INT-014, EXT17-L1-003 | CI + EXT |
| T17-30 Cache/split-brain | Durable authority | G17-137 G17-139 G17-140 | Stale cache DENY | FI17-003/012, CS17-I/J | CI |
| T17-31 Role concentration | SoD combinations | G17-144 G17-145 | Full-stack deny | T17-IMPL-023 | CI |
| T17-32 Untrusted evidence | Evidence + supersession | G17-151 G17-152 | No lone hash | T17-IMPL-015–017 | CI |
| T17-33 Onboard/currentness race | Send-boundary recheck | G17-138 G17-145 | Post-grant change DENY | FI17-004–010, CS17-B–G | CI |

No threat is unmapped.

---

## 34. Architecture gate → evidence map (G17-001–G17-158)

Machine-readable equivalent: `docs/implementation/phase17-gate-evidence-map.json`.

JSON `evidenceTypes` are the **full names** of the Markdown Proof-column letters: S=`STRUCTURAL`, B=`BEHAVIORAL`, I=`INTEGRATION`, D=`DATABASE`, SEC=`SECURITY`, INF=`INFRASTRUCTURE`, EXT=`EXTERNAL_CERTIFICATION`. They are proof-category labels, not a second evidence catalogue. Markdown Evidence IDs and JSON `evidenceIds` MUST be semantically equivalent after range expansion.

Legend: S=STRUCTURAL B=BEHAVIORAL I=INTEGRATION D=DATABASE SEC=SECURITY INF=INFRASTRUCTURE EXT=EXTERNAL_CERTIFICATION. Status at spec freeze is SPECIFIED for local evidence; EXT remains NOT EXECUTED / NOT SATISFIED.

| Gate | Requirement | Component | Proof | Evidence IDs | CI | Freeze |
|---|---|---|---|---|---|---|
| G17-001 | Phase 16 governance unreplaced | ProductionChangeService + Invalidation | B,I | T17-STR-001, T17-INT-016–019 | L0 | SPECIFIED |
| G17-002 | App no write creds / mutation | App isolation | S,SEC | T17-STR-027, T17-SEC-020 | L0 | SPECIFIED |
| G17-003 | Separate gateway | production-write-gateway | S,INF | T17-STR-002/009, T17-INF-001 | L0 | SPECIFIED |
| G17-004 | Durable cert authority | ProductionCertificationAuthority | B,I | T17-IMPL-037/038, T17-INT-001 | L0 | SPECIFIED |
| G17-005 | No protocol guessing | Spec + structural scan | S | T17-STR-003/024 | L0 | SPECIFIED |
| G17-006 | EricssonWriteTransport unresolved | Transport SPI | S,I | T17-STR-005, T17-INT-002 | L0 | SPECIFIED |
| G17-007 | Authoritative interface required | VendorInterfaceDefinitionService | B,EXT | T17-IMPL-007, EXT17-L1-001 | L0+EXT | EXT NOT EXECUTED |
| G17-008 | Interface fields complete | VendorInterfaceDefinition | D,B | T17-DB-002, T17-STR-024 | L0 | SPECIFIED |
| G17-009 | Docs are metadata only | Schema | S,D | T17-STR-023, T17-DB-001 | L0 | SPECIFIED |
| G17-010 | No invented protocol fields | Structural scan | S | T17-STR-024 | L0 | SPECIFIED |
| G17-011 | Selected type UNRESOLVED | Interface type category | S,B | T17-STR-024, T17-IMPL-007 | L0 | SPECIFIED |
| G17-012 | EnmTransport read-only | EnmTransport | S | T17-STR-004 | L0 | SPECIFIED |
| G17-013 | Transport profile bindings | VendorWriteTransportProfile | D | T17-DB-010 | L0 | SPECIFIED |
| G17-014 | Certified profiles immutable | Profile service | B,D | T17-IMPL-014, T17-DB-004 | L0 | SPECIFIED |
| G17-015 | Gateway resolves certified only | CertifiedTransportResolver | I | T17-INT-001/002 | L0 | SPECIFIED |
| G17-016 | No dynamic plugin load | Structural | S | T17-STR-010 | L0 | SPECIFIED |
| G17-017 | Unconfigured production path | UnconfiguredProductionEricssonWriteTransport | S,I | T17-STR-005, T17-INT-002 | L0 | SPECIFIED |
| G17-018 | SPI layering preserved | Adapters | S | T17-STR-016 | L0 | SPECIFIED |
| G17-019 | Registries/services defined | §7 services | S,B | T17-STR-008, T17-IMPL-001 | L0 | SPECIFIED |
| G17-020 | Lifecycle states defined | TransportCertificationState | B | T17-IMPL-001/002 | L0 | SPECIFIED |
| G17-021 | PRODUCTION_REGISTERED ≠ execute | Lifecycle + preflight | B,I | T17-IMPL-006, CS17-A | L0 | SPECIFIED |
| G17-022 | Auto safety only; no auto re-enable | Lifecycle | B | T17-IMPL-003/005 | L0 | SPECIFIED |
| G17-023 | L0 local only | CI plan | I | T17-INT-002, T17-STR-029 | L0 | SPECIFIED |
| G17-024 | L1 lab requirements | External catalogue | EXT | EXT17-L1-001–022 | EXT | NOT EXECUTED |
| G17-025 | First real integration is L1 | External plan | EXT | EXT17-L1-009 | EXT | NOT EXECUTED |
| G17-026 | L2 production-equivalence | External catalogue | EXT | EXT17-L2-001–015 | EXT | NOT EXECUTED |
| G17-027 | L3 zero mutation | Onboarding | B,EXT | T17-IMPL-006, EXT17-L3-001–016 | L0+EXT | L3 NOT SATISFIED |
| G17-028 | L4 = current P16 auth + P17 certs | Level4Satisfied | B,I | T17-IMPL-034–036, CS17-A/B | L0 | SPECIFIED |
| G17-029 | L3 ≠ L4 | Preflight | B | T17-IMPL-035, CS17-A | L0 | SPECIFIED |
| G17-030 | Architecture ≠ L4 | Spec status | S | T17-STR-014 | L0 | SPECIFIED |
| G17-031 | Code/CI ≠ L4 | Defaults | S,I | T17-STR-029/030, T17-INT-030 | L0 | SPECIFIED |
| G17-032 | ProductionNetworkTarget authoritative | Onboarding | I | T17-INT-021 | L0 | SPECIFIED |
| G17-033 | Onboarding not second mutation authority | Onboarding + gateway | I | T17-INT-021 | L0 | SPECIFIED |
| G17-034 | Onboarding bindings | Onboarding version | D | T17-DB-001/018 | L0 | SPECIFIED |
| G17-035 | No caller endpoint/credential | APIs | SEC,I | T17-SEC-001/002, T17-INT-009/010 | L0 | SPECIFIED |
| G17-036 | No Agent/MCP/proposal endpoint | APIs | SEC | T17-SEC-001/003/004 | L0 | SPECIFIED |
| G17-037 | Endpoint is infra data | Endpoint profile | S,D | T17-STR-007, T17-DB-012 | L0 | SPECIFIED |
| G17-038 | Endpoint ≠ credential | Schema | S,D | T17-STR-023, T17-DB-011 | L0 | SPECIFIED |
| G17-039 | Endpoint change cascade | Invalidation | B,I | T17-IMPL-040, FI17-009, CS17-D | L0 | SPECIFIED |
| G17-040 | Phase 10 secret authority | Credential binder | S,SEC | T17-STR-025, T17-SEC-019 | L0 | SPECIFIED |
| G17-041 | Read/write identity separate | Infra/security | S,INF | T17-STR-026, T17-INF-002 | L0 | SPECIFIED |
| G17-042 | Only gateway resolves write creds | Isolation | S,SEC | T17-STR-027, T17-SEC-020 | L0 | SPECIFIED |
| G17-043 | No secrets in DB | V18 | S,D | T17-STR-023, T17-DB-011 | L0 | SPECIFIED |
| G17-044 | Credential change cascade | Invalidation | B,I | T17-IMPL-041, FI17-007, CS17-F | L0 | SPECIFIED |
| G17-045 | Secret fail-closed | Credential binder | SEC | T17-SEC-019 | L0 | SPECIFIED |
| G17-046 | No secret fallback/cache | Credential binder | SEC | T17-SEC-019 | L0 | SPECIFIED |
| G17-047 | TLS hostname verification | TLS profile | D,SEC,I | T17-DB-022, T17-SEC-014, T17-IMPL-062, T17-INT-035 | L0 | SPECIFIED |
| G17-048 | Trusted CA; no trust-all | TLS profile | B,SEC | T17-IMPL-044, T17-SEC-014 | L0 | SPECIFIED |
| G17-049 | Hostname-verify disable prohibited | TLS CHECK | D,B | T17-DB-022, T17-IMPL-043 | L0 | SPECIFIED |
| G17-050 | No insecure TLS fallback | TLS profile | B | T17-IMPL-043/044 | L0 | SPECIFIED |
| G17-051 | Security-profile change invalidates | Invalidation | B | T17-IMPL-013, FI17-008 | L0 | SPECIFIED |
| G17-052 | mTLS keys via secure mechanism | Credential refs | S,SEC | T17-STR-023, T17-SEC-018 | L0 | SPECIFIED |
| G17-053 | Explicit version predicate | Compatibility | B | T17-IMPL-028/029 | L0 | SPECIFIED |
| G17-054 | Out-of-range suspend | Compatibility | B | T17-IMPL-030, CS17-K | L0 | SPECIFIED |
| G17-055 | No auto-expand predicate | Compatibility | B | T17-IMPL-029 | L0 | SPECIFIED |
| G17-056 | Unknown version denies | Preflight | B | T17-IMPL-028, T17-SEC-016 | L0 | SPECIFIED |
| G17-057 | CELL/txPower only | Capability | B | T17-IMPL-024 | L0 | SPECIFIED |
| G17-058 | Cardinality 1/1/1 | Capability + P16 | B | T17-IMPL-025 | L0 | SPECIFIED |
| G17-059 | Capability metadata complete | Capability table | D | T17-DB-002 | L0 | SPECIFIED |
| G17-060 | Capability version-specific | Capability | B | T17-IMPL-024, CS17-L | L0 | SPECIFIED |
| G17-061 | No generic commands | SPI | S,B | T17-STR-016, T17-IMPL-025 | L0 | SPECIFIED |
| G17-062 | ATOMIC only if CAS proven | Expected-state | B | T17-IMPL-026, CS17-M | L0 | SPECIFIED |
| G17-063 | Else READ_THEN_WRITE | Expected-state | B | T17-IMPL-027 | L0 | SPECIFIED |
| G17-064 | No ATOMIC from lab timing | Expected-state | B | T17-IMPL-026 | L0 | SPECIFIED |
| G17-065 | Direct vendor observe before mutate | Phase16 preflight | I | T17-INT-007 | L0 | SPECIFIED |
| G17-066 | Ack meaning certified | External | EXT | EXT17-L1-010 | EXT | NOT EXECUTED |
| G17-067 | Ack ≠ applied | Readback | I,EXT | T17-INT-007, EXT17-L1-010 | L0+EXT | SPECIFIED+NOT EXECUTED |
| G17-068 | Exception ≠ not-applied | Send class | B | T17-IMPL-049 | L0 | SPECIFIED |
| G17-069 | Every failure classified | Send class | B | T17-IMPL-048/049 | L0 | SPECIFIED |
| G17-070 | No retry after MAY_HAVE_SENT | Attempt state | B,I | T17-IMPL-049, T17-IMPL-066, FI17-017–020, CS17-N | L0 | SPECIFIED |
| G17-071 | HTTP method ≠ retry authority | Send class | B | T17-IMPL-050 | L0 | SPECIFIED |
| G17-072 | Bounded retry only POSITIVE_NOT_SENT | Send class | B | T17-IMPL-048 | L0 | SPECIFIED |
| G17-073 | Vendor IDs correlation only | Protocol DTO | S,B | T17-STR-023, T17-IMPL-050 | L0 | SPECIFIED |
| G17-074 | Independent readback certified | Readback | I,EXT | T17-INT-007, EXT17-L1-011 | L0+EXT | SPECIFIED+NOT EXECUTED |
| G17-075 | Fresh desired-state only VERIFIED | Verification | I | T17-INT-003/007 | L0 | SPECIFIED |
| G17-076 | No canonical radio_configuration write | Isolation | S | T17-STR-008 | L0 | SPECIFIED |
| G17-077 | Rollback = Phase14 value | Rollback cert | I | T17-INT-008 | L0 | SPECIFIED |
| G17-078 | Separate rollback governance | Phase16 rollback | I | T17-INT-008 | L0 | SPECIFIED |
| G17-079 | Transport rollback ≠ auto rollback | Policy | B | T17-IMPL-047 | L0 | SPECIFIED |
| G17-080 | Named failures at cert level | FI + EXT | I,EXT | FI17-017–019, EXT17-L1-013–016 | L0+EXT | SPECIFIED+NOT EXECUTED |
| G17-081 | Ambiguous four-way table | Controlled transport | I | T17-INT-003–006, CS17-O–R | L0 | SPECIFIED |
| G17-082 | Desired/expected/third/unavailable | Controlled transport | I | CS17-O–R | L0 | SPECIFIED |
| G17-083 | Health states defined | TransportHealthState | B | T17-IMPL-031–033 | L0 | SPECIFIED |
| G17-084 | Non-healthy blocks | Health composer | B | T17-IMPL-032/033, FI17-014 | L0 | SPECIFIED |
| G17-085 | No auto re-enable after security/capability | Lifecycle | B | T17-IMPL-005 | L0 | SPECIFIED |
| G17-086 | Evidence metadata defined | Evidence table | D | T17-DB-009 | L0 | SPECIFIED |
| G17-087 | No false immutable-store claim | Spec | S | T17-STR-023 | L0 | SPECIFIED |
| G17-088 | Bundle bindings complete | Bundle | D | T17-DB-010/018 | L0 | SPECIFIED |
| G17-089 | Component change invalidates bundle | Invalidation | B | T17-IMPL-013 | L0 | SPECIFIED |
| G17-090 | Authoritative artifact identity | Artifact | B,I,SEC | T17-IMPL-039, T17-IMPL-063–065, T17-INT-013, T17-INT-036/037, T17-SEC-029 | L0 | SPECIFIED |
| G17-091 | No false SBOM/provenance claim | Artifact | S | T17-STR-023 | L0 | SPECIFIED |
| G17-092 | Deployed vs certified compare | Preflight | B,I | T17-IMPL-039, FI17-011, CS17-X | L0 | SPECIFIED |
| G17-093 | Certifier ≠ requester/authorizer/executor | SoD | B,SEC | T17-IMPL-020–022, T17-SEC-005 | L0 | SPECIFIED |
| G17-094 | Permissions + accepted combos | SoD | B,SEC | T17-IMPL-018/019/023, T17-IMPL-061, T17-SEC-021–028 | L0 | SPECIFIED |
| G17-095 | Agents/MCP no certify | Guards | SEC,I | T17-SEC-003/004, T17-INT-022/023 | L0 | SPECIFIED |
| G17-096 | Expiry/revocation block mutation | Preflight | B | CS17-V, T17-IMPL-011 | L0 | SPECIFIED |
| G17-097 | Named expiry/revoke triggers | Invalidation | B | T17-IMPL-007–013, FI17-004–015 | L0 | SPECIFIED |
| G17-098 | Gateway proves full cert set + L4 | Preflight | I | T17-INT-001, CS17-A | L0 | SPECIFIED |
| G17-099 | Unknown cert denies | Authority | B | T17-IMPL-038, CS17-H | L0 | SPECIFIED |
| G17-100 | Phase16 checks remain | Gateway | I | T17-INT-016–019 | L0 | SPECIFIED |
| G17-101 | First prod mutation not silent | Defaults | S,B | T17-STR-029, T17-IMPL-006 | L0 | SPECIFIED |
| G17-102 | Future L4 exercise constraints | Spec | S,EXT | T17-STR-014, EXT17-L3-016 | EXT | L4 NOT SATISFIED |
| G17-103 | This spec does not authorize L4 | Status | S | T17-STR-014 | L0 | SPECIFIED |
| G17-104 | Metrics exclude secrets/high-card | Metrics | I | T17-INT-029 | L0 | SPECIFIED |
| G17-105 | High-priority alerts listed | Alerts | S,I | T17-INF-008, T17-INT-028 | L0 | SPECIFIED |
| G17-106 | Certification audit separate | Phase17 audit | B,I | T17-INT-028 | L0 | SPECIFIED |
| G17-107 | Audit no secrets/payloads | Audit | I,SEC | T17-INT-028, T17-SEC-018 | L0 | SPECIFIED |
| G17-108 | Vendor-neutral above adapter | SPI | S | T17-STR-016 | L0 | SPECIFIED |
| G17-109 | Nokia deferred | Structural | S | T17-STR-011 | L0 | SPECIFIED |
| G17-110 | V18 specified only at authoring | This task | S | T17-STR-006 | L0 | SPECIFIED |
| G17-111 | V1–V17 unchanged | Flyway | S | T17-STR-006 | L0 | SPECIFIED |
| G17-112 | Cert in app; resolve in gateway | Packages | S | T17-STR-008/009 | L0 | SPECIFIED |
| G17-113 | Transport not in app | Isolation | S | T17-STR-003/009 | L0 | SPECIFIED |
| G17-114 | Dependency direction 13→…→interface | Packages | S | T17-STR-001/016 | L0 | SPECIFIED |
| G17-115 | Agent execute unauthorized | Guards | SEC | T17-SEC-003, T17-STR-019 | L0 | SPECIFIED |
| G17-116 | MCP execute unauthorized | Guards | SEC | T17-SEC-004, T17-STR-018 | L0 | SPECIFIED |
| G17-117 | Scheduled execute unauthorized | Isolation | B,S | T17-IMPL-045, T17-STR-017 | L0 | SPECIFIED |
| G17-118 | Event execute unauthorized | Isolation | B | T17-IMPL-046 | L0 | SPECIFIED |
| G17-119 | Auto rollback unauthorized | Policy | B | T17-IMPL-047 | L0 | SPECIFIED |
| G17-120 | Closed loop unauthorized | Isolation | S,B | T17-STR-017, T17-IMPL-045 | L0 | SPECIFIED |
| G17-121 | Default CI independent | CI | I | T17-INT-002 | L0 | SPECIFIED |
| G17-122 | CI ≠ vendor certification | Spec | S | T17-STR-030 | L0 | SPECIFIED |
| G17-123 | L1–L4 evidence layers distinct | Catalogues | S,EXT | EXT17-L1-001–022, EXT17-L2-001–015, EXT17-L3-001–016 | EXT | DISTINCT / NOT EXECUTED |
| G17-124 | Infra described not implemented now | Infra | INF | T17-INF-001–008 | review | SPECIFIED |
| G17-125 | No real endpoint in spec | Spec/tests | S | T17-STR-007 | L0 | SPECIFIED |
| G17-126 | No real credential in spec | Spec/tests | S,SEC | T17-STR-007, T17-SEC-018 | L0 | SPECIFIED |
| G17-127 | Production default disabled | Config | S,I | T17-STR-029, T17-INT-002 | L0 | SPECIFIED |
| G17-128 | Empty cert cannot mutate | Preflight | I | T17-INT-030, T17-STR-030 | L0 | SPECIFIED |
| G17-129 | Threats T17-01–33 present | §33 | S | T17-STR-022 | L0 | SPECIFIED |
| G17-130 | Impl/spec/P18 not started by architecture | Status | S | T17-STR-012 | L0 | SPECIFIED |
| G17-131 | Auth method UNRESOLVED | Spec | S | T17-STR-024 | L0 | SPECIFIED |
| G17-132 | L1/L2/L3/L4 unsatisfied/not executed | Status | EXT | EXT17-L1-001–022, EXT17-L2-001–015, EXT17-L3-001–016 | EXT | NOT EXECUTED / NOT SATISFIED |
| G17-133 | Real production execution NOT AUTHORIZED | Status | S | T17-STR-029 | L0 | SPECIFIED |
| G17-134 | Interface revoke cascade | Invalidation | B,I,D | T17-IMPL-007, T17-IMPL-051–055, T17-INT-031/032, FI17-005, CS17-C, T17-DB-031 | L0 | SPECIFIED |
| G17-135 | Documentation cascade | Invalidation | B,I | T17-IMPL-008, CS17-Z | L0 | SPECIFIED |
| G17-136 | Consumed grants not reset; no resend | Invalidation | B,I | T17-IMPL-010, T17-IMPL-051–056, T17-INT-031/032, FI17-017, CS17-N | L0 | SPECIFIED |
| G17-137 | Durable currentness at boundary | Authority | B,I | T17-INT-001, T17-IMPL-037 | L0 | SPECIFIED |
| G17-138 | Send-boundary recheck | Preflight | I | T17-INT-001, FI17-004–016 | L0 | SPECIFIED |
| G17-139 | Cache not permission | Cache policy | B,I | T17-IMPL-037, FI17-003, CS17-I | L0 | SPECIFIED |
| G17-140 | Replica fail-closed | Authority | I | T17-INT-012, FI17-012, CS17-J | L0 | SPECIFIED |
| G17-141 | Startup artifact identity | Identity provider | I | T17-INT-013 | L0 | SPECIFIED |
| G17-142 | Pre-mutation artifact compare | Preflight | B,I | T17-IMPL-039, FI17-011, CS17-X | L0 | SPECIFIED |
| G17-143 | Transition table enforced | Lifecycle | B | T17-IMPL-001–005 | L0 | SPECIFIED |
| G17-144 | Certification SoD | SoD policy | B,SEC | T17-IMPL-019–023, T17-SEC-006 | L0 | SPECIFIED |
| G17-145 | Onboarding SoD + executor ban | Onboarding | B,SEC | T17-IMPL-018/020, T17-SEC-005, CS17-S | L0 | SPECIFIED |
| G17-146 | Endpoint identity tuple | Endpoint profile | D,I | T17-DB-012, T17-INT-014 | L0 | SPECIFIED |
| G17-147 | DNS not vendor proof | DestinationTrustValidator | I,SEC | T17-INT-014, T17-SEC-001 | L0 | SPECIFIED |
| G17-148 | Target-bound credential resolve | Credential binder | B,SEC | T17-IMPL-041/042, T17-SEC-007/008 | L0 | SPECIFIED |
| G17-149 | POSITIVE_NOT_SENT needs proof | Send class | B | T17-IMPL-048/049 | L0 | SPECIFIED |
| G17-150 | Health AND composition | Health composer | B,I | T17-IMPL-031, CS17-Y | L0 | SPECIFIED |
| G17-151 | Trusted evidence requirements | Evidence service | B | T17-IMPL-015 | L0 | SPECIFIED |
| G17-152 | Evidence supersession | Evidence service | B | T17-IMPL-016/017 | L0 | SPECIFIED |
| G17-153 | Immutable versioned snapshots | Schema + service | D,B | T17-DB-004, T17-IMPL-014 | L0 | SPECIFIED |
| G17-154 | §36A matrix implemented | Invalidation | B,I,D | T17-IMPL-007–013, T17-IMPL-051–059, T17-INT-031–034, T17-DB-029/031, FI17-004–016 | L0 | SPECIFIED |
| G17-155 | L4 send currentness | Level4Satisfied | B,I | T17-IMPL-034–036, FI17-015, CS17-A/B | L0 | SPECIFIED |
| G17-156 | No inferred idempotency | Send class | B | T17-IMPL-050 | L0 | SPECIFIED |
| G17-157 | Bundle binds TLS + network versions | Bundle | D | T17-DB-010, T17-DB-030, T17-IMPL-069/070 | L0 | SPECIFIED |
| G17-158 | Change-control abstract; MANUAL ok | Onboarding policy | S | T17-STR-020 | L0 | SPECIFIED |

Gates mapped: **158/158**. No duplicate IDs. No gate with zero linkage.

---

## 35. Unresolved external inputs

The following remain unresolved until authoritative vendor evidence exists. Implementation MUST leave them unresolved and fail closed.

```text
ERICSSON PRODUCTION WRITE PROTOCOL: UNRESOLVED
ERICSSON PRODUCTION ENDPOINT: NOT CONFIGURED
ERICSSON PRODUCTION AUTH METHOD: UNRESOLVED
WRITE ACKNOWLEDGEMENT MEANING: UNRESOLVED
VENDOR-SIDE IDEMPOTENCY: UNRESOLVED
VENDOR OPERATION IDENTIFIERS: UNRESOLVED
ATOMIC CAPABILITY: UNRESOLVED (default READ_THEN_WRITE)
REAL VENDOR FAILURE SEMANTICS: UNRESOLVED
```

Do not convert any of these into code assumptions.

---

## 36. Acceptance criteria (specification review)

Reviewers accept this specification only if:

1. Frozen architecture is unmodified and SHA-256 remains `ea92c6e9183234485da83798ab4fc91c224cfbd1dad80bc464d41009fce576a0`.
2. All 158 gates and 33 threats are mapped.
3. High-risk invariants have non-structural proof IDs.
4. V18 is specified and not created.
5. No Java, no real protocol/endpoint/credential, no Phase 18.
6. L1/L2 marked NOT EXECUTED; L3 NOT SATISFIED; L4 NOT SATISFIED.
7. Real production execution remains NOT AUTHORIZED.
8. Unresolved Ericsson inputs remain unresolved.
9. Machine-readable map is consistent with §34.
10. No git commit/push occurred during authoring.

---

## 37. Known residual / editorial (preserved A17-S observations)

A17-S-01: Architecture §36A naming is informal but acceptable. Do not redesign architecture numbering. This specification names `CertificationInvalidationService` and cites architecture §36A without renaming the architecture section.

A17-S-02: Phase 17 implementation is the certification/onboarding control plane plus production-gateway runtime safety enforcement; no vendor production transport implementation. Do not describe it as purely control-plane if that would hide gateway runtime safety work.

A17-S-03: Optional future Agent `VIEW_CERTIFICATION_STATUS` remains default-deny and does not authorize mutation. Do not expand Agent permissions.

A17-S-04: JSON `evidenceTypes` follow proof-category semantics equivalent to the Markdown Proof column (S/B/I/D/SEC/INF/EXT). They are not a second catalogue.

A17-S-05: Evidence catalogue entries not directly attached to a gate are not broken if they are legitimate supporting tests. Do not delete them merely to make every evidence ID gate-linked.

A17-S-06: Preserve `UnconfiguredProductionEricssonWriteTransport` for Phase 17 implementation absent authoritative vendor protocol evidence.

---

## Final status

```text
PHASE 17 IMPLEMENTATION SPECIFICATION:
CORRECTED CANDIDATE — READY FOR IMPLEMENTATION-SPECIFICATION RE-REVIEW

PHASE 17 IMPLEMENTATION:
NOT STARTED

V18:
NOT CREATED

JAVA:
NONE

REAL PRODUCTION EXECUTION:
NOT AUTHORIZED

ERICSSON PRODUCTION WRITE PROTOCOL:
UNRESOLVED

ERICSSON PRODUCTION WRITE TRANSPORT:
NOT IMPLEMENTED

REAL ENDPOINT: NONE
REAL CREDENTIAL: NONE

LEVEL 1: NOT EXECUTED
LEVEL 2: NOT EXECUTED
LEVEL 3: NOT SATISFIED
LEVEL 4: NOT SATISFIED

NOKIA: DEFERRED
AGENT EXECUTION: NOT AUTHORIZED
MCP EXECUTION: NOT AUTHORIZED
SCHEDULED EXECUTION: NOT AUTHORIZED
EVENT EXECUTION: NOT AUTHORIZED
AUTOMATIC ROLLBACK: NOT AUTHORIZED
CLOSED LOOP: NOT AUTHORIZED

PHASE 18:
NOT STARTED
```

---

*End of Phase 17 implementation specification.*
