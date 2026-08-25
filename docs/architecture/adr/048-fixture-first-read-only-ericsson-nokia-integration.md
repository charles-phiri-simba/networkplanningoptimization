# ADR 048 — Fixture-First Read-Only Ericsson/Nokia Integration

## Status

Accepted for Phase 7.

## Context

Production ENM/NetAct connectivity would introduce credentials, write risk, and CI coupling.

## Decision

Phase 7 uses local classpath JSON fixtures only. Source systems `ERICSSON_FIXTURE` / `NOKIA_FIXTURE` are `mode=FIXTURE`, `readOnly=true`, with no credentials. POST import APIs trigger configured fixture kinds only and do not accept paths, URLs, credentials, or vendor write instructions.

## Consequences

Real ENM/NetAct, REST/SFTP/SNMP/NETCONF/gNMI/Bulk CM remain out of scope. CI needs no vendor network.
