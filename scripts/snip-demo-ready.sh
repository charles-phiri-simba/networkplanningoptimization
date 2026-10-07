#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
API="${SNIP_API_TARGET:-http://127.0.0.1:8080}"
API="${API%/}"

fail() { echo "$1"; exit 1; }

get() {
  local url="$1"
  shift || true
  curl -fsS "$@" "$url" || fail "SNIP demo ready failed: GET $url"
}

health="$(get "$API/health")"
echo "$health" | grep -q '"status":"UP"' || fail "SNIP demo ready failed: backend is not UP"

sites="$(get "$API/api/v1/sites")"
for id in SITE-001 SITE-002 SITE-003 SITE-004 SITE-005 SITE-006; do
  echo "$sites" | grep -q "$id" || fail "unexpected demo inventory; run snip-demo-reset (missing $id)"
done

cells="$(get "$API/api/v1/cells")"
for n in $(seq 1 18); do
  id="$(printf 'CELL-%03d' "$n")"
  echo "$cells" | grep -q "$id" || fail "unexpected demo inventory; run snip-demo-reset (missing $id)"
done

cases="$(get "$API/api/v1/cells/CELL-001/assurance")"
echo "$cases" | grep -q 'DEGRADING_RADIO_QUALITY' || fail "SNIP demo ready failed: CELL-001 Assurance case missing"
echo "$cases" | grep -q 'CRITICAL' || fail "SNIP demo ready failed: CELL-001 OPEN CRITICAL Assurance case missing"

TWIN_FILE="$ROOT/.snip-demo/featured-twins.properties"
[ -f "$TWIN_FILE" ] || fail "SNIP demo ready failed: featured twin registry missing (bootstrap may have failed)"
TWIN_ID="$(grep '^CELL-001=' "$TWIN_FILE" | head -n1 | cut -d= -f2 | tr -d '\r')"
[ -n "$TWIN_ID" ] || fail "SNIP demo ready failed: featured twin registry missing CELL-001"
twin="$(get "$API/api/v1/twins/$TWIN_ID")"
echo "$twin" | grep -q '"freshness":"CURRENT"' || fail "SNIP demo ready failed: CELL-001 cell Digital Twin is not CURRENT"

knowledge="$(get "$API/api/v1/integration/sync/sources/ERICSSON_ENM_SIMULATOR/DEFAULT" -H "X-SNIP-VENDOR-IMPORT-PERMISSION: VIEW_SYNCHRONIZATION_STATUS")"
echo "$knowledge" | grep -Eq '"knowledgeConfidence":"(HIGH|MEDIUM)"' || fail "SNIP demo ready failed: knowledge is not recommendable"

APP_YML="$ROOT/snip-npo-app/src/main/resources/application.yml"
DEMO_YML="$ROOT/snip-npo-app/src/main/resources/application-demo.yml"
grep -A2 'change-execution:' "$APP_YML" | grep -q 'enabled: false' || fail "SNIP demo ready failed: committed application.yml must keep change-execution.enabled false"
grep -A3 'production-change:' "$DEMO_YML" | grep -q 'enabled: false' || fail "SNIP demo ready failed: demo profile must keep production-change disabled"
grep -q 'global-execution-enabled: false' "$DEMO_YML" || fail "SNIP demo ready failed: demo profile must keep global-execution-enabled false"
grep -A2 'change-execution:' "$DEMO_YML" | grep -q 'enabled: true' || fail "SNIP demo ready failed: demo profile does not enable sandbox change-execution"

if ! (echo >/dev/tcp/127.0.0.1/5173) >/dev/null 2>&1; then
  fail "SNIP demo ready failed: customer UI is not listening on 127.0.0.1:5173. Authoritative UI is http://127.0.0.1:5173 (not :8080)."
fi

echo "SNIP demo ready"
echo "Authoritative customer UI: http://127.0.0.1:5173"
echo "Legacy static UI on :8080 is not the SNIP 1.0 customer demo."
exit 0
