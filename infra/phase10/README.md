# Phase 10 infrastructure (no secret values)

This module represents AKS Workload Identity, a dedicated connector UAMI, Key Vault **GET** RBAC, and the Kubernetes service-account binding.

It does **not** create, update, or store connector usernames, passwords, tokens, PKCS12 material, or private keys.

Synthetic E2E secrets are provisioned outside Terraform. See `SYNTHETIC-SECRET-BOOTSTRAP.md`.

## Private Endpoint / DNS

This repository does not own the platform VNet, Private DNS zone, or Key Vault firewall. Private Endpoint + private DNS + disabled public network access is the defense-in-depth **target**. If the platform team has not delegated those resources, do not invent a public-Key-Vault workaround that weakens production. Record the gap in the Phase 10 completion report.
