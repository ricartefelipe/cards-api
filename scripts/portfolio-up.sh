#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PORT="${PORT:-8080}"
HEALTH_URL="${HEALTH_URL:-http://localhost:${PORT}/q/health/ready}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-300}"
REALM_JSON="${ROOT}/src/main/resources/quarkus-realm.json"

ensure_env() {
  if [[ ! -f .env ]]; then
    cp .env.example .env
    echo "Criado .env a partir de .env.example"
  fi

  if [[ -f "$REALM_JSON" ]] && command -v python3 >/dev/null 2>&1; then
    local secret
    secret="$(python3 - <<'PY'
import json
from pathlib import Path
realm = Path("src/main/resources/quarkus-realm.json")
doc = json.loads(realm.read_text(encoding="utf-8"))
secret = next(
    (c.get("secret") for c in doc.get("clients", []) if c.get("clientId") == "backend-service"),
    None,
)
print(secret or "")
PY
)"
    if [[ -n "$secret" ]] && grep -Eq '^KEYCLOAK_CLIENT_SECRET=\s*$' .env; then
      sed -i "s|^KEYCLOAK_CLIENT_SECRET=.*|KEYCLOAK_CLIENT_SECRET=${secret}|" .env
      echo "KEYCLOAK_CLIENT_SECRET preenchido a partir de quarkus-realm.json"
    fi
  fi

  # Defaults locais se ainda vazios (alinhados ao docker-compose.yml)
  grep -Eq '^MARIADB_PASSWORD=\s*$' .env && sed -i 's|^MARIADB_PASSWORD=.*|MARIADB_PASSWORD=cards|' .env || true
  grep -Eq '^MARIADB_ROOT_PASSWORD=\s*$' .env && sed -i 's|^MARIADB_ROOT_PASSWORD=.*|MARIADB_ROOT_PASSWORD=root|' .env || true
  grep -Eq '^DB_PASSWORD=\s*$' .env && sed -i 's|^DB_PASSWORD=.*|DB_PASSWORD=cards|' .env || true
  grep -Eq '^KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=\s*$' .env && sed -i 's|^KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=.*|KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD=admin|' .env || true
  grep -Eq '^CARRIER_WEBHOOK_API_KEY=\s*$' .env && sed -i 's|^CARRIER_WEBHOOK_API_KEY=.*|CARRIER_WEBHOOK_API_KEY=carrier-local-key|' .env || true
  grep -Eq '^PROCESSOR_WEBHOOK_API_KEY=\s*$' .env && sed -i 's|^PROCESSOR_WEBHOOK_API_KEY=.*|PROCESSOR_WEBHOOK_API_KEY=processor-local-key|' .env || true
}

ensure_env

set -a
# shellcheck disable=SC1091
source .env
set +a

echo "Subindo stack completa (Keycloak + MariaDB + cards-api)..."
echo "Nota Railway: Keycloak torna o deploy single-service mais difícil; use este Compose para demo local."
docker compose up -d --build

echo "Aguardando health em ${HEALTH_URL}..."
deadline=$((SECONDS + MAX_WAIT_SECONDS))
until curl -fsS "${HEALTH_URL}" >/dev/null 2>&1; do
  if (( SECONDS >= deadline )); then
    echo "Timeout aguardando health (${MAX_WAIT_SECONDS}s): ${HEALTH_URL}" >&2
    docker compose ps >&2 || true
    docker compose logs --tail=80 cards-api >&2 || true
    exit 1
  fi
  sleep 3
done

echo "OK: cards-api saudável em ${HEALTH_URL}"
echo "API:      http://localhost:${PORT}"
echo "OpenAPI:  http://localhost:${PORT}/openapi"
echo "Swagger:  http://localhost:${PORT}/q/swagger-ui"
echo "Keycloak: http://localhost:8180"
