# ADR 074 — Kubernetes Egress Enforcement and Defense in Depth

## Status

Accepted for Phase 10.

## Context

Phase 9 application-level network policy remains canonical. Deployment-level egress is additional defense in depth, not a replacement.

## Decision

Connector pods use default-deny egress plus explicit allows for DNS, PostgreSQL, Key Vault/token endpoints, and the approved secure mock vendor endpoint. Cilium FQDN policy is preferred where the cluster already has Cilium. Metadata (169.254.169.254) and arbitrary internet egress are denied. VNet/firewall/Private Endpoint remain platform-owned layers; this repo documents the gap rather than inventing an unsafe public workaround.

## Consequences

Application NetworkPolicyEnforcer remains authoritative for connector URIs. Kubernetes policy must not be relaxed to `0.0.0.0/0` to make tests pass.
