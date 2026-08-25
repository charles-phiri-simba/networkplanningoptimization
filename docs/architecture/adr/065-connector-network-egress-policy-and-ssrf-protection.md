# ADR 065 — Connector Network Egress Policy and SSRF Protection

## Status

Accepted for Phase 9.

## Context

Callers must not supply URLs. Connector HTTP clients must not reach metadata endpoints, arbitrary private hosts, or non-HTTPS schemes.

## Decision

Each connector has a typed egress policy: allow-listed hostnames and ports, `httpsOnly=true`, `allowRedirects=false`. Destinations are taken from `ConnectorEndpointRegistry` only. Loopback/test hosts must be explicitly registered. Cloud metadata and unapproved schemes/hosts/ports are denied as `NETWORK_POLICY_DENIED` before connect.

## Consequences

Kubernetes NetworkPolicy and Azure NSG remain future infrastructure controls, not Phase 9 implementation.
