# SNIP 1.0 — Demonstration guide

For a technical presenter who did not build SNIP. Duration **30–45 minutes**.

**Authoritative customer UI:** [http://127.0.0.1:5173](http://127.0.0.1:5173)

Do **not** use `http://127.0.0.1:8080/` as the customer demo. That static page is a legacy developer UI.

## Prerequisites

- Java 17, Maven 3.9+, Node 20+, Docker (PostgreSQL 16)
- **No** Azure CLI, **no** Ollama, **no** Kafka, **no** Go simulator, **no** production vendor credentials

Supported local environment: Windows (PowerShell) or POSIX shell, localhost only.

The demo launcher selects Java 17 for the SNIP process only. It does not change global `JAVA_HOME`. If several JDKs are installed, set `SNIP_JAVA17_HOME` to a JDK 17 directory.

## Environment overrides

When another local application already uses 5432 or 8080, do **not** stop it. Use:

| Variable | Maps to |
|----------|---------|
| `SNIP_DB_PORT` | Docker PostgreSQL **host** port (`127.0.0.1:${SNIP_DB_PORT}→5432` in the container) and Spring `SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:${SNIP_DB_PORT}/snip` |
| `SNIP_HOST_PORT` | Spring `SERVER_PORT` / `server.port` |
| `SNIP_API_TARGET` | Vite proxy target and readiness health URL. Default: `http://127.0.0.1:${SNIP_HOST_PORT}` |

`SNIP_HOST_PORT` is the host API port. It is **not** inferred from Docker. The launcher always sets Spring `SERVER_PORT` to the same value.

Example when WAODN or another stack holds 5432 and 8080:

```powershell
$env:SNIP_DB_PORT='15432'
$env:SNIP_HOST_PORT='18080'
$env:SNIP_API_TARGET='http://127.0.0.1:18080'
.\scripts\snip-demo-up.ps1
```

```bash
SNIP_DB_PORT=15432 SNIP_HOST_PORT=18080 SNIP_API_TARGET=http://127.0.0.1:18080 ./scripts/snip-demo-up.sh
```

Frontend remains `http://127.0.0.1:5173`. Do not bind another process’s database.

Startup logs (no secrets): `.snip-demo/logs/api.log` and `.snip-demo/logs/vite.log`.

`.snip-demo/featured-twins.properties` is a **hint** written by demo bootstrap. **CURRENT** Cell Digital Twin status is proven only by `GET /api/v1/twins/{id}` against the running backend.

## Bootstrap

From the repository root:

```powershell
.\scripts\snip-demo-up.ps1
```

```bash
./scripts/snip-demo-up.sh
```

This starts:

1. PostgreSQL via `docker compose -p snip-demo`
2. SNIP API with Spring profile `demo`
3. Vite customer UI on port 5173
4. `snip-demo-ready`

Do not start Kafka, Ollama, the Go simulator, or the production write gateway.

## Readiness

```powershell
.\scripts\snip-demo-ready.ps1
```

If you overrode ports, keep the same `SNIP_HOST_PORT` / `SNIP_API_TARGET` in the shell.

Health: `GET ${SNIP_API_TARGET}/health` → `{"status":"UP"}` (default `http://127.0.0.1:8080/health`).

Ready does **not** print secrets.

## Reset

Local demo reset only. Dual confirmation is mandatory and **case-sensitive**:

```powershell
$env:SNIP_DEMO_RESET='YES'
$env:SNIP_DEMO_RESET_CONFIRM='snip-demo'
.\scripts\snip-demo-reset.ps1
```

```bash
SNIP_DEMO_RESET=YES SNIP_DEMO_RESET_CONFIRM=snip-demo ./scripts/snip-demo-reset.sh
```

`yes` or any other spelling is rejected. Reset removes only the proven Compose volume `snip-demo_snip-postgres` after ownership labels match. If that volume is already absent, reset exits 0. Then run `snip-demo-up` again.

## Ports

| Service | Default | Role |
|---------|---------|------|
| Postgres | 127.0.0.1:5432 | Demo database (override with `SNIP_DB_PORT`) |
| API | 127.0.0.1:8080 | Backend (legacy static UI — not the demo; override with `SNIP_HOST_PORT`) |
| Vite | 127.0.0.1:5173 | **Authoritative SNIP 1.0 customer UI** |

## Demo identity

Select **Priya Naidoo / RF Optimisation Engineer**. This is frontend-only demo identity, **not** production IAM. Governance calls send demo permission headers from the browser. The same demo actor can review then authorize. That is **demo authorization behavior**, not segregation of duties.

## 30–45 minute script

| Min | Page | Action | Message | Caveat |
|-----|------|--------|---------|--------|
| 0–2 | `/login` | Priya Naidoo | Controlled demo identity | Not SSO |
| 2–6 | `/network` | Map + queue | Operations, not a health score | Synthetic cluster; extra SITE-SIM-001 is allowed |
| 6–12 | Assurance case | Open CRITICAL CELL-001 | Prioritized finding | Rule-based; stub narrative |
| 12–16 | Cell + site | CELL-001, CELL-002, SITE-002/CELL-003 | Contrast, not interference | Synthetic KPIs |
| 16–22 | Planning | CELL-001+CELL-002; Evaluate | Independent cell-local synthetic preview | LOW confidence; CURRENT cell Digital Twin |
| 22–28 | Optimize | Generate proposal | Deterministic recommendation | Not the LLM |
| 28–36 | Change plan | Approve → review → authorize → readiness | Humans stay in control | READY FOR SANDBOX ADMISSION |
| 36–43 | Sandbox | Request → review → authorize → execute → verify | Rehearsal ≠ production | Simulator only |
| 43–45 | Four-way | Point at Real network **Unchanged** | Next: read-only PoC | No write promise |

## Presenter recovery

Do not use SQL or curl to manufacture the story. Use reset + bootstrap, or the UI Synchronize action.

| Symptom | Recovery |
|---------|----------|
| Backend unavailable | Check `.snip-demo/logs/api.log`. Confirm `SNIP_HOST_PORT` / `SNIP_API_TARGET`. Re-run `snip-demo-up`. Do not use the legacy `:8080` page. |
| Frontend unavailable | Confirm port 5173. Check `.snip-demo/logs/vite.log`. Re-run `snip-demo-up`. Authoritative UI is `:5173`. |
| Port collision (5432/8080) | Leave the other application running. Set `SNIP_DB_PORT`, `SNIP_HOST_PORT`, and `SNIP_API_TARGET` as in Environment overrides. |
| Empty Assurance queue | Reset + bootstrap. Do not SQL-insert cases. |
| Knowledge UNKNOWN / LOW | Bootstrap knowledge import failed. Reset + up. Do not SQL-update knowledge. |
| Twin STALE | Use Synchronize on the Planning scenario. Featured cells are synced at demo startup. |
| Planning evaluation failed | Confirm CURRENT Cell Digital Twins, then Evaluate again. Comparison failures appear as an on-page alert. |
| Optimize yields EVALUATED / not RECOMMENDED | Knowledge or eligibility failed. Reset + up. Do not SQL-insert a proposal. |
| Sandbox unavailable | API must run with profile `demo` (`snip-demo-up`). |
| Reset rejected | Dual confirm must be exactly `SNIP_DEMO_RESET=YES` and `SNIP_DEMO_RESET_CONFIRM=snip-demo` (lowercase `yes` is rejected). Run from the SNIP repository root. Docker must be available. Volume labels must be `com.docker.compose.project=snip-demo` and `com.docker.compose.volume=snip-postgres`. |
| Flyway validation / checksum mismatch | Demo database was created from a different source tree. After dual-confirm `snip-demo-reset`, run `snip-demo-up`. Do not edit V1–V20 or run SQL. Logs: `.snip-demo/logs/api.log`. |
| Wrong UI | Close `:8080`. Open `:5173`. |

## STOP conditions

Stop the demo if any of the following occurs:

- A production-change or production-campaign call
- `/mcp` or `/api/v1/agent-runs` used from the customer UI
- Real network shown as changed
- Missing synthetic / LOW / sandbox labels
- Presenter uses SQL or curl to manufacture the story

## Repeatability

```text
RESET → BOOTSTRAP → DEMO → RESET → BOOTSTRAP → DEMO
```

No manual SQL, curl, or source edits.
