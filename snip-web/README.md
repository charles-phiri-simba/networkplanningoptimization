# SNIP Web — Product Increment 1A / 1B

Read-only browser application for the first SNIP network-intelligence vertical slice.

This is **not** production authentication, **not** Phase 19, and **not** a production-write UI.

## What it shows

Demo persona → application shell → network map → site → cell workspace (configuration, KPI/telemetry, neighbours, assurance) → AI explanation.

All data comes from existing SNIP application APIs (`/api/v1/...`) against the seeded/canonical/simulator network. Real Ericsson or Nokia connectivity is **not required**.

## Prerequisites

- Node.js 20+ (developed with Node 24)
- npm
- SNIP backend running locally (PostgreSQL + Flyway demo seed)

## Install

```bash
cd snip-web
npm ci
```

## Run backend

From the repository root, start `snip-npo-app` with its existing demo/local configuration.

If port 8080 is already in use, start the backend on another localhost port at runtime only, for example:

```bash
java -jar snip-npo-app/target/network-planning-optimisation-0.1.0-SNAPSHOT.jar --server.port=8081 --server.address=127.0.0.1
```

The demo network is created by `V2__seed_demo_network.sql` (`SITE-001`, `CELL-001`, and related objects). Kafka, Azure, and vendor production transports are not required.

## Run frontend

```bash
cd snip-web
npm run dev
```

Open `http://127.0.0.1:5173`.

### Development proxy

Vite proxies only:

- `/api`
- `/health`

Default target: `http://127.0.0.1:8080`.

To point the development server at a backend on 8081 without changing source:

```bash
# Windows PowerShell
$env:SNIP_API_TARGET='http://127.0.0.1:8081'
npm run dev
```

```bash
# Unix
SNIP_API_TARGET=http://127.0.0.1:8081 npm run dev
```

`SNIP_API_TARGET` is read by the Vite development server only. It is not a production client setting and must not contain secrets. See `.env.example`. Do not commit `.env.local`.

The Vite proxy is a local convenience, not a security boundary. `/mcp`, the Production Write Gateway, and vendor endpoints are not proxied.

## Demo identity

The login screen is labelled **SNIP demo login**. It stores a frontend-only persona in `sessionStorage`. There are no passwords and no secrets. This is **not** production authentication.

## Test

```bash
cd snip-web
npm test
```

## Build

```bash
cd snip-web
npm run build
```

## Security boundary

The frontend must not call:

- `/mcp`
- Production Write Gateway `/execute`
- credential or connector-secret endpoints
- vendor systems directly
- SYSTEM-only campaign transitions

Production network mutation remains **disabled** in the backend. This UI does not authorise live-network execution.

## Product Increment 2

Cell workspace → Propose txPower optimization → proposal → synthetic simulation evidence → approve/reject → change plan → engineering review → authorize → readiness → sandbox execution on `snip-simulator`.

Ask SNIP remains advisory. The proposed txPower is selected by the deterministic Phase 13 pipeline.

The committed backend default is:

```text
snip.change-execution.enabled=false
```

Do not change that committed default. For a local sandbox demonstration only, pass a runtime override:

```bash
java -jar snip-npo-app/target/network-planning-optimisation-0.1.0-SNAPSHOT.jar --snip.change-execution.enabled=true
```

Do not enable `production-change`. Do not contact vendor systems. Phase 15 mutates simulator state only.

## Known product limits

- No region/sector/azimuth/GeoJSON backend
- Telemetry history is bounded by the backend, not a long-term historian
- AI output is decision support, not an authorised change
- Increment 2 sandbox execution requires the local runtime override above
