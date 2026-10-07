#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ "${SNIP_DEMO_RESET:-}" != "YES" ] || [ "${SNIP_DEMO_RESET_CONFIRM:-}" != "snip-demo" ]; then
  echo "SNIP demo reset refused: set SNIP_DEMO_RESET=YES and SNIP_DEMO_RESET_CONFIRM=snip-demo"
  exit 2
fi

if [ ! -f docker-compose.yml ] || [ ! -d snip-npo-app ]; then
  echo "SNIP demo reset refused: run from the SNIP repository root"
  exit 1
fi

if ! grep -Eq '^[[:space:]]+snip-postgres:' docker-compose.yml; then
  echo "SNIP demo reset refused: docker-compose.yml does not define volume key snip-postgres"
  exit 1
fi

VOLUME_NAME="snip-demo_snip-postgres"
if ! docker volume inspect "$VOLUME_NAME" >/tmp/snip-demo-volume.json 2>/dev/null; then
  echo "SNIP demo volume already absent"
  exit 0
fi

PROJECT_LABEL="$(docker volume inspect -f '{{index .Labels "com.docker.compose.project"}}' "$VOLUME_NAME")"
VOLUME_LABEL="$(docker volume inspect -f '{{index .Labels "com.docker.compose.volume"}}' "$VOLUME_NAME")"
if [ "$PROJECT_LABEL" != "snip-demo" ] || [ "$VOLUME_LABEL" != "snip-postgres" ]; then
  echo "SNIP demo reset refused: volume labels do not prove snip-demo/snip-postgres ownership"
  exit 1
fi

PID_DIR="$ROOT/.snip-demo"
for name in api.pid vite.pid; do
  if [ -f "$PID_DIR/$name" ]; then
    pid="$(cat "$PID_DIR/$name" || true)"
    if [ -n "${pid:-}" ]; then
      kill "$pid" 2>/dev/null || true
    fi
    rm -f "$PID_DIR/$name"
  fi
done

if [ "${SNIP_DEMO_RESET_TELEMETRY:-NO}" = "YES" ]; then
  docker compose -p snip-demo --profile telemetry down
fi

docker compose -p snip-demo down
docker volume rm "$VOLUME_NAME"
echo "Reset complete. Run snip-demo-up."
exit 0
