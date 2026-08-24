# Network Planning and Optimisation

First **SNIP** domain application: a local, **read-only** 5G planning copilot.

It resolves structured cell context from PostgreSQL, retrieves cited sample planning notes, and returns a grounded recommendation. It does **not** change the live network.

This repository is not the full Simba Network Intelligence Platform. Target-state product requirements are in [`docs/requirements/product-requirements.md`](docs/requirements/product-requirements.md). Phase 1B bounds are in [`SNIP-PHASE-1B-CORE-NETWORK-CONTEXT-SPECIFICATION.md`](SNIP-PHASE-1B-CORE-NETWORK-CONTEXT-SPECIFICATION.md).

## Prerequisites

- Java 17
- Maven 3.9+
- Docker (PostgreSQL for the app and for Testcontainers in `mvn test`)
- Semantic path only: [Ollama](https://ollama.com) with models `nomic-embed-text` and `qwen2.5:7b`

No AWS account is required. Local Postgres credentials default to user/db `snip` and password `snip` (development only).

## Deterministic path (CI / default)

```bash
mvn test
docker compose up --build
```

Or, with Postgres already on `127.0.0.1:5432`:

```bash
mvn spring-boot:run
```

- Health: `GET http://127.0.0.1:8080/health`
- UI: [http://127.0.0.1:8080/](http://127.0.0.1:8080/)
- Cell context: `GET http://127.0.0.1:8080/api/v1/cells/CELL-001/context`
- Recommend: `POST http://127.0.0.1:8080/api/v1/recommendations`

Uses **lexical** retrieval and the **stub** generator. No model download. Tests start PostgreSQL via Testcontainers.

Canonical Phase 1B question:

```bash
curl -s http://127.0.0.1:8080/api/v1/recommendations -H "Content-Type: application/json" -d "{\"question\":\"Why might BLER be high on CELL-001?\",\"cellId\":\"CELL-001\"}"
```

## Semantic RAG path (local-ai)

Real embeddings, vector similarity, local LLM, PostgreSQL cell context.

```bash
docker compose up postgres -d
ollama pull nomic-embed-text
ollama pull qwen2.5:7b
mvn spring-boot:run -Dspring-boot.run.profiles=local-ai
```

Then POST the CELL-001 question. Response fields include `retrievalMode=vector`, `contextEvidence`, citations, and latency fields including `contextResolutionLatencyMs`.

Compose alternative:

```bash
docker compose --profile local-ai up postgres ollama api-local --build
```

Host port is configurable (`SNIP_HOST_PORT`, default 8080). Database port: `SNIP_DB_PORT` (default 5432).

## Domain APIs (read-only)

```text
GET /api/v1/sites
GET /api/v1/sites/{siteId}
GET /api/v1/gnbs
GET /api/v1/gnbs/{gnbId}
GET /api/v1/cells
GET /api/v1/cells/{cellId}
GET /api/v1/cells/{cellId}/kpis
GET /api/v1/cells/{cellId}/neighbours
GET /api/v1/cells/{cellId}/context
```

Demo dataset: `SITE-001` / `GNB-001` / `CELL-001` (elevated DL BLER) plus healthier `CELL-002` and comparison `CELL-003`. All operational rows are synthetic (`DEMO_SEED`). Schema is Flyway-managed (`src/main/resources/db/migration/`).

## What this phase does not include

MCP, agents, Kafka, EKS, Kong, ALICE, live network writes, billing, Ionic, Phase 2.

## License

Apache-2.0. See [`LICENSE`](LICENSE).

Sample corpus provenance: [`testdata/PROVENANCE.md`](testdata/PROVENANCE.md).
