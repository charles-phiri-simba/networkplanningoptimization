# Product requirements (SRS)

Extracted from the original `README.md` during Phase 1A. These statements are **design intent**, not as-built documentation. Implementation is bounded by `docs/implementation/SNIP-PHASE-1A-IMPLEMENTATION-SPEC.md`.

**Project:** Intelligent 5G Network Planning & Optimization Platform

## 1. Executive summary and core objective

The objective is to build an enterprise-grade Network Planning and Optimization application. The system leverages AI agents to analyze live telemetry, simulate coverage, query dense 3GPP technical specifications via Retrieval-Augmented Generation (RAG), and allow telecom engineers to optimize 5G Next-Generation NodeBs (gNBs) through a unified cross-platform user interface.

## 2. High-level system architecture (target end-state)

```text
[ Ionic Cross-Platform App ] (Mobile/Web)
             |
             v (HTTPS / WSS)
   +--------------------+
   | AWS ALB (Load Bal) |
   +--------------------+
             |
             v (mTLS / HTTP2)
   +--------------------+
   | Kong API Gateway   | <---> [ Identity Provider (Keycloak/Cognito) ]
   +--------------------+                  |
             |                             v
             |                     [ ALICE API / ABAC Engine ]
             +-----------------------------+
             | (Routed with OAuth2 Tokens) |
             v                             v
+------------------------+    +--------------------------+
| Go Telemetry Ingest    |    | Java Network Planner     |
| (High-Throughput Core) |    | (Spring AI Engine)       |
+------------------------+    +--------------------------+
             |                             |
             v                             v
   [ Amazon MSK (Kafka) ]     [ OpenSearch / Vector DB ] <--- [ 3GPP Specs ]
```

- **Frontend:** Ionic (Angular or React) as PWA and native binaries.
- **Ingress:** AWS ALB to Kong inside the cluster.
- **Security:** OAuth 2.0 / OIDC via Keycloak or Cognito; ALICE ABAC for engineering roles and territorial clearance.
- **Go service:** telemetry ingestion from gNBs into Amazon MSK (Kafka).
- **Java service:** Spring Boot / Spring AI for optimization models and RAG.
- **Knowledge:** Amazon OpenSearch Serverless for 3GPP documentation chunks.
- **Deploy:** AWS EKS, containerized workloads.

Phase 1A does **not** implement this stack.

## 3. Functional requirements

### FR-1 Automated telemetry ingestion (Go)

Ingest real-time 5G KPIs (BLER, call drop rates, latency) from 5G Core (AMF/UDM) and radio nodes via Protobuf into Kafka.

### FR-2 AI-driven optimization and simulation (Java Spring AI)

Evaluate topology and suggest physical/logical adjustments (antenna tilt, beamforming, channel bandwidth) via an enterprise LLM with localized metrics as prompts.

### FR-3 3GPP specification RAG copilot (Java Spring AI)

Natural-language queries against official 3GPP specifications: embed, vector lookup, extract sections, generate a compliant resolution step.

### FR-4 Role and attribute authorization (ALICE)

Operational modifications must pass policies on user tier, region, and senior sign-off. Kong passes JWTs to ALICE before the Java core.

## 4. Non-functional requirements

### NFR-1 Scalability and performance

- Go ingest: up to 10,000 requests per second per node; ingest-to-Kafka latency &lt; 15 ms.
- Ionic regional topology maps: &lt; 2 s initial load on typical 4G/5G.

### NFR-2 Cloud-native resiliency

- Multi-stage Docker images (&lt; 150 MB Go, &lt; 350 MB Java JRE).
- EKS multi-AZ; HPA at 70% CPU or memory.

### NFR-3 Security and auditability

- mTLS from ALB to Kong and between pods.
- Immutable ALICE policy-outcome audit stream.

## 5. Interface mapping (target)

| Inbound trigger | Entry | Security | Handling service | Store |
|-----------------|-------|----------|------------------|-------|
| Real-time KPI streams | ALB → Kong | OAuth2 | Go microservice | Amazon MSK |
| Engineering chat / RAG | ALB → Kong | Token + ALICE | Java Spring AI | OpenSearch |
| Topology overrides | ALB → Kong | ALICE admin sign-off | Java microservice | Relational DB / GitOps |

## 6. Later billing preview

A later billing microservice may consume Kafka (AI tokens, gNB/telemetry volume). Not in Phase 1A.
