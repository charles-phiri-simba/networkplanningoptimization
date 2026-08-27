# ADR 076 — Terraform/IaC Secret-State Prohibition

## Status

Accepted for Phase 10.

## Context

Terraform state is not an acceptable store for vendor credentials or private keys.

## Decision

IaC may create identity, federation, and GET-only RBAC. It must not set Key Vault secret values, PKCS12 material, or private keys. Synthetic E2E secrets are bootstrapped outside Terraform. Git must not contain those values.

## Consequences

`az keyvault secret set` (or equivalent protected bootstrap) is required before Azure E2E. Terraform plan/state audit is part of Phase 10 completion evidence.
