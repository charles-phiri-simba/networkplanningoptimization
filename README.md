# Network Planning and Optimisation

First **SNIP** domain application: a local, **read-only** 5G planning copilot.

It answers an engineering question using cited sample planning notes and optional synthetic KPI context. It does **not** change the live network.

This repository is not the full Simba Network Intelligence Platform. Target-state product requirements are in [`docs/requirements/product-requirements.md`](docs/requirements/product-requirements.md). Phase 1A.1 bounds are in [`docs/implementation/SNIP-PHASE-1A.1-SEMANTIC-RAG-VALIDATION.md`](docs/implementation/SNIP-PHASE-1A.1-SEMANTIC-RAG-VALIDATION.md).

## Prerequisites

- Java 17
- Maven 3.9+
- Optional: Docker / Docker Compose
- Semantic path only: [Ollama](https://ollama.com) with models `nomic-embed-text` and `qwen2.5:7b`

No AWS account is required.

## Deterministic path (CI / default)

```bash
mvn test
mvn spring-boot:run
```

- Health: `GET http://127.0.0.1:8080/health`
- UI: [http://127.0.0.1:8080/](http://127.0.0.1:8080/)
- Recommend: `POST http://127.0.0.1:8080/api/v1/recommendations`

Uses **lexical** retrieval and the **stub** generator. No model download.

Canonical question:

```bash
curl -s http://127.0.0.1:8080/api/v1/recommendations -H "Content-Type: application/json" -d "{\"question\":\"What should I check if BLER is high on a mid-band cell?\",\"contextId\":\"cell-midband-001\"}"
```

## Semantic RAG path (Phase 1A.1)

This is the acceptance path: real embeddings, vector similarity, local LLM.

```bash
ollama pull nomic-embed-text
ollama pull qwen2.5:7b
mvn spring-boot:run -Dspring-boot.run.profiles=local-ai
```

Then POST the same canonical question. Response fields include `retrievalMode=vector`, citations from retrieved chunks, and `retrievalLatencyMs` / `generationLatencyMs` / `totalLatencyMs`.

Ollama default URL: `http://localhost:11434`. Override with `OLLAMA_BASE_URL`.

## Docker Compose

Host port is configurable (`SNIP_HOST_PORT`, default 8080) because 8080 is often taken:

```bash
docker compose up --build
# or
set SNIP_HOST_PORT=18080
docker compose up --build
```

Local-AI Compose (Ollama in Docker; pull models into that instance first):

```bash
docker compose --profile local-ai up ollama api-local --build
```

Do not start `api` and `api-local` on the same host port at once.

## What this phase does not include

MCP, agents, Kafka, EKS, Kong, ALICE, live network writes, billing, Ionic, Phase 1B platform foundation.

## License

Apache-2.0. See [`LICENSE`](LICENSE).

Sample corpus provenance: [`testdata/PROVENANCE.md`](testdata/PROVENANCE.md).
