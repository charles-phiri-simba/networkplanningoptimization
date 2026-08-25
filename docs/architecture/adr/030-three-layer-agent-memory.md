# ADR 030 — Three-layer Agent memory

## Status

Accepted for Phase 5.

## Context

Agents need working context without becoming a second system of record.

## Decision

Run Memory is ephemeral. Case Memory is a concise persisted summary tied to an Assurance Case and run. Enterprise Memory remains PostgreSQL, RAG, Assurance, and Action stores. Agent memory is never authoritative over those stores.

## Consequences

No persistent conversational memory. No duplication of KPI/assurance/action truth.
