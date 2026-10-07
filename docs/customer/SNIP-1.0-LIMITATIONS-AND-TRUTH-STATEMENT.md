# SNIP 1.0 — Limitations and truth statement

This demonstration uses **controlled synthetic data** (`DEMO_SEED`). It is not a customer network, not a production dataset, and not RF planning evidence.

## Digital Twin

SNIP 1.0 provides a **cell Digital Twin** only: a versioned, freshness-aware (CURRENT / STALE / EXPIRED) cell-scoped snapshot used to admit synthetic cell-local `txPower` simulation.

It is **not** a site Digital Twin, **not** a network Digital Twin, and **not** a coupled RF Digital Twin.

## Simulation

| Field | Truth |
|-------|--------|
| Model | `snip.synthetic.cell-parameter.v1` version 1.0 |
| Parameter | `txPower` only, 20–50 dBm |
| Confidence | **LOW** |
| Synthetic | true |
| Vendor calibrated | no |
| Cross-cell / coupled RF | no |
| Interference / coverage / handover / load | not modelled |

## Assurance and optimization

Assurance detection is **rule-based**. Contributors and checks are **deterministic**. Optimization candidate generation and ranking are **deterministic**. Planning evaluation is **synthetic simulation**.

The LLM does not select `txPower`, does not control the network, and is not the Digital Twin.

## Ask SNIP

Default demonstration generation is a **stub** with **lexical** retrieval. It is decision support, not an approved change and not a live-network action. Optional local LLM configuration is not part of the customer-demo startup.

Agents are not part of the normal customer journey.

## Execution

Sandbox execution is **simulator only**. Production change execution is **not authorized**. Closed-loop optimization is **not authorized**. The Four-Way State Real network value remains **Unchanged**.

Committed default: `snip.production-change.enabled=false`. The demo profile enables sandbox change-execution only.

## Identity

Demo login is frontend-only persona state. Demo authorization uses request headers. This is **not** production IAM and **not** segregation of duties.

## Vendors

Multi-vendor **abstraction** exists. Live Ericsson writes, live Nokia writes, and Huawei support are not in this demonstration. Production ENM transport is not configured.
