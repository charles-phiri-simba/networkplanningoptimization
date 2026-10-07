# SNIP Web

Customer UI for SNIP 1.0: Network Operations, Assurance, investigation, Planning, Optimization, and sandbox rehearsal.

This is **not** production authentication, **not** Phase 19, and **not** a production-write UI.

**Authoritative SNIP 1.0 UI:** `http://127.0.0.1:5173`. The backend static page on `:8080` is a legacy developer UI.

## What it shows

Demo persona → Network map and Assurance queue → cell / site investigation → what-if Planning → Optimize → change plan → sandbox execution → Four-Way State (real network unchanged) → Ask SNIP decision support.

All data comes from existing SNIP application APIs against the synthetic demo / canonical / simulator network. Real Ericsson or Nokia connectivity is **not required**.

Security boundary: this UI must not call `/mcp`, `/api/v1/agent-runs`, `/api/v1/production-changes`, or `/api/v1/production-campaigns`.

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

The SNIP 1.0 demo network is created by Flyway V2 plus `V21__snip_1_0_customer_demo_network.sql` and the `demo` Spring profile bootstrap. Kafka, Azure, and vendor production transports are not required. See `docs/customer/SNIP-1.0-DEMONSTRATION-GUIDE.md`.

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
