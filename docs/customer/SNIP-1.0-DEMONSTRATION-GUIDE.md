# SNIP 1.0 — Demonstration guide

For a technical presenter who did not build SNIP. Duration **30–45 minutes**.

**Authoritative customer UI:** [http://127.0.0.1:5173](http://127.0.0.1:5173)

Do **not** use `http://127.0.0.1:8080/` as the customer demo. That static page is a legacy developer UI.

## Prerequisites

- Java 17, Maven 3.9+, Node 20+, Docker (PostgreSQL 16)
- **No** Azure CLI, **no** Ollama, **no** Kafka, **no** Go simulator, **no** production vendor credentials

Supported local environment: Windows (PowerShell) or POSIX shell, localhost only.

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

If port `5432` or `8080` is occupied, the script fails. Set `SNIP_DB_PORT`, `SNIP_HOST_PORT`, and `SNIP_API_TARGET`. Do not bind another process’s database.

## Readiness

```powershell
.\scripts\snip-demo-ready.ps1
```

Health: `GET http://127.0.0.1:8080/health` → `{"status":"UP"}`.

Ready does **not** print secrets.

## Reset

Local demo reset only. Dual confirmation is mandatory:

```powershell
$env:SNIP_DEMO_RESET='YES'
$env:SNIP_DEMO_RESET_CONFIRM='snip-demo'
.\scripts\snip-demo-reset.ps1
```

```bash
SNIP_DEMO_RESET=YES SNIP_DEMO_RESET_CONFIRM=snip-demo ./scripts/snip-demo-reset.sh
```

Reset removes only the proven Compose volume `snip-demo_snip-postgres` after ownership labels match. Then run `snip-demo-up` again.

## Ports

| Service | Default | Role |
|---------|---------|------|
| Postgres | 127.0.0.1:5432 | Demo database |
| API | 127.0.0.1:8080 | Backend (legacy static UI — not the demo) |
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

| Symptom | Recovery |
|---------|----------|
| Empty Assurance queue | Reset + bootstrap. Do not SQL-insert cases. |
| Optimize yields EVALUATED / NETWORK_KNOWLEDGE_UNKNOWN | Bootstrap knowledge import failed. Reset + up. |
| Twin STALE | Use Synchronize on the scenario. Featured cells are synced at demo startup. |
| Sandbox disabled | API must run with profile `demo`. |
| Wrong UI | Close :8080. Open :5173. |
| Occupied ports | Set SNIP_DB_PORT / SNIP_HOST_PORT / SNIP_API_TARGET. |

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
