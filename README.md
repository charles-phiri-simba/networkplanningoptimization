# Network Planning and Optimisation

First **SNIP** domain application: a local, **read-only** 5G planning copilot.

It ingests synthetic cell telemetry, projects KPI state, detects deterministic assurance conditions, persists an Assurance Case with operational evidence, and returns a cited, advisory decision assessment. It does **not** change the live network.

This repository is not the full Simba Network Intelligence Platform. Target-state product requirements are in [`docs/requirements/product-requirements.md`](docs/requirements/product-requirements.md). Phase 3 bounds are in [`SNIP-PHASE-3-ASSURANCE-DECISION-INTELLIGENCE-ARCHITECTURE.md`](SNIP-PHASE-3-ASSURANCE-DECISION-INTELLIGENCE-ARCHITECTURE.md) and [`SNIP-PHASE-3-ASSURANCE-DECISION-INTELLIGENCE-SPECIFICATION.md`](SNIP-PHASE-3-ASSURANCE-DECISION-INTELLIGENCE-SPECIFICATION.md).

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
```

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
POST /api/v1/recommendations
```

Canonical detector: `DEGRADING_RADIO_QUALITY` when `BLER_DL >= 0.08` (ratio) and BLER trend is `INCREASING`. Severity/confidence are deterministic (see ADR 016). Repeated detections update the active case.

Demo dataset: `SITE-001` / `GNB-001` / `CELL-001` (elevated DL BLER) plus healthier `CELL-002` and comparison `CELL-003`. Seed rows are synthetic (`DEMO_SEED`). Schema is Flyway-managed (`src/main/resources/db/migration/`).

## What this phase does not include

MCP, autonomous agents, live OSS/NMS/EMS, network writes, incident-management / ITSM, Schema Registry, Avro, Protobuf, Flink, Spark, Kafka Streams, a dedicated time-series DB, EKS/Kubernetes, RL, ML anomaly detection, Phase 4.

## License

Apache-2.0. See [`LICENSE`](LICENSE).

Sample corpus provenance: [`testdata/PROVENANCE.md`](testdata/PROVENANCE.md).
