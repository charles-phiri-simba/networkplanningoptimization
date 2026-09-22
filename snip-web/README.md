# SNIP Web — Product Increment 1A

Read-only browser application for the first SNIP network-intelligence vertical slice.

This is **not** production authentication, **not** Phase 19, and **not** a production-write UI.

## What it shows

Demo persona → application shell → network map → site → cell workspace (configuration, KPI/telemetry, assurance) → AI explanation.

All data comes from existing SNIP application APIs (`/api/v1/...`) against the seeded/canonical/simulator network. Real Ericsson or Nokia connectivity is **not required**.

## Prerequisites

- Node.js 20+ (developed with Node 24)
- npm
- SNIP backend running locally on `http://127.0.0.1:8080` (PostgreSQL + Flyway demo seed)

## Install

```bash
cd snip-web
npm install
```

## Run backend

From the repository root, start the SNIP application as you normally would (for example the `snip-npo-app` Spring Boot process listening on `127.0.0.1:8080`).

The demo network is created by `V2__seed_demo_network.sql` (`SITE-001`, `CELL-001`, and related objects). Kafka, Azure, and vendor production transports are not required.

## Run frontend

```bash
cd snip-web
npm run dev
```

Open the Vite URL (default `http://127.0.0.1:5173`).

### Development proxy

Vite proxies:

- `/api` → `http://127.0.0.1:8080`
- `/health` → `http://127.0.0.1:8080`

The browser therefore calls same-origin `/api/*` paths. This increment does **not** open backend CORS. `/mcp` and the Production Write Gateway are **not** proxied.

## Demo identity

The login screen is labelled **SNIP demo login**. It stores a frontend-only persona in `sessionStorage`:

- `actorId`
- `displayName`
- `role`
- `profile`

There are no passwords and no secrets. This is **not** production authentication and can later be replaced by OIDC without rewriting screens.

Increment 1A read APIs do not require backend authorization headers.

## Build

```bash
cd snip-web
npm run build
```

## Test

```bash
cd snip-web
npm test
```

HTTP is mocked at the `fetch` boundary. These tests do not replace SNIP backend integration tests.

## Security boundary

The frontend must not call:

- `/mcp`
- Production Write Gateway `/execute`
- credential or connector-secret endpoints
- vendor systems directly
- SYSTEM-only campaign transitions

Production network mutation remains disabled in the backend.

## Known product limits

- No region/sector/azimuth/GeoJSON backend
- Telemetry history is bounded by the backend, not a long-term historian
- AI output is decision support, not an authorised change
