"""Probe Key Vault as the SNIP workload identity. Never print secret values."""
from __future__ import annotations

import json
import os
import urllib.error
import urllib.parse
import urllib.request

VAULT = os.environ.get("SNIP_AZURE_KEY_VAULT_URI", "https://kvsnipp10e59l.vault.azure.net/").rstrip("/")
TENANT = os.environ["AZURE_TENANT_ID"]
CLIENT = os.environ["AZURE_CLIENT_ID"]
TOKEN_FILE = os.environ["AZURE_FEDERATED_TOKEN_FILE"]
API = "7.4"
CONFIGURED = "snip-int-ericsson-inventory-reader"
UNRELATED = "snip-unrelated-scope"


def access_token() -> str:
    assertion = open(TOKEN_FILE, encoding="utf-8").read().strip()
    body = urllib.parse.urlencode(
        {
            "client_id": CLIENT,
            "grant_type": "client_credentials",
            "scope": "https://vault.azure.net/.default",
            "client_assertion_type": "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
            "client_assertion": assertion,
        }
    ).encode("utf-8")
    req = urllib.request.Request(
        f"https://login.microsoftonline.com/{TENANT}/oauth2/v2.0/token",
        data=body,
        method="POST",
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    with urllib.request.urlopen(req, timeout=20) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    token = payload.get("access_token")
    if not token:
        raise SystemExit("token-endpoint-missing-access-token")
    return token


def call(token: str, method: str, secret_name: str, body: bytes | None = None) -> tuple[int, dict | None]:
    url = f"{VAULT}/secrets/{secret_name}?api-version={API}"
    req = urllib.request.Request(
        url,
        data=body,
        method=method,
        headers={
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            raw = resp.read()
            parsed = json.loads(raw.decode("utf-8")) if raw else None
            return resp.status, parsed
    except urllib.error.HTTPError as err:
        err.read()
        return err.code, None


def version_of(parsed: dict | None) -> str:
    if not parsed or "id" not in parsed:
        return ""
    return parsed["id"].rstrip("/").split("/")[-1]


def main() -> None:
    token = access_token()
    status, parsed = call(token, "GET", CONFIGURED)
    version = version_of(parsed) if status == 200 else ""
    print(f"GET {CONFIGURED} HTTP={status} version={version}")
    if "value" in json.dumps(parsed or {}):
        # parsed is not printed; this is a local sanity that we do not dump it.
        pass
    status, _ = call(token, "GET", UNRELATED)
    print(f"GET {UNRELATED} HTTP={status}")
    status, _ = call(token, "PUT", "snip-probe-set-denied", b'{"value":"redacted-probe"}')
    print(f"SET snip-probe-set-denied HTTP={status}")
    status, _ = call(token, "DELETE", UNRELATED)
    print(f"DELETE {UNRELATED} HTTP={status}")


if __name__ == "__main__":
    main()
