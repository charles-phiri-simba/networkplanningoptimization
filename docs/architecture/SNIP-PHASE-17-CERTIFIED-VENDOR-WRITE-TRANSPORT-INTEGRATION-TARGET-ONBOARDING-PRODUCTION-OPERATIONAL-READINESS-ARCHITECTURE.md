# SNIP Phase 17 — Certified Vendor Write Transport Integration, Target Onboarding & Production Operational Readiness

## Architecture Document

**Status:** CORRECTED CANDIDATE — READY FOR FINAL ARCHITECTURAL REVIEW  
**Date:** 2026-09-02  
**Phase:** 17  
**Document type:** Architecture (normative; post-review correction candidate)  
**Parent Phase 16 immutable implementation baseline:** `f4e09b42f7b8f56c3794fae3c91a50a7af490c82`  
**Phase 16 architecture Git baseline:** `8c0791b67ddd9121b1dd5d0abf452c056a8c9a52`  
**Phase 16 architecture SHA-256:** `dfb4f477e813161843036482d3a6aafc7e19528c91cba1dbdecf2adfb5a5a3b0`  
**Phase 16 implementation specification SHA-256:** `ffbe180a6caddc4bccba38bf9621c203f8b069ee50b70a10fbdab2632a84c616`  
**Phase 16 exact-SHA CI run:** `33554119251`  
**Implementation:** NOT STARTED  
**Implementation specification:** NOT AUTHORIZED  
**Architecture acceptance:** NOT YET ACCEPTED / NOT YET FROZEN  
**V18:** NOT CREATED  
**Real production execution:** NOT AUTHORIZED  
**Ericsson production write protocol:** UNRESOLVED  
**Ericsson production write transport:** NOT IMPLEMENTED / NOT CONFIGURED  
**Ericsson production endpoint:** NOT CONFIGURED  
**Ericsson production auth method:** UNRESOLVED / EXTERNALLY CONFIGURED AS APPROPRIATE  
**Nokia production write support:** DEFERRED  
**Closed-loop optimization:** NOT AUTHORIZED  
**Phase 18:** NOT STARTED  

This document is architecture only. It does **not** authorize implementation, V18 creation, a real Ericsson write transport, production endpoint configuration, credential creation, architecture freeze, or Level-4 production execution.

---

# 0. Purpose

This architecture defines the **certified vendor write transport, target onboarding, and production operational readiness** plane that may — only after independent certification through required environment stages and a later, distinct Level-4 operational authorization — replace the Phase 16 `UnconfiguredProductionEricssonWriteTransport` fail-closed boundary with a vendor-supported, independently certified write transport.

Phase 17 starts where Phase 16 stops. Phase 16 remains the immutable owner of production change governance, grants, consume-before-send, lease/fencing, kill switch, rate/blast controls, expected-state policy, mutation attempt state, verification state, rollback governance, audit, credential isolation, and the separate production-write-gateway runtime.

Phase 17 **MUST NOT** weaken, replace, or bypass any Phase 16 governance or safety boundary.

This document does **not** make the first production mutation part of normal automated workflow. Acceptance of this architecture does **not** authorize real production execution.

---

# 1. Defining Principle

> A vendor write transport becomes eligible for production only after its protocol, capabilities, security properties, mutation semantics, verification behavior and operational failure modes have been proven against an authoritative vendor interface and independently certified through the required environment stages. Transport certification does not itself authorize production execution.

Corollaries:

1. No production Ericsson write protocol may be invented, inferred, or guessed.
2. `EricssonWriteTransport` remains **UNRESOLVED** until an authoritative interface source exists and is registered.
3. Certification of a transport, capability, security profile, or production target is **not** Level-4 execution authorization.
4. **LEVEL 3 ≠ LEVEL 4.** A production target may remain Level 3 indefinitely with zero production mutations permitted.
5. **LEVEL 4** is the existing Phase 16 production authorization for a **specific** production execution (fingerprint and authorization generation), still current at the send boundary, **plus** current Phase 17 certification prerequisites. It is **not** a standing target flag.
6. The production-write-gateway remains the authoritative mutation boundary.
7. Certification/onboarding is **durable authoritative state**. The gateway validates **current** safety-significant state at the mutation boundary. A stale projection, cache hit, or replica-local memory is **not** execution authority.
8. Phase 16 deny-by-default, human-gated, consume-before-send, no-blind-retry, and no-automatic-rollback rules remain normative.
9. Agent, MCP, scheduler, event-driven, and closed-loop production execution remain **NOT AUTHORIZED**.
10. Architecture acceptance does **not** imply production authorization.

---

# 2. Parent Baseline and Phase Relationship

## 2.1 Immutable parents

| Artifact | SHA / status |
|---|---|
| Phase 16 immutable implementation baseline | `f4e09b42f7b8f56c3794fae3c91a50a7af490c82` |
| Phase 16 architecture Git baseline | `8c0791b67ddd9121b1dd5d0abf452c056a8c9a52` |
| Phase 16 architecture SHA-256 | `dfb4f477e813161843036482d3a6aafc7e19528c91cba1dbdecf2adfb5a5a3b0` |
| Phase 16 implementation specification SHA-256 | `ffbe180a6caddc4bccba38bf9621c203f8b069ee50b70a10fbdab2632a84c616` |
| Phase 16 exact-SHA CI | workflow `ci`, run `33554119251`, SUCCESS |
| Phase 15 immutable implementation baseline | `ae9c13d55b444fa50090813495b32b82f97c2ec3` |
| Failed historical Phase 15 candidate | `0cb1223e41ced5462ad552f993e6001a028ddb96` (preserve; do not rewrite) |
| Phase 13 Git baseline | `5e9400005626fb93d5e61f96be680bea5540df31` |
| Phase 11 Git baseline | `78e699380be37109cfdd2111dd0f29c7052709c3` |

Phase 16 remains **ACCEPTED**, **FROZEN**, **IMMUTABLY BASELINED**, and **EXACT-SHA CI VERIFIED**. Phase 17 **MUST NOT** amend Phase 16 architecture, specification, implementation, V17 schema, or Git baseline.

## 2.2 Authorization chain (non-substitutable)

```text
Phase 13 proposal approval
  ≠ Phase 14 plan authorization
  ≠ Phase 15 execution authorization
  ≠ Phase 16 production authorization
  ≠ Phase 17 transport / target certification
  ≠ Level-4 controlled production execution authorization
```

Each layer requires an independent human decision against its own bound artifact. Transport certification, target onboarding, and Level-3 registration are **never** sufficient for production mutation.

## 2.3 What Phase 16 already proved (and what it did not)

Phase 16 proved the production change-control plane: separate write gateway, durable grant consume, SoD, fingerprints, lease/fencing, kill switch, rate/blast-radius, expected-state policy, mutation-outcome taxonomy, independent verification, ambiguous-outcome recovery, governed rollback, audit chain, and fail-closed unconfigured Ericsson production transport.

Phase 16 **did not**:

- configure a real Ericsson write protocol
- implement a production Ericsson write transport
- add a real vendor endpoint
- add a real production credential
- execute Level 1 / Level 2 certification
- satisfy Level 3 or Level 4
- authorize real production execution

## 2.4 Current security state inherited at Phase 16 closure

```text
ERICSSON PRODUCTION WRITE TRANSPORT: NOT CONFIGURED
REAL ERICSSON ENDPOINT: NONE
REAL PRODUCTION CREDENTIAL: NONE
PRODUCTION EXECUTION DEFAULT: DISABLED
LEVEL 0: SATISFIED
LEVEL 1: NOT EXECUTED
LEVEL 2: NOT EXECUTED
LEVEL 3: NOT SATISFIED
LEVEL 4: NOT SATISFIED
REAL PRODUCTION EXECUTION: NOT AUTHORIZED
AGENT / MCP / SCHEDULED / EVENT EXECUTION: NOT AUTHORIZED
AUTOMATIC ROLLBACK: NOT AUTHORIZED
CLOSED-LOOP EXECUTION: NOT AUTHORIZED
```

Phase 17 architecture **MUST** preserve this default security state until later, separately authorized certification and operational steps occur. This document itself does not change that state.

---

# 3. Scope

## 3.1 Initial Phase 17 scope (hard)

| Constraint | Value |
|---|---|
| Vendor | ERICSSON |
| Platform | ENM |
| Object | CELL |
| Parameter | txPower |
| Max cells per execution | 1 |
| Max parameters per execution | 1 |
| Max forward mutation operations | 1 |
| Rollback | exact persisted Phase 14 rollback mutation only |
| First vendor interface | UNRESOLVED until authoritative evidence |
| Nokia | DEFERRED |

## 3.2 What Phase 17 adds

Phase 17 adds architecture for:

- vendor interface registration and provenance
- vendor interface evidence (metadata/reference, not proprietary blobs)
- transport implementation profile
- transport / capability / security / environment certification
- target onboarding
- operational readiness
- vendor-version compatibility
- transport health
- protocol evidence
- controlled vendor-lab execution (Level 1)
- pre-production certification (Level 2)
- production target registration (Level 3; **no production mutation**)
- certification expiry / revocation
- artifact-bound certification bundles
- gateway final certification enforcement before any production mutation

## 3.3 What Phase 17 MUST NOT replace

Phase 16 remains authoritative for:

production change governance; request / review / authorization; production execution grants; grant consume; lease / fencing; kill switch; target health; rate / blast controls; expected-state guard **policy**; mutation attempt state; verification state; rollback **governance**; audit of production changes; credential isolation; `production-write-gateway`.

## 3.4 Explicit non-goals

Phase 17 does **NOT**:

- add Nokia write support
- expand beyond txPower
- support bulk / multi-cell mutations
- add arbitrary or generic vendor commands
- add autonomous optimization execution
- enable closed-loop optimization
- authorize production mutation
- create real credentials
- create real endpoints
- guess an Ericsson interface
- replace Phase 16 governance
- replace Phase 10 credentials
- replace Phase 12 synchronization
- replace Phase 11 read-only integration
- implement Java, V18, Terraform, or Kubernetes changes
- start Phase 18

---

# 4. Context and Logical Architecture

## 4.1 Logical components

```text
SNIP application (snip-npo-app)
  Phase 13 recommendation
  Phase 14 planning
  Phase 15 execution readiness
  Phase 16 production governance / grant issuance
  Phase 17 certification & onboarding control plane
      VendorInterfaceDefinitionRegistry
      CertifiedVendorTransportRegistry
      TransportProfileResolver (policy/state)
      TransportCertificationService
      VendorCapabilityCertificationService
      ProductionTargetOnboarding
      TransportHealthService (advisory + safety states)
        |
        | durable authoritative certification / onboarding / health records
        | (no mutation; no write credentials; not execution permission)
        v
production-write-gateway
  Phase 16 consume / preflight / lease / kill / rate
  Phase 17 send-boundary currentness validation (durable read or proven current binding)
  VendorNetworkWriteAdapter
      EricssonEnmWriteAdapter
          Certified EricssonWriteTransport   ← UNRESOLVED until evidence
              approved vendor interface      ← UNRESOLVED until evidence
```

The certification control plane **owns durable authoritative records**. It does **not** push execution permission into the gateway. The gateway **MUST** validate current safety-significant certification state at the mutation boundary against that durable authority (or a version-bound representation whose currentness and revocation status can be proven against it). A stale event projection, cache hit, or replica-local memory is **not** execution authority. Control-plane services **MUST NOT** bypass the gateway.

## 4.2 Package / module ownership (logical)

| Owner | Logical packages | Role |
|---|---|---|
| `snip-npo-app` | `com.simba.snip.npo.vendorcertification` | interface registry, certification authority, evidence metadata, expiry/revocation |
| `snip-npo-app` | `com.simba.snip.npo.targetonboarding` | production target onboarding / Level-3 registration |
| `production-write-gateway` | `com.simba.snip.npo.vendortransport` | certified transport resolution, health enforcement at mutation boundary, adapter/transport SPI |
| `production-change-protocol` | shared DTOs / enums only | certification status, health, profile identifiers — no Key Vault, no vendor I/O |

Vendor write transport **MUST NOT** collapse into `snip-npo-app`. The ordinary SNIP application **MUST NOT** resolve write credentials or invoke vendor mutation.

Exact package names are directional. The implementation specification may refine names without changing ownership or trust boundaries.

## 4.3 Dependency direction (normative)

```text
Phase 13 recommendation
  → Phase 14 planning
  → Phase 15 execution readiness
  → Phase 16 production governance
  → Phase 16 production-write-gateway
  → Phase 17 certified transport resolver
  → VendorNetworkWriteAdapter
  → EricssonEnmWriteAdapter
  → certified EricssonWriteTransport
  → approved vendor interface
```

No reverse dependency may allow a transport, adapter, Agent, MCP, or scheduler to issue grants, certify transports, or authorize Level 4.

---

# 5. Runtime Topology

## 5.1 Workloads

| Workload | Holds write identity? | May mutate vendor? | Phase 17 role |
|---|---|---|---|
| `snip-npo-app` | **NO** | **NO** | governance + certification control plane |
| `production-write-gateway` | **YES** (write-only WI) | **YES**, only after Phase 16 consume **and** Phase 17 certification enforcement **and** independent Level 4 | mutation plane |
| Default CI | **NO** | **NO** | Level 0 only |
| L1 lab pipeline | lab-only, non-production | lab cells only | Level 1 evidence |
| L2 pre-prod pipeline | pre-prod only | operator pre-prod only | Level 2 evidence |

## 5.2 Infrastructure architecture (describe only; do not implement)

Target architecture continues the Phase 16 separate-gateway model and adds certification-aware constraints:

- dedicated write gateway Deployment / Service / ServiceAccount
- dedicated write user-assigned managed identity
- target-specific egress (no `0.0.0.0/0` write egress)
- private routing where the operator environment requires it
- DNS resolution constrained to approved names
- TLS / mTLS trust material via Phase 10 secret mechanism
- Key Vault access scoped to write credential and trust material only
- NetworkPolicy isolation between app and gateway, and gateway and vendor
- secret rotation without in-process vendor-secret cache
- pod disruption: in-flight MAY_HAVE_SENT attempts recover from durable state; no automatic second send
- replica/concurrency: atomic grant consume remains the single-success rule; **all gateway replicas use the same durable certification-authority / currentness model**
- no replica-local positive certification authority
- replica partition from certification authority ⇒ **DENY**
- observability and audit export without secrets

This document **MUST NOT** implement Terraform or Kubernetes changes and **MUST NOT** introduce real environment identifiers that are not already part of approved repository architecture.

## 5.3 Fail-closed default

A fresh deployment with Phase 17 control-plane tables empty, certification unknown, transport unresolved, or Level 4 absent **MUST** remain incapable of production mutation. Default production writes remain **DISABLED**. `UnconfiguredProductionEricssonWriteTransport` remains the production fail-closed transport until a certified replacement is registered **and** Level 4 is independently authorized for a specific execution.

---

# 6. Trust Boundaries

| Boundary | Inside | Must not cross |
|---|---|---|
| Governance | app request/review/authorize/grant | vendor write, write secret resolution |
| Certification control plane | interface/profile/cert/onboarding records | vendor mutation, secret values |
| Mutation plane | gateway consume, preflight, adapter, transport | grant issuance, certification issuance |
| Vendor | approved interface only | SNIP governance decisions |
| Identity | write WI ≠ read WI ≠ human principals | credential reuse across planes |

Unknown, stale, source-unavailable, version-mismatched, or revocation-uncertain certification, health, vendor version, or Level-4 authorization **MUST** deny.

---

# 7. Vendor Interface Definition and Provenance

## 7.1 Authoritative source rule

Phase 17 **MUST NOT** guess the Ericsson mutation interface.

A production Ericsson transport may be designed concretely only when **at least one** authoritative interface source exists, such as:

- official Ericsson product documentation
- operator-approved Ericsson integration guide
- contractually supplied API / interface specification
- approved SDK / interface definition
- operator architecture document that explicitly identifies the supported write mechanism

Until that evidence exists:

```text
ERICSSON PRODUCTION WRITE PROTOCOL: UNRESOLVED
EricssonWriteTransport: UNRESOLVED
```

## 7.2 `VendorInterfaceDefinition`

Conceptual fields:

| Field | Purpose |
|---|---|
| `interfaceDefinitionId` | stable identity |
| `vendor` | ERICSSON for Phase 17 |
| `platform` | ENM for Phase 17 |
| `productVersionRange` | approved vendor/platform versions |
| `interfaceType` | abstract class only until evidence (see §8) |
| `documentationReference` | metadata/reference, not a proprietary blob |
| `documentationVersion` | version of the referenced document |
| `approvedBy` | human/operator authority |
| `approvedAt` | approval time |
| `protocolProfileId` | bound protocol profile |
| `securityProfileId` | bound security profile |
| `capabilityProfileId` | bound capability profile |
| `status` | DRAFT / APPROVED / SUPERSEDED / REVOKED |
| `effectiveFrom` / `effectiveUntil` | validity window |

The documentation reference **MUST** be metadata/reference only. SNIP **MUST NOT** store proprietary vendor documentation blobs unless a later implementation specification separately authorizes an encrypted evidence store. Architecture does **not** claim that raw evidence storage is immutable.

## 7.3 Approval

Interface registration requires `VENDOR_INTERFACE_REVIEW` by a human distinct from the requester. Agents and MCP **MUST NOT** approve interface definitions.

A `VendorInterfaceDefinition` is a **logical** object identified by `interfaceDefinitionId`. Certification **MUST** bind an **immutable version** (`interfaceDefinitionVersion` / content digest), not a mutable pointer. Changing documentation reference, documentation version, approval, status, effective window, or linked profile versions **MUST** create a new interface-definition version.

## 7.4 Authoritative interface revocation cascade (normative)

The following events **MUST** invalidate **every** certification derived from the affected authoritative interface, **even when** `interfaceDefinitionId` or vendor product version has **not** changed:

- `VendorInterfaceDefinition` → `SUPERSEDED`
- `VendorInterfaceDefinition` → `REVOKED`
- vendor documentation withdrawn
- vendor documentation superseded
- operator approval revoked
- interface approval revoked
- authoritative interface provenance becoming invalid (including `effectiveUntil` elapsed or approval identity no longer valid)

Required consequences (normative, simultaneous as a safety set):

| Affected artifact | Required consequence |
|---|---|
| `TransportCertificationBundle` | **INVALID** |
| transport certification | **INVALID** |
| capability certification dependent on the interface | **INVALID** |
| security certification dependent on the interface | **INVALID** |
| `ProductionTargetOnboardingRecord` | **INVALID / STALE** |
| production target certification | **INVALID** |
| Phase 16 production authorization / fingerprint | **STALE** |
| unconsumed production execution grants | **REVOKED** |
| gateway mutation eligibility | **DENY** |

Already-**CONSUMED** grants **MUST NOT** be reset to `ISSUED`.

In-flight `MAY_HAVE_SENT` semantics remain governed by Phase 16 recovery. Revocation **MUST NOT** cause blind resend.

---

# 8. No Protocol Guessing

Until authoritative evidence exists, architecture **MUST NOT** state that ENM uses a specific write API.

Phase 17 **MUST NOT** invent:

- REST URLs
- Bulk CM semantics
- CLI syntax
- SSH invocation
- SOAP methods
- SDK calls
- NETCONF assumptions
- CORBA assumptions
- vendor-specific payload structure
- authentication mechanisms

Architecture **MAY** model these only as **abstract alternative interface types** on `VendorInterfaceDefinition.interfaceType`, with no selected production value.

Selected production `interfaceType` remains **UNRESOLVED**.

Phase 11 `EnmTransport` remains **READ_ONLY**. Phase 17 **MUST NOT** add write methods to it.

---

# 9. Vendor Write Transport Profile

## 9.1 `VendorWriteTransportProfile`

A transport profile binds:

- vendor, platform, vendor product/version range
- interface definition
- transport implementation version
- security profile
- credential profile
- capability profile
- expected-state strategy
- mutation strategy
- readback strategy
- rollback strategy
- timeout policy
- retry policy (PRE-SEND only; see §20)
- TLS profile
- session policy
- supported object types
- supported parameters
- certification state
- certification expiry

## 9.2 Immutability after certification

A transport profile is **immutable and versioned** once certified. Certification **MUST** bind the profile **version / digest**, not a mutable profile id pointer.

Material changes **MUST** produce a new profile version and **MUST** require recertification. Material changes include interface definition version, implementation, artifact digest, security, credential, capability, TLS, network-policy/egress, endpoint-identity binding, expected-state strategy, mutation strategy, readback strategy, rollback strategy, or vendor version predicate.

---

# 10. Transport Implementation Boundary

Preserve the Phase 16 SPI:

```text
VendorNetworkWriteAdapter
    ↓
EricssonEnmWriteAdapter
    ↓
EricssonWriteTransport
```

Phase 17 extends this conceptually with:

| Component | Responsibility |
|---|---|
| `VendorInterfaceDefinitionRegistry` | authoritative interface records |
| `CertifiedVendorTransportRegistry` | certified profiles only |
| `TransportProfileResolver` | resolve profile for target + vendor + version |
| `TransportCertificationService` | lifecycle and bundle issuance |
| `VendorCapabilityCertificationService` | CELL / txPower capability proof |
| `TransportHealthService` | health observation and safety-state publication |

The gateway resolves **only certified** transport profiles.

**No** dynamic arbitrary class or plugin loading from an untrusted caller. Transport implementations are compiled, versioned artifacts registered by privileged operators, not uploaded at request time.

While unresolved, the production path **MUST** continue to bind `UnconfiguredProductionEricssonWriteTransport`.

---

# 11. Certification Lifecycle

## 11.1 Transport certification states

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

Transitions are human-gated except automatic safety transitions to `SUSPENDED`, `EXPIRED`, or `REVOKED` when policy triggers fire (version mismatch, expiry, security failure, revocation). An **unknown** transition **MUST** be **DENIED**.

## 11.2 Certification lifecycle transition table (normative)

| From | To | Actor / permission | Evidence | Auto? | Reversible? | Recertify? | Execution eligibility |
|---|---|---|---|---|---|---|---|
| (none) | `DRAFT` | `TRANSPORT_CERTIFY` (create) | draft record | Human | N/A | N/A | **NO** |
| `DRAFT` | `INTERFACE_VERIFIED` | `VENDOR_INTERFACE_REVIEW` | approved interface definition **version** | Human | No (new version if change) | N/A | **NO** |
| `INTERFACE_VERIFIED` | `LAB_CERTIFICATION_PENDING` | `TRANSPORT_CERTIFY` | L1 plan bound to interface version | Human | Withdraw to DRAFT only | N/A | **NO** |
| `LAB_CERTIFICATION_PENDING` | `LAB_CERTIFIED` | `TRANSPORT_CERTIFY` + `CAPABILITY_CERTIFY` + `SECURITY_CERTIFY` as required | L1 evidence set (lab mutation, readback, rollback, failures) | Human | No | New L1 if material change | **NO** (lab only, not production) |
| `LAB_CERTIFIED` | `PREPROD_CERTIFICATION_PENDING` | `TRANSPORT_CERTIFY` | L2 plan | Human | No | N/A | **NO** |
| `PREPROD_CERTIFICATION_PENDING` | `PREPROD_CERTIFIED` | independent certifiers per §27 | L2 objective production-equivalence evidence | Human | No | New L2 if material change | **NO** |
| `PREPROD_CERTIFIED` | `PRODUCTION_REGISTRATION_PENDING` | `TARGET_ONBOARD_CREATE` | onboarding draft bound to certified versions | Human | Withdraw pending | N/A | **NO** |
| `PRODUCTION_REGISTRATION_PENDING` | `PRODUCTION_REGISTERED` | `TARGET_ONBOARD_REVIEW` then `TARGET_ONBOARD_APPROVE` (independent) | L3 registration evidence; **zero** production mutation | Human | No | New L3 if material change | **NO** (Level 3 ≠ Level 4) |
| any certified state | `SUSPENDED` | policy auto **or** `TARGET_SUSPEND` | version/security/capability/operator trigger | Auto or human | Only via `TARGET_REACTIVATE` | Yes if cause requires | **NO** |
| any certified state | `EXPIRED` | system clock / `expiresAt` | expiry | Auto | **No auto return** | Explicit renewal/recertification | **NO** |
| any state except none | `REVOKED` | policy auto **or** certifier/onboard approver | revoke cause (interface, defect, advisory, approval) | Auto or human | **NEVER** auto | Full recertification as a **new** versioned path | **NO** |
| `SUSPENDED` | `PRODUCTION_REGISTERED` | `TARGET_REACTIVATE` (not the executor) | proof suspension cause resolved + current certs | **Human only** | N/A | Recertify if cause was security/capability/version | **NO** until Level 4 on a specific execution |
| `EXPIRED` | `PRODUCTION_REGISTERED` | certifiers + `TARGET_ONBOARD_APPROVE` | explicit renewal/recertification bundle | **Human only** | N/A | **Required** | **NO** until Level 4 |
| `REVOKED` | any prior certified state | — | — | **Forbidden** | **Forbidden** | New DRAFT→… path only | **NO** |

Rules:

- `REVOKED` **MUST NEVER** automatically return to any executable or `PRODUCTION_REGISTERED` certification.
- `EXPIRED` **MUST** require explicit recertification / renewal.
- Security or capability suspension **MUST NEVER** auto-reactivate.
- `SUSPENDED` → `PRODUCTION_REGISTERED` requires explicit human-controlled reactivation and proof that the suspension cause is resolved.
- `PRODUCTION_REGISTERED` is **never** execution eligibility.

## 11.3 PRODUCTION_REGISTERED is not execution authority

`PRODUCTION_REGISTERED` means the transport/target pair has completed Level-3 technical and security registration.

It **MUST NOT** be equated with:

```text
PRODUCTION EXECUTION AUTHORIZED
```

Level 4 remains a separate operational authorization on a specific Phase 16 production execution.

---

# 12. Certification Levels (Phase 16 retained and formalized)

| Level | Name | Vendor system? | Production mutation? |
|---|---|---|---|
| LEVEL 0 | LOCAL / SIMULATED | No | No |
| LEVEL 1 | APPROVED VENDOR LAB | Vendor lab / test ENM | Lab cell only |
| LEVEL 2 | OPERATOR PRE-PRODUCTION | Operator-managed non-production | Pre-prod only |
| LEVEL 3 | PRODUCTION TARGET REGISTRATION | Production target identity only | **NO** |
| LEVEL 4 | CONTROLLED PRODUCTION AUTHORIZATION | Production | Only after independent human authorization of a specific execution |

**Code existence, configuration presence, passing default CI, or architecture acceptance MUST NOT imply LEVEL 4.**

## 12.1 LEVEL 0 — local / simulated

Evidence: unit tests; integration tests; controlled test transport; failure injection; protocol-independent safety behavior.

No vendor system required. Default CI is Level 0 only.

## 12.2 LEVEL 1 — approved vendor lab

Requirements include:

- actual supported interface
- vendor lab / test ENM
- non-production credentials
- TLS / security validation
- real transport session
- actual txPower mutation on an approved lab cell
- expected-state observation
- exactly one mutation attempt
- independent vendor readback
- rollback
- response-loss behavior
- timeout behavior
- ambiguous-outcome handling
- transport disconnect behavior
- credential expiry
- certificate failure
- version compatibility

Level 1 **MUST** be the first real Ericsson integration. It is **NOT EXECUTED** by this architecture.

## 12.3 LEVEL 2 — operator pre-production

Requirements include:

- operator-managed non-production target
- production-equivalent network / security path
- workload identity / secret path
- real firewall / egress policy
- operator change process
- real target registration in the pre-prod class
- controlled mutation
- verification
- rollback
- audit export
- monitoring / alerts
- operational support exercise

Level 2 certification **MUST** require **objective production-equivalence criteria**. The implementation specification **MUST** later define the exact checklist. At architecture level, L2 evidence **MUST** include:

- network path
- identity model
- secret resolution
- TLS
- target registration (pre-prod class)
- monitoring
- audit
- change-control process
- verification
- rollback

Level 2 is **NOT EXECUTED** by this architecture. Level 2 **MUST NOT** perform a production mutation.

## 12.4 LEVEL 3 — production target registration

Level 3 **MUST NOT** execute a production mutation.

It certifies that a specific production target has:

- approved endpoint identity
- approved transport profile
- approved capability profile
- approved security profile
- approved credential profile
- approved network route
- approved TLS / mTLS identity
- approved change-control integration
- approved monitoring
- approved support ownership
- approved rollback / runbook
- approved maintenance window policy

Level 3 is **NOT SATISFIED** by this architecture.

## 12.5 LEVEL 4 — exact identity (normative)

Phase 17 **MUST NOT** automatically satisfy Level 4. Phase 17 **MUST NOT** introduce a standing Level-4 target flag or V18 Level-4 entitlement.

**Level 4 is** the existing **Phase 16 production authorization** for a **specific** production execution, including its production fingerprint and authorization generation, **still current at the send boundary**, **combined with** current Phase 17 certification prerequisites.

Level 4 is:

- per execution
- human / operational
- time- and scope-bound
- revocable
- **not** inherited by target
- **not** inherited by certification bundle
- **not** inherited by transport
- **not** inherited by Level 3

A target at Level 3 has **zero** production execution authority without that specific current Phase 16 Level-4 authorization.

Gateway final preflight **MUST** verify Level-4 **currentness**.

Level 4 is **NOT SATISFIED**. Real production execution remains **NOT AUTHORIZED**.

**NO LEVEL-4 EXECUTION IS AUTHORIZED BY THE PHASE 17 ARCHITECTURE DOCUMENT ITSELF.**

---

# 13. Target Onboarding

## 13.1 `ProductionTargetOnboardingRecord`

Phase 16 `ProductionNetworkTarget` remains **authoritative for execution**.

Phase 17 onboarding **enriches and certifies** that target. It does not become a second mutation authority.

Onboarding binds:

| Binding | Notes |
|---|---|
| `productionTargetId` | Phase 16 target identity |
| vendor / platform / vendor software version | compatibility |
| network domain / region / site scope | blast and ownership |
| vendor interface definition | provenance |
| transport / capability / security / credential / TLS / network-policy / monitoring profiles | certification inputs |
| support owner | operational |
| change-control system / reference policy | abstract; **not** itself authorization; Phase 16 **MANUAL** validation remains acceptable until a named integration is approved. Do **not** introduce ServiceNow or another product without approved external requirements |
| maintenance / rollback / verification policies | operational |
| certification level | 0–3 for registration; 4 is execution-scoped |
| certification evidence bundle | references/hashes |
| `lastCertifiedAt` / `expiresAt` / status | currency |

## 13.2 Caller injection prohibition

A caller **MUST NOT** inject endpoint or credential values dynamically during execution.

No endpoint or credential may be supplied by:

- API caller
- Agent
- MCP
- optimization proposal
- Phase 15 execution

---

# 14. Endpoint Registration and Identity

Phase 17 **MAY** introduce a secure target endpoint registry.

Endpoint values are **platform configuration / infrastructure data**, not request parameters. **No** runtime endpoint override.

Architecture **MUST** distinguish:

- **endpoint identity metadata**
- **credential material** (never stored in SNIP domain tables)

## 14.1 Normalized endpoint identity (normative)

A certified endpoint identity **MUST** bind:

| Binding | Notes |
|---|---|
| `productionTargetId` | Phase 16 target |
| environment | LAB / PREPROD / PROD class |
| network domain | operator scope |
| approved hostname / FQDN | normalized |
| approved port | explicit |
| TLS server identity | certified expected identity |
| approved route / network zone | egress / private routing class |
| vendor / platform | ERICSSON / ENM for Phase 17 |
| endpoint profile version | immutable snapshot |

IP **MAY** be dynamic where DNS is required. Architecture does **not** require a fixed IP unless a later specification proves it is appropriate.

## 14.2 DNS / TLS destination trust model

Trust is **composed**, not DNS-only:

1. DNS resolves the **approved name**.
2. Network policy / egress **constrains** the destination (no `0.0.0.0/0` write egress).
3. TLS hostname / server identity **MUST** match the certified TLS identity.
4. Private routing is used where available.
5. mTLS peer identity is required where the certified profile requires it.

A DNS answer **alone** is **never** proof of vendor identity.

If the resolved destination or TLS identity no longer satisfies the certified endpoint profile ⇒ **DENY**.

A material change to endpoint identity, route, TLS identity, or endpoint-profile version **MUST** invalidate certification and downstream execution authority (see §47 invalidation matrix).

## 14.3 Endpoint change consequences

Changing an endpoint **MUST**:

1. invalidate affected certification
2. mark Phase 16 production authorization fingerprints **STALE**
3. revoke unconsumed grants
4. require target recertification as appropriate
5. gateway mutation eligibility **DENY**

---

# 15. Credentials

Phase 10 remains authoritative for secrets and workload identity.

Phase 17 **MUST** preserve:

- separate read identity
- separate write identity
- gateway-only write credential resolution
- no local-provider fallback in production
- current enabled secret version only; no older-version fallback
- no vendor-secret value cache

Credential **references** may be registered on profiles and onboarding records.

Secret **values** **MUST NEVER** be stored in the SNIP database, Agent memory, prompts, logs, traces, metrics, audit payloads, API responses, or exception messages.

## 15.1 Target-bound credential resolution (normative)

Gateway credential resolution **MUST** bind all of:

- authorized `productionTargetId`
- certified `credentialProfileId` (version)
- certified transport / security profile versions
- write identity

Credential resolution **MUST NOT** be vendor-global, caller-selected, endpoint-selected, Agent-selected, or MCP-selected.

A generic vendor-wide write credential that is not explicitly bound to the authorized target/profile **MUST** **DENY**.

Prevent:

- target A grant → target B credential
- target A credential → target B transport session

Credential or profile changes **MUST** trigger certification invalidation, authorization stale/revocation, and grant revocation (see §47).

Fail closed on:

- secret unavailable
- secret rotated out from under a live profile
- credential expired
- credential revoked
- identity binding changed
- unbound / generic vendor write credential

---

# 16. TLS / mTLS

`TransportSecurityProfile` **MUST** support:

- hostname verification
- trusted CA chain
- certificate expiry
- certificate rotation
- certificate revocation handling where available
- TLS version policy
- cipher policy
- mTLS where the vendor interface requires it

**No** trust-all.  
**No** hostname-verification disable.  
**No** insecure fallback.

Security-profile changes **invalidate** certification.

mTLS private keys, when required, resolve only through the Phase 10 secure credential mechanism inside the gateway.

---

# 17. Vendor Version Compatibility

A certified transport **MUST** bind an approved vendor/platform version **predicate**.

Vendor version compatibility predicates are **explicit**, **evidence-based**, and **certification-controlled**.

Do **not** infer compatibility from SemVer. Patch, minor, and major compatibility are **not** assumed.

If the target vendor software version changes **outside** the certified predicate, the target automatically becomes `SUSPENDED` (or equivalent).

No mutation until recertified.

Compatibility predicates **MUST NOT** auto-expand.

Unknown vendor version **MUST** deny.

---

# 18. Capability Certification

## 18.1 `CertifiedVendorCapability`

Initial Phase 17 capability:

| Object | Parameter | Cardinality |
|---|---|---|
| CELL | txPower | one cell, one parameter, one operation |

Prove, for the certified profile / target class / vendor version:

- object addressing
- parameter existence
- parameter type
- unit
- allowed range
- precision
- read semantics
- write semantics
- rollback semantics
- verification semantics
- conditional-write capability if any
- eventual-consistency behavior
- known propagation delay

Capability certification **MUST** be target/profile specific where vendor behavior differs by release.

Do not expand Phase 17 to arbitrary parameters. Do not add generic command execution.

---

# 19. Expected-State Certification

Phase 16 defines:

| Strength | Meaning |
|---|---|
| `ATOMIC` | vendor compare-and-set / conditional mutation |
| `READ_THEN_WRITE` | observe then mutate; residual TOCTOU |

Phase 17 **MUST** determine which guard strength the **approved Ericsson interface** actually supports.

`ATOMIC` may be certified **ONLY** if the authoritative vendor interface provides an actual compare-and-set / conditional mutation guarantee.

Otherwise `READ_THEN_WRITE` remains the certified capability with explicit residual TOCTOU.

**Do not** infer `ATOMIC` from optimistic behavior in a lab test.

While the Ericsson protocol remains UNRESOLVED, production expected-state strength remains **policy-gated and fail-closed**.

Canonical SNIP state **MUST NOT** be the sole final expected-state authority. Direct vendor observation remains mandatory immediately before mutation.

---

# 20. Write Semantics and Retry Certification

## 20.1 Formal write semantics

Phase 17 **MUST** formally certify, against the approved interface:

- what vendor acknowledgement means
- whether acknowledgement means accepted, queued, or applied
- whether the write is synchronous or asynchronous
- what constitutes a deterministic rejection
- what constitutes an unknown outcome
- whether vendor operation identifiers exist
- whether duplicate requests are possible
- whether vendor-side idempotency exists

**Never** assume vendor acknowledgement == applied.  
**Never** assume transport exception == not applied.

## 20.2 Retry classification and `POSITIVE_NOT_SENT` proof

Phase 16 no-blind-retry remains authoritative.

Every transport failure point **MUST** be classified as:

| Class | Meaning | Automatic mutation retry? |
|---|---|---|
| `POSITIVE_NOT_SENT` | **certified positive evidence** that the vendor mutation invocation did **not** cross the point at which the vendor could have accepted/applied it | bounded transport/session retry **MAY** be allowed only where explicitly approved |
| `MAY_HAVE_SENT` | mutation-capable invocation began, or transmission is uncertain, or proof is absent | **PROHIBITED** |

`POSITIVE_NOT_SENT` is **narrow**. Only certified positive evidence may classify it. Architecture **MAY** later certify categories such as:

- failure before transport session establishment
- credential resolution failure
- TLS establishment failure
- local serialization/validation failure before invocation
- certified transport rejection **before** mutation dispatch

These examples **MUST NOT** be assumed to apply to the unresolved Ericsson protocol.

After mutation invocation begins, default **conservatively**:

| Event | Classification |
|---|---|
| Timeout | `MAY_HAVE_SENT` unless positively proven otherwise |
| Connection loss **during** invocation | `MAY_HAVE_SENT` |
| Connection loss **after** invocation | `MAY_HAVE_SENT` |
| Response loss | `MAY_HAVE_SENT` |
| Unknown transport exception after send eligibility | `MAY_HAVE_SENT` unless certified transport semantics prove `NOT_SENT` |

No HTTP verb, request id, desired-value equality, or operation id proves idempotency or `POSITIVE_NOT_SENT`.

## 20.3 Vendor operation correlation and anti-inference

If the real interface exposes vendor operation IDs, transaction IDs, or job IDs, architecture **MAY** persist sanitized correlation metadata.

It **MUST NOT** expose secrets.

Vendor operation / job / request IDs are **correlation evidence only**. They are **not**:

- success proof
- application proof
- verification
- authorization
- permission to retry

They **MUST NOT** trigger automatic mutation retry.

Do **not** infer vendor mutation idempotency from HTTP PUT, same desired value, request id, operation id, job id, or transport-library behavior.

Vendor-side idempotency remains **UNRESOLVED** until authoritative evidence and certification prove it.

---

# 21. Readback Certification

Independent vendor readback **MUST** be certified against the actual interface.

Define:

- observation source
- freshness signal
- timestamp semantics
- eventual-consistency window
- timeout
- staleness threshold
- read-after-write behavior

Only **fresh desired-state** readback may satisfy production verification.

Vendor mutation response replay is **not** verification.

Phase 12 remains authoritative for later canonical reconciliation. Phase 17 **MUST NOT** write canonical `radio_configuration`.

---

# 22. Rollback Certification

Certify that the approved transport can perform the **exact Phase 14 rollback mutation** for txPower.

Rollback remains:

- separately requested
- separately reviewed
- separately authorized
- separately granted
- separately consumed
- independently verified

Transport support for rollback **DOES NOT** authorize automatic rollback.

Automatic rollback remains **NOT AUTHORIZED**.

---

# 23. Failure-Mode Certification

Phase 17 **MUST** require real-transport testing (at the appropriate certification level) of:

| Failure | Expected SNIP posture (normative direction) |
|---|---|
| connection refused / connect timeout | PRE-SEND deny if before send; no mutation |
| TLS handshake failure | SECURITY_FAILURE; deny; no insecure fallback |
| credential unavailable / rejected / permission denied | fail closed; no mutation |
| session establishment failure / expiration | deny or MAY_HAVE_SENT if expiry during send |
| vendor validation failure / deterministic rejection | `REJECTED`; no retry as success path |
| write accepted / write applied | persist `VENDOR_ACCEPTED`; verify independently |
| response lost after mutation | `OUTCOME_UNKNOWN`; readback; no resend |
| connection lost during mutation | `MAY_HAVE_SENT` / `OUTCOME_UNKNOWN`; no resend |
| vendor timeout / 5xx equivalent after send may have occurred | `OUTCOME_UNKNOWN`; no resend |
| readback mismatch | `VERIFICATION_FAILED` / `RECOVERY_REQUIRED`; no auto rollback |
| readback unavailable / stale | cannot `VERIFIED`; unresolved or fail closed |
| vendor restart / gateway restart / network partition | reconstruct from durable attempt; no second send |
| duplicate caller request | consume CAS; one success |
| expired / revoked grant | deny |
| stale fencing token | deny |
| kill switch during operation | deny if still PRE-SEND; if MAY_HAVE_SENT continue outcome determination, no second send |

Classify expected SNIP state for each in the implementation specification evidence catalog. Architecture forbids converting any MAY_HAVE_SENT row into automatic retry.

---

# 24. Ambiguous Outcome

Preserve the exact Phase 16 recovery model:

| Observation after `OUTCOME_UNKNOWN` | Result |
|---|---|
| desired observed | `VERIFIED` |
| original expected observed | safe stop; **no automatic resend**; separate new governed execution if required |
| third value | `MANUAL_INTERVENTION_REQUIRED` |
| observation unavailable | `PRODUCTION_OUTCOME_UNRESOLVED` |

Phase 17 **MUST** certify this model against actual transport behavior at Level 1 and Level 2. It **MUST NOT** invent a different recovery automaton.

---

# 25. Transport Health and Eligibility Composition

## 25.1 Effective execution eligibility (normative)

```text
effective execution eligibility =
    Phase 16 target health eligible
AND Phase 17 transport health eligible
AND Phase 17 certification current
AND Phase 17 target certification current
AND all remaining Phase 16 controls satisfied
```

A `HEALTHY` Phase 17 transport **MUST NEVER** override: Phase 16 suspended target, kill switch, expired authorization, invalid grant, closed window, rate limit, or lease/fencing failure.

## 25.2 `VendorTransportHealth` states

| State | Informational / blocking | Derived / human-set | Auto transition? | Human reactivation? | Production execution |
|---|---|---|---|---|---|
| `HEALTHY` | Informational | Derived | Yes (observation) | N/A | Does **not** authorize; other controls still required |
| `DEGRADED` | **Blocking** until a certified policy names an executable degraded class | Derived | Yes | Human if policy later permits a named class | **NO** by default |
| `UNAVAILABLE` | **Blocking** | Derived | Yes | Human if persistent | **NO** |
| `SECURITY_FAILURE` | **Blocking** | Derived or human | Yes | **Required**; no auto re-enable | **NO** |
| `CAPABILITY_MISMATCH` | **Blocking** | Derived | Yes | **Required**; recertify | **NO** |
| `VERSION_MISMATCH` | **Blocking** | Derived | Yes | **Required**; recertify | **NO** |
| `SUSPENDED` | **Blocking** | Human or policy | Yes (policy) | `TARGET_REACTIVATE` only | **NO** |
| Unknown | **Blocking** | N/A | N/A | N/A | **NO** |

`DEGRADED` is **non-executable** until a certified policy says otherwise.

Security / capability / version mismatch **BLOCK**. Unknown **BLOCK**.

**No** automatic production re-enable after a security or capability suspension.

---

# 26. Certification Evidence and Bundle

## 26.1 `TransportCertificationEvidence`

Categories:

`INTERFACE_DOCUMENTATION`, `SECURITY`, `CONNECTIVITY`, `CAPABILITY`, `MUTATION`, `EXPECTED_STATE`, `VERIFICATION`, `ROLLBACK`, `AMBIGUOUS_OUTCOME`, `FAILURE_INJECTION`, `PERFORMANCE`, `OPERATIONS`, `AUDIT`, `MONITORING`

A hash/reference **alone** is **not** trusted certification evidence.

Trusted evidence **MUST** include:

- authenticated certifier identity
- authorized certification permission
- durable evidence record
- binding to certification subject / version / environment
- audit event
- result
- timestamp
- evidence reference / hash

External signature **MAY** be used where available and is **not** required unless a later specification explicitly chooses it.

Do **not** claim raw evidence storage itself is immutable unless backed by an immutable evidence store. Phase 16 tamper-evident audit principles apply to certification **events**.

### Evidence supersession and retention

- Historical evidence is retained according to policy and remains historical / auditable.
- Historical `PASS` is **not** current authority.
- Evidence is superseded / versioned.
- New evidence **MUST NOT** silently coexist with an old `PASS` as equally current.
- Failed recertification **MUST NOT** leave old `PASS` authoritative.
- Failed or newer certification **MUST NOT** silently fall back to older `PASS`.
- Withdrawn or revoked evidence **MUST** invalidate dependent certification.
- Current certification **MUST** explicitly reference its **active** evidence set.

## 26.2 `TransportCertificationBundle`

Certified bindings **MUST** be immutable / versioned snapshots. A stable identity (`interfaceDefinitionId`, profile id) **MAY** identify a logical object; certification **MUST** bind the immutable **version / digest**.

The bundle **MUST** bind versions/digests for:

- vendor, platform
- interface definition version
- documentation approval version
- transport profile version
- **authoritative** artifact identity: artifact digest, transport implementation version, source baseline SHA, certification bundle version
- capability profile version
- security profile version **or** an immutable security snapshot that contains the exact TLS profile version and network-policy/egress profile version
- credential profile version
- TLS profile version (explicit or inside the security snapshot)
- network-policy / egress profile version (explicit or inside the security snapshot)
- vendor version predicate
- target onboarding profile version
- evidence set
- target class / environment
- certifier
- certification time
- expiry
- status

Informational metadata (optional, **not** authority): container image digest, JAR digest, SBOM, CI run, build provenance — only if actually available. Do **not** claim signed provenance unless present.

No mutable pointer may change certified content underneath an unchanged certification bundle.

Any material component change **invalidates** the bundle (see §47).

## 26.3 Runtime artifact identity (normative)

Certification **MUST** bind the exact deployable transport artifact.

**Authoritative identity fields:** artifact digest; transport implementation version; source baseline SHA; certification bundle version.

**Informational metadata** (optional): container image digest; JAR digest; SBOM; CI run; build provenance.

Gateway **startup MUST** establish its deployed artifact identity.

Before production mutation, the gateway **MUST** compare deployed artifact identity with the current certified bundle/profile.

Mismatch ⇒ **DENY**; set target/transport health to an appropriate safety state; alert and audit.

Prevent: certify artifact A, deploy artifact B, execute under A certification.

---

# 27. Certification Authority, Expiry, and Revocation

## 27.1 Authority and accepted SoD (normative)

Certification requires a human/operator authority **distinct from**:

- requester
- production authorizer
- production executor

Certification roles:

| Permission | Purpose |
|---|---|
| `VENDOR_INTERFACE_REVIEW` | approve interface definition version |
| `TRANSPORT_CERTIFY` | certify transport profile/bundle |
| `CAPABILITY_CERTIFY` | certify CELL/txPower capability |
| `SECURITY_CERTIFY` | certify TLS/credential/network security |

Onboarding permissions (split):

| Permission | Purpose |
|---|---|
| `TARGET_ONBOARD_CREATE` | create Level-3 onboarding draft |
| `TARGET_ONBOARD_REVIEW` | review onboarding |
| `TARGET_ONBOARD_APPROVE` | independent final production target registration |
| `TARGET_SUSPEND` | safety or operational suspend |
| `TARGET_REACTIVATE` | human reactivation after recertification |

**Accepted combinations**

- `TRANSPORT_CERTIFY` and `CAPABILITY_CERTIFY` **MAY** be held by the same principal for the same lab campaign.
- `SECURITY_CERTIFY` **MUST** be a different principal from `TRANSPORT_CERTIFY` for `PRODUCTION_REGISTERED`.
- `TARGET_ONBOARD_CREATE`, `TARGET_ONBOARD_REVIEW`, and `TARGET_ONBOARD_APPROVE` **MUST** be distinct principals for the same target registration (`CREATE` ≠ `REVIEW` ≠ `APPROVE`).
- Final `TARGET_ONBOARD_APPROVE` **MUST** be independent of `TARGET_ONBOARD_CREATE` and of the production authorizer and executor for executions against that target.
- No single principal **MAY** hold all of `VENDOR_INTERFACE_REVIEW` + `TRANSPORT_CERTIFY` + `CAPABILITY_CERTIFY` + `SECURITY_CERTIFY` + `TARGET_ONBOARD_APPROVE` for the same production registration.

**Forbidden**

- Production **executor MUST NOT** create, review, approve, or reactivate **its own** production target onboarding.
- Production **requester MUST NOT** self-certify the complete transport stack.
- Production **authorizer MUST NOT** alone establish the certification state on which their own execution authorization depends.

Agents and MCP have **NO** certification or onboarding permissions.

## 27.2 Expiry / revocation triggers

Including:

- vendor version change outside range
- interface version change
- transport implementation change
- security / credential / TLS trust / endpoint identity change
- capability change
- critical defect
- vendor advisory
- operator suspension
- failed production verification threshold
- bundle expiry

Expiry or revocation **MUST** block production mutation and revoke unconsumed grants for affected targets/profiles.

---

# 28. Gateway Certification Enforcement and Currentness

The production-write-gateway remains authoritative at the mutation boundary.

## 28.1 Durable certification authority (normative)

Certification / onboarding / health records are **durable authoritative state**.

The gateway **MUST** validate current safety-significant certification state at the mutation boundary using:

**(a)** authoritative durable reads, **or**

**(b)** a bounded / version-bound representation whose **currentness and revocation status can be proven** against authoritative durable state.

A stale event projection **alone** is **NOT** execution authority.

Local replica memory is **NOT** certification authority.

Unbounded caches are **prohibited** for safety-significant execution state.

If a cache exists:

- cache **MAY** accelerate reads
- cache **MUST NOT** establish execution permission
- safety-significant **positive** authorization **MUST** be current
- revocation **MUST NOT** depend solely on eventual event delivery

At final pre-mutation validation, **unknown**, **stale**, **source unavailable**, **version mismatch**, or **revocation uncertainty** ⇒ **DENY**.

**Multi-replica:** all gateway replicas use the same durable authority / currentness model; no replica-local positive certification authority; revocation is visible through authoritative state; replica partition from authority ⇒ **DENY**.

## 28.2 Send-boundary currentness (normative)

A valid grant issued earlier **does not waive** these checks.

Immediately before crossing the vendor mutation boundary, the gateway **MUST** establish currentness for **Phase 16**:

production authorization; fingerprint; grant binding/status; lease/fencing; kill switch; target state; change-control / window; rate / blast state

**AND Phase 17:**

interface definition currentness; transport certification; certification bundle; transport profile; artifact identity; target onboarding / certification; endpoint identity; vendor version compatibility; capability certification; security profile; credential profile; TLS profile; network profile; transport health

**AND** Level-4 execution authority currentness (Phase 16 production authorization for **this** execution, still current).

Unknown / non-affirmative ⇒ **DENY**.

These checks are **in addition to** Phase 16 consume, kill switch, rate limit, lease/fencing, window, change-control, SoD, and expected-state checks. They do not replace them.

## 28.3 Send-boundary race analysis (normative)

If any of the following is detected **before** crossing the mutation boundary ⇒ **DENY**:

| Race | Required pre-mutation outcome |
|---|---|
| Interface revoked after grant issuance | DENY |
| Certification revoked after grant issuance | DENY |
| Target suspended after grant issuance | DENY |
| Endpoint changed after grant issuance | DENY |
| Credential profile changed after grant issuance | DENY |
| TLS profile changed after grant issuance | DENY |
| Vendor version changes after grant issuance | DENY |
| Artifact deployment changes after certification | DENY |
| Level 4 revoked after grant issuance | DENY |
| Kill switch disabled after preflight begins (still PRE-SEND) | DENY |
| Gateway replica partitioned from certification authority | DENY |
| Stale cache after revocation | DENY (must not treat cache as authority) |

If revocation occurs **after** `MAY_HAVE_SENT`: use Phase 16 ambiguous-outcome / recovery semantics. **Never** resend automatically. Already-consumed grants **MUST NOT** be reset.

---

# 29. Level 3 versus Level 4

**LEVEL 3** means: this target and transport have been technically and security certified and registered.

**LEVEL 4** means: this **specific** production execution has the required operational/human authorization to cross the real mutation boundary.

LEVEL 3 **MUST NOT** imply LEVEL 4.

A production target may remain LEVEL 3 indefinitely with **zero** production changes permitted.

---

# 30. First Real Production Mutation

Phase 17 architecture **MUST NOT** silently make the first production mutation part of normal automated workflow.

If a later, separately authorized Level-4 certification exercise is ever permitted, it **MUST** be:

- explicitly human initiated
- single target
- single cell
- txPower only
- single mutation
- scheduled maintenance window
- real change ticket
- independent observer
- kill switch ready
- rollback prepared
- monitoring active
- audit active
- manual go / no-go

**NO LEVEL-4 EXECUTION IS AUTHORIZED BY THE PHASE 17 ARCHITECTURE DOCUMENT ITSELF.**

---

# 31. Observability and Alerting

## 31.1 Metrics (no secrets, no raw credentials)

Conceptual metrics:

- transport session attempts / failures
- credential failures
- TLS failures
- vendor mutation attempts
- vendor deterministic rejects
- unknown outcomes
- verification failures
- rollback attempts
- transport health
- certification expiry
- target suspension
- vendor-version mismatch

Allowed labels remain low-cardinality (`vendor`, `platform`, `environment`, `result`, `reasonCategory`, `healthState`, `certificationState`).

No cell parameter values in high-cardinality metrics unless explicitly approved. Forbidden labels continue to include `cellId`, `executionId`, `userId`, `endpoint`, `fingerprint`, secret names.

## 31.2 High-priority alerts

- unknown production outcome
- verification mismatch
- rollback failure
- credential failure
- TLS / security failure
- certification expiry
- target suspension
- vendor-version mismatch
- audit-chain invalidity
- rate-limit exhaustion
- kill-switch activation
- transport health degradation

---

# 32. Audit

Extend Phase 16 tamper-evident audit for:

- interface registration / approval
- transport / capability / security certification
- target onboarding / suspension / reactivation
- certification expiry / revocation
- vendor version change
- transport profile change

Reuse Phase 16 `previousEventHash` / `eventHash` principles where certification events share the production-change audit chain **or** a dedicated certification audit chain with the same tamper-evidence rules.

Do **not** redesign Phase 16 production-change audit unnecessarily.

No secrets, credential values, private keys, or proprietary vendor payloads in audit events.

---

# 33. Multi-Vendor Future

Architecture remains vendor-neutral above `VendorNetworkWriteAdapter`.

Phase 17 implementation scope is **Ericsson only**.

Nokia write support is **DEFERRED**. Do not create pretend Nokia support, Nokia transport profiles, or Nokia certification records.

---

# 34. Data Model (architecture proposal — V18 NOT CREATED)

Phase 17 **MAY** propose future **V18** tables. This architecture **MUST NOT** create V18.

Candidate entities (names are not implementation commitments until the implementation specification):

- `vendor_interface_definition`
- `vendor_write_transport_profile`
- `vendor_capability_certification`
- `transport_certification`
- `transport_certification_evidence`
- `transport_certification_bundle`
- `production_target_onboarding`
- `production_target_certification`
- `vendor_transport_health`
- `vendor_version_compatibility`
- `target_security_certification`

V1–V17 content remains unchanged by Phase 17 architecture authoring.

---

# 35. No New Autonomy

Phase 17 **MUST NOT** add:

- agent production authorization
- agent execution
- MCP execution
- scheduler execution
- event-driven production execution
- automatic rollback
- closed-loop optimization
- self-healing production mutation

Those remain explicitly **NOT AUTHORIZED**.

---

# 36. CI and Certification Pipeline

| Layer | Where | Writes? | Credentials? |
|---|---|---|---|
| Default CI | Azure-independent, vendor-independent | No | No — controlled transport only |
| L1 pipeline | approved vendor lab only | Lab cell txPower only | Non-production lab |
| L2 pipeline | operator pre-production only | Pre-prod only | Pre-prod write identity |
| L3 | target / security registration review | **No production mutation** | References only |
| L4 | manual production authorization | Only if separately authorized | Production write identity |

Default CI **MUST NEVER** masquerade as vendor certification.

Passing `mvn test` / `go test` satisfies Level 0 only.

---

# 36A. Change → invalidation matrix (normative)

Impact codes: **INV** = invalid; **STALE** = Phase 16 authorization/fingerprint stale; **REV** = unconsumed grants revoked; **DENY** = gateway eligibility deny. Consumed grants are never reset.

| Change | Bundle | Transport cert | Target cert | Phase 16 fingerprint / auth | Unconsumed grants | Gateway |
|---|---|---|---|---|---|---|
| Interface revoked / superseded | INV | INV | INV | STALE | REV | DENY |
| Documentation withdrawn / superseded | INV | INV | INV | STALE | REV | DENY |
| Interface or operator approval revoked | INV | INV | INV | STALE | REV | DENY |
| Provenance invalid (effective window / approval identity) | INV | INV | INV | STALE | REV | DENY |
| Transport implementation changed | INV | INV | INV where bound | STALE | REV | DENY |
| Artifact digest changed | INV | INV | INV where bound | STALE | REV | DENY |
| Endpoint identity changed | INV | INV where bound | INV | STALE | REV | DENY |
| Network profile changed | INV | INV where bound | INV | STALE | REV | DENY |
| TLS profile changed | INV | INV | INV | STALE | REV | DENY |
| Security profile changed | INV | INV | INV | STALE | REV | DENY |
| Credential profile changed | INV | INV | INV | STALE | REV | DENY |
| Capability profile changed | INV | INV | INV | STALE | REV | DENY |
| Vendor version changed / out of range | INV where range bound | INV / SUSPEND | INV / SUSPEND | STALE | REV | DENY |
| Target onboarding changed | INV where bound | — | INV | STALE | REV | DENY |
| Target suspended | — | — | INV / SUSPEND | STALE | REV | DENY |
| Certification expired | INV | INV | INV | STALE | REV | DENY |
| Certification revoked | INV | INV | INV | STALE | REV | DENY |
| Level 4 revoked (Phase 16 auth/generation) | — | — | — | STALE / revoked | REV | DENY |
| Kill switch disabled | — | — | — | — | — | DENY |

Material mutation of any certified snapshot **MUST** produce a **new version**. Old certification is stale/invalid as appropriate. Gateway final preflight **DENY**s old bindings.

---

# 37. Threat Model

| ID | Threat | Attack path | Impact | Preventive | Detective | Fail-safe | Residual |
|---|---|---|---|---|---|---|---|
| T17-01 | Fake vendor interface definition | Register invented protocol as “official” | Unsafe/wrong transport | Authoritative-source rule; `VENDOR_INTERFACE_REVIEW` | Interface audit | UNRESOLVED deny | Collusive approver |
| T17-02 | Unapproved protocol implementation | Ship guessed REST/NETCONF/CLI | Wrong mutation semantics | No protocol guessing; profile binds interface | Artifact/profile audit | Unconfigured transport | Privileged implementer |
| T17-03 | Endpoint substitution | Swap FQDN/reference after cert | Mutation to wrong system | Endpoint change invalidates cert/fingerprint/grants | Endpoint-change audit | Deny stale | Registry admin abuse |
| T17-04 | Credential substitution | Point profile at other secret | Cross-target write | Profile bind; write WI isolation; invalidation | KV/audit | Fail closed | Vault RBAC breach |
| T17-05 | Certificate spoofing | Fake ENM cert | MITM write | Hostname verify; trusted CA; no trust-all | TLS failure alerts | Deny | Compromised CA |
| T17-06 | Transport downgrade | Revert to weaker TLS/profile | Interception / weaker auth | Immutable certified profile; recertify on change | Profile version audit | Deny mismatch | Privileged downgrade |
| T17-07 | Vendor version drift | ENM upgrade outside range | Uncertified semantics | Range bind; auto SUSPENDED | Version-mismatch alert | Deny | Incomplete version signal |
| T17-08 | Capability drift | txPower semantics change | Wrong apply/verify | Capability recertify per release | Capability audit | Deny | Vendor silent change |
| T17-09 | Stale certification | Use expired bundle | Uncertified production path | Expiry enforcement at gateway | Expiry alerts | DENY unknown/expired | Clock skew |
| T17-10 | Forged certification evidence | Fake PASS hashes | False L1–L3 | Human certifier SoD; hash+reference; no CI masquerade | Evidence review | Deny missing bundle | Collusion |
| T17-11 | Certification privilege escalation | Executor certifies own transport | Self-authorized path | Certifier ≠ requester/authorizer/executor | Permission audit | Deny | Directory compromise |
| T17-12 | Target onboarding bypass | Execute without onboarding | Unregistered production target | Gateway requires current target certification | Deny metrics | Deny | Implementation defect |
| T17-13 | Transport implementation substitution | Deploy uncertified binary | Unproven mutation path | Artifact digest bind | Artifact mismatch deny | Deny | Supply-chain if no SBOM |
| T17-14 | Artifact mismatch | Certified SHA ≠ deployed SHA | Uncertified code path | Digest check | Deploy provenance | Deny | Missing provenance |
| T17-15 | Response spoofing | Fake vendor accept | False VENDOR_ACCEPTED | TLS; typed adapter; independent readback | Verify mismatch | No ack=applied | Compromised vendor |
| T17-16 | Readback spoofing | Fake desired state | False VERIFIED | Independent observe; freshness | Stale/unavailable taxonomy | No VERIFIED | Compromised observe path |
| T17-17 | Retry-induced duplicate write | Blind retry after timeout | Double mutation | MAY_HAVE_SENT prohibition | Outcome metrics | No second send | Operator manual duplicate |
| T17-18 | Cross-target credential reuse | One write secret, many targets | Lateral write | Per-profile credential bind; no read/write reuse | KV access logs | Deny unbound | Shared secret misconfig |
| T17-19 | Cross-target transport confusion | Lab profile on prod target | Prod hit via lab cert | Environment/target class bind; fingerprint | Profile checks | Deny | Mis-registration |
| T17-20 | Unsafe failover | Fail over to uncertified replica/endpoint | Uncertified mutation path | No uncertified failover; cert bind | Health/failover audit | Deny | Ops pressure |
| T17-21 | Network policy bypass | Open egress / alternate route | Uncontrolled vendor reach | Target-specific egress; no 0.0.0.0/0 | NP/tests | Deny | CNI/FQDN-cache limits |
| T17-22 | Agent / MCP bypass | Agent certifies or executes | Unauthorized mutation | No agent/MCP cert or execute perms | Denial audit | Deny | Feature creep |
| T17-23 | Production activation without Level 4 | Treat L3 as L4 | Unauthorized prod mutation | Explicit L3≠L4; gateway requires L4 | Auth audit | Deny | Operator override |
| T17-24 | Dynamic endpoint injection | Caller supplies ENM URL | Arbitrary target | Reject request-time endpoint | Admission deny | Deny | App compromise minting records |
| T17-25 | Secret persistence | Cert record stores secret | Credential leak | Values forbidden in DB | Schema/tests | Fail closed | Memory dump |
| T17-26 | Automatic re-enable after suspend | Health flip HEALTHY | Unsafe resume | No auto re-enable; `TARGET_REACTIVATE` | Reactivation audit | Stay SUSPENDED | Ops error |
| T17-27 | Closed-loop activation | Optimizer triggers write | Autonomous mutation | Explicit prohibition | Structural absence | N/A | Feature creep |
| T17-28 | ATOMIC false claim | Lab timing treated as CAS | Hidden TOCTOU | ATOMIC only if interface guarantees CAS | Capability review | READ_THEN_WRITE residual | Reviewer error |
| T17-29 | DNS / destination substitution | Approved FQDN resolves to uncertified destination | Mutation to wrong system | Endpoint identity tuple; egress; TLS identity match; DNS alone not vendor proof | TLS/route deny | DENY on identity mismatch | Compromised resolver + matching stolen cert |
| T17-30 | Certification cache stale / split-brain | Replica or cache serves revoked cert as current | Unauthorized mutation after revoke | Durable authority; cache not permission; partition DENY | Currentness deny metrics | DENY unknown/stale | Clock skew |
| T17-31 | Certification role concentration | One person certifies interface+transport+security+onboard | Self-authorized L3 path | SoD combinations; independent TARGET_ONBOARD_APPROVE | Permission audit | Deny combined stack | Directory compromise |
| T17-32 | Untrusted / forged evidence accepted as PASS | Hash string without authenticated certifier | False L1–L3 | Evidence trust authority; supersession; no lone hash | Evidence/audit review | Deny untrusted evidence | Collusion |
| T17-33 | Target onboarding during execution / currentness race | Onboard or change target after grant, before send | Uncertified production path | Send-boundary currentness; CREATE≠execute; stale auth | Race deny metrics | DENY | Implementation defect |

Residual risks accepted until later phases: external non-SNIP writers on ENM; honest TOCTOU under `READ_THEN_WRITE`; absence of supply-chain attestation until actually available; privileged administrator collusion; total write-gateway compromise (Phase 16 residual).

**Threat count: 33**  
`T17-01`–`T17-28` retained. `T17-29`–`T17-33` added by post-review correction.

Threat → primary gates (non-exhaustive): T17-01 G17-007/134; T17-02 G17-005/010; T17-03 G17-039/146; T17-04 G17-044/148; T17-05 G17-047/147; T17-06 G17-014/157; T17-07 G17-053/054; T17-08 G17-060; T17-09 G17-096/137; T17-10 G17-151; T17-11 G17-144/145; T17-12 G17-098/145; T17-13 G17-141/142; T17-14 G17-090/142; T17-15 G17-067; T17-16 G17-075; T17-17 G17-070/149; T17-18 G17-148; T17-19 G17-146; T17-20 G17-140; T17-21 G17-147; T17-22 G17-095/115; T17-23 G17-021/028/155; T17-24 G17-035; T17-25 G17-043; T17-26 G17-085/143; T17-27 G17-120; T17-28 G17-062/064; T17-29 G17-146/147; T17-30 G17-137/139/140; T17-31 G17-144/145; T17-32 G17-151/152; T17-33 G17-138/145.

---

# 38. Architecture Acceptance Gates

Each gate is an architectural invariant. Count emerges from the architecture; it is not a target.

1. **G17-001** Phase 16 governance, grants, consume, lease, kill switch, rate/blast, verification, rollback governance, and gateway remain authoritative and unreplaced.  
2. **G17-002** Ordinary SNIP application must not resolve write credentials or invoke vendor mutation.  
3. **G17-003** Separate production-write-gateway remains the mutation plane.  
4. **G17-004** Certification/onboarding is durable authoritative state; the gateway validates currentness at the mutation boundary and control-plane services must not bypass the gateway.  
5. **G17-005** No protocol guessing; Ericsson production write protocol remains UNRESOLVED until authoritative evidence.  
6. **G17-006** `EricssonWriteTransport` remains UNRESOLVED until an approved `VendorInterfaceDefinition` exists.  
7. **G17-007** At least one authoritative interface source is required before a concrete production transport may be designed.  
8. **G17-008** `VendorInterfaceDefinition` includes identity, vendor, platform, version range, type, documentation reference/version, approver, profiles, status, and effectiveness window.  
9. **G17-009** Documentation references are metadata only; proprietary blobs are not required.  
10. **G17-010** Architecture must not invent REST URLs, Bulk CM, CLI, SSH, SOAP, SDK, NETCONF, CORBA, payloads, or auth mechanisms.  
11. **G17-011** Abstract interface-type alternatives may exist; selected production type remains UNRESOLVED.  
12. **G17-012** Phase 11 `EnmTransport` remains read-only; no write methods.  
13. **G17-013** `VendorWriteTransportProfile` binds vendor/platform/version/interface/implementation/security/credential/capability/strategies/policies/objects/parameters/certification.  
14. **G17-014** Certified transport profiles are immutable/versioned; material change requires recertification.  
15. **G17-015** Gateway resolves only certified transport profiles.  
16. **G17-016** No dynamic untrusted class/plugin loading of transports.  
17. **G17-017** Production path remains `UnconfiguredProductionEricssonWriteTransport` until a certified replacement is registered.  
18. **G17-018** SPI layering `VendorNetworkWriteAdapter` → `EricssonEnmWriteAdapter` → `EricssonWriteTransport` is preserved.  
19. **G17-019** Registries/services for interface, certified transport, profile resolution, certification, capability, and health are defined.  
20. **G17-020** Certification lifecycle states from DRAFT through REVOKED are defined.  
21. **G17-021** `PRODUCTION_REGISTERED` must not mean production execution authorized.  
22. **G17-022** Automatic safety transitions to SUSPENDED/EXPIRED/REVOKED are permitted; automatic production re-enable is forbidden.  
23. **G17-023** Level 0 is local/simulated only; no vendor system required.  
24. **G17-024** Level 1 requires approved vendor lab, real supported interface, lab mutation, readback, rollback, and named failure modes.  
25. **G17-025** First real Ericsson integration must be vendor lab / test (Level 1).  
26. **G17-026** Level 2 requires objective production-equivalence evidence for network, identity, secrets, TLS, target registration, monitoring, audit, change-control, verification, and rollback; no production mutation.  
27. **G17-027** Level 3 certifies production target registration and must execute zero production mutations.  
28. **G17-028** Level 4 is the Phase 16 production authorization for a specific execution (fingerprint and generation), still current at send, plus current Phase 17 certification prerequisites; not a standing target flag.  
29. **G17-029** Level 3 must not imply Level 4.  
30. **G17-030** Architecture acceptance must not satisfy Level 4.  
31. **G17-031** Code, config, or default CI must not imply Level 4.  
32. **G17-032** `ProductionNetworkTarget` remains execution-authoritative.  
33. **G17-033** `ProductionTargetOnboardingRecord` enriches/certifies and does not become a second mutation authority.  
34. **G17-034** Onboarding binds target, vendor/platform/version, scope, interface, profiles, ownership, policies, evidence, and currency.  
35. **G17-035** Callers must not inject endpoint or credential dynamically at execution.  
36. **G17-036** Endpoints must not be supplied by API, Agent, MCP, proposal, or Phase 15 execution.  
37. **G17-037** Endpoint values are infrastructure/configuration data, not request parameters.  
38. **G17-038** Endpoint identity is distinct from credential material.  
39. **G17-039** Endpoint change invalidates certification, marks Phase 16 production authorization fingerprints STALE, and revokes unconsumed grants.  
40. **G17-040** Phase 10 remains authoritative for secrets and workload identity.  
41. **G17-041** Read identity and write identity remain separate.  
42. **G17-042** Only the gateway resolves write credentials.  
43. **G17-043** Secret values must never be stored in the SNIP database.  
44. **G17-044** Credential/profile changes invalidate certification and revoke/stale authorization and grants as required.  
45. **G17-045** Secret unavailable, rotated, expired, revoked, or identity-binding change fails closed.  
46. **G17-046** No older-version secret fallback; no vendor-secret value cache.  
47. **G17-047** TLS hostname verification is mandatory.  
48. **G17-048** Trusted CA chain is required; trust-all is prohibited.  
49. **G17-049** Hostname-verification disable is prohibited.  
50. **G17-050** Insecure TLS fallback is prohibited.  
51. **G17-051** Security-profile changes invalidate certification.  
52. **G17-052** mTLS private keys, when required, resolve only via the secure credential mechanism.  
53. **G17-053** Certified transport binds an approved vendor/platform version predicate that is explicit, evidence-based, and certification-controlled; SemVer is not inferred.
54. **G17-054** Version outside predicate suspends the target; no mutation until recertified.
55. **G17-055** Compatibility predicates must not auto-expand.
56. **G17-056** Unknown vendor version denies.  
57. **G17-057** Initial certified capability is CELL / txPower only.  
58. **G17-058** Cardinality remains one cell, one parameter, one operation.  
59. **G17-059** Capability certification proves addressing, type, unit, range, precision, read/write/rollback/verify, consistency, and delay.  
60. **G17-060** Capability certification is target/profile/version specific where behavior differs.  
61. **G17-061** No generic command execution.  
62. **G17-062** `ATOMIC` may be certified only if the authoritative interface provides real CAS/conditional mutation.  
63. **G17-063** Otherwise `READ_THEN_WRITE` remains certified with explicit residual TOCTOU.  
64. **G17-064** ATOMIC must not be inferred from optimistic lab timing.  
65. **G17-065** Direct vendor expected-state observation remains mandatory immediately before mutation.  
66. **G17-066** Write acknowledgement semantics must be formally certified.  
67. **G17-067** Vendor acknowledgement must not be treated as applied.  
68. **G17-068** Transport exception must not be treated as proof of not-applied.  
69. **G17-069** Every transport failure point is classified `POSITIVE_NOT_SENT` or `MAY_HAVE_SENT`.  
70. **G17-070** Automatic mutation retry after `MAY_HAVE_SENT` is prohibited.  
71. **G17-071** HTTP method semantics alone are not retry authority.  
72. **G17-072** Bounded retry may exist only for explicit `POSITIVE_NOT_SENT` cases.  
73. **G17-073** Vendor operation IDs are sanitized correlation/evidence only and are not proof of success.  
74. **G17-074** Independent vendor readback must be certified against the actual interface.  
75. **G17-075** Only fresh desired-state readback may satisfy production verification.  
76. **G17-076** Phase 17 must not mutate canonical `radio_configuration`; Phase 12 remains reconciliation authority.  
77. **G17-077** Rollback certification is the exact Phase 14 txPower rollback mutation.  
78. **G17-078** Rollback remains separately requested, reviewed, authorized, granted, consumed, and verified.  
79. **G17-079** Transport rollback support does not authorize automatic rollback.  
80. **G17-080** Named failure modes must be tested at the appropriate certification level.  
81. **G17-081** Ambiguous-outcome model remains the Phase 16 four-way observation table.  
82. **G17-082** Desired after UNKNOWN → VERIFIED; expected → safe stop and no resend; third → MANUAL_INTERVENTION_REQUIRED; unavailable → PRODUCTION_OUTCOME_UNRESOLVED.  
83. **G17-083** Transport health states HEALTHY/DEGRADED/UNAVAILABLE/SECURITY_FAILURE/CAPABILITY_MISMATCH/VERSION_MISMATCH/SUSPENDED are defined.  
84. **G17-084** Non-healthy safety states block execution.  
85. **G17-085** No automatic production re-enable after security or capability suspension.  
86. **G17-086** Evidence categories and metadata (hash/reference/type/issuer/environment/version/time/result) are defined.  
87. **G17-087** Raw evidence storage is not claimed immutable unless an immutable store exists.  
88. **G17-088** `TransportCertificationBundle` binds vendor, platform, interface, implementation, artifact hash, profiles, target class, version range, environment, evidence, certifier, time, expiry, and status.  
89. **G17-089** Material bundle-component change invalidates the bundle.  
90. **G17-090** Certification MUST bind authoritative artifact identity: artifact digest, transport implementation version, source baseline SHA, and certification bundle version.  
91. **G17-091** SBOM/provenance attestation must not be claimed unless actually available.  
92. **G17-092** Deployed binary must be traceable to the certified artifact; mismatch denies.  
93. **G17-093** Certifier is distinct from requester, production authorizer, and executor.  
94. **G17-094** Certification and onboarding permissions include VENDOR_INTERFACE_REVIEW, TRANSPORT_CERTIFY, CAPABILITY_CERTIFY, SECURITY_CERTIFY, TARGET_ONBOARD_CREATE/REVIEW/APPROVE, TARGET_SUSPEND, TARGET_REACTIVATE, with the accepted SoD combinations in §27.1.  
95. **G17-095** Agents and MCP must not grant certification.  
96. **G17-096** Expiry and revocation block production mutation.  
97. **G17-097** Named expiry/revocation triggers include version, interface, implementation, security, credential, endpoint, TLS, capability, defect, advisory, operator suspension, and verification-threshold failure.  
98. **G17-098** Gateway must prove certified profile, current cert, registered target, compatible version, certified capability, current security/credential profiles, sufficient health, and independent Level 4 before production mutation.  
99. **G17-099** Unknown certification state denies.  
100. **G17-100** Phase 16 consume/kill/rate/lease/window/change-control/SoD/expected-state checks remain mandatory in addition to certification checks.  
101. **G17-101** First production mutation must not be a silent automated workflow step.  
102. **G17-102** Any future Level-4 exercise, if separately authorized, must be human-initiated, single cell, txPower, single mutation, windowed, ticketed, observed, with kill switch, rollback, monitoring, audit, and go/no-go.  
103. **G17-103** This architecture document does not authorize Level-4 execution.  
104. **G17-104** Metrics exclude secrets, raw credentials, and unapproved high-cardinality cell parameter labels.  
105. **G17-105** High-priority alerts cover unknown outcome, verify/rollback failure, credential/TLS failure, expiry, suspension, version mismatch, audit invalidity, rate exhaustion, kill switch, and health degradation.  
106. **G17-106** Certification lifecycle events are audited without redesigning Phase 16 production-change audit.  
107. **G17-107** Audit events contain no secrets or proprietary vendor payloads.  
108. **G17-108** Architecture is vendor-neutral above `VendorNetworkWriteAdapter`.  
109. **G17-109** Nokia write support is deferred; no pretend Nokia implementation.  
110. **G17-110** V18 may be proposed only; V18 must not be created by architecture authoring.  
111. **G17-111** V1–V17 content remains unchanged by this document.  
112. **G17-112** Logical packages keep certification/onboarding in the app and transport resolution in the gateway.  
113. **G17-113** Vendor transport must not collapse into `snip-npo-app`.  
114. **G17-114** Dependency direction is 13→14→15→16 governance→gateway→certified resolver→adapter→transport→interface.  
115. **G17-115** Agent production authorization/execution remains unauthorized.  
116. **G17-116** MCP execution remains unauthorized.  
117. **G17-117** Scheduled production execution remains unauthorized.  
118. **G17-118** Event-driven production execution remains unauthorized.  
119. **G17-119** Automatic rollback remains unauthorized.  
120. **G17-120** Closed-loop optimization remains unauthorized.  
121. **G17-121** Default CI remains Azure-independent, vendor-independent, credential-independent, and write-free.  
122. **G17-122** Default CI must never masquerade as vendor certification.  
123. **G17-123** L1/L2/L3/L4 evidence layers are distinct pipelines/reviews.  
124. **G17-124** Infrastructure architecture describes separate gateway, write WI, target egress, TLS/mTLS, KV, NetworkPolicy, rotation, disruption, concurrency, and observability without implementing them.  
125. **G17-125** No real vendor endpoint is introduced by this architecture.  
126. **G17-126** No real production credential is introduced by this architecture.  
127. **G17-127** Production execution default remains disabled.  
128. **G17-128** Fresh deployment with empty/unknown certification remains incapable of production mutation.  
129. **G17-129** Threat model T17-01–T17-33 is present.  
130. **G17-130** Phase 17 implementation, specification, and Phase 18 are not started by this document.  
131. **G17-131** Production Ericsson auth method remains UNRESOLVED / externally configured as appropriate until evidence.  
132. **G17-132** Level 1 remains NOT EXECUTED; Level 2 NOT EXECUTED; Level 3 NOT SATISFIED; Level 4 NOT SATISFIED.  
133. **G17-133** Real production execution remains NOT AUTHORIZED.  
134. **G17-134** Interface SUPERSEDED/REVOKED, documentation withdrawn/superseded, or approval/provenance invalidation MUST cascade-invalidate derived certifications, stale Phase 16 authorizations, revoke unconsumed grants, and DENY — even if interfaceDefinitionId or vendor version is unchanged.  
135. **G17-135** Documentation withdrawal or supersession MUST trigger the same cascade as interface revocation.  
136. **G17-136** Already-consumed grants MUST NOT be reset; revocation MUST NOT cause blind resend.  
137. **G17-137** Gateway MUST validate safety-significant certification state at the mutation boundary via authoritative durable reads or a version-bound representation proven current against durable state.  
138. **G17-138** Send-boundary currentness MUST re-establish Phase 16 controls, Phase 17 certification bindings, and Level 4 currentness immediately before vendor mutation; a prior grant does not waive the checks.  
139. **G17-139** Unbounded caches are prohibited for safety-significant execution permission; cache MUST NOT establish authorization; unknown/stale/unavailable/revocation-uncertain ⇒ DENY.  
140. **G17-140** All gateway replicas share the same durable currentness model; replica-local positive certification authority is forbidden; partition from authority ⇒ DENY.  
141. **G17-141** Gateway startup MUST establish deployed artifact identity.  
142. **G17-142** Before production mutation the gateway MUST compare deployed artifact identity to the current certified bundle; mismatch ⇒ DENY, safety health, alert, audit.  
143. **G17-143** Certification lifecycle transitions follow the §11.2 table; unknown transitions DENY; REVOKED never auto-returns; EXPIRED requires explicit recertification; security/capability suspension never auto-reactivates.  
144. **G17-144** Certification SoD: requester must not self-certify the complete stack; authorizer must not alone establish the certification their authorization depends on; SECURITY_CERTIFY ≠ TRANSPORT_CERTIFY for PRODUCTION_REGISTERED; no single principal holds the full cert+approve set.  
145. **G17-145** TARGET_ONBOARD_CREATE, REVIEW, and APPROVE are distinct principals; executor MUST NOT create, review, approve, or reactivate its own production target onboarding.  
146. **G17-146** Certified endpoint identity binds productionTargetId, environment, network domain, FQDN, port, TLS server identity, route/zone, vendor/platform, and endpoint profile version.  
147. **G17-147** DNS alone is never vendor-identity proof; TLS/server identity and egress constraints MUST match the certified endpoint profile or DENY.  
148. **G17-148** Gateway credential resolution MUST bind authorized productionTargetId + certified credentialProfileId + certified transport/security profile + write identity; generic/unbound vendor write credentials DENY.  
149. **G17-149** POSITIVE_NOT_SENT requires certified positive evidence that mutation invocation did not reach vendor accept/apply; timeout and connection/response loss default MAY_HAVE_SENT.  
150. **G17-150** Effective execution eligibility is the AND of Phase 16 target health, Phase 17 transport health, current Phase 17 certifications, and remaining Phase 16 controls; HEALTHY transport never overrides Phase 16 denials.  
151. **G17-151** Trusted evidence requires authenticated certifier, authorized permission, durable record, subject/version/environment binding, audit event, result, timestamp, and reference/hash; a hash alone is not trusted.  
152. **G17-152** Historical PASS is not current authority; evidence is superseded/versioned; failed recertification must not leave old PASS authoritative; current certification references its active evidence set.  
153. **G17-153** Certified bindings are immutable versioned snapshots; no mutable pointer may change certified content under an unchanged bundle.  
154. **G17-154** The §36A change→invalidation matrix is normative for interface, documentation, approval, artifact, endpoint, network, TLS, security, credential, capability, version, onboarding, expiry, revoke, Level 4, and kill-switch changes.  
155. **G17-155** Level 4 send currentness is the Phase 16 production authorization for this execution, still current, plus current Phase 17 prerequisites.  
156. **G17-156** HTTP PUT, same desired value, request/operation/job ids, and transport-library behavior MUST NOT infer idempotency or POSITIVE_NOT_SENT and MUST NOT trigger mutation retry.  
157. **G17-157** Certification bundle MUST bind TLS profile version and network-policy profile version explicitly or via an immutable security snapshot containing those exact versions.  
158. **G17-158** Change-control remains abstract; Phase 16 MANUAL validation remains acceptable until a named integration is approved; no product-specific ITSM API is invented.  

**Architecture gate count: 158**

---

# 39. Failure Modes (architecture summary)

Phase 17 adds certification/onboarding failure modes without replacing the Phase 16 distributed-failure table:

| Mode | Mutation? | Notes |
|---|---|---|
| Interface UNRESOLVED | No | Unconfigured transport |
| Certification unknown/expired/revoked | No | Gateway DENY |
| Version mismatch | No | Target SUSPENDED |
| Health safety state | No | Deny; no auto resume |
| Artifact mismatch | No | Deny |
| Level 3 only | No | Registration ≠ execution |
| Level 4 absent | No | Independent authorization required |
| MAY_HAVE_SENT | No second send | Phase 16 recovery |
| Secret/TLS failure | No | Fail closed |

---

# 40. Test and Certification Strategy

| Stage | Proof | Not proof of |
|---|---|---|
| Default CI / Level 0 | Protocol-independent safety, unconfigured transport, isolation | Vendor correctness |
| Level 1 | Real lab interface, one lab txPower mutation, failure modes | Production readiness |
| Level 2 | Operator pre-prod path and operations | Production authorization |
| Level 3 | Target/security registration review | Any production write |
| Level 4 | Separate human execution authorization + external evidence | Permanent standing write right |

High-risk gates (protocol binding, retry, ambiguous outcome, Level 3≠4, gateway enforcement) require behavioral or integration evidence in a future implementation specification. Documentation alone is insufficient for those gates.

---

# 41. Out of Scope

- Nokia NetAct write
- Multi-cell / multi-parameter / bulk writes
- Generic vendor commands
- Agent / MCP / scheduler / event production execution
- Automatic rollback and closed loop
- Real credential or endpoint creation
- Guessed Ericsson protocol
- V18 creation
- Java / gateway / transport implementation
- Terraform / Kubernetes implementation
- Phase 18
- Production IAM redesign beyond existing SNIP patterns
- ServiceNow deep integration (still deferred unless already inherited)

---

# 42. Known Unresolved Items

Until authoritative Ericsson information exists, the following **MUST** remain unresolved. Do not convert them into assumptions.

```text
ERICSSON PRODUCTION WRITE PROTOCOL: UNRESOLVED
ERICSSON PRODUCTION ENDPOINT: NOT CONFIGURED
ERICSSON PRODUCTION AUTH METHOD: UNRESOLVED / EXTERNALLY CONFIGURED AS APPROPRIATE
ERICSSON PRODUCTION WRITE TRANSPORT: NOT IMPLEMENTED / NOT CONFIGURED
LEVEL 1: NOT EXECUTED
LEVEL 2: NOT EXECUTED
LEVEL 3: NOT SATISFIED
LEVEL 4: NOT SATISFIED
REAL PRODUCTION EXECUTION: NOT AUTHORIZED
EXPECTED-STATE GUARD STRENGTH FOR PRODUCTION ERICSSON: UNRESOLVED (ATOMIC only if interface proves CAS)
VENDOR-SIDE IDEMPOTENCY: UNRESOLVED
VENDOR OPERATION IDENTIFIERS: UNRESOLVED
WRITE ACKNOWLEDGEMENT MEANING: UNRESOLVED
```

---

# 43. Acceptance Criteria (architecture review)

Architecture may be accepted only if reviewers confirm:

1. No protocol guessing; Ericsson write protocol remains UNRESOLVED.  
2. Authoritative interface provenance is mandatory.  
3. Transport profile versioning and recertification are defined.  
4. Artifact binding is defined without false supply-chain claims.  
5. Separate write gateway is preserved.  
6. Target onboarding is defined and does not replace `ProductionNetworkTarget`.  
7. Capability certification is defined for CELL/txPower only.  
8. Security certification and TLS/mTLS policy are defined.  
9. Vendor-version compatibility and suspension are defined.  
10. Credential isolation and Phase 10 authority are preserved.  
11. Expected-state certification forbids false ATOMIC claims.  
12. Write semantics and no-blind-retry semantics are defined.  
13. Independent readback and rollback certification are defined.  
14. Ambiguous-outcome certification preserves Phase 16 recovery.  
15. Transport health, expiry, and revocation are defined.  
16. Level 0–4 separation is explicit; Level 3 ≠ Level 4.  
17. Gateway final certification enforcement is mandatory.  
18. No Agent / MCP / scheduler / event production execution.  
19. No closed loop; no automatic rollback.  
20. Production default remains disabled.  
21. Architecture acceptance does not imply production authorization.  
22. Nokia remains deferred.  
23. V18 is not created.  
24. Phase 16 baselines are unmodified.  
25. Interface/documentation/approval revocation cascades.  
26. Durable certification authority, send-boundary currentness, cache fail-closed, and multi-replica currentness are defined.  
27. Runtime artifact identity is MUST-bound and compared before mutation.  
28. Certification transitions, SoD, endpoint identity, target-bound credentials, POSITIVE_NOT_SENT proof, health AND composition, evidence trust/supersession, versioned snapshots, invalidation matrix, and Level 4 identity are implementable invariants.

---

# 44. Deferred Capabilities

- Nokia write adapter  
- Parameters other than txPower  
- Bulk / multi-cell production changes  
- Concrete Ericsson production protocol (pending evidence)  
- Level 1 / 2 execution  
- Level 3 satisfaction  
- Level 4 authorization  
- Supply-chain attestation unless/until available  
- ServiceNow deep integration  
- Phase 18  

---

# 45. Implementation Lifecycle (after future acceptance)

```text
architecture review
→ architecture freeze (when authorized)
→ architecture Git baseline (when authorized)
→ implementation specification (when authorized)
→ Cursor implementation (when authorized)
→ Level 0 / default CI
→ Level 1 vendor lab (when authorized)
→ Level 2 pre-production (when authorized)
→ Level 3 target registration (when authorized; no production mutation)
→ Level 4 only with independent operational authorization and external evidence
```

This document does **not** authorize those steps and does **not** declare freeze.

---

# 46. Final Status

```text
PHASE 17 ARCHITECTURE STATUS:
CORRECTED CANDIDATE — READY FOR FINAL ARCHITECTURAL REVIEW

PHASE 17 IMPLEMENTATION:
NOT STARTED

PHASE 17 IMPLEMENTATION SPECIFICATION:
NOT AUTHORIZED

V18:
NOT CREATED

REAL PRODUCTION EXECUTION:
NOT AUTHORIZED

ERICSSON PRODUCTION WRITE PROTOCOL:
UNRESOLVED

ERICSSON PRODUCTION WRITE TRANSPORT:
NOT IMPLEMENTED / NOT CONFIGURED

ERICSSON PRODUCTION ENDPOINT:
NOT CONFIGURED

ERICSSON PRODUCTION AUTH METHOD:
UNRESOLVED / EXTERNALLY CONFIGURED AS APPROPRIATE

LEVEL 1:
NOT EXECUTED

LEVEL 2:
NOT EXECUTED

LEVEL 3:
NOT SATISFIED

LEVEL 4:
NOT SATISFIED

NOKIA WRITE SUPPORT:
DEFERRED

AGENT / MCP / SCHEDULED / EVENT EXECUTION:
NOT AUTHORIZED

AUTOMATIC ROLLBACK:
NOT AUTHORIZED

CLOSED-LOOP OPTIMIZATION:
NOT AUTHORIZED

PHASE 16 IMPLEMENTATION BASELINE (IMMUTABLE):
f4e09b42f7b8f56c3794fae3c91a50a7af490c82

PHASE 16 ARCHITECTURE BASELINE:
8c0791b67ddd9121b1dd5d0abf452c056a8c9a52

PHASE 16 ARCHITECTURE SHA-256:
dfb4f477e813161843036482d3a6aafc7e19528c91cba1dbdecf2adfb5a5a3b0

PHASE 18:
NOT STARTED
```

---

*End of Phase 17 architecture document (corrected candidate — ready for final architectural review).*
