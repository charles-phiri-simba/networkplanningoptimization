# Network Planning and Optimisation

First **SNIP** domain application: a local, **read-only** 5G planning copilot.

It ingests synthetic cell telemetry, projects KPI state, detects deterministic assurance conditions, persists an Assurance Case with operational evidence, returns a cited advisory assessment, can propose **governed** actions through a local Java MCP server, can run a **bounded Agent orchestration** that gathers evidence and proposes those same Phase 4 actions, can synchronize a **cell Digital Twin** so a hypothetical `txPower` change is simulated deterministically after approval, and can **import read-only Ericsson/Nokia fixture inventory** through a durable, lease-fenced runtime into the same canonical Site/gNB/Cell graph. It does **not** change the live network.

This repository is not the full Simba Network Intelligence Platform. Target-state product requirements are in [`docs/requirements/product-requirements.md`](docs/requirements/product-requirements.md). Phase 11 bounds are in [`docs/architecture/SNIP-PHASE-11-FIRST-REAL-VENDOR-CONNECTOR-PRODUCTION-READ-ONLY-INTEGRATION-ARCHITECTURE.md`](docs/architecture/SNIP-PHASE-11-FIRST-REAL-VENDOR-CONNECTOR-PRODUCTION-READ-ONLY-INTEGRATION-ARCHITECTURE.md). Phase 12 architecture is **accepted** in [`docs/architecture/SNIP-PHASE-12-CONTINUOUS-SYNCHRONIZATION-DRIFT-NETWORK-KNOWLEDGE-CONFIDENCE-ARCHITECTURE.md`](docs/architecture/SNIP-PHASE-12-CONTINUOUS-SYNCHRONIZATION-DRIFT-NETWORK-KNOWLEDGE-CONFIDENCE-ARCHITECTURE.md). Phase 12 implementation is **complete** and **architecturally accepted** (2026-08-28); simulator/contract is **verified** (not real Ericsson verification); real vendor continuous synchronization is **not yet verified**; production ENM transport is **not configured**; Phase 12 Git baseline is **not yet established**; Phase 13 has not started. Phase 7 reconciliation, Phase 8 import runtime, Phase 9 connector security, Phase 10 production secrets, Phase 11 read-only ENM, and Phase 12 continuous synchronization remain frozen.

## Prerequisites

- Java 17
- Maven 3.9+
- Go 1.22+ (simulator / `go test`)
- Docker (PostgreSQL, Kafka, and Testcontainers)
- Semantic path only: [Ollama](https://ollama.com) with models `nomic-embed-text` and `qwen2.5:7b`

No AWS account is required. Local Postgres credentials default to user/db `snip` and password `snip` (development only).

## Deterministic path (CI / default)

```bash
mvn test
go test ./...
docker compose up --build
```

Go tests from `simulator/`. Ordinary CI does **not** download Ollama models. Kafka is required only for Kafka integration tests (Testcontainers) and the optional telemetry Compose profile.

Or, with Postgres already on `127.0.0.1:5432`:

```bash
mvn spring-boot:run
```

- Health: `GET http://127.0.0.1:8080/health`
- UI: [http://127.0.0.1:8080/](http://127.0.0.1:8080/)
- Cell context: `GET http://127.0.0.1:8080/api/v1/cells/CELL-001/context`
- Telemetry: `GET http://127.0.0.1:8080/api/v1/cells/CELL-001/telemetry`
- Assurance: `GET http://127.0.0.1:8080/api/v1/cells/CELL-001/assurance`
- Actions: `GET http://127.0.0.1:8080/api/v1/actions`
- Recommend: `POST http://127.0.0.1:8080/api/v1/recommendations`

Uses **lexical** retrieval and the **stub** generator. Kafka consumption is **off** unless `SNIP_KAFKA_ENABLED=true`.

## Event telemetry (Compose profile `telemetry`)

```bash
docker compose --profile telemetry up postgres kafka api --build
# in another shell, with SNIP_KAFKA_ENABLED=true on the API:
docker compose --profile telemetry run --rm simulator
```

Host-run alternative (Kafka on `127.0.0.1:9092`, configurable via `SNIP_KAFKA_PORT`):

```bash
docker compose --profile telemetry up postgres kafka -d
set SNIP_KAFKA_ENABLED=true
mvn spring-boot:run
go run ./simulator/cmd/simulator -scenario high-bler-load -brokers 127.0.0.1:9092
```

Canonical Phase 3 question (after `high-bler-load` has been projected):

```bash
curl -s http://127.0.0.1:8080/api/v1/cells/CELL-001/assurance
# then:
curl -s http://127.0.0.1:8080/api/v1/assurance/cases/{caseId}/assessment
# then propose/execute a LOW remediation plan:
curl -s http://127.0.0.1:8080/api/v1/assurance/cases/{caseId}/actions -H "Content-Type: application/json" -d "{\"actionType\":\"GENERATE_REMEDIATION_PLAN\",\"capabilityId\":\"remediation.generate.v1\",\"targetType\":\"CELL\",\"targetId\":\"CELL-001\",\"rationale\":\"plan\",\"proposedBy\":\"demo-user\"}"
```

Simulation (`SIMULATE_CELL_PARAMETER_CHANGE`) is `MEDIUM` / `REQUIRE_APPROVAL` and is blocked until `POST /api/v1/actions/{id}/approve`. After approval, MCP `simulation.cell-parameter.v1` delegates to the Phase 6 Digital Twin model. Apply (`APPLY_CELL_PARAMETER_CHANGE`) is `HIGH` / `DENY` and never reaches MCP.

Canonical Phase 6 question (CELL-001 fixture `txPower=46 dBm`; equivalent to the architecture’s 40→38 example):

```bash
curl -s -X POST http://127.0.0.1:8080/api/v1/twins/cells/CELL-001/synchronize
# create a scenario 46 -> 44, then:
curl -s http://127.0.0.1:8080/api/v1/assurance/cases/{caseId}/actions -H "Content-Type: application/json" -d "{\"actionType\":\"SIMULATE_CELL_PARAMETER_CHANGE\",\"capabilityId\":\"simulation.cell-parameter.v1\",\"targetType\":\"CELL\",\"targetId\":\"CELL-001\",\"parameters\":{\"dryRun\":true,\"parameter\":\"txPower\",\"currentValue\":46,\"proposedValue\":44,\"scenarioId\":\"{scenarioId}\"},\"rationale\":\"twin dry-run\",\"proposedBy\":\"demo-user\"}"
# execute is 409 until:
curl -s -X POST http://127.0.0.1:8080/api/v1/actions/{actionId}/approve -H "Content-Type: application/json" -d "{\"decidedBy\":\"demo-approver\",\"comment\":\"synthetic dry-run only\"}"
curl -s -X POST http://127.0.0.1:8080/api/v1/actions/{actionId}/execute
```

There is no public `POST /simulate`. Twin-management APIs mutate SNIP Twin/scenario state only.

Canonical Phase 5 question (after a CELL-001 Assurance Case exists):

```bash
curl -s http://127.0.0.1:8080/api/v1/agent-runs -H "Content-Type: application/json" -d "{\"objective\":\"Investigate the DEGRADING_RADIO_QUALITY case for CELL-001 and recommend the next safe action.\",\"assuranceCaseId\":\"{caseId}\",\"initiatedBy\":\"demo-user\"}"
```

One Chief Orchestration Agent delegates to Context, Assurance, Knowledge, then Decision. Agents may propose a Phase 4 `ProposedAction` (`proposedBy=AGENT`). They cannot approve, override policy, or call MCP. CI uses the stub narrator; `local-ai` uses shared `qwen2.5:7b` via `AgentModelResolver`.

Canonical Phase 2 question:

```bash
curl -s http://127.0.0.1:8080/api/v1/recommendations -H "Content-Type: application/json" -d "{\"question\":\"What is happening on CELL-001, and what should I investigate?\",\"cellId\":\"CELL-001\"}"
```

## Semantic RAG path (local-ai)

Real embeddings, vector similarity, local LLM, PostgreSQL cell context, optional Kafka telemetry.

```bash
docker compose up postgres -d
ollama pull nomic-embed-text
ollama pull qwen2.5:7b
mvn spring-boot:run -Dspring-boot.run.profiles=local-ai
```

With telemetry:

```bash
docker compose --profile telemetry --profile local-ai up postgres kafka ollama api-local --build
```

Set `SNIP_KAFKA_ENABLED=true` so `api-local` consumes `snip.telemetry.cell-kpi.v1`. Then run the simulator and POST the CELL-001 question. Response fields include `retrievalMode=vector`, `contextEvidence`, `historyObservationCount`, `lastEventTime`, citations, and latency fields.

Host port is configurable (`SNIP_HOST_PORT`, default 8080). Database port: `SNIP_DB_PORT` (default 5432). Kafka host port: `SNIP_KAFKA_PORT` (default 9092).

## Domain APIs (read-only)

```text
GET /api/v1/sites
GET /api/v1/sites/{siteId}
GET /api/v1/gnbs
GET /api/v1/gnbs/{gnbId}
GET /api/v1/cells
GET /api/v1/cells/{cellId}
GET /api/v1/cells/{cellId}/kpis
GET /api/v1/cells/{cellId}/telemetry
GET /api/v1/cells/{cellId}/telemetry/{metric}
GET /api/v1/cells/{cellId}/neighbours
GET /api/v1/cells/{cellId}/context
GET /api/v1/assurance/cases
GET /api/v1/assurance/cases/{caseId}
GET /api/v1/cells/{cellId}/assurance
GET /api/v1/assurance/cases/{caseId}/assessment
POST /api/v1/assurance/cases/{caseId}/actions
GET /api/v1/actions
GET /api/v1/actions/{actionId}
POST /api/v1/actions/{actionId}/approve
POST /api/v1/actions/{actionId}/reject
POST /api/v1/actions/{actionId}/execute
POST /api/v1/agent-runs
GET /api/v1/agent-runs
GET /api/v1/agent-runs/{runId}
POST /api/v1/twins/cells/{cellId}/synchronize
GET /api/v1/twins/{twinId}
GET /api/v1/twins/{twinId}/versions
GET /api/v1/twins/{twinId}/versions/{version}
POST /api/v1/twins/{twinId}/scenarios
GET /api/v1/twins/{twinId}/scenarios
GET /api/v1/scenarios/{scenarioId}
GET /api/v1/simulations/{simulationId}
GET /api/v1/simulation-comparisons?left={id}&right={id}
POST /api/v1/integration/imports/ericsson
POST /api/v1/integration/imports/nokia
POST /api/v1/integration/imports/connectors/{connectorId}
GET /api/v1/integration/imports
GET /api/v1/integration/imports/{importId}
GET /api/v1/integration/imports/{importId}/checkpoints
GET /api/v1/integration/imports/{importId}/security-audit
GET /api/v1/integration/health
GET /api/v1/integration/connectors/security
GET /api/v1/integration/conflicts
GET /api/v1/integration/conflicts/{conflictId}
GET /api/v1/integration/rejections
POST /api/v1/recommendations
POST /mcp
```

Action APIs mutate SNIP governance state only. Agent-run APIs mutate SNIP orchestration state only. Twin synchronize/scenario APIs mutate SNIP Twin state only. Integration import APIs mutate SNIP integration/operational state only; they trigger configured local fixtures and never write an external network. Registered MCP capabilities: `remediation.generate.v1` (ALLOW) and `simulation.cell-parameter.v1` (approval + `dryRun=true`, delegates to the Digital Twin model). There is no live apply capability and no vendor MCP tool. Agents never invoke MCP directly.

Canonical detector: `DEGRADING_RADIO_QUALITY` when `BLER_DL >= 0.08` (ratio) and BLER trend is `INCREASING`. Severity/confidence are deterministic (see ADR 016). Repeated detections update the active case.

Demo dataset: `SITE-001` / `GNB-001` / `CELL-001` (elevated DL BLER) plus healthier `CELL-002` and comparison `CELL-003`. Seed rows are synthetic (`DEMO_SEED`). Phase 7 normal fixtures add isolated `SITE-E001`/`CELL-E001` (Ericsson) and `SITE-N001`/`CELL-N001` (Nokia) without mutating `CELL-001`. Schema is Flyway-managed (`src/main/resources/db/migration/`).

Canonical Phase 8 question (local fixtures only; no ENM/NetAct). A second identical successful snapshot is `REPLAY` with zero canonical mutation. Same-scope contention returns HTTP 409.

```bash
curl -s -X POST http://127.0.0.1:8080/api/v1/integration/imports/ericsson -H "Content-Type: application/json" -d "{\"fixtureKind\":\"NORMAL\"}"
curl -s -X POST http://127.0.0.1:8080/api/v1/integration/imports/ericsson -H "Content-Type: application/json" -d "{\"fixtureKind\":\"NORMAL\"}"
curl -s http://127.0.0.1:8080/api/v1/integration/health
curl -s http://127.0.0.1:8080/api/v1/integration/imports
curl -s http://127.0.0.1:8080/api/v1/integration/connectors/security
```

Canonical Phase 7 question (local fixtures only; no ENM/NetAct):

```bash
curl -s -X POST http://127.0.0.1:8080/api/v1/integration/imports/ericsson -H "Content-Type: application/json" -d "{\"fixtureKind\":\"NORMAL\"}"
curl -s -X POST http://127.0.0.1:8080/api/v1/integration/imports/nokia -H "Content-Type: application/json" -d "{\"fixtureKind\":\"NORMAL\"}"
curl -s http://127.0.0.1:8080/api/v1/cells/CELL-E001
curl -s http://127.0.0.1:8080/api/v1/integration/conflicts
```

Phase 11 simulator ENM import (not real ENM; requires explicit permission; default CI does not call a vendor):

```bash
curl -s -X POST http://127.0.0.1:8080/api/v1/integration/imports/connectors/ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER -H "X-SNIP-VENDOR-IMPORT-PERMISSION: TRIGGER_VENDOR_IMPORT"
```

## What this phase does not include

Live network writes, real Ericsson ENM production connectivity, Nokia NetAct connectivity, production vendor credentials, OSS/NMS/EMS write integration, vendor REST/SFTP/SNMP/NETCONF, vendor telemetry adapters, automatic Twin synchronization, automatic conflict resolution, auto-remediation, Agent Factory, dynamic Agent creation, long-running autonomous Agents, direct Agent-to-MCP execution, remote third-party MCP, production RF simulation, electricalTilt simulation, automatic optimization, Kafka-triggered Twin synchronization, Schema Registry, Avro, Protobuf, Flink, Spark, Kafka Streams, a dedicated time-series DB, RL, import queues, automatic retry loops, cancellation APIs, record-level resume, OAuth vendor token flow, Terraform-managed secret values, Phase 13.

## License

Apache-2.0. See [`LICENSE`](LICENSE).

Sample corpus provenance: [`testdata/PROVENANCE.md`](testdata/PROVENANCE.md).
