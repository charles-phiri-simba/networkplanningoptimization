#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
DB_PORT="${SNIP_DB_PORT:-5432}"
API_PORT="${SNIP_HOST_PORT:-8080}"
API_TARGET="${SNIP_API_TARGET:-http://127.0.0.1:${API_PORT}}"
API_TARGET="${API_TARGET%/}"
export SNIP_DB_PORT="$DB_PORT"
export SNIP_HOST_PORT="$API_PORT"
export SNIP_API_TARGET="$API_TARGET"
export SERVER_PORT="$API_PORT"
export SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:${DB_PORT}/snip"
export SNIP_KAFKA_ENABLED=false

PID_DIR="$ROOT/.snip-demo"
LOG_DIR="$PID_DIR/logs"
mkdir -p "$LOG_DIR"
RUN_STAMP="$(date +%Y%m%d-%H%M%S)"
API_LOG="$LOG_DIR/api-${RUN_STAMP}.log"
VITE_LOG="$LOG_DIR/vite-${RUN_STAMP}.log"
KPI_FILE="$ROOT/testdata/kpis.json"

listening() {
  (echo >/dev/tcp/127.0.0.1/"$1") >/dev/null 2>&1
}

if curl -fsS "$API_TARGET/health" 2>/dev/null | grep -q '"status":"UP"' && listening 5173; then
  echo "SNIP demo startup category: reuse-existing health=$API_TARGET/health ui=http://127.0.0.1:5173"
  if bash "$ROOT/scripts/snip-demo-ready.sh"; then
    echo "SNIP demo already ready"
    echo "Authoritative customer UI: http://127.0.0.1:5173"
    exit 0
  fi
fi

own_postgres() {
  docker compose -p snip-demo ps --status running --format '{{.Name}}' 2>/dev/null | grep -q postgres
}

if listening "$DB_PORT"; then
  if ! own_postgres; then
    echo "Port $DB_PORT is occupied. Set SNIP_DB_PORT to a free localhost port. Do not bind another process's database."
    echo "Example: SNIP_DB_PORT=15432 SNIP_HOST_PORT=18080 SNIP_API_TARGET=http://127.0.0.1:18080"
    exit 1
  fi
  echo "Reusing snip-demo PostgreSQL already listening on 127.0.0.1:$DB_PORT"
else
  docker compose -p snip-demo up postgres -d
fi

if listening "$API_PORT"; then
  if ! curl -fsS "$API_TARGET/health" | grep -q '"status":"UP"'; then
    echo "Port $API_PORT is occupied. Set SNIP_HOST_PORT and SNIP_API_TARGET. Authoritative UI remains http://127.0.0.1:5173."
    exit 1
  fi
fi

if listening 5173; then
  echo "Port 5173 is occupied. Stop the other process. Authoritative SNIP 1.0 UI is http://127.0.0.1:5173, not :8080."
  exit 1
fi

if [ ! -f "$KPI_FILE" ]; then
  echo "SNIP demo KPI file is missing: $KPI_FILE"
  echo "Expected repository-root testdata/kpis.json relative to the demo JVM working directory. Do not copy testdata into snip-npo-app."
  exit 1
fi
echo "SNIP demo KPI file: $KPI_FILE"
echo "SNIP demo current-run logs: $API_LOG"

echo "SNIP demo startup category: sibling-install (does not repackage the NPO boot jar)"
mvn -pl production-change-protocol,production-write-gateway -am -DskipTests install >"$LOG_DIR/deps.log" 2>&1 || {
  echo "SNIP demo module install failed. Log: $LOG_DIR/deps.log"
  tail -n 80 "$LOG_DIR/deps.log" | sed -E 's/[Pp]assword=[^ ]+/password=***/g'
  exit 1
}
echo "SNIP demo startup category: backend-launch command=spring-boot:run profile=demo server.port=$API_PORT workingDirectory=$ROOT"
nohup mvn -pl snip-npo-app spring-boot:run -Dspring-boot.run.workingDirectory="$ROOT" -Dspring-boot.run.profiles=demo -Dspring-boot.run.arguments="--server.port=${API_PORT} --spring.datasource.url=${SPRING_DATASOURCE_URL}" >"$API_LOG" 2>&1 &
echo $! > "$PID_DIR/api.pid"
echo "SNIP demo process status: api pid=$(cat "$PID_DIR/api.pid") log=$API_LOG"

api_up=0
for _ in $(seq 1 100); do
  if ! kill -0 "$(cat "$PID_DIR/api.pid")" 2>/dev/null; then
    echo "SNIP demo process status: api terminated before health. Log file location: $API_LOG"
    echo "SNIP demo API process exited before health. Log: $API_LOG"
    tail -n 80 "$API_LOG" | sed -E 's/[Pp]assword=[^ ]+/password=***/g'
    if grep -Eq 'checksum mismatch|FlywayValidateException|Migrations have failed validation' "$API_LOG"; then
      echo "SNIP demo backend health failure: Flyway validation. After dual-confirm snip-demo-reset, run snip-demo-up. Do not edit V1-V20 or run SQL."
    fi
    rm -f "$PID_DIR/api.pid"
    exit 1
  fi
  if curl -fsS "$API_TARGET/health" | grep -q '"status":"UP"'; then
    api_up=1
    break
  fi
  sleep 3
done
if [ "$api_up" -ne 1 ]; then
  echo "SNIP demo backend health failure: $API_TARGET/health not UP. Log file location: $API_LOG"
  tail -n 80 "$API_LOG" | sed -E 's/[Pp]assword=[^ ]+/password=***/g'
  exit 1
fi

echo "SNIP demo startup category: frontend-launch command=npm-run-dev port=5173 proxy=$API_TARGET"
(
  cd "$ROOT/snip-web"
  if [ ! -d node_modules ]; then
    npm ci
  fi
  npm run dev
) >"$VITE_LOG" 2>&1 &
echo $! > "$PID_DIR/vite.pid"
echo "SNIP demo process status: vite pid=$(cat "$PID_DIR/vite.pid") log=$VITE_LOG"

vite_up=0
for _ in $(seq 1 40); do
  if ! kill -0 "$(cat "$PID_DIR/vite.pid")" 2>/dev/null; then
    echo "SNIP demo Vite process exited before port 5173. Log file location: $VITE_LOG"
    tail -n 80 "$VITE_LOG" | sed -E 's/[Pp]assword=[^ ]+/password=***/g'
    rm -f "$PID_DIR/vite.pid"
    exit 1
  fi
  if listening 5173; then
    vite_up=1
    break
  fi
  sleep 2
done
if [ "$vite_up" -ne 1 ]; then
  echo "SNIP demo Vite did not listen on 127.0.0.1:5173. Log file location: $VITE_LOG"
  tail -n 80 "$VITE_LOG" | sed -E 's/[Pp]assword=[^ ]+/password=***/g'
  exit 1
fi

bash "$ROOT/scripts/snip-demo-ready.sh"
echo "Authoritative customer UI: http://127.0.0.1:5173"
echo "Do not use http://127.0.0.1:8080/ as the SNIP 1.0 customer demo."
exit 0
