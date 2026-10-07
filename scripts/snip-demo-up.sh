#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
DB_PORT="${SNIP_DB_PORT:-5432}"
API_PORT="${SNIP_HOST_PORT:-8080}"
API_TARGET="${SNIP_API_TARGET:-http://127.0.0.1:${API_PORT}}"

listening() {
  (echo >/dev/tcp/127.0.0.1/"$1") >/dev/null 2>&1
}

if bash "$ROOT/scripts/snip-demo-ready.sh"; then
  echo "SNIP demo already ready"
  echo "Authoritative customer UI: http://127.0.0.1:5173"
  exit 0
fi

if listening "$DB_PORT"; then
  echo "Port $DB_PORT is occupied. Set SNIP_DB_PORT to a free localhost port. Do not bind another process's database."
  exit 1
fi
if listening "$API_PORT"; then
  echo "Port $API_PORT is occupied. Set SNIP_HOST_PORT and SNIP_API_TARGET. Authoritative UI remains http://127.0.0.1:5173."
  exit 1
fi
if listening 5173; then
  echo "Port 5173 is occupied. Stop the other process. Authoritative SNIP 1.0 UI is http://127.0.0.1:5173, not :8080."
  exit 1
fi

docker compose -p snip-demo up postgres -d

PID_DIR="$ROOT/.snip-demo"
mkdir -p "$PID_DIR"
mvn -pl snip-npo-app -am spring-boot:run -Dspring-boot.run.profiles=demo >/tmp/snip-demo-api.log 2>&1 &
echo $! > "$PID_DIR/api.pid"

for _ in $(seq 1 80); do
  if curl -fsS "$API_TARGET/health" | grep -q '"status":"UP"'; then
    break
  fi
  sleep 3
done

(
  cd "$ROOT/snip-web"
  if [ ! -d node_modules ]; then
    npm ci
  fi
  npm run dev
) >/tmp/snip-demo-vite.log 2>&1 &
echo $! > "$PID_DIR/vite.pid"

for _ in $(seq 1 40); do
  if listening 5173; then
    break
  fi
  sleep 2
done

bash "$ROOT/scripts/snip-demo-ready.sh"
echo "Authoritative customer UI: http://127.0.0.1:5173"
echo "Do not use http://127.0.0.1:8080/ as the SNIP 1.0 customer demo."
exit 0
