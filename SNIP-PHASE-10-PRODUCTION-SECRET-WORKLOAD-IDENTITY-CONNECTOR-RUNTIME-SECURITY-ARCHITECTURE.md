# SNIP Phase 10 — Production Secret Integration, Workload Identity & Connector Runtime Security Architecture

## 1. Purpose

Phase 10 establishes SNIP's first production-grade connector security runtime by integrating real Azure Key Vault access through AKS Workload Identity, strengthening deployment/network isolation, and proving multi-instance connector security — while still keeping real Ericsson ENM and Nokia NetAct connectivity out of scope.

Starting baseline:

```text
Branch: main
Commit: 4dfd8f0ec7d254ea292ab909b709eee3e599ef45
Message: feat: establish SNIP Phase 9 integration security foundation
Phase 9: ARCHITECTURALLY ACCEPTED / FROZEN
CI: PASS
Maven: 143 tests, 0 failures
Go: tests/build PASS
Working tree: clean
Phase 10: not started
```

The architectural question is:

> **How can SNIP move from mock-local connector credentials to production-grade secret retrieval, workload identity, deployment-level egress enforcement, and multi-instance connector security without yet connecting to a real vendor system?**

The governing principle is:

> **Production identity and secret delivery must be proven before any real vendor connector is introduced.**

## 2. Architectural Position

```text
AKS Pod
   ↓
Dedicated Kubernetes ServiceAccount
   ↓
Microsoft Entra Workload Identity
   ↓
Dedicated User-Assigned Managed Identity
   ↓
Azure Key Vault
   ↓
Synthetic Connector Credential / Certificate
   ↓
Phase 9 Secure Connector Security Context
   ↓
Secure Mock Vendor Endpoint
   ↓
Phase 8 Reliable Import Runtime
   ↓
Phase 7 Canonical Reconciliation
```

Phase 10 changes the production credential source, workload identity, deployment-level network enforcement, and multi-instance proof. It does not redesign Phase 7–9.

## 3. Core Phase 10 Invariants

- Real Azure Key Vault integration: YES.
- Real AKS Workload Identity: YES.
- Real vendor connector: NO.
- Real vendor credentials: NO.
- Synthetic connector secrets/certificates only.
- Production credential delivery uses Workload Identity.
- Key Vault access is read-only and least privilege.
- Connector credential resolution remains per session.
- No connector secret-value cache.
- No fallback to local credentials in production.
- No secret values in Git, Terraform state, database, logs, APIs, Agents, LLMs, MCP, or RAG.
- Phase 8 lease/fencing occurs before vault secret resolution.
- A losing replica must not retrieve the same-scope connector secret.
- Default CI remains Azure-independent.
- Environment-gated Azure E2E is mandatory before architectural acceptance.
- Real Ericsson/Nokia connectivity remains deferred.

## 4. Direct Azure Key Vault SDK Strategy

Phase 10 uses direct application-level Azure Key Vault access:

```text
AzureKeyVaultCredentialProvider
        ↓
Azure Identity TokenCredential
        ↓
SecretClient
        ↓
Azure Key Vault
```

Key Vault CSI is not the canonical Phase 10 credential path. It remains a future deployment option.

Direct SDK access preserves the Phase 9 per-session credential-resolution and rotation model.

## 5. Workload Identity Model

Production AKS identity model:

```text
Kubernetes ServiceAccount
      ↓
Federated Identity Credential
      ↓
User-Assigned Managed Identity
      ↓
Azure Key Vault RBAC
```

SNIP does not store a bootstrap client secret for vault authentication.

The connector-vault identity is distinct from human, deployment, CI, cluster-control-plane, node, Agent, and end-user identities.

## 6. Dedicated Connector Vault Identity

Use a dedicated user-assigned managed identity conceptually equivalent to:

```text
snip-connector-secrets-mi
```

Its purpose is limited to reading approved connector secret/certificate material.

## 7. Kubernetes ServiceAccount

Use a dedicated service account such as:

```text
snip-connector-runtime
```

Only connector-secret-capable workloads use it. The connector runtime may remain inside the monolith in Phase 10.

## 8. Azure Credential Selection

Locked behavior:

```text
Local development:
  DefaultAzureCredential may be used

AKS production:
  WorkloadIdentityCredential is preferred / explicitly bound
```

Production must not silently fall back to developer CLI credentials, environment client secrets, or node/IMDS identity.

## 9. Environment Isolation

Identity and vault access are environment-scoped:

```text
INT workload identity -> INT vault
PROD workload identity -> PROD vault
```

An INT identity must not retrieve PROD/unrelated connector material.

## 10. Vault Topology

Use one vault per environment/security boundary rather than one vault per secret.

Example:

```text
SNIP INT Vault
SNIP PROD Vault
```

Each vault may contain separate Ericsson/Nokia reader credentials, client certificates, and trust-material references.

## 11. Azure RBAC

Use Azure RBAC for Key Vault data-plane access.

Required semantics:

```text
GET configured connector secret/certificate -> allowed
SET -> denied
DELETE -> denied
PURGE -> denied
RBAC administration -> denied
```

Secret enumeration is not required.

## 12. Secret References and Naming

Use deterministic environment-aware logical names, for example:

```text
snip-{environment}-{vendor}-{purpose}-{credential}
```

Model safe references:

```text
AzureVaultCredentialReference
  vaultUri
  secretName
  optionalVersion
  credentialType
```

Public APIs never accept arbitrary vault URIs, secret names, or versions.

## 13. Secret Version Policy

Default resolution uses the latest enabled version.

Pinned versions are exceptional and explicit.

If the configured/latest version is disabled, expired, not-yet-valid, or missing, fail closed.

Do not automatically fall back to an older version.

## 14. Per-Session Secret Resolution

Every new connector session resolves the current vault credential version.

No application or pod restart is required after rotation.

No vendor connector secret-value cache is introduced.

## 15. Client Reuse vs Secret Cache

Allowed:

```text
reuse SecretClient per vault
reuse Azure SDK token cache
```

Prohibited:

```text
indefinite vendor-secret value cache
```

## 16. Username/Password Packaging

Store username/password as one versioned logical secret package so both rotate atomically.

The payload is parsed only in `AzureKeyVaultCredentialProvider`.

## 17. Client Certificate Packaging

Support a versioned secure representation sufficient to reconstruct, in memory:

```text
certificate chain
private key
```

Private keys must not appear in Git, database, Terraform state, logs, or API responses.

## 18. External Trust Material

Allow custom server CA/trust material to be externally managed through a provider such as:

```text
ConnectorTrustMaterialProvider
```

Production source may be Key Vault or an approved enterprise trust source.

New sessions can pick up rotated trust material.

## 19. Key Vault Failure Model

Use bounded safe failures:

```text
VAULT_AUTHENTICATION_FAILED
VAULT_ACCESS_DENIED
VAULT_SECRET_NOT_FOUND
VAULT_SECRET_DISABLED
VAULT_UNAVAILABLE
TRUST_MATERIAL_RESOLUTION_FAILED
```

No raw Azure SDK exception details are exposed.

## 20. Retryability

Recommended:

```text
VAULT_UNAVAILABLE -> retryable
VAULT_AUTHENTICATION_FAILED -> non-retryable
VAULT_ACCESS_DENIED -> non-retryable
VAULT_SECRET_NOT_FOUND -> non-retryable
VAULT_SECRET_DISABLED -> non-retryable
TRUST_MATERIAL_RESOLUTION_FAILED -> non-retryable by default
```

Azure SDK transport retry may remain bounded infrastructure behavior; Phase 8 import retry remains explicit only.

## 21. Key Vault Timeout

Vault operations must be bounded and fit inside the Phase 8 overall watchdog.

No indefinite credential lookup.

## 22. No Local Provider Fallback in Production

Production profile selects `AZURE_KEY_VAULT` and disables `LOCAL_DEVELOPMENT`.

If Key Vault fails, the import fails. No local fallback is permitted.

## 23. Phase 8 Ordering

Required ordering:

```text
Import request
   ↓
classify
   ↓
acquire PostgreSQL lease
   ↓
obtain fencing token
   ↓
resolve Azure Key Vault credential
   ↓
create Phase 9 secure connector session
   ↓
read snapshot
```

No lease means no connector-secret retrieval.

## 24. Multi-Instance Runtime Proof

Phase 10 requires at least two independently instantiated runtime nodes sharing PostgreSQL and the same source/scope.

Same-scope concurrency must prove:

```text
one lease winner
only winner resolves vault secret
only winner opens secure connector session
only winner mutates canonical state
```

Different scopes remain independently executable.

## 25. Pod/Instance Recovery

One bounded proof must show:

```text
instance A owns lease
A terminates
lease expires
instance B recovers/retries
B gets new fencing token
B resolves current vault credential
stale A cannot commit
```

## 26. Rotation Across Replicas

Required:

```text
replica/session A -> vault version v1
rotate synthetic secret
replica/session B -> vault version v2
```

No restart and no stale vendor-secret cache.

## 27. Credential Kill Switches

Both remain valid:

```text
connector enabled=false
```

and external:

```text
disable/revoke Key Vault secret
remove Key Vault RBAC
```

No fallback to older/local credentials.

## 28. Azure Access Token Safety

Entra access tokens are secrets.

Never persist or expose them in logs, audit, metrics, DB, API, or traces.

Azure SDK token caching is acceptable; application code must not manage tokens manually.

## 29. Azure SDK Isolation

Azure SDK types remain inside provider/infrastructure code.

Do not leak `SecretClient`, `KeyVaultSecret`, `TokenCredential`, or `WorkloadIdentityCredential` into import, reconciliation, Agent, MCP, Twin, or API DTO layers.

## 30. Infrastructure-as-Code Scope

Phase 10 includes bounded infrastructure/manifests necessary for the non-production proof.

Extend the existing repository infrastructure structure.

Potential resources:

```text
user-assigned managed identity
federated identity credential
AKS ServiceAccount
Key Vault RBAC
Key Vault references
Kubernetes NetworkPolicy
deployment/service-account wiring
optional Private Endpoint / Private DNS if supported by existing platform architecture
```

## 31. Terraform Secret-State Prohibition

Terraform may create identity, vault, RBAC, federation, and network resources.

Terraform must not provision/store connector secret values, passwords, tokens, PKCS#12 bundles, or private keys in state.

Synthetic E2E secrets are provisioned through a protected test/bootstrap mechanism outside Terraform state.

## 32. Mandatory Real Azure E2E

Architectural acceptance requires a controlled non-production proof:

```text
AKS pod
   ↓
Workload Identity
   ↓
Azure Key Vault
   ↓
synthetic connector credential
   ↓
Phase 9 secure mock connector
```

No real vendor credential or endpoint is used.

This proof is separate from default CI.

## 33. Default CI

Default CI remains Azure-independent:

```text
mvn -B test
go test ./...
go build ./cmd/simulator
```

No Azure login, AKS, Key Vault, vendor system, production certificate, or production secret is required.

## 34. Environment-Gated Azure E2E

Create/document a protected workflow/process where the pod itself authenticates to Key Vault through Workload Identity.

GitHub Actions may orchestrate the test but must not retrieve the connector secret on behalf of the pod.

## 35. Acceptance Gate

Default CI passing is necessary but insufficient.

Phase 10 cannot be architecturally accepted until the real AKS Workload Identity → Key Vault proof passes.

## 36. Key Vault Network Security

Production target:

```text
public network access disabled
+
Private Endpoint
+
Private DNS
```

where compatible with existing platform architecture.

If non-production proof cannot fully implement this without disproportionate external platform work, document exactly what is and is not proven.

## 37. Kubernetes NetworkPolicy

Phase 10 requires deployment-level egress enforcement:

```text
default deny egress
allow required DNS
allow Key Vault/private endpoint
allow approved secure mock vendor endpoint
allow required SNIP internal dependencies
```

Phase 9 application-level policy remains defense in depth.

## 38. Network Policy Engine

Kubernetes NetworkPolicy semantics are required.

Cilium is preferred where already supported, but cluster migration solely for this phase is not required.

## 39. VNet / Firewall Defense in Depth

Production direction:

```text
Pod NetworkPolicy
+
VNet / NSG / Firewall egress controls
```

Implement available non-production controls consistent with existing infrastructure.

## 40. Network Isolation Proof

From the Phase 10 AKS workload prove:

```text
Key Vault -> ALLOW
approved secure mock vendor endpoint -> ALLOW
arbitrary external endpoint -> DENY
cloud metadata endpoint -> DENY
```

## 41. Pod Security

Deployment manifests should enforce where practical:

```text
runAsNonRoot
allowPrivilegeEscalation=false
drop Linux capabilities
seccompProfile: RuntimeDefault
readOnlyRootFilesystem
```

Document exceptions.

## 42. Short-Lived Kubernetes Identity

Use projected/federated workload identity tokens.

Do not use long-lived Kubernetes service-account secret tokens.

## 43. Security Audit Extension

Phase 9 security audit may add safe metadata:

```text
provider = AZURE_KEY_VAULT
vault reference
secret name/reference
credential version
trust material version
```

Never values or Azure tokens.

## 44. Key Vault Diagnostic Correlation

Document correlation between SNIP execution/session audit and Azure Key Vault diagnostics via:

```text
executionId
connectorId
credentialVersion
timestamp
```

Do not ingest Azure diagnostics into SNIP in Phase 10.

## 45. Production Readiness

Expose safe readiness metadata:

```text
credentialProviderMode
workloadIdentityConfigured
vaultConfigured
networkPolicyConfigured
connectorSecurityStatus
```

Do not retrieve secrets on every health request.

## 46. Relationship to Phase 9

Phase 9 remains authoritative for connector identity, fixed binding, TLS/mTLS, read-only authorization, network endpoint policy, redaction, and security audit.

Phase 10 changes only:

```text
production credential/trust source
Azure workload identity
deployment-level egress
multi-instance proof
```

## 47. Relationship to Phase 8

Phase 8 remains authoritative for lease, fencing, NEW/RETRY/REPLAY, watchdog, immutable attempts, and atomic commit.

Phase 10 proves those semantics across multiple runtime instances with real vault resolution.

## 48. Agent / LLM / MCP Boundaries

Agents cannot access Key Vault, Azure credentials/tokens, connector secrets, or trust private keys.

No Key Vault/Azure identity MCP tools.

No Azure tokens/secret material in prompts or RAG.

## 49. Real Vendor Connectivity

Explicitly prohibited in Phase 10.

The first real vendor connector is Phase 11 or later.

## 50. Threat Model Additions

Phase 10 addresses:

```text
workload identity impersonation
service-account token theft
Key Vault overprivilege
cross-environment secret access
vault outage
secret downgrade/fallback
local-provider fallback in production
Terraform-state secret leakage
multi-replica duplicate secret resolution
network-policy bypass
node/IMDS identity fallback
```

## 51. Required Proofs

- Real Workload Identity → Key Vault.
- Least-privilege RBAC.
- Environment isolation.
- No local fallback.
- Rotation without restart.
- Secret disable/revocation fail-closed.
- Multi-replica same-scope one-winner behavior.
- Independent-scope behavior.
- Pod/instance recovery.
- Network enforcement.
- Pod security.
- No Terraform secret-state leakage.
- Phase 9/8 security/runtime regression.

## 52. Observability

Add safe metrics such as:

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

Never use secret/token values as labels.

## 53. Explicitly Out of Scope

Do not implement:

```text
real Ericsson ENM
real Nokia NetAct
real vendor credentials
vendor writes
scheduled imports
continuous synchronization
vendor telemetry
automatic import retry
secret rotation watcher
OAuth vendor authentication
dynamic connector admin CRUD
connector worker microservice split
full chaos engineering
general-purpose Key Vault admin
secret creation/update API
Terraform-managed secret values
production PKI automation
Phase 11
```

## 54. Locked Phase 10 Decisions

- Phase: **Production Secret Integration, Workload Identity & Connector Runtime Security**
- Baseline: `4dfd8f0ec7d254ea292ab909b709eee3e599ef45`
- Real vendor connector: NO
- Real Azure Key Vault: YES
- Real AKS Workload Identity: YES
- Azure vault access: direct Azure SDK
- Key Vault CSI: deferred/non-canonical
- Production credential: WorkloadIdentityCredential
- DefaultAzureCredential: local development only
- Managed identity: dedicated user-assigned identity
- Vault authorization: Azure RBAC
- Vault permission: read-only/least privilege
- Secret enumeration: not required
- Environment isolation: required
- Secret resolution: per connector session
- Secret value cache: none
- Azure client reuse: yes
- Secret version: latest enabled by default
- Older-version fallback: prohibited
- Credential rotation: no restart
- Trust material externalization: included
- Trust rotation: new session
- Local provider in production: prohibited
- Kubernetes ServiceAccount: dedicated
- Pod security: hardened
- NetworkPolicy: required
- Cilium: preferred/environment-dependent
- VNet/firewall: defense in depth
- Multi-replica proof: required
- Minimum acceptance replicas: 2
- Same-scope import: one winner
- Losing replica secret resolution: should not occur
- Pod/instance recovery proof: required
- Terraform secret/private-key values: prohibited
- Default CI: Azure-independent
- Real Azure E2E: separate and mandatory for acceptance
- Azure E2E secrets: synthetic only
- Phase 9 security envelope: unchanged
- Phase 8 runtime: unchanged
- Agents: no Azure/secret access
- MCP: none
- Automatic retry: no
- Scheduled synchronization: no
- Real ENM/NetAct: deferred
- Phase 11: not started

## 55. Expected ADR Direction

After Phase 9 ADR 068:

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

## 56. Architectural Outcome

At completion:

```text
AKS Runtime (2+ replicas)
        ↓
Phase 8 Lease / Fencing
        ↓
Dedicated Workload Identity
        ↓
Azure Key Vault
        ↓
Versioned Synthetic Connector Credential
        ↓
Phase 9 Secure Connector Session
        ↓
Approved Secure Mock Endpoint
        ↓
Phase 8 / Phase 7 Processing
```

with real vault identity, Key Vault access, multi-instance safety, and deployment egress enforcement proven — but still no real vendor connector or live network write path.
