# ADR 059 — Connector Identity and Fixed Security Binding

## Status

Accepted for Phase 9.

## Context

Phase 8 proved reliable fixture import. Real vendor connectivity must not let callers choose secrets, URLs, or trust material.

## Decision

Each connector has a fixed identity per source system + environment + purpose, with immutable bindings to endpointRef, credentialRef, trust profile, authorization profile, and network policy. Public import requests select a registered connector id only.

## Consequences

Arbitrary credential or endpoint substitution is impossible through the API. Dynamic connector admin CRUD remains out of scope.
