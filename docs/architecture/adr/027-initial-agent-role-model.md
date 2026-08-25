# ADR 027 — Initial Agent role model

## Status

Accepted for Phase 5.

## Context

Phase 5 needs bounded specialist reasoning without an Agent Factory.

## Decision

Exactly five in-code Agents: `chief-orchestrator`, `knowledge-agent`, `context-agent`, `assurance-agent`, `decision-agent`. No dynamic creation.

## Consequences

A larger Agent ecosystem is deferred. Roles are specialised by prompt, permissions, and allowed services, not by extra physical models.
