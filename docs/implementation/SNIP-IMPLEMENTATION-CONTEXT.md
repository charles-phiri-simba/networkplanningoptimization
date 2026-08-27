# SNIP Implementation Context Pack

## 0. Current repository binding

This file is the architecture library. The binding below is **authoritative for this repository**. It does not rewrite the target platform; it bounds what may be implemented now.

**Platform:** SNIP — Simba Network Intelligence Platform  
**This repository:** first SNIP **domain application** (Network Planning & Optimisation), not the entire enterprise platform.  
**Phase 0:** complete — see `SNIP-PHASE-0-DISCOVERY-REPORT.md`  
**Current authorised phase:** Phase 10 is **architecturally accepted and frozen**. Phase 11 is **not** authorised. Git baseline is pending explicit authorization.  
**Architecture:** `docs/architecture/SNIP-PHASE-10-PRODUCTION-SECRET-WORKLOAD-IDENTITY-CONNECTOR-RUNTIME-SECURITY-ARCHITECTURE.md`  
**Coding contract:** `docs/implementation/SNIP-PHASE-10-PRODUCTION-SECRET-WORKLOAD-IDENTITY-CONNECTOR-RUNTIME-SECURITY-SPECIFICATION.md`  
**Phase 9 baseline:** `4dfd8f0ec7d254ea292ab909b709eee3e599ef45` (architecturally accepted — see `SNIP-PHASE-9-COMPLETION-REPORT.md`)  
**Phase 9:** architecturally accepted — frozen.  
**Phase 10:** architecturally accepted — frozen (Git baseline not yet established).  
**Status:** `SNIP-IMPLEMENTATION-STATUS.md`

```text
Phase 0     Repository Discovery                         (done)
    │
    ▼
Phase 1A    Read-Only Knowledge Intelligence Slice       (done)
    │
    ▼
Phase 1A.1  Semantic RAG Validation                      (done)
    │
    ▼
Phase 1B    Core Network Domain & Context Intelligence   (done)
    │
    ▼
Phase 2     Context + Event Intelligence                 (done)
    │
    ▼
Phase 3     Assurance & Decision Intelligence            (done — frozen)
    │
    ▼
Phase 4     Governed Action Intelligence & MCP           (done — frozen)
    │
    ▼
Phase 5     Agentic Orchestration & Controlled Autonomy  (done — frozen)
    │
    ▼
Phase 6     Digital Twin & Simulation Intelligence       (done — frozen)
    │
    ▼
Phase 7     Multi-Vendor Network Integration             (done — frozen)
    │
    ▼
Phase 8     Integration Runtime Hardening                (done — frozen)
    │
    ▼
Phase 9     Integration Security / Connector Identity    (done — frozen)
    │
    ▼
Phase 10    Production Secret / Workload Identity        (done — frozen)
    │
    ▼
Phase 11    (closed)
```

This repository’s Phase 10 is a **production-grade secret and workload-identity proof** around the frozen Phase 9 MOCK_SECURE connector envelope and frozen Phase 8 import runtime: real Azure Key Vault via AKS Workload Identity, synthetic connector secrets only, deployment-level egress, and multi-instance lease-before-secret resolution. It does not connect to real Ericsson ENM or Nokia NetAct. Phase 9 remains authoritative for connector identity, fixed binding, TLS/mTLS, read-only authorization, application network policy, redaction, and security audit. Phase 8 remains authoritative for NEW/RETRY/REPLAY, PostgreSQL source-scope leases with fencing, checkpoints, atomic canonical commit, and the import watchdog. Phase 7 remains authoritative for vendor adapters, the SNIP-owned canonical model, and deterministic reconciliation. Vendor credentials are infrastructure security material and never domain data. Vendor-specific DTOs must not leak into Assurance, Decision, Agents, Twin, RAG, or governed actions. Import may make an existing Twin STALE and must not call Twin synchronization. Replay must not mutate canonical state. **Agents must not invoke MCP, approve actions, override policy, mutate Twin baselines, write the live network, or access credentials, Azure tokens, or Key Vault. The LLM must not produce authoritative numeric predictions or reconciliation outcomes. Do not implement Agent Factory, remote MCP, live network writes, real ENM/NetAct, vendor writes, scheduled synchronization, or Phase 11.**

Cursor must not add Phase 4, Phase 5, Phase 6, Phase 7, Phase 8, Phase 9, or Phase 10 functionality and must not start Phase 11. Do not redesign Phase 1A/1A.1 semantic RAG, the Phase 1B domain model, the Phase 2 Kafka/telemetry slice, the frozen Phase 3 assurance slice, the frozen Phase 4 governed-action slice, the frozen Phase 5 agent slice, the frozen Phase 6 Twin/simulation slice, the frozen Phase 7 reconciliation rules, the frozen Phase 8 import runtime, the frozen Phase 9 connector security envelope, or the frozen Phase 10 production secret / Workload Identity envelope. Non-interruptible per-Agent timeout remains accepted Phase 5 technical debt. Failed Twin simulation attempts are not persisted as `SimulationRun` records; that remains accepted Phase 6 technical debt. Do not add import queues, automatic retries, schedulers, record-level resume, cancellation APIs, raw snapshot archival, vendor telemetry, vendor writes, OAuth vendor flows, or new MCP tools. PostgreSQL lease + fencing remains the accepted coordination mechanism and canonical commit authority. Application-level egress policy remains canonical; Kubernetes NetworkPolicy is defense in depth. Connector/trust/authorization/network profiles remain static/in-code. Secrets, private keys, and Azure tokens must never be committed, logged, or returned on APIs. Terraform must not store connector secret values. Default CI must remain Azure-independent.

---

## 1. Platform Identity

**SNIP — Simba Network Intelligence Platform**

SNIP is an enterprise intelligence platform providing a common foundation for enterprise data, operational intelligence, knowledge management, AI and Agentic capabilities, enterprise actions, digital twins, event-driven integration, continuous learning, governance and operational automation.

SNIP is **not simply an AI application**. It is an enterprise platform upon which conventional software services, data products, AI services, AI Agents and enterprise capabilities operate.

```text
Traditional Enterprise Software
            |
            +-- APIs
            +-- Events
            +-- Services
            +-- Data
            |
            v
       SNIP PLATFORM
            |
       +----+----+
       |         |
       v         v
      AI/ML    Agents
       |         |
       +----+----+
            |
            v
   Enterprise Intelligence
```

## 2. Architecture Library

SNIP is governed by these architecture volumes:

- Volume 1 — Enterprise Solution Architecture
- Volume 2 — Business Architecture
- Volume 3 — Logical Architecture
- Volume 4 — Software Architecture
- Volume 5 — AI & Agentic Architecture
- Volume 6 — OSS Architecture
- Volume 7 — BSS Architecture
- Volume 8 — Data Architecture
- Volume 9 — Integration Architecture
- Volume 10 — Infrastructure Architecture
- Volume 11 — Security Architecture
- Volume 12 — DevSecOps Architecture
- Volume 13 — Operations Architecture
- Volume 14 — User Experience Architecture
- Volume 15 — Implementation & Delivery Guide

These volumes form an architectural hierarchy. Implementation must respect their relationships.

## 3. Implementation Philosophy

> **Design enough architecture to make the right decisions — then build, validate, learn and refine.**

Architecture and implementation are iterative:

```text
Architecture -> Implementation -> Validation
      ^                              |
      |                              v
      +------ Operational Feedback --+
```

The architecture is the target direction, not a reason to postpone implementation.

## 4. Central Architectural Principle

> **Centralise what must be consistent. Federate what requires domain expertise. Govern what creates enterprise risk. Automate everything that can safely be automated.**

## 5. Core Principles

- Platform over point solution.
- API and event driven.
- Domain ownership.
- Enterprise standards.
- Cloud agnostic.
- Kubernetes first.
- Security by design.
- Observable by design.
- Testable by design.
- Enterprise AI must be grounded in enterprise knowledge and context rather than relying solely on an LLM's internal knowledge.

## 6. Target Enterprise Architecture

```text
                    ENTERPRISE
                        |
       +----------------+----------------+
       |                |                |
       v                v                v
     DATA          INTEGRATION        SECURITY
       |                |                |
       +----------------+----------------+
                        |
                        v
                 SNIP PLATFORM
                        |
       +----------------+----------------+
       |                |                |
       v                v                v
 APPLICATIONS         AI/ML            AGENTS
       |                |                |
       +----------------+----------------+
                        |
                        v
              ENTERPRISE INTELLIGENCE
                        |
                        v
                 ENTERPRISE ACTION
```

## 7. Core SNIP Platform

Major common capabilities include:

- Identity and Security
- API and Integration
- Event Platform
- Data Platform
- Knowledge Platform
- AI Platform
- Agent Runtime
- MCP Platform
- Observability
- Governance
- Developer Platform
- Operations

## 8. Technology Landscape

### Backend
- Java
- Quarkus
- Go

### AI / ML
- Python for training, experimentation, data science, reinforcement learning, evaluation and AI/ML pipelines.
- Model serving and AI infrastructure as appropriate.
- Vector embedding technologies.

### Data
- PostgreSQL
- Vector storage
- Object storage
- Knowledge graph technologies where justified
- Time-series technologies where justified
- Enterprise data products

### Integration
- REST
- Kafka
- Asynchronous messaging
- MCP

### Infrastructure
- Containers
- Kubernetes
- On-premises infrastructure
- Public/private cloud
- Hybrid infrastructure
- GPU infrastructure where required

Azure may be used as a cloud example, but SNIP must not become Azure-dependent without an explicit architectural decision.

## 9. Enterprise Data Architecture

Enterprise information is distributed and ownership remains federated.

```text
Domain A Data --+
Domain B Data --+--> Enterprise Data Products
Domain C Data --+            |
                             v
                       Knowledge Layer
                             |
                             v
                            AI
```

Do not create uncontrolled copies of enterprise data merely to support AI.

## 10. Enterprise Knowledge Intelligence

Knowledge Intelligence provides grounded enterprise knowledge.

```text
Enterprise Sources
       |
       v
Data Products
       |
       v
Knowledge Processing
       |
   +---+---+
   |       |
   v       v
 Vector   Graph
 Store    Knowledge
   |       |
   +---+---+
       |
       v
      RAG
       |
       v
     Agent
```

RAG is not simply documents in a vector database. Consider semantic chunking, metadata, access control, source authority, freshness, embeddings, hybrid retrieval, semantic relationships, knowledge graphs where appropriate and provenance.

## 11. Enterprise Context Intelligence

Context Intelligence provides current enterprise state, including entities, relationships, operational state, topology, temporal state, digital twins, events, user context and process context.

```text
Knowledge + Context + Current State
                |
                v
          Agent Reasoning
```

## 12. Enterprise Action Intelligence

AI Agents interact with enterprise capabilities through governed mechanisms such as MCP.

```text
                    AGENT
                      |
                      v
                 MCP Gateway
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
       Local MCP   Remote MCP   Enterprise
       Server       Server      MCP Server
          |           |           |
          v           v           v
       Service      API       Enterprise
                              Capability
```

MCP is treated as an enterprise action interface, not merely an AI integration technology.

## 13. MCP Architecture

SNIP may use:

- Local MCP Servers
- Remote MCP Servers
- Enterprise MCP Servers
- MCP Gateway
- MCP Registry

MCP capabilities require authentication, authorisation, capability ownership, auditing, observability, lifecycle management and policy enforcement. High-risk actions may require human approval.

## 14. AI Agent Architecture

An Agent is not simply an LLM wrapper.

```text
Agent
 |
 +-- Objective
 +-- Instructions
 +-- Model
 +-- Memory
 +-- RAG
 +-- Context
 +-- Tools
 +-- MCP
 +-- Policies
 +-- Evaluation
 +-- Observability
 +-- Learning
```

Every production Agent requires an owner.

## 15. AI Runtime

The Agent Runtime provides common capabilities such as orchestration, workflow execution, model routing, Agent state, memory, tool invocation, MCP integration, policy enforcement, retries, timeouts and execution tracing.

Agents should not independently implement their own orchestration infrastructure unless explicitly justified.

## 16. Enterprise AI Governance

AI governance includes AI policies, risk classification, autonomy controls, model governance, Agent governance, data policies, human oversight, evaluation requirements and auditability.

A Governance and Policy AI Agent may assist with policy interpretation and governance activities but does not replace accountable human governance.

## 17. AI Security

SNIP follows Zero Trust principles.

```text
User -> Agent -> Model -> RAG -> MCP -> Enterprise Capability
```

Controls include identity, authentication, authorisation, RBAC/ABAC, secrets, encryption, audit and policy enforcement.

## 18. AI Observability

Capture, where appropriate:

- Agent execution traces
- Model latency
- Model usage
- Token consumption
- RAG retrieval quality
- MCP invocation
- Tool failures
- Policy decisions
- Agent outcomes
- Evaluation results

```text
Agent
 |
 +-- Logs
 +-- Metrics
 +-- Traces
 +-- Evaluations
 +-- Outcomes
        |
        v
  AI Observability
```

## 19. Enterprise Learning Intelligence

SNIP is intended to improve over time using operational feedback, evaluation results, user feedback, historical enterprise data, on-premises data, supervised learning, reinforcement learning and model fine-tuning where justified.

```text
Data -> Training -> Evaluation -> Deployment -> Production
  ^                                              |
  +---------------- Feedback -------------------+
```

Learning remains governed and must not automatically change production behaviour without appropriate controls.

## 20. Agent Factory

SNIP should eventually provide a self-service Agent Factory.

```text
Developer
   |
   v
Agent Factory
   |
   +-- Templates
   +-- SDK
   +-- RAG
   +-- MCP
   +-- Testing
   +-- Evaluation
   +-- CI/CD
   +-- Governance
          |
          v
        Agent
          |
          v
     Certification
          |
          v
      Production
```

## 21. Federated AI Operating Model

> **Centralise the platform. Federate intelligence. Govern the enterprise.**

Domain teams own business outcomes. Platform teams own shared capabilities. Enterprise Architecture owns architectural coherence. Security owns security controls. Governance owns AI policy. Operations owns production reliability.

## 22. Implementation Strategy

```text
Phase 0 -> Context & Architecture
    |
Phase 1 -> Foundation
    |
Phase 2 -> Core Platform
    |
Phase 3 -> First Vertical Slice
    |
Phase 4 -> AI / RAG / MCP Platform
    |
Phase 5 -> Agent Factory & Learning
    |
Phase 6 -> Enterprise Scale
```

The full architecture provides context for all phases, but only the current phase should be actively implemented unless explicitly authorised.

**This repository:** execute the Phase 1A.1 sequence in §0, not the generic Phase 1 foundation list in §23.

## 23. First Implementation Target

Expected foundation capabilities:

```text
SNIP FOUNDATION
 |
 +-- Repository Structure
 +-- Build System
 +-- Configuration
 +-- Containerisation
 +-- Kubernetes
 +-- PostgreSQL
 +-- Kafka
 +-- API Foundation
 +-- Identity / Security Foundation
 +-- Observability Foundation
 +-- CI/CD Foundation
```

## 24. First Vertical Slice

```text
Real Business Problem
        |
        v
      Agent
        |
   +----+----+
   |    |    |
   v    v    v
  RAG Context MCP
   |    |    |
   +----+----+
        |
        v
     Decision
        |
        v
 Human / Autonomous Action
        |
        v
     Outcome
        |
        v
     Feedback
```

The first vertical slice should be selected based on business value and architectural learning potential.

## 25. Architecture-to-Code Relationship

```text
Architecture Requirement
          |
          v
Logical Capability
          |
          v
Software Component
          |
          v
Implementation
          |
          v
Test
          |
          v
Deployment
```

## 26. Repository Principles

Cursor must inspect the existing repository before changing anything. Determine existing modules, services, APIs, entities, configuration, databases, tests, deployment and CI/CD. Do not recreate existing capabilities without understanding them.

## 27. Architectural Decision Records

Document significant decisions such as technology selection, database selection, API style, event model, MCP topology, model selection, RAG strategy, security architecture and deployment architecture using lightweight ADRs.

## 28. Quality Requirements

Meaningful implementations should have appropriate unit, integration, API, contract, security, performance and resilience testing.

AI components additionally require evaluation datasets, regression evaluation, hallucination assessment, retrieval evaluation, Agent behaviour testing and tool/MCP testing where appropriate.

## 29. Definition of Done

```text
Code -> Tests -> Security -> Observability -> Documentation
     -> Deployment -> Validation -> Architecture Traceability
```

A capability is not complete merely because it compiles.

## 30. Cursor's Role

Cursor operates as a senior implementation team working under Enterprise Architecture.

It should understand the architecture, inspect the repository, implement incrementally, identify contradictions, propose alternatives, explain important decisions, maintain quality, test changes and preserve architectural traceability.

Cursor must not silently redefine the architecture.

## 31. Full Context, Bounded Execution

> **Full architectural context does not mean full implementation scope.**

Cursor should understand the complete target architecture but implement only the currently authorised phase.

## 32. Phase 0 Discovery

Before writing production code, Cursor should inspect the repository and architecture, map existing capabilities to the target architecture, identify gaps, identify contradictions and produce a proposed Phase 1 implementation plan.

Phase 0 should not modify the codebase unless explicitly requested.

**This repository:** Phase 0 and Phase 1A are complete. Production code is authorised only for Phase 1A.1 as specified in `SNIP-PHASE-1A.1-SEMANTIC-RAG-VALIDATION.md`.
