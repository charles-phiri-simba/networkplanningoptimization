# ADR 024 — Local Java MCP and future polyglot MCP

## Status

Accepted for Phase 4.

## Context

Architecture requires a real MCP client/server boundary without remote MCP or a Spring AI 2.0 rewrite.

## Decision

The first MCP server is local Java Spring MVC at `POST /mcp` (JSON-RPC 2.0: `initialize`, `tools/list`, `tools/call`). The gateway is an HTTP client to that endpoint (loopback). Python/Go MCP servers and remote MCP are deferred. Spring Boot 3.3 / Spring AI 1.0 are preserved.

## Consequences

CI exercises the HTTP protocol boundary without Ollama.
