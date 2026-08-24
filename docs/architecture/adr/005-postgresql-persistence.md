# ADR 005 — PostgreSQL persistence

## Status

Accepted for Phase 1B.

## Context

Structured network relationships and constraints need a relational store. Phase 1A file JSON is not enough for Site/gNB/Cell integrity.

## Decision

Use PostgreSQL as the only relational database, accessed through Spring Data JPA.

Flyway owns schema lifecycle (`V1` domain tables, `V2` deterministic demo seed). Hibernate is set to `validate` only. Runtime APIs are read-only; writes are limited to migrations, demo seed, and tests.

Connection URL, username and password are externalised (`SPRING_DATASOURCE_*`). Compose uses a local development password, not a production secret.

Tests use Testcontainers PostgreSQL, not H2.

## Consequences

The application requires PostgreSQL to start. CI must have Docker. This is accepted so integration tests exercise the real database.
