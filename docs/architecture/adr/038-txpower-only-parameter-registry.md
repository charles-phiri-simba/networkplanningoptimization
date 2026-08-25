# ADR 038 — txPower-only simulatable parameter registry

## Status

Accepted for Phase 6.

## Context

Arbitrary parameter simulation would imply an RF model SNIP does not have.

## Decision

An in-code whitelist exposes exactly `txPower` (dBm, 20–50, CELL, enabled). `electricalTilt`, `pci`, unknown, disabled, wrong-scope, and out-of-range values are rejected. Scenario `currentValue` must match the selected Twin baseline.

## Consequences

Phase 6 cannot search the parameter space. Additional parameters require a later architecture decision.
