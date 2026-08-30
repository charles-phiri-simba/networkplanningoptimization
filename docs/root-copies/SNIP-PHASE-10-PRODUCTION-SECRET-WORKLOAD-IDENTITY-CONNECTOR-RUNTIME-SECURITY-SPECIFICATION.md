# SNIP Phase 10 — Production Secret Integration, Workload Identity & Connector Runtime Security Implementation Specification

## 1. Authority and Baseline

This is the bounded Cursor implementation contract for Phase 10.

Start exactly from:

```text
Branch: main
Commit: 4dfd8f0ec7d254ea292ab909b709eee3e599ef45
Commit message: feat: establish SNIP Phase 9 integration security foundation
Phase 9: ARCHITECTURALLY ACCEPTED / FROZEN
CI: success
Maven: 143 tests, 0 failures
Go: tests/build PASS
Working tree: clean
Phase 10: not started
```

Read `SNIP-PHASE-10-PRODUCTION-SECRET-WORKLOAD-IDENTITY-CONNECTOR-RUNTIME-SECURITY-ARCHITECTURE.md` completely before modifying code.

The architecture document is authoritative.

Implement Phase 10 only.

Do not start Phase 11.

Do not implement a real Ericsson ENM or Nokia NetAct connector.

Do not commit or push a Phase 10 baseline until architectural review, freeze, and explicit authorization.

## 2. Objective

Implement and prove:

```text
AKS Pod
   ↓
Dedicated ServiceAccount
   ↓
Workload Identity
   ↓
Dedicated User-Assigned Managed Identity
   ↓
Azure Key Vault
   ↓
Synthetic Connector Credential / Trust Material
   ↓
Phase 9 Secure Connector Session
   ↓
Secure Mock Vendor Endpoint
   ↓
Phase 8 Reliable Runtime
```

while preserving Phase 1–9 architecture.

## 3. Preserve Frozen Architecture

Do not redesign Phase 9 connector identity/fixed binding, TLS/mTLS, read-only authorization, application network policy, secret redaction/audit, Phase 8 lease/fencing/replay/retry/watchdog, Phase 7 reconciliation, Phase 6 Twin semantics, Phase 5 five-Agent model, or Phase 4 MCP governance.

No live network write path.

## 4. Repository / Infrastructure Discovery

Before editing:

1. verify HEAD is exactly the Phase 9 baseline;
2. inspect existing Terraform/Kubernetes/deployment structure;
3. identify whether AKS, service accounts, workload identity, Key Vault and network policy are already represented;
4. inspect Phase 9 `AzureKeyVaultCredentialProvider` contract;
5. inspect Phase 9 local provider and credential types;
6. inspect Phase 9 trust material handling;
7. inspect Phase 8 import entry point and lease ordering;
8. identify current CI/workflow conventions;
9. identify next Flyway migration;
10. update `.cursor/rules/snip-architecture.mdc` to authorize Phase 10 only.

Extend existing deployment tooling. Do not create parallel infrastructure without need.

## 5. Azure SDK Dependencies

Add only the minimal Azure SDK dependencies required for Azure Identity and Key Vault Secrets, plus certificate/trust retrieval only if required.

Do not add unrelated Azure management SDKs.

## 6. Real AzureKeyVaultCredentialProvider

Implement a real `AzureKeyVaultCredentialProvider`.

It must use a configured vault URI and fixed connector credential references from `ConnectorDefinition`.

Public API requests cannot provide:

```text
vaultUri
secretName
secretVersion
credentialRef
```

## 7. Production Workload Identity

Production profile must use explicit Workload Identity semantics.

Prefer `WorkloadIdentityCredential` or an explicitly constrained Azure credential configuration that cannot silently fall back to developer/environment credentials.

Document exact behavior.

## 8. Local Development Azure Identity

A local-development profile may use `DefaultAzureCredential`.

This behavior must not be enabled in production connector runtime.

## 9. No Local Provider Fallback

Production configuration must enforce:

```text
credentialProvider = AZURE_KEY_VAULT
LOCAL_DEVELOPMENT = disabled
```

If vault resolution fails, the connector import fails.

Add a release-blocking test proving no local fallback.

## 10. Azure Secret Reference

Extend safe references to contain:

```text
vault URI/reference
secret name
optional pinned version
credential type
```

Default: latest enabled version.

Pinned version is exceptional.

## 11. Secret Version Semantics

Persist/use actual Key Vault version ID as Phase 9 `credentialVersion`.

Do not store secret values.

## 12. No Older-Version Fallback

If the configured/latest version is disabled, expired, not-yet-valid, missing, or inaccessible:

- fail closed;
- safe failure code;
- no older-version fallback;
- no connector call;
- no canonical mutation.

## 13. Username/Password Secret Packaging

Use one versioned Key Vault secret payload containing both username and password.

Parsing occurs only in provider/infrastructure code.

Never log or persist payload values.

## 14. Client Certificate Packaging

Support a versioned secure representation for Phase 9 `CLIENT_CERTIFICATE` handles.

Prefer in-memory reconstruction.

Do not persist the private key or expose it to DTOs/audit/logs.

## 15. Trust Material Provider

Implement/evolve externalized trust-material retrieval where Phase 9 currently uses in-memory custom CA data.

Keep strict hostname verification unchanged.

## 16. Trust Material Rotation

A new connector session should pick up a new trust-material version without restart.

Add proof locally and in Azure E2E where practical.

## 17. Key Vault Client Reuse

Reuse safe SDK clients per vault where useful.

Do not cache resolved vendor secret values.

## 18. Key Vault Request Timeout

Bound vault requests and keep them inside the Phase 8 overall watchdog.

No indefinite calls.

## 19. Failure Codes

Add bounded safe failure codes:

```text
VAULT_AUTHENTICATION_FAILED
VAULT_ACCESS_DENIED
VAULT_SECRET_NOT_FOUND
VAULT_SECRET_DISABLED
VAULT_UNAVAILABLE
TRUST_MATERIAL_RESOLUTION_FAILED
```

Do not expose raw Azure SDK exception text.

## 20. Retryability

Implement:

```text
VAULT_UNAVAILABLE -> retryable
VAULT_AUTHENTICATION_FAILED -> non-retryable
VAULT_ACCESS_DENIED -> non-retryable
VAULT_SECRET_NOT_FOUND -> non-retryable
VAULT_SECRET_DISABLED -> non-retryable
TRUST_MATERIAL_RESOLUTION_FAILED -> non-retryable by default
```

No automatic import retry.

## 21. Lease-Before-Secret Ordering

Ensure:

```text
classify
  ↓
lease/fencing acquired
  ↓
vault secret resolved
  ↓
secure connector I/O
```

A same-scope losing execution must not retrieve the connector secret.

Add proof.

## 22. Multi-Instance Harness

Add a realistic proof with at least two independently instantiated runtime nodes sharing PostgreSQL.

Acceptable:

```text
two Spring application processes
two isolated runtime instances
or two AKS replicas in Azure E2E
```

The criterion is independent runtime ownership, not merely two threads within one service object.

## 23. Same-Scope Multi-Instance Proof

Two instances start the same source/scope import concurrently.

Assert:

```text
one lease winner
one non-owner
only winner resolves Key Vault credential
only winner opens secure connector session
only winner mutates canonical state
```

## 24. Cross-Scope Proof

Different source/scope imports may proceed independently.

No global lock.

## 25. Pod / Instance Recovery Proof

In Azure or equivalent multi-instance runtime:

1. instance/pod A acquires lease;
2. terminate A;
3. lease expires;
4. instance/pod B recovers/retries;
5. B gets new fencing token;
6. B resolves current vault credential;
7. stale A cannot commit.

Keep this bounded; do not expand into full chaos engineering.

## 26. Rotation Across Replicas

Required real Azure proof:

```text
session/replica A -> Key Vault version v1
rotate synthetic test secret -> v2
session/replica B -> v2
```

No pod/app restart.

## 27. Disabled / Revoked Secret Proof

Disable/revoke synthetic connector secret.

Expected:

```text
safe vault/credential failure
no older-version fallback
no connector call
no canonical mutation
```

## 28. Azure RBAC Infrastructure

Implement/extend IaC for:

```text
dedicated user-assigned managed identity
federated identity credential
Key Vault RBAC read role
AKS service-account binding
```

Use existing Terraform/module conventions.

## 29. Least-Privilege Proof

E2E should prove as much as safely possible:

```text
GET configured synthetic secret -> success
unrelated secret/scope -> denied
SET secret -> denied
DELETE secret -> denied
```

Do not broaden permissions for test convenience.

## 30. Environment Isolation Proof

If distinct INT/PROD vaults exist, prove INT workload identity cannot access PROD.

Otherwise use an equivalent unauthorized vault/secret scope and document the limitation.

## 31. Dedicated Kubernetes ServiceAccount

Create/use a dedicated service account for connector runtime.

Use workload-identity annotations/labels according to the existing AKS pattern.

No long-lived service-account secret token.

## 32. Pod Security Context

Add/validate:

```text
runAsNonRoot
allowPrivilegeEscalation=false
drop capabilities
seccompProfile: RuntimeDefault
readOnlyRootFilesystem where compatible
```

Document exceptions.

## 33. Kubernetes NetworkPolicy

Implement deployment-level egress policy:

```text
default deny egress
allow required DNS
allow Key Vault/private endpoint
allow approved secure mock vendor endpoint
allow required PostgreSQL/SNIP internal dependencies
```

Do not accidentally block required runtime dependencies without documenting them.

## 34. Network Policy Engine

Use the environment's supported policy engine.

Cilium is preferred where already available.

Do not migrate the cluster solely for Phase 10.

## 35. VNet / Firewall Controls

Extend existing Azure network/IaC controls where practical.

Document defense-in-depth target if an infrastructure layer is outside current repo authority.

## 36. Key Vault Private Endpoint

If existing platform architecture supports it cleanly, implement/configure:

```text
Private Endpoint
Private DNS
public network access disabled
```

If not feasible within current environment authority, document exact gap and do not invent an unsafe workaround.

## 37. Network Isolation E2E

From the Phase 10 AKS workload prove:

```text
Key Vault allowed
secure mock vendor endpoint allowed
arbitrary external endpoint denied
metadata endpoint denied
```

Record exact commands/results.

## 38. Azure E2E Synthetic Secret Provisioning

Use synthetic connector secret/cert material only.

Do not put values in Git or Terraform variables/state.

Provision through a protected bootstrap/test action or equivalent secure mechanism.

## 39. Terraform State Audit

Inspect Terraform configuration/plan inputs.

Confirm no:

```text
username
password
token
PKCS12 private key
client private key
synthetic connector secret value
```

is embedded through Phase 10 IaC.

Document proof.

## 40. Key Vault Diagnostics

Enable/document Key Vault diagnostics according to existing governance where in scope.

Do not ingest those logs into SNIP.

Document correlation using:

```text
executionId
connectorId
credentialVersion
timestamp
```

## 41. Security Audit Extension

Extend Phase 9 audit safely for:

```text
provider = AZURE_KEY_VAULT
vault reference
secret reference
credential version
trust-material version
```

No value/token.

## 42. Azure Token Redaction

Ensure access tokens are never logged, persisted, or exposed.

Do not serialize Azure `AccessToken`.

## 43. Production Readiness Status

Expose safe readiness metadata:

```text
credentialProviderMode
workloadIdentityConfigured
vaultConfigured
networkPolicyConfigured
connectorSecurityStatus
```

Do not call Key Vault for every health request.

## 44. Default CI Tests

Default Maven suite must include:

```text
provider contract tests
failure mapping
no-local-fallback
lease-before-secret-resolution using local/mock provider
secret version behavior
rotation logic
redaction regression
Phase 1–9 regressions
```

No real Azure requirement.

## 45. Real Azure E2E Workflow

Create/document a protected environment-gated Azure E2E workflow/process.

Requirements:

```text
not default CI
uses non-production AKS
pod authenticates using Workload Identity
pod retrieves synthetic Key Vault secret
runs Phase 10 acceptance probes
records evidence
```

GitHub Actions may orchestrate but must not retrieve the connector secret for the pod.

## 46. Mandatory Azure Acceptance Gate

Completion report must distinguish:

```text
DEFAULT CI STATUS
AZURE E2E STATUS
```

Phase 10 may only be `ACCEPTANCE RECOMMENDED` if both local/default verification and required Azure E2E pass.

If Azure E2E cannot run or fails, status must remain not recommended/incomplete.

## 47. Preserve Secure Mock Connector

All connector traffic still targets secure mock/synthetic endpoints.

Do not add:

```text
EricssonEnmAdapter
NokiaNetActAdapter
real vendor base URL
real vendor credential
```

## 48. No Secret APIs

Do not add endpoints for showing, reading, rotating, setting, or listing Key Vault secrets.

Import selects `connectorId` only.

## 49. Agent / LLM / MCP Boundaries

No Agent accesses Azure credential/provider SDK.

No LLM receives vault/token/secret material.

No MCP Key Vault/identity/secret tools.

No RAG ingestion of vault/security material.

## 50. Observability

Add safe metrics:

```text
vaultCredentialResolutions
vaultCredentialResolutionFailures
vaultAccessDenied
vaultUnavailable
vaultSecretDisabled
workloadIdentityAuthenticationFailures
credentialVersionChangesObserved
multiReplicaLeaseContention
```

No secret/token metric labels.

## 51. Threat Model Update

Document controls for:

```text
workload identity impersonation
service-account token theft
vault overprivilege
cross-environment secret access
vault outage
secret downgrade/fallback
local-provider fallback in production
Terraform-state secret leakage
multi-replica duplicate secret resolution
network-policy bypass
IMDS/node identity fallback
```

## 52. Flyway

Only add DB migration if safe metadata persistence changes.

Inspect the actual next migration.

Never create secret-value columns.

## 53. ADRs

Create sequential ADRs after Phase 9 ADR 068:

```text
069 Direct Azure Key Vault Provider for Production Connector Secrets
070 AKS Workload Identity and Dedicated Connector Managed Identity
071 Environment-Scoped Vault and Least-Privilege Azure RBAC
072 Per-Session Key Vault Resolution and No Secret Fallback
073 External Trust Material and Rotation
074 Kubernetes Egress Enforcement and Defense in Depth
075 Multi-Replica Connector Security and Lease-Before-Secret Resolution
076 Terraform/IaC Secret-State Prohibition
077 Mandatory Environment-Gated Azure Security E2E
078 No Real Vendor Connector Until Production Secret Gate Is Accepted
```

## 54. Documentation

Create/copy according to repository conventions:

```text
SNIP-PHASE-10-PRODUCTION-SECRET-WORKLOAD-IDENTITY-CONNECTOR-RUNTIME-SECURITY-ARCHITECTURE.md
SNIP-PHASE-10-PRODUCTION-SECRET-WORKLOAD-IDENTITY-CONNECTOR-RUNTIME-SECURITY-SPECIFICATION.md
```

Update:

```text
README.md
docs/implementation/SNIP-IMPLEMENTATION-CONTEXT.md
docs/implementation/SNIP-IMPLEMENTATION-STATUS.md
.cursor/rules/snip-architecture.mdc
```

Phase 9 remains frozen.

## 55. Required Local Verification

Run:

```text
mvn -B test
go test ./...
go build ./cmd/simulator
```

All Phase 1–9 regressions must pass.

## 56. Acceptance — Baseline

- [ ] Starts from `4dfd8f0ec7d254ea292ab909b709eee3e599ef45`.
- [ ] Phase 1–9 regressions pass.
- [ ] Phase 9 remains frozen.
- [ ] Phase 11 not started.
- [ ] No real vendor connector.

## 57. Acceptance — Vault / Identity

- [ ] Real AzureKeyVaultCredentialProvider implemented.
- [ ] Production uses Workload Identity.
- [ ] Dedicated user-assigned managed identity represented/provisioned.
- [ ] Dedicated Kubernetes ServiceAccount represented/provisioned.
- [ ] Azure RBAC is least-privilege read.
- [ ] No local provider fallback in production.
- [ ] No secret enumeration required.
- [ ] Environment isolation proven.
- [ ] Secret values absent from Git/DB/config/IaC state.

## 58. Acceptance — Version / Rotation

- [ ] Per-session secret resolution.
- [ ] Actual Key Vault version captured.
- [ ] Rotation without restart.
- [ ] Disabled/revoked secret fails closed.
- [ ] No older-version fallback.
- [ ] No connector secret cache.
- [ ] SDK client reuse does not cache secret values.

## 59. Acceptance — Multi-Instance

- [ ] At least two runtime instances/replicas proven.
- [ ] Same source/scope has one lease winner.
- [ ] Only winner resolves vault secret.
- [ ] Losing replica does not call connector.
- [ ] Different scopes can proceed independently.
- [ ] Pod/instance death recovery proven.
- [ ] New fencing token prevents zombie commit.

## 60. Acceptance — Deployment Security

- [ ] Kubernetes NetworkPolicy implemented.
- [ ] Default-deny egress semantics.
- [ ] DNS allowed as required.
- [ ] Key Vault endpoint allowed.
- [ ] Secure mock endpoint allowed.
- [ ] Arbitrary external egress denied.
- [ ] Metadata endpoint denied.
- [ ] Pod security context hardened.
- [ ] No long-lived service-account token.
- [ ] Existing deployment tooling extended.

## 61. Acceptance — Terraform / IaC

- [ ] Identity/federation/RBAC represented where repo owns them.
- [ ] No connector secret in Terraform state.
- [ ] No private key in Terraform state.
- [ ] Synthetic E2E secret provisioned outside Terraform state.
- [ ] Private Endpoint/DNS status documented.

## 62. Acceptance — Azure E2E

- [ ] Non-production AKS workload used.
- [ ] Pod uses Workload Identity.
- [ ] Real Key Vault accessed.
- [ ] Synthetic secret retrieved.
- [ ] Least privilege proven.
- [ ] Rotation proven.
- [ ] Secure mock connector succeeds.
- [ ] No real vendor credentials/endpoints.
- [ ] Azure E2E evidence recorded.
- [ ] Azure E2E remains separate from default CI.

## 63. Acceptance — Existing Security

- [ ] Phase 9 TLS/mTLS remains intact.
- [ ] Phase 9 canary/redaction remains intact.
- [ ] Read-only authorization remains intact.
- [ ] Phase 9 endpoint policy remains intact.
- [ ] Phase 8 lease/fencing/replay/retry remains intact.
- [ ] No canonical mutation on vault/security failure.
- [ ] No Agent/LLM/MCP secret access.
- [ ] No automatic Twin sync.
- [ ] No vendor write path.

## 64. Acceptance — CI / Hygiene

- [ ] `mvn -B test` passes.
- [ ] `go test ./...` passes.
- [ ] `go build ./cmd/simulator` passes.
- [ ] Default CI is Azure-independent.
- [ ] Azure E2E is protected/environment-gated.
- [ ] No secret/cert/private-key artifacts committed.
- [ ] ADRs 069–078 created.
- [ ] Documentation/status/rule updated.
- [ ] Phase 11 not started.

## 65. Required Completion Report

Create:

```text
docs/implementation/SNIP-PHASE-10-COMPLETION-REPORT.md
```

Include:

1. Executive Summary
2. Phase 9 Baseline Verification
3. Scope Delivered
4. Production Secret Architecture
5. Azure Key Vault Provider
6. Workload Identity Model
7. Dedicated Managed Identity
8. Kubernetes ServiceAccount
9. Azure Credential Selection
10. Environment Isolation
11. Vault Topology
12. Azure RBAC
13. Secret Naming / References
14. Secret Version Semantics
15. No Older-Version Fallback
16. Per-Session Resolution
17. Key Vault Client Reuse
18. Username/Password Packaging
19. Client Certificate Packaging
20. External Trust Material
21. Trust Rotation
22. Key Vault Failure Mapping
23. Retryability
24. Request Timeout
25. No Local Provider Fallback
26. Phase 8 Lease-Before-Secret Ordering
27. Multi-Instance Architecture
28. Same-Scope Multi-Replica Proof
29. Independent-Scope Proof
30. Pod/Instance Recovery Proof
31. Rotation Across Replicas
32. Secret Disable/Revocation Proof
33. Infrastructure-as-Code
34. Federated Identity Credential
35. Key Vault RBAC Proof
36. Environment Isolation Proof
37. Kubernetes NetworkPolicy
38. VNet/Firewall Defense in Depth
39. Key Vault Private Endpoint/DNS Status
40. Network Isolation E2E
41. Pod Security
42. Terraform Secret-State Audit
43. Azure Token Redaction
44. Security Audit Extension
45. Azure E2E Workflow
46. Real AKS Workload Identity → Key Vault Proof
47. Default CI
48. Phase 9 Regression
49. Phase 8 Runtime Regression
50. Phase 7 Reconciliation Boundary
51. Agent / LLM / MCP Boundary
52. Threat Model
53. Observability
54. Tests
55. Local E2E Evidence
56. Azure E2E Evidence
57. ADRs
58. Performance
59. Acceptance PASS/FAIL
60. Known Limitations
61. Technical Debt
62. Lessons Learned
63. Recommended Next Phase
64. Architectural Questions

End with exactly one:

```text
PHASE 10 STATUS: ACCEPTANCE RECOMMENDED
```

or:

```text
PHASE 10 STATUS: ACCEPTANCE NOT RECOMMENDED
```

Do not mark Phase 10 architecturally accepted yourself.

## 66. Architectural Questions for Review

Without broadening implementation, recommend:

1. whether Phase 11 should finally implement the first real vendor connector;
2. whether Ericsson or Nokia should be first;
3. whether Private Endpoint/private DNS must be mandatory before first connector;
4. whether mTLS should become mandatory for the selected first vendor;
5. whether connector runtime should remain in the monolith or become a dedicated workload before real vendor I/O;
6. whether cooperative cancellation should be implemented before the first real connector;
7. whether scheduled synchronization should remain deferred for the first real connector.

## 67. Git Safety

During implementation:

- do not commit;
- do not push;
- do not amend Phase 9;
- do not create a Phase 11 branch;
- do not self-authorize acceptance.

Leave Phase 10 as uncommitted working-tree changes.

## 68. Final Instruction to Cursor

Treat this as authorization for **Phase 10 only**.

The objective is:

> **Prove that SNIP can use a production-grade AKS workload identity to retrieve versioned synthetic connector credentials from real Azure Key Vault, enforce deployment-level egress controls, and preserve Phase 8/9 connector security across multiple runtime instances — without yet connecting to a real vendor system.**

Preserve all Phase 1–9 architecture.

Do not implement real Ericsson ENM.
Do not implement real Nokia NetAct.
Do not add real vendor credentials.
Do not add vendor writes.
Do not add scheduled synchronization.
Do not add vendor telemetry.
Do not add automatic import retry.
Do not place secrets/private keys in Terraform state.
Do not start Phase 11.

When implementation and validation are complete:

1. run the full local/default verification;
2. execute the required protected Azure E2E;
3. produce the Phase 10 completion report;
4. leave all Phase 10 work uncommitted;
5. do not push;
6. STOP for architectural review.
